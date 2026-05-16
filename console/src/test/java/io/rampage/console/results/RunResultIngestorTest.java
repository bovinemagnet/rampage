package io.rampage.console.results;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.rampage.console.history.RunHistoryService;
import io.rampage.console.orchestrator.QueuedRun;
import io.rampage.console.orchestrator.RunRecord;
import io.rampage.console.orchestrator.RunStatus;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class RunResultIngestorTest {

    @Inject
    RunResultIngestor ingestor;

    @Inject
    RunHistoryService history;

    @Inject
    StoredRunRepository repository;

    /** Lay down one Gatling-style simulation directory under {@code root}. */
    private Path writeSimDir(Path root, String name, boolean withMetadata) throws Exception {
        Path simDir = root.resolve(name);
        Files.createDirectories(simDir);
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("fixture-gatling-index.html")) {
            Files.write(simDir.resolve("index.html"), is.readAllBytes());
        }
        if (withMetadata) {
            Files.writeString(simDir.resolve("run-metadata.json"),
                "{\"runId\":\"smoke\",\"runName\":\"Smoke Test\",\"environment\":\"local\","
                    + "\"gitCommit\":\"abc1234\",\"gitBranch\":\"main\"}");
        }
        return simDir;
    }

    @Test
    @TestTransaction
    void importFromFilesystemStoresParsedRunsWithScenarioStats(@org.junit.jupiter.api.io.TempDir Path tmp)
            throws Exception {
        repository.deleteAll();
        writeSimDir(tmp, "rampagesimulation-20260515000946259", true);
        history.setReportsDir(tmp.toString());

        ingestor.importFromFilesystem();

        assertThat(repository.count()).isEqualTo(1);
        StoredRun run = repository.findById("imported-rampagesimulation-20260515000946259");
        assertThat(run).isNotNull();
        assertThat(run.source).isEqualTo(RunSource.IMPORTED);
        assertThat(run.name).isEqualTo("Smoke Test");
        assertThat(run.gitCommit).isEqualTo("abc1234");
        assertThat(run.runConfigKey).isEqualTo("local::smoke");
        assertThat(run.assertionsOk).isTrue();
        assertThat(run.scenarioStats).extracting(s -> s.scenarioName).contains("Quick GET");
        assertThat(run.scenarioStats).allSatisfy(s -> assertThat(s.p95Ms).isNotNull());
    }

    @Test
    @TestTransaction
    void importFromFilesystemIsIdempotent(@org.junit.jupiter.api.io.TempDir Path tmp) throws Exception {
        repository.deleteAll();
        writeSimDir(tmp, "rampagesimulation-20260515000946259", true);
        history.setReportsDir(tmp.toString());

        ingestor.importFromFilesystem();
        ingestor.importFromFilesystem();

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @TestTransaction
    void ingestCompletedStoresConsoleRunKeyedOnRunId(@org.junit.jupiter.api.io.TempDir Path tmp)
            throws Exception {
        repository.deleteAll();
        history.setReportsDir(tmp.toString());

        RunRecord record = new RunRecord(QueuedRun.create("config/environments/local.yaml",
            "config/runs/smoke.yaml"));
        record.transitionTo(RunStatus.RUNNING);
        record.markStarted(null);
        // Report appears after the run starts.
        writeSimDir(tmp, "rampagesimulation-20260515000946259", true);
        record.markFinished(0);
        record.transitionTo(RunStatus.COMPLETED);

        ingestor.ingestCompleted(record);

        StoredRun run = repository.findById(record.id());
        assertThat(run).isNotNull();
        assertThat(run.source).isEqualTo(RunSource.CONSOLE);
        assertThat(run.status).isEqualTo(RunStatus.COMPLETED);
        assertThat(run.simulationDir).isEqualTo("rampagesimulation-20260515000946259");
        assertThat(run.scenarioStats).isNotEmpty();

        // Ingesting the same record again is a no-op (idempotent).
        ingestor.ingestCompleted(record);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @TestTransaction
    void ingestCompletedStoresKilledRunEvenWithNoReport(@org.junit.jupiter.api.io.TempDir Path tmp) {
        repository.deleteAll();
        history.setReportsDir(tmp.toString());

        RunRecord record = new RunRecord(QueuedRun.create("env.yaml", "run.yaml"));
        record.transitionTo(RunStatus.RUNNING);
        record.markStarted(null);
        record.transitionTo(RunStatus.KILLED);
        record.markFinished(-1);

        ingestor.ingestCompleted(record);

        StoredRun run = repository.findById(record.id());
        assertThat(run).isNotNull();
        assertThat(run.status).isEqualTo(RunStatus.KILLED);
        assertThat(run.scenarioStats).isEmpty();
    }
}
