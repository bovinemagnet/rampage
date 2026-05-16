package io.rampage.console.results;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.StartupEvent;
import io.rampage.console.history.RunHistoryService;
import io.rampage.console.orchestrator.RunRecord;
import io.rampage.console.orchestrator.RunStatus;
import io.rampage.reporting.RunSummaryGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Persists finished Gatling runs into the results store. Two entry points:
 * {@link #ingestCompleted(RunRecord)} for a live console run, and
 * {@link #importFromFilesystem()} for the startup backfill of reports produced
 * outside the console. Both are idempotent and best-effort — any parse or I/O
 * failure is caught and logged, never propagated.
 */
@ApplicationScoped
public class RunResultIngestor {

    private static final Logger log = LoggerFactory.getLogger(RunResultIngestor.class);
    private static final ObjectMapper JSON = new ObjectMapper(); // ObjectMapper is thread-safe once configured.

    @Inject
    StoredRunRepository repository;

    @Inject
    RunHistoryService history;

    /** Self-reference: the per-directory @Transactional importOne must be called
     *  through the CDI proxy for its interceptor to apply (self-invocation via
     *  {@code this} is not intercepted). */
    @Inject
    RunResultIngestor self;

    /** Backfill pre-existing reports when the console starts. */
    void onStartup(@Observes StartupEvent event) {
        try {
            importFromFilesystem();
        } catch (Exception e) {
            log.warn("Startup results backfill failed: {}", e.getMessage());
        }
    }

    /** Ingest a console-launched run that has just reached a terminal state. */
    @Transactional
    public void ingestCompleted(RunRecord record) {
        try {
            if (repository.findById(record.id()) != null) {
                return;
            }
            StoredRun run = new StoredRun();
            run.id = record.id();
            run.source = RunSource.CONSOLE;
            run.environmentPath = record.queued().envPath();
            run.runPath = record.queued().runPath();
            run.runConfigKey = run.environmentPath + "::" + run.runPath;
            run.name = basename(run.runPath);
            run.status = record.status();
            run.startedAt = record.startedAt();
            run.finishedAt = record.finishedAt();
            run.exitCode = record.exitCode();

            Optional<Path> simDir = history.latestSimulationDirSince(record.startedAt());
            if (simDir.isPresent()) {
                populateFromReport(run, simDir.get());
            }
            repository.persist(run);
            log.info("Ingested console run {} ({} scenario stats)",
                run.id, run.scenarioStats.size());
        } catch (Exception e) {
            log.warn("Failed to ingest run {}: {}", record.id(), e.getMessage(), e);
        }
    }

    /**
     * Scan build/reports/gatling/ and store any simulation directory not yet
     * known. Each directory is imported in its own transaction (via
     * {@link #importOne(Path)}) so a failure on one report cannot abort the rest.
     */
    public void importFromFilesystem() {
        List<Path> dirs;
        try {
            dirs = history.scanSimulationDirs();
        } catch (Exception e) {
            log.warn("Cannot scan reports directory, skipping backfill: {}", e.getMessage());
            return;
        }
        int imported = 0;
        for (Path simDir : dirs) {
            if (self.importOne(simDir)) {
                imported++;
            }
        }
        if (imported > 0) {
            log.info("Backfill imported {} run(s) from build/reports/gatling/", imported);
        }
    }

    /**
     * Import one simulation directory as its own unit of work. Public and
     * {@code @Transactional} so it is called through the CDI proxy ({@code self})
     * and each directory gets an independent transaction. Returns true when a new
     * run was stored.
     */
    @Transactional
    public boolean importOne(Path simDir) {
        String dirName = simDir.getFileName().toString();
        if (repository.existsBySimulationDir(dirName)) {
            return false;
        }
        try {
            StoredRun run = new StoredRun();
            run.id = "imported-" + dirName;
            run.source = RunSource.IMPORTED;
            run.status = RunStatus.COMPLETED;
            run.name = dirName;
            // Pre-existing reports carry no real timing; the directory mtime is
            // the best approximation, used for both startedAt and finishedAt.
            Instant mtime = Files.getLastModifiedTime(simDir).toInstant();
            run.startedAt = mtime;
            run.finishedAt = mtime;
            populateFromReport(run, simDir);
            if (run.runConfigKey == null) {
                run.runConfigKey = "imported::" + dirName;
            }
            repository.persist(run);
            return true;
        } catch (Exception e) {
            log.warn("Failed to import report {}: {}", dirName, e.getMessage(), e);
            return false;
        }
    }

    /** Fill in identity (from run-metadata.json) and metrics (from the report HTML). */
    private void populateFromReport(StoredRun run, Path simDir) {
        run.simulationDir = simDir.getFileName().toString();

        Path metaFile = simDir.resolve("run-metadata.json");
        if (Files.isRegularFile(metaFile)) {
            try {
                Map<?, ?> meta = JSON.readValue(metaFile.toFile(), Map.class);
                if (meta.get("runName") != null) {
                    run.name = String.valueOf(meta.get("runName"));
                }
                if (meta.get("environment") != null) {
                    run.environmentId = String.valueOf(meta.get("environment"));
                }
                if (meta.get("gitCommit") != null) {
                    run.gitCommit = String.valueOf(meta.get("gitCommit"));
                }
                if (meta.get("gitBranch") != null) {
                    run.gitBranch = String.valueOf(meta.get("gitBranch"));
                }
                if (meta.get("environment") != null && meta.get("runId") != null) {
                    run.runConfigKey = meta.get("environment") + "::" + meta.get("runId");
                }
            } catch (IOException e) {
                log.warn("Could not read {}: {}", metaFile, e.getMessage());
            }
        }

        try {
            Map<String, Object> summary = RunSummaryGenerator.summarise(simDir);
            run.assertionsOk = "PASS".equals(summary.get("status"));
            Object requests = summary.get("requests");
            if (requests instanceof List<?> rows) {
                for (Object row : rows) {
                    if (row instanceof Map<?, ?> req) {
                        run.addScenarioStat(toScenarioStat(req));
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Could not summarise report {}: {}", simDir, e.getMessage());
        }
    }

    private static ScenarioStat toScenarioStat(Map<?, ?> req) {
        ScenarioStat s = new ScenarioStat();
        s.scenarioName = str(req.get("name"));
        s.requestCount = lng(req.get("total"));
        s.okCount = lng(req.get("ok"));
        s.koCount = lng(req.get("ko"));
        s.errorPercent = dbl(req.get("koPct"));
        s.requestsPerSecond = dbl(req.get("rps"));
        s.meanMs = dbl(req.get("mean"));
        s.p50Ms = dbl(req.get("p50"));
        s.p75Ms = dbl(req.get("p75"));
        s.p95Ms = dbl(req.get("p95"));
        s.p99Ms = dbl(req.get("p99"));
        s.maxMs = dbl(req.get("max"));
        return s;
    }

    private static String basename(String path) {
        if (path == null) {
            return "run";
        }
        String name = path.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static Long lng(Object o) {
        return o instanceof Number n ? n.longValue() : null;
    }

    private static Double dbl(Object o) {
        return o instanceof Number n ? n.doubleValue() : null;
    }
}
