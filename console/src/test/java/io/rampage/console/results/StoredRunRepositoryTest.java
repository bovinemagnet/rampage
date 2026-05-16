package io.rampage.console.results;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.rampage.console.orchestrator.RunStatus;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class StoredRunRepositoryTest {

    @Inject
    StoredRunRepository repository;

    private StoredRun newRun(String id, String name, String env, String configKey,
                             RunStatus status, Instant startedAt) {
        StoredRun run = new StoredRun();
        run.id = id;
        run.name = name;
        run.environmentId = env;
        run.runConfigKey = configKey;
        run.status = status;
        run.startedAt = startedAt;
        run.source = RunSource.CONSOLE;
        return run;
    }

    @Test
    @TestTransaction
    void persistsAndListsNewestFirst() {
        repository.deleteAll();
        repository.persist(newRun("r1", "Smoke", "local", "local::smoke",
            RunStatus.COMPLETED, Instant.parse("2026-05-10T10:00:00Z")));
        repository.persist(newRun("r2", "Load", "local", "local::load",
            RunStatus.FAILED, Instant.parse("2026-05-12T10:00:00Z")));

        List<StoredRun> runs = repository.listNewestFirst();

        assertThat(runs).extracting(r -> r.id).containsExactly("r2", "r1");
    }

    @Test
    @TestTransaction
    void searchFiltersByQueryTagAndStatus() {
        repository.deleteAll();
        StoredRun a = newRun("a", "Smoke", "local", "local::smoke",
            RunStatus.COMPLETED, Instant.parse("2026-05-10T10:00:00Z"));
        a.tags.add("nightly");
        StoredRun b = newRun("b", "Load", "staging", "staging::load",
            RunStatus.FAILED, Instant.parse("2026-05-12T10:00:00Z"));
        repository.persist(a);
        repository.persist(b);

        assertThat(repository.search("smoke", null, null)).extracting(r -> r.id).containsExactly("a");
        assertThat(repository.search(null, "nightly", null)).extracting(r -> r.id).containsExactly("a");
        assertThat(repository.search(null, null, "FAILED")).extracting(r -> r.id).containsExactly("b");
        assertThat(repository.search(null, null, null)).hasSize(2);
    }

    @Test
    @TestTransaction
    void groupsRunsByConfigKeyOldestFirst() {
        repository.deleteAll();
        repository.persist(newRun("late", "Load", "local", "local::load",
            RunStatus.COMPLETED, Instant.parse("2026-05-12T10:00:00Z")));
        repository.persist(newRun("early", "Load", "local", "local::load",
            RunStatus.COMPLETED, Instant.parse("2026-05-09T10:00:00Z")));

        List<StoredRun> series = repository.byRunConfigKey("local::load");

        assertThat(series).extracting(r -> r.id).containsExactly("early", "late");
    }

    @Test
    @TestTransaction
    void existsBySimulationDirDetectsAlreadyStoredRuns() {
        repository.deleteAll();
        StoredRun run = newRun("s", "Smoke", "local", "local::smoke",
            RunStatus.COMPLETED, Instant.now());
        run.simulationDir = "rampagesimulation-20260512000000000";
        repository.persist(run);

        assertThat(repository.existsBySimulationDir("rampagesimulation-20260512000000000")).isTrue();
        assertThat(repository.existsBySimulationDir("rampagesimulation-nope")).isFalse();
    }
}
