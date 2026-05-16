package io.rampage.console.orchestrator;

import io.rampage.console.config.PathResolver;
import io.rampage.console.logs.LogBroadcaster;
import io.rampage.console.logs.LogLine;
import io.rampage.console.results.RunResultIngestor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Owns the single-slot + FIFO queue model for Gatling runs. Spawns one Gatling
 * JVM at a time via {@link ProcessLauncher}, taps stdout into the
 * {@link LogBroadcaster}, and emits status transitions via the
 * {@link RunStatusBroadcaster}. Concurrent execution is intentionally disallowed
 * to keep load measurements clean.
 */
@ApplicationScoped
public class RunOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(RunOrchestrator.class);

    private final ConcurrentLinkedDeque<RunRecord> queue = new ConcurrentLinkedDeque<>();
    private final AtomicReference<RunRecord> current = new AtomicReference<>();
    private final Map<String, RunRecord> known = new LinkedHashMap<>();
    private final Object lock = new Object();

    @Inject
    LogBroadcaster logBroadcaster;

    @Inject
    RunStatusBroadcaster statusBroadcaster;

    @Inject
    RunResultIngestor resultIngestor;

    @ConfigProperty(name = "rampage.console.repo-root")
    java.util.Optional<String> repoRootRaw;

    String repoRoot;

    @ConfigProperty(name = "rampage.console.gradle-command", defaultValue = "./gradlew")
    String gradleCommand;

    @ConfigProperty(name = "rampage.console.gatling-task", defaultValue = "gatlingRun")
    String gatlingTask;

    @ConfigProperty(name = "rampage.console.kill-timeout-seconds", defaultValue = "5")
    int killTimeoutSeconds;

    @ConfigProperty(name = "rampage.console.carbon-port", defaultValue = "2003")
    int carbonPort;

    @ConfigProperty(name = "rampage.console.carbon-host", defaultValue = "localhost")
    String carbonHost;

    @ConfigProperty(name = "rampage.console.live-metrics", defaultValue = "true")
    boolean liveMetricsEnabled;

    private ProcessLauncher processLauncher = new DefaultProcessLauncher();
    private ExecutorService dispatcher;
    private ExecutorService stdoutPumps;

    public RunOrchestrator() {
        // CDI no-arg constructor.
    }

    /** Test constructor — wires collaborators directly without CDI. */
    RunOrchestrator(LogBroadcaster logBroadcaster,
                    RunStatusBroadcaster statusBroadcaster,
                    String repoRoot,
                    String gradleCommand,
                    String gatlingTask,
                    int killTimeoutSeconds) {
        this.logBroadcaster = logBroadcaster;
        this.statusBroadcaster = statusBroadcaster;
        this.repoRoot = repoRoot;
        this.gradleCommand = gradleCommand;
        this.gatlingTask = gatlingTask;
        this.killTimeoutSeconds = killTimeoutSeconds;
        this.carbonPort = 2003;
        this.carbonHost = "localhost";
        this.liveMetricsEnabled = false;
        init();
    }

    @PostConstruct
    void init() {
        // Resolve to the actual repo root (the directory holding settings.gradle.kts)
        // so spawned `./gradlew` resolves correctly.
        if (repoRoot == null || repoRoot.isBlank()) {
            String start = repoRootRaw != null && repoRootRaw.isPresent() && !repoRootRaw.get().isBlank()
                    ? repoRootRaw.get() : System.getProperty("user.dir");
            repoRoot = PathResolver.resolveRepoRoot(start).toString();
        }

        dispatcher = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "rampage-orchestrator");
            t.setDaemon(true);
            return t;
        });
        stdoutPumps = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "rampage-stdout-pump");
            t.setDaemon(true);
            return t;
        });
    }

    @PreDestroy
    void shutdown() {
        RunRecord active = current.get();
        if (active != null) {
            killActive();
        }
        if (dispatcher != null) {
            dispatcher.shutdownNow();
        }
        if (stdoutPumps != null) {
            stdoutPumps.shutdownNow();
        }
    }

    /** Test seam — replace the launcher used to spawn Gatling. */
    public void setProcessLauncher(ProcessLauncher launcher) {
        this.processLauncher = launcher;
    }

    /** Test seam — replace the ingestor that receives finished runs. */
    public void setResultIngestor(RunResultIngestor ingestor) {
        this.resultIngestor = ingestor;
    }

    public RunRecord enqueue(String envPath, String runPath) {
        QueuedRun queued = QueuedRun.create(envPath, runPath);
        RunRecord record = new RunRecord(queued);
        synchronized (lock) {
            queue.addLast(record);
            known.put(record.id(), record);
        }
        statusBroadcaster.publish(RunStatusEvent.of(record));
        scheduleNextIfIdle();
        return record;
    }

    public Optional<RunRecord> kill(String runId) {
        RunRecord active = current.get();
        if (active != null && active.id().equals(runId)) {
            killActive();
            return Optional.of(active);
        }
        synchronized (lock) {
            for (RunRecord r : queue) {
                if (r.id().equals(runId)) {
                    queue.remove(r);
                    if (r.transitionTo(RunStatus.KILLED)) {
                        statusBroadcaster.publish(RunStatusEvent.of(r));
                    }
                    return Optional.of(r);
                }
            }
        }
        return Optional.empty();
    }

    public List<RunRecord> queueSnapshot() {
        synchronized (lock) {
            return new ArrayList<>(queue);
        }
    }

    public Optional<RunRecord> currentRun() {
        return Optional.ofNullable(current.get());
    }

    public OrchestratorView snapshot() {
        return new OrchestratorView(current.get(), queueSnapshot());
    }

    public List<RunRecord> recentRuns(int limit) {
        synchronized (lock) {
            List<RunRecord> all = new ArrayList<>(known.values());
            int from = Math.max(0, all.size() - limit);
            List<RunRecord> tail = new ArrayList<>(all.subList(from, all.size()));
            java.util.Collections.reverse(tail);
            return tail;
        }
    }

    public Optional<RunRecord> get(String runId) {
        synchronized (lock) {
            return Optional.ofNullable(known.get(runId));
        }
    }

    private void scheduleNextIfIdle() {
        if (current.get() != null) {
            return;
        }
        RunRecord next;
        synchronized (lock) {
            if (current.get() != null) {
                return;
            }
            next = queue.pollFirst();
            if (next == null) {
                return;
            }
            current.set(next);
        }
        dispatcher.submit(() -> runOne(next));
    }

    private void runOne(RunRecord record) {
        if (!record.transitionTo(RunStatus.RUNNING)) {
            // Was killed before it ever started.
            current.set(null);
            scheduleNextIfIdle();
            return;
        }

        List<String> command = buildCommand(record.queued());
        Path workingDir = Paths.get(repoRoot);

        Process process;
        try {
            process = processLauncher.launch(command, workingDir);
        } catch (IOException e) {
            log.error("Failed to launch Gatling for run {}", record.id(), e);
            logBroadcaster.publish(LogLine.of(record.id(),
                    "FAILED to launch: " + e.getMessage()));
            record.markFinished(-1);
            record.transitionTo(RunStatus.FAILED);
            statusBroadcaster.publish(RunStatusEvent.of(record));
            // Launch failures are intentionally not ingested into the results
            // store: the run never started, produced no Gatling report, and
            // startedAt is null — there is no result data to record.
            current.set(null);
            scheduleNextIfIdle();
            return;
        }

        record.markStarted(process);
        statusBroadcaster.publish(RunStatusEvent.of(record));
        logBroadcaster.publish(LogLine.of(record.id(),
                "$ " + String.join(" ", command)));

        stdoutPumps.submit(() -> pumpStdout(record, process));

        int exit;
        try {
            exit = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            exit = -1;
        }
        record.markFinished(exit);
        RunStatus terminal = switch (record.status()) {
            case KILLED -> RunStatus.KILLED;
            default -> exit == 0 ? RunStatus.COMPLETED : RunStatus.FAILED;
        };
        record.transitionTo(terminal);
        logBroadcaster.publish(LogLine.of(record.id(),
                "[exit=" + exit + " status=" + terminal + "]"));
        statusBroadcaster.publish(RunStatusEvent.of(record));
        ingest(record);

        current.set(null);
        scheduleNextIfIdle();
    }

    /** Hand a finished run to the results store. Never lets ingestion break the queue. */
    private void ingest(RunRecord record) {
        RunResultIngestor ingestor = this.resultIngestor;
        if (ingestor == null) {
            return;
        }
        try {
            ingestor.ingestCompleted(record);
        } catch (Exception e) {
            log.warn("Result ingestion failed for run {}: {}", record.id(), e.getMessage(), e);
        }
    }

    private void pumpStdout(RunRecord record, Process process) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logBroadcaster.publish(LogLine.of(record.id(), line));
            }
        } catch (IOException e) {
            log.debug("stdout pump for run {} terminated: {}", record.id(), e.getMessage());
        }
    }

    private void killActive() {
        RunRecord active = current.get();
        if (active == null) {
            return;
        }
        Process p = active.process();
        if (p == null) {
            return;
        }
        active.transitionTo(RunStatus.KILLED);
        p.destroy();
        try {
            if (!p.waitFor(killTimeoutSeconds, TimeUnit.SECONDS)) {
                p.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            p.destroyForcibly();
        }
    }

    /**
     * Builds the Gradle command line to run one Gatling simulation. Forwards
     * the env/run YAML paths via the {@code -Dloadtest.*} system properties the
     * existing engine already understands. When live metrics are enabled, also
     * passes {@code -Drampage.console.extra-writer=graphite} (HOCON optional
     * substitution in {@code gatling.conf} appends it to the writers list) and
     * pins the Carbon endpoint at the in-process receiver.
     */
    List<String> buildCommand(QueuedRun queued) {
        Set<String> args = new LinkedHashSet<>();
        args.add(gradleCommand);
        args.add(gatlingTask);
        args.add("-Dloadtest.env=" + queued.envPath());
        args.add("-Dloadtest.run=" + queued.runPath());
        if (liveMetricsEnabled) {
            args.add("-Drampage.console.extra-writer=graphite");
            args.add("-Drampage.console.carbon-host=" + carbonHost);
            args.add("-Drampage.console.carbon-port=" + carbonPort);
        }
        return new ArrayList<>(args);
    }
}
