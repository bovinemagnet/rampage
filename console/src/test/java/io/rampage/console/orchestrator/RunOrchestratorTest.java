package io.rampage.console.orchestrator;

import io.rampage.console.logs.LogBroadcaster;
import io.rampage.console.logs.LogLine;
import io.smallrye.mutiny.subscription.Cancellable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RunOrchestratorTest {

    private LogBroadcaster logBroadcaster;
    private RunStatusBroadcaster statusBroadcaster;
    private RunOrchestrator orchestrator;
    private ConcurrentLinkedQueue<LogLine> capturedLogs;
    private ConcurrentLinkedQueue<RunStatusEvent> capturedStatuses;
    private Cancellable logSub;
    private Cancellable statusSub;

    @BeforeEach
    void setUp() {
        logBroadcaster = new LogBroadcaster();
        statusBroadcaster = new RunStatusBroadcaster();
        capturedLogs = new ConcurrentLinkedQueue<>();
        capturedStatuses = new ConcurrentLinkedQueue<>();
        logSub = logBroadcaster.stream().subscribe().with(capturedLogs::add);
        statusSub = statusBroadcaster.stream().subscribe().with(capturedStatuses::add);
        orchestrator = new RunOrchestrator(
                logBroadcaster,
                statusBroadcaster,
                System.getProperty("user.dir"),
                "ignored-gradle-command",
                "ignored-task",
                2);
    }

    @AfterEach
    void tearDown() {
        if (logSub != null) logSub.cancel();
        if (statusSub != null) statusSub.cancel();
    }

    @Test
    void enqueueRunsCommandAndReportsCompleted() throws Exception {
        CountDownLatch finished = awaitTerminalStatus();

        orchestrator.setProcessLauncher((cmd, dir) -> stubProcess(0, "hello world"));

        RunRecord rec = orchestrator.enqueue("envA.yaml", "runA.yaml");
        assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();

        assertThat(rec.status()).isEqualTo(RunStatus.COMPLETED);
        assertThat(rec.exitCode()).isZero();
        assertThat(capturedLogs).extracting(LogLine::text)
                .anyMatch(s -> s.contains("hello world"));
    }

    @Test
    void runsExecuteSeriallyInOrder() throws Exception {
        AtomicInteger live = new AtomicInteger();
        AtomicInteger maxLive = new AtomicInteger();
        CountDownLatch allDone = new CountDownLatch(3);

        orchestrator.setProcessLauncher((cmd, dir) -> {
            int now = live.incrementAndGet();
            maxLive.accumulateAndGet(now, Math::max);
            return stubProcess(0, "tick", () -> {
                live.decrementAndGet();
                allDone.countDown();
            });
        });

        orchestrator.enqueue("envA", "runA");
        orchestrator.enqueue("envA", "runB");
        orchestrator.enqueue("envA", "runC");

        assertThat(allDone.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(maxLive.get())
                .as("never more than one run executing at the same time")
                .isEqualTo(1);
    }

    @Test
    void killTerminatesActiveRun() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finished = awaitTerminalStatus();

        orchestrator.setProcessLauncher((cmd, dir) -> {
            Process p = longRunningProcess();
            started.countDown();
            return p;
        });

        RunRecord rec = orchestrator.enqueue("envA", "runA");
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

        // Wait for the orchestrator to have set the process on the record.
        for (int i = 0; i < 50 && rec.process() == null; i++) {
            Thread.sleep(20);
        }
        assertThat(rec.process()).as("process attached to record").isNotNull();

        orchestrator.kill(rec.id());
        assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();

        assertThat(rec.status()).isEqualTo(RunStatus.KILLED);
    }

    private CountDownLatch awaitTerminalStatus() {
        CountDownLatch latch = new CountDownLatch(1);
        statusBroadcaster.stream().subscribe().with(event -> {
            if (event.status() == RunStatus.COMPLETED
                    || event.status() == RunStatus.FAILED
                    || event.status() == RunStatus.KILLED) {
                latch.countDown();
            }
        });
        return latch;
    }

    private static Process stubProcess(int exit, String output) throws IOException {
        return stubProcess(exit, output, () -> {});
    }

    private static Process stubProcess(int exit, String output, Runnable onExit) throws IOException {
        // Use a shell to produce stdout then exit cleanly. Portable on Linux/macOS.
        Process p = new ProcessBuilder("sh", "-c", "echo '" + output + "'; exit " + exit)
                .redirectErrorStream(true)
                .start();
        p.onExit().thenRun(onExit);
        return p;
    }

    private static Process longRunningProcess() throws IOException {
        return new ProcessBuilder("sh", "-c", "sleep 30")
                .redirectErrorStream(true)
                .start();
    }

    @Test
    void exitCodeNonZeroBecomesFailed() throws Exception {
        CountDownLatch finished = awaitTerminalStatus();

        orchestrator.setProcessLauncher((cmd, dir) -> stubProcess(7, "boom"));
        RunRecord rec = orchestrator.enqueue("env", "run");

        assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(rec.status()).isEqualTo(RunStatus.FAILED);
        assertThat(rec.exitCode()).isEqualTo(7);
    }

    @Test
    void killOnQueuedRunRemovesItWithoutLaunching() {
        AtomicInteger launched = new AtomicInteger();
        CountDownLatch firstRunning = new CountDownLatch(1);

        orchestrator.setProcessLauncher((cmd, dir) -> {
            launched.incrementAndGet();
            firstRunning.countDown();
            return longRunningProcess();
        });

        RunRecord first = orchestrator.enqueue("envA", "runA");
        RunRecord second = orchestrator.enqueue("envB", "runB");

        assertThat(awaitCondition(firstRunning, 5)).isTrue();

        orchestrator.kill(second.id());
        List<RunRecord> queued = orchestrator.queueSnapshot();
        assertThat(queued).extracting(RunRecord::id).doesNotContain(second.id());
        assertThat(second.status()).isEqualTo(RunStatus.KILLED);

        // Clean up the still-running first run.
        orchestrator.kill(first.id());
    }

    private static boolean awaitCondition(CountDownLatch latch, int seconds) {
        try {
            return latch.await(seconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Test
    void completedRunIsHandedToTheIngestor() throws Exception {
        java.util.concurrent.atomic.AtomicReference<String> ingestedId =
                new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.CountDownLatch ingested = new java.util.concurrent.CountDownLatch(1);
        orchestrator.setResultIngestor(new io.rampage.console.results.RunResultIngestor() {
            @Override
            public void ingestCompleted(RunRecord record) {
                ingestedId.set(record.id());
                ingested.countDown();
            }
        });
        orchestrator.setProcessLauncher((cmd, dir) -> stubProcess(0, "done"));

        RunRecord rec = orchestrator.enqueue("envA.yaml", "runA.yaml");

        assertThat(ingested.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(ingestedId.get()).isEqualTo(rec.id());
    }
}
