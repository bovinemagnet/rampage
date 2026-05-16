# Results Store, History and Trends Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the Rampage console a persistent memory — every Gatling run is stored in an embedded database and surfaced through a searchable, taggable history with run-to-run comparison and trend charts.

**Architecture:** All work is in the `console/` Quarkus subproject plus one additive method on the engine. Finished runs are parsed (reusing the engine's `RunSummaryGenerator`) and persisted to an embedded H2 file database via Hibernate ORM Panache. A `RunResultIngestor` is triggered both on run completion (from `RunOrchestrator`) and by a startup backfill scan of `build/reports/gatling/`. The web layer queries the database for history, comparison and trend views.

**Tech Stack:** Java 21, Quarkus 3.35.3, Hibernate ORM Panache, H2 (file mode), Qute templates, HTMX, uPlot (CDN), JUnit 5 + AssertJ, Playwright (e2e).

---

## Background for the implementer

Read these before starting — they are the patterns this plan extends:

- `console/src/main/java/io/rampage/console/orchestrator/RunOrchestrator.java` — single-slot FIFO run executor. `runOne()` (lines 227-280) performs the terminal status transition; that is where ingestion hooks in.
- `console/src/main/java/io/rampage/console/orchestrator/RunRecord.java` / `QueuedRun.java` — run identity (`id()`), paths (`queued().envPath()` / `runPath()`), timing (`startedAt()`, `finishedAt()`), `exitCode()`, `status()`.
- `console/src/main/java/io/rampage/console/history/RunHistoryService.java` — filesystem scan of `build/reports/gatling/`; resolves the reports directory relative to the repo root.
- `src/main/java/io/rampage/reporting/RunSummaryGenerator.java` — parses a Gatling `index.html` into per-request stats + assertion outcomes. Currently only exposes `generate(reportRoot, outputFile)`.
- `console/src/main/java/io/rampage/console/web/HistoryResource.java` — the page being replaced; shows the `@CheckedTemplate` pattern.
- `console/src/main/resources/templates/Dashboard/dashboard.html` — shows how HTMX and CDN scripts are loaded.

**Conventions:** British spelling in user-facing strings and docs. Author `Paul Snow`. Each task ends with a commit; commit messages are conventional (`feat:`, `refactor:`, `test:`, `chore:`) and must not mention AI tooling. Run the full build with `gradle21w build test`; console-only with `gradle21w :console:test`; e2e with `gradle21w :console:e2eTest`.

---

## Task 1: Engine — parse a specific simulation directory

`RunSummaryGenerator` can only summarise the *latest* report and writes JSON as a side effect. The ingestor needs to summarise a *named* directory and get the result back as data. Add an additive public method; refactor `generate(...)` to delegate to it.

**Files:**
- Modify: `src/main/java/io/rampage/reporting/RunSummaryGenerator.java`
- Test: `src/test/java/io/rampage/reporting/RunSummaryGeneratorTest.java`

- [ ] **Step 1: Write the failing test**

Add this method to `RunSummaryGeneratorTest` (the class already has a `loadFixture()` helper that reads `gatling-report-index.html`):

```java
    @Test
    void summarise_parsesNamedSimulationDirectoryWithoutWritingJson(@TempDir Path tmp) throws Exception {
        Path simDir = tmp.resolve("rampagesimulation-20260515000946259");
        Files.createDirectories(simDir);
        Files.writeString(simDir.resolve("index.html"), loadFixture());

        Map<String, Object> summary = RunSummaryGenerator.summarise(simDir);

        assertEquals("PASS", summary.get("status"));
        assertEquals("rampagesimulation-20260515000946259", summary.get("simulationDir"));
        assertFalse(((List<?>) summary.get("requests")).isEmpty());
        assertEquals(2, ((List<?>) summary.get("assertions")).size());
    }

    @Test
    void summarise_throwsWhenIndexHtmlMissing(@TempDir Path tmp) throws Exception {
        Path simDir = tmp.resolve("rampagesimulation-empty");
        Files.createDirectories(simDir);
        IOException ex = assertThrows(IOException.class, () -> RunSummaryGenerator.summarise(simDir));
        assertTrue(ex.getMessage().contains("index.html"));
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `gradle21w test --tests io.rampage.reporting.RunSummaryGeneratorTest`
Expected: FAIL — compilation error, `summarise` is not defined.

- [ ] **Step 3: Add the `summarise` method and refactor `generate`**

In `RunSummaryGenerator.java`, add this public method (place it directly above the existing `generate` method):

```java
    /**
     * Parse a single Gatling simulation directory's {@code index.html} into a
     * structured summary map — request stats, assertion outcomes and an overall
     * PASS/FAIL status — without writing any file.
     */
    public static Map<String, Object> summarise(Path simulationDir) throws IOException {
        File simDir = simulationDir.toFile();
        if (!simDir.isDirectory()) {
            throw new IOException("Not a simulation directory: " + simulationDir);
        }
        File index = new File(simDir, "index.html");
        if (!index.isFile()) {
            throw new IOException("Gatling report missing index.html: " + index);
        }
        String html = Files.readString(index.toPath(), StandardCharsets.UTF_8);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("generatedAt", Instant.now().toString());
        summary.put("simulationDir", simDir.getName());
        summary.put("simulationPath", simDir.getAbsolutePath());
        summary.put("requests", parseRequests(html));
        summary.put("assertions", parseAssertions(html));

        boolean allAssertionsPassed = ((List<?>) summary.get("assertions")).stream()
            .map(a -> ((Map<?, ?>) a).get("result"))
            .allMatch(r -> "OK".equals(r));
        summary.put("status", allAssertionsPassed ? "PASS" : "FAIL");
        return summary;
    }
```

Then replace the body of the existing `generate(Path reportRoot, Path outputFile)` method so it delegates:

```java
    public static Map<String, Object> generate(Path reportRoot, Path outputFile) throws IOException {
        File simDir = findLatestSimulationDir(reportRoot.toFile());
        if (simDir == null) {
            throw new IOException("No Gatling simulation directory found under " + reportRoot
                + " (expected a 'rampagesimulation-*' subdirectory)");
        }
        Map<String, Object> summary = summarise(simDir.toPath());
        Files.createDirectories(outputFile.getParent());
        JSON.writeValue(outputFile.toFile(), summary);
        log.info("Wrote run summary to {} (status={}, requestRows={}, assertions={})",
            outputFile, summary.get("status"),
            ((List<?>) summary.get("requests")).size(),
            ((List<?>) summary.get("assertions")).size());
        return summary;
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `gradle21w test --tests io.rampage.reporting.RunSummaryGeneratorTest`
Expected: PASS — all methods green, including the pre-existing `generate_*` tests (unchanged behaviour).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/io/rampage/reporting/RunSummaryGenerator.java src/test/java/io/rampage/reporting/RunSummaryGeneratorTest.java
git commit -m "feat: add RunSummaryGenerator.summarise for a named simulation dir"
```

---

## Task 2: Console — add persistence dependencies and datasource config

Add Hibernate ORM Panache + H2, configure the file datasource for production and an in-memory datasource for tests, and keep the database file out of git.

**Files:**
- Modify: `console/build.gradle.kts`
- Modify: `console/src/main/resources/application.properties`
- Modify: `console/src/test/resources/application.properties`
- Modify: `.gitignore`

- [ ] **Step 1: Add the dependencies**

In `console/build.gradle.kts`, inside the `dependencies { }` block, after the line `implementation("io.quarkus:quarkus-mutiny")`, add:

```kotlin
    implementation("io.quarkus:quarkus-hibernate-orm-panache")
    implementation("io.quarkus:quarkus-jdbc-h2")
```

- [ ] **Step 2: Configure the production datasource**

Append to `console/src/main/resources/application.properties`:

```properties

# --- Results store -------------------------------------------------------
# Embedded H2 file database holding persisted run history. The file is created
# under data/ relative to the console's working directory, so launch the
# console from the repo root (the documented norm) for it to land at
# <root>/data/. AUTO_SERVER allows dev-mode live reload to reconnect cleanly.
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:file:./data/rampage;AUTO_SERVER=TRUE
quarkus.hibernate-orm.database.generation=update
```

- [ ] **Step 3: Configure the test datasource**

Append to `console/src/test/resources/application.properties`:

```properties

# Results store: in-memory H2 for the test profile so suites never touch the
# real data/ file and every boot starts from an empty schema.
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:mem:rampage-test;DB_CLOSE_DELAY=-1
quarkus.hibernate-orm.database.generation=drop-and-create
```

- [ ] **Step 4: Ignore the database file**

In `.gitignore`, after the `Gradle build outputs and caches` block (after the `.gradle/` lines), add:

```
# Console results database (local state)
/data/
```

- [ ] **Step 5: Verify the console still builds and boots**

Run: `gradle21w :console:build`
Expected: BUILD SUCCESSFUL. Quarkus picks up the H2 datasource; with no entities yet there is nothing to map, so the build is clean.

- [ ] **Step 6: Commit**

```bash
git add console/build.gradle.kts console/src/main/resources/application.properties console/src/test/resources/application.properties .gitignore
git commit -m "chore: add H2 + Hibernate Panache datasource to the console"
```

---

## Task 3: Console — results entities and repository

Create the `io.rampage.console.results` package with the persistence model and a Panache repository.

**Files:**
- Create: `console/src/main/java/io/rampage/console/results/RunSource.java`
- Create: `console/src/main/java/io/rampage/console/results/ScenarioStat.java`
- Create: `console/src/main/java/io/rampage/console/results/StoredRun.java`
- Create: `console/src/main/java/io/rampage/console/results/StoredRunRepository.java`
- Test: `console/src/test/java/io/rampage/console/results/StoredRunRepositoryTest.java`

- [ ] **Step 1: Create the `RunSource` enum**

`console/src/main/java/io/rampage/console/results/RunSource.java`:

```java
package io.rampage.console.results;

/** How a {@link StoredRun} entered the results store. */
public enum RunSource {
    /** Ingested live when a console-launched run finished. */
    CONSOLE,
    /** Discovered by the startup backfill scan of build/reports/gatling/. */
    IMPORTED
}
```

- [ ] **Step 2: Create the `ScenarioStat` entity**

`console/src/main/java/io/rampage/console/results/ScenarioStat.java`:

```java
package io.rampage.console.results;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Per-request aggregate metrics for one {@link StoredRun}, parsed from the Gatling report. */
@Entity
@Table(name = "scenario_stat")
public class ScenarioStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "run_id")
    public StoredRun run;

    /** The Gatling stats-table request name (e.g. "All Requests", "Quick GET"). */
    public String scenarioName;

    /** Rampage scenario id, when the request name matches a configured scenario; otherwise null. */
    public String scenarioId;

    public Long requestCount;
    public Long okCount;
    public Long koCount;
    public Double errorPercent;
    public Double meanMs;
    public Double p50Ms;
    public Double p75Ms;
    public Double p95Ms;
    public Double p99Ms;
    public Double maxMs;
    public Double requestsPerSecond;

    public ScenarioStat() {
    }
}
```

- [ ] **Step 3: Create the `StoredRun` entity**

`console/src/main/java/io/rampage/console/results/StoredRun.java`:

```java
package io.rampage.console.results;

import io.rampage.console.orchestrator.RunStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** One persisted Gatling run — the unit of the results store. */
@Entity
@Table(name = "stored_run")
public class StoredRun {

    /** Orchestrator run UUID for console runs, or "imported-&lt;simDir&gt;" for backfilled runs. */
    @Id
    public String id;

    public String name;
    public String environmentPath;
    public String runPath;
    public String environmentId;

    /** Stable grouping key for trend charts — environment + run identity. */
    public String runConfigKey;

    @Enumerated(EnumType.STRING)
    public RunStatus status;

    public Instant startedAt;
    public Instant finishedAt;
    public Integer exitCode;
    public String gitCommit;
    public String gitBranch;

    /** Gatling output directory name — links the run to its HTML report. */
    public String simulationDir;

    public Boolean assertionsOk;

    @Enumerated(EnumType.STRING)
    public RunSource source;

    @Column(length = 4000)
    public String notes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "stored_run_tag", joinColumns = @JoinColumn(name = "run_id"))
    @Column(name = "tag")
    public Set<String> tags = new LinkedHashSet<>();

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    public List<ScenarioStat> scenarioStats = new ArrayList<>();

    public StoredRun() {
    }

    /** Attach a scenario stat and set its back-reference. */
    public void addScenarioStat(ScenarioStat stat) {
        stat.run = this;
        scenarioStats.add(stat);
    }

    /** Worst (highest) P95 across this run's scenarios, or null when no stats were parsed. */
    public Double worstP95() {
        return scenarioStats.stream()
            .map(s -> s.p95Ms)
            .filter(Objects::nonNull)
            .max(Double::compareTo)
            .orElse(null);
    }

    /** Worst (highest) error percentage across this run's scenarios, or null when no stats. */
    public Double worstErrorPercent() {
        return scenarioStats.stream()
            .map(s -> s.errorPercent)
            .filter(Objects::nonNull)
            .max(Double::compareTo)
            .orElse(null);
    }
}
```

Note: `tags` is a `Set` and `scenarioStats` is a `List` deliberately — Hibernate forbids fetching two `List` bags eagerly, but a `Set` + a `List` is fine.

- [ ] **Step 4: Create the repository**

`console/src/main/java/io/rampage/console/results/StoredRunRepository.java`:

```java
package io.rampage.console.results;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import io.rampage.console.orchestrator.RunStatus;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Panache repository for {@link StoredRun}. String-keyed (the run id). */
@ApplicationScoped
public class StoredRunRepository implements PanacheRepositoryBase<StoredRun, String> {

    /** All runs, newest first. */
    public List<StoredRun> listNewestFirst() {
        return listAll(Sort.by("startedAt").descending());
    }

    /**
     * Filtered search. Every argument is optional (null or blank = no filter):
     * {@code query} matches name / environment id / git commit; {@code tag}
     * matches an exact tag; {@code status} matches the run status.
     */
    public List<StoredRun> search(String query, String tag, String status) {
        StringBuilder jpql = new StringBuilder("FROM StoredRun r WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        if (query != null && !query.isBlank()) {
            jpql.append(" AND (lower(r.name) LIKE :q OR lower(r.environmentId) LIKE :q"
                + " OR lower(r.gitCommit) LIKE :q)");
            params.put("q", "%" + query.toLowerCase() + "%");
        }
        if (tag != null && !tag.isBlank()) {
            jpql.append(" AND :tag MEMBER OF r.tags");
            params.put("tag", tag);
        }
        if (status != null && !status.isBlank()) {
            jpql.append(" AND r.status = :status");
            params.put("status", RunStatus.valueOf(status));
        }
        jpql.append(" ORDER BY r.startedAt DESC");
        return find(jpql.toString(), params).list();
    }

    /** All runs for one configuration, oldest first — the series for a trend chart. */
    public List<StoredRun> byRunConfigKey(String runConfigKey) {
        return list("runConfigKey = ?1 ORDER BY startedAt ASC", runConfigKey);
    }

    /** Distinct run-config keys that have at least one stored run. */
    public List<String> distinctRunConfigKeys() {
        return getEntityManager()
            .createQuery("SELECT DISTINCT r.runConfigKey FROM StoredRun r"
                + " WHERE r.runConfigKey IS NOT NULL ORDER BY r.runConfigKey", String.class)
            .getResultList();
    }

    /** Every distinct tag in use, for the history filter dropdown. */
    public List<String> distinctTags() {
        return getEntityManager()
            .createQuery("SELECT DISTINCT t FROM StoredRun r JOIN r.tags t ORDER BY t", String.class)
            .getResultList();
    }

    public boolean existsBySimulationDir(String simulationDir) {
        return count("simulationDir = ?1", simulationDir) > 0;
    }
}
```

- [ ] **Step 5: Write the failing repository test**

`console/src/test/java/io/rampage/console/results/StoredRunRepositoryTest.java`:

```java
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
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `gradle21w :console:test --tests io.rampage.console.results.StoredRunRepositoryTest`
Expected: PASS — Quarkus boots with the in-memory H2 datasource, the schema is created from the entities, all four tests green.

- [ ] **Step 7: Commit**

```bash
git add console/src/main/java/io/rampage/console/results/ console/src/test/java/io/rampage/console/results/
git commit -m "feat: add StoredRun/ScenarioStat results entities and repository"
```

---

## Task 4: Console — extend `RunHistoryService` with simulation-directory scanning

The ingestor needs to enumerate simulation directories and locate the one a finished run produced. Add these methods alongside the existing `listRecent` (which stays until Task 8).

**Files:**
- Modify: `console/src/main/java/io/rampage/console/history/RunHistoryService.java`
- Test: `console/src/test/java/io/rampage/console/history/RunHistoryServiceTest.java`

- [ ] **Step 1: Write the failing tests**

Add these methods to `RunHistoryServiceTest` (it already builds `simA`, `simB`, `notARun` in `setUp()`):

```java
    @Test
    void scanSimulationDirsReturnsOnlyDirsWithIndexHtmlNewestFirst() {
        List<Path> dirs = service.scanSimulationDirs();
        assertThat(dirs).extracting(p -> p.getFileName().toString())
                .containsExactly(
                        "rampagesimulation-20260515000946259",
                        "rampagesimulation-20260515000724458");
    }

    @Test
    void latestSimulationDirSinceFiltersByModificationTime() {
        Instant future = Instant.now().plusSeconds(3600);
        assertThat(service.latestSimulationDirSince(future)).isEmpty();

        Instant past = Instant.now().minusSeconds(3600);
        assertThat(service.latestSimulationDirSince(past)).isPresent();
        assertThat(service.latestSimulationDirSince(past).get().getFileName().toString())
                .startsWith("rampagesimulation-");
    }
```

Add the import `import java.time.Instant;` to the test file.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `gradle21w :console:test --tests io.rampage.console.history.RunHistoryServiceTest`
Expected: FAIL — compilation error, `scanSimulationDirs` / `latestSimulationDirSince` not defined.

- [ ] **Step 3: Add the methods**

In `RunHistoryService.java`, add `import java.time.Instant;` and `import java.util.Optional;` to the imports, then add these methods (place them after `listRecent`, before `resolveReport`):

```java
    /**
     * Every Gatling simulation directory under the reports root — a directory is
     * a finished run if it contains an {@code index.html}. Sorted newest first by
     * directory name (the name embeds a fixed-width timestamp).
     */
    public List<Path> scanSimulationDirs() {
        Path root = Paths.get(reportsDir).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(root)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(p -> Files.isRegularFile(p.resolve("index.html")))
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan " + reportsDir, e);
        }
    }

    /**
     * The newest simulation directory modified at or after {@code since} (with a
     * five-second slack for clock/mtime granularity). Used to attribute a report
     * directory to the console run that just finished. Empty when a run produced
     * no report (e.g. a kill before Gatling rendered output).
     */
    public Optional<Path> latestSimulationDirSince(Instant since) {
        return scanSimulationDirs().stream()
                .filter(p -> modifiedAtOrAfter(p, since))
                .findFirst();
    }

    private static boolean modifiedAtOrAfter(Path dir, Instant since) {
        if (since == null) {
            return true;
        }
        try {
            return !Files.getLastModifiedTime(dir).toInstant().isBefore(since.minusSeconds(5));
        } catch (IOException e) {
            return false;
        }
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `gradle21w :console:test --tests io.rampage.console.history.RunHistoryServiceTest`
Expected: PASS — new methods green, pre-existing `listRecent` tests still green.

- [ ] **Step 5: Commit**

```bash
git add console/src/main/java/io/rampage/console/history/RunHistoryService.java console/src/test/java/io/rampage/console/history/RunHistoryServiceTest.java
git commit -m "feat: add simulation-directory scanning to RunHistoryService"
```

---

## Task 5: Console — the `RunResultIngestor`

The ingestor turns a finished Gatling run into a `StoredRun` + `ScenarioStat` rows. It handles both a live console run (`ingestCompleted`) and the startup backfill of pre-existing reports (`importFromFilesystem`). All failures are caught and logged — ingestion never breaks a run.

**Files:**
- Create: `console/src/main/java/io/rampage/console/results/RunResultIngestor.java`
- Create: `console/src/test/resources/fixture-gatling-index.html` (copy of the engine fixture)
- Test: `console/src/test/java/io/rampage/console/results/RunResultIngestorTest.java`

- [ ] **Step 1: Copy the Gatling report fixture into the console test resources**

```bash
cp src/test/resources/gatling-report-index.html console/src/test/resources/fixture-gatling-index.html
```

This fixture is a real Gatling 3.15 report: 300 requests, a "ROOT"/"All Requests" row and a "Quick GET" row, two passing assertions.

- [ ] **Step 2: Create the `RunResultIngestor`**

`console/src/main/java/io/rampage/console/results/RunResultIngestor.java`:

```java
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
    private static final ObjectMapper JSON = new ObjectMapper();

    @Inject
    StoredRunRepository repository;

    @Inject
    RunHistoryService history;

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
            log.warn("Failed to ingest run {}: {}", record.id(), e.getMessage());
        }
    }

    /** Scan build/reports/gatling/ and store any simulation directory not yet known. */
    @Transactional
    public void importFromFilesystem() {
        int imported = 0;
        for (Path simDir : history.scanSimulationDirs()) {
            String dirName = simDir.getFileName().toString();
            if (repository.existsBySimulationDir(dirName)) {
                continue;
            }
            try {
                StoredRun run = new StoredRun();
                run.id = "imported-" + dirName;
                run.source = RunSource.IMPORTED;
                run.status = RunStatus.COMPLETED;
                run.name = dirName;
                Instant mtime = Files.getLastModifiedTime(simDir).toInstant();
                run.startedAt = mtime;
                run.finishedAt = mtime;
                populateFromReport(run, simDir);
                if (run.runConfigKey == null) {
                    run.runConfigKey = "imported::" + dirName;
                }
                repository.persist(run);
                imported++;
            } catch (Exception e) {
                log.warn("Failed to import report {}: {}", dirName, e.getMessage());
            }
        }
        if (imported > 0) {
            log.info("Backfill imported {} run(s) from build/reports/gatling/", imported);
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
```

- [ ] **Step 3: Write the failing ingestor test**

`console/src/test/java/io/rampage/console/results/RunResultIngestorTest.java`:

```java
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
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `gradle21w :console:test --tests io.rampage.console.results.RunResultIngestorTest`
Expected: PASS — all four tests green.

- [ ] **Step 5: Commit**

```bash
git add console/src/main/java/io/rampage/console/results/RunResultIngestor.java console/src/test/java/io/rampage/console/results/RunResultIngestorTest.java console/src/test/resources/fixture-gatling-index.html
git commit -m "feat: add RunResultIngestor for live and backfilled run results"
```

---

## Task 6: Console — wire the ingestor into `RunOrchestrator`

When a run reaches a terminal state, the orchestrator must hand it to the ingestor. Add a CDI-injected ingestor plus a test seam (mirroring the existing `setProcessLauncher`).

**Files:**
- Modify: `console/src/main/java/io/rampage/console/orchestrator/RunOrchestrator.java`
- Test: `console/src/test/java/io/rampage/console/orchestrator/RunOrchestratorTest.java`

- [ ] **Step 1: Write the failing test**

Add this to `RunOrchestratorTest`. It installs a recording ingestor subclass (its overridden method touches no injected fields, so plain `new` is safe):

```java
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
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `gradle21w :console:test --tests io.rampage.console.orchestrator.RunOrchestratorTest`
Expected: FAIL — compilation error, `setResultIngestor` not defined.

- [ ] **Step 3: Add the ingestor field, seam and call**

In `RunOrchestrator.java`:

(a) Add the import: `import io.rampage.console.results.RunResultIngestor;`

(b) After the existing `@Inject RunStatusBroadcaster statusBroadcaster;` field, add:

```java
    @Inject
    RunResultIngestor resultIngestor;
```

(c) After the existing `setProcessLauncher` method, add the test seam:

```java
    /** Test seam — replace the ingestor that receives finished runs. */
    public void setResultIngestor(RunResultIngestor ingestor) {
        this.resultIngestor = ingestor;
    }
```

(d) In `runOne(...)`, immediately after the line `statusBroadcaster.publish(RunStatusEvent.of(record));` that follows `record.transitionTo(terminal)` (around line 276) and before `current.set(null);`, add:

```java
        ingest(record);
```

(e) Add this private method (place it after `runOne`):

```java
    /** Hand a finished run to the results store. Never lets ingestion break the queue. */
    private void ingest(RunRecord record) {
        RunResultIngestor ingestor = this.resultIngestor;
        if (ingestor == null) {
            return;
        }
        try {
            ingestor.ingestCompleted(record);
        } catch (Exception e) {
            log.warn("Result ingestion failed for run {}: {}", record.id(), e.getMessage());
        }
    }
```

The `null` guard means the package-private test constructor (which never sets an ingestor) is unaffected — existing `RunOrchestratorTest` cases keep passing.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `gradle21w :console:test --tests io.rampage.console.orchestrator.RunOrchestratorTest`
Expected: PASS — the new test green, all pre-existing orchestrator tests still green.

- [ ] **Step 5: Commit**

```bash
git add console/src/main/java/io/rampage/console/orchestrator/RunOrchestrator.java console/src/test/java/io/rampage/console/orchestrator/RunOrchestratorTest.java
git commit -m "feat: ingest finished runs from the orchestrator into the results store"
```

---

## Task 7: Console — the `RunComparisonService`

Diff two stored runs scenario-by-scenario. Produces a view model the comparison template renders directly.

**Files:**
- Create: `console/src/main/java/io/rampage/console/results/MetricRow.java`
- Create: `console/src/main/java/io/rampage/console/results/ScenarioComparison.java`
- Create: `console/src/main/java/io/rampage/console/results/RunComparison.java`
- Create: `console/src/main/java/io/rampage/console/results/RunComparisonService.java`
- Test: `console/src/test/java/io/rampage/console/results/RunComparisonServiceTest.java`

- [ ] **Step 1: Create `MetricRow`**

`console/src/main/java/io/rampage/console/results/MetricRow.java`:

```java
package io.rampage.console.results;

/**
 * One metric compared across two runs. {@code delta} = B − A; {@code pctChange}
 * is that delta as a percentage of A. {@code regressed} is true when the change
 * exceeds the threshold in the worsening direction.
 */
public record MetricRow(
        String label,
        Double valueA,
        Double valueB,
        Double delta,
        Double pctChange,
        boolean regressed) {

    /**
     * @param lowerIsBetter           true for latency/error metrics (a rise is bad)
     * @param regressionThresholdPct  percentage change beyond which a move counts as a regression
     */
    public static MetricRow of(String label, Double a, Double b,
                               boolean lowerIsBetter, double regressionThresholdPct) {
        Double delta = (a != null && b != null) ? b - a : null;
        Double pct = (a != null && b != null && a != 0.0) ? (b - a) / a * 100.0 : null;
        boolean regressed = false;
        if (pct != null) {
            regressed = lowerIsBetter
                    ? pct > regressionThresholdPct
                    : pct < -regressionThresholdPct;
        }
        return new MetricRow(label, a, b, delta, pct, regressed);
    }
}
```

- [ ] **Step 2: Create `ScenarioComparison`**

`console/src/main/java/io/rampage/console/results/ScenarioComparison.java`:

```java
package io.rampage.console.results;

import java.util.List;

/** The metric rows for one scenario, present in run A, run B, or both. */
public record ScenarioComparison(
        String scenarioName,
        Presence presence,
        List<MetricRow> metrics) {

    public enum Presence { BOTH, ONLY_A, ONLY_B }

    /** A 10% threshold flags meaningful latency/throughput moves; errors flag on any rise. */
    static ScenarioComparison of(String name, ScenarioStat a, ScenarioStat b) {
        Presence presence = (a != null && b != null) ? Presence.BOTH
                : (a != null ? Presence.ONLY_A : Presence.ONLY_B);
        List<MetricRow> metrics = List.of(
                MetricRow.of("Requests", a == null ? null : asDouble(a.requestCount),
                        b == null ? null : asDouble(b.requestCount), false, 0.0),
                MetricRow.of("Error %", a == null ? null : a.errorPercent,
                        b == null ? null : b.errorPercent, true, 0.0),
                MetricRow.of("RPS", a == null ? null : a.requestsPerSecond,
                        b == null ? null : b.requestsPerSecond, false, 10.0),
                MetricRow.of("Mean (ms)", a == null ? null : a.meanMs,
                        b == null ? null : b.meanMs, true, 10.0),
                MetricRow.of("P95 (ms)", a == null ? null : a.p95Ms,
                        b == null ? null : b.p95Ms, true, 10.0),
                MetricRow.of("P99 (ms)", a == null ? null : a.p99Ms,
                        b == null ? null : b.p99Ms, true, 10.0));
        return new ScenarioComparison(name, presence, metrics);
    }

    /** True when any metric in this scenario regressed. */
    public boolean hasRegression() {
        return metrics.stream().anyMatch(MetricRow::regressed);
    }

    private static Double asDouble(Long v) {
        return v == null ? null : v.doubleValue();
    }
}
```

- [ ] **Step 3: Create `RunComparison`**

`console/src/main/java/io/rampage/console/results/RunComparison.java`:

```java
package io.rampage.console.results;

import java.util.List;

/** The result of comparing run A against run B. */
public record RunComparison(
        StoredRun runA,
        StoredRun runB,
        List<ScenarioComparison> scenarios) {

    /** True when any scenario regressed — the headline verdict for the page. */
    public boolean hasRegression() {
        return scenarios.stream().anyMatch(ScenarioComparison::hasRegression);
    }
}
```

- [ ] **Step 4: Write the failing service test**

`console/src/test/java/io/rampage/console/results/RunComparisonServiceTest.java`:

```java
package io.rampage.console.results;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RunComparisonServiceTest {

    private final RunComparisonService service = new RunComparisonService();

    private StoredRun runWith(String id, ScenarioStat... stats) {
        StoredRun run = new StoredRun();
        run.id = id;
        for (ScenarioStat s : stats) {
            run.addScenarioStat(s);
        }
        return run;
    }

    private ScenarioStat stat(String name, double p95, double errorPct) {
        ScenarioStat s = new ScenarioStat();
        s.scenarioName = name;
        s.p95Ms = p95;
        s.p99Ms = p95 * 1.2;
        s.meanMs = p95 * 0.6;
        s.errorPercent = errorPct;
        s.requestsPerSecond = 50.0;
        s.requestCount = 1000L;
        return s;
    }

    @Test
    void flagsP95RegressionWhenLatencyRisesBeyondThreshold() {
        StoredRun a = runWith("a", stat("Quick GET", 100.0, 0.0));
        StoredRun b = runWith("b", stat("Quick GET", 130.0, 0.0));

        RunComparison comparison = service.compare(a, b);

        assertThat(comparison.hasRegression()).isTrue();
        MetricRow p95 = comparison.scenarios().get(0).metrics().stream()
                .filter(m -> m.label().equals("P95 (ms)")).findFirst().orElseThrow();
        assertThat(p95.delta()).isEqualTo(30.0);
        assertThat(p95.pctChange()).isEqualTo(30.0);
        assertThat(p95.regressed()).isTrue();
    }

    @Test
    void noRegressionWhenMetricsImprove() {
        StoredRun a = runWith("a", stat("Quick GET", 130.0, 2.0));
        StoredRun b = runWith("b", stat("Quick GET", 100.0, 0.0));

        RunComparison comparison = service.compare(a, b);

        assertThat(comparison.hasRegression()).isFalse();
    }

    @Test
    void marksScenariosPresentOnOnlyOneSide() {
        StoredRun a = runWith("a", stat("Only A", 100.0, 0.0));
        StoredRun b = runWith("b", stat("Only B", 100.0, 0.0));

        RunComparison comparison = service.compare(a, b);

        assertThat(comparison.scenarios()).extracting(ScenarioComparison::presence)
                .containsExactlyInAnyOrder(
                        ScenarioComparison.Presence.ONLY_A,
                        ScenarioComparison.Presence.ONLY_B);
    }
}
```

- [ ] **Step 5: Run the test to verify it fails**

Run: `gradle21w :console:test --tests io.rampage.console.results.RunComparisonServiceTest`
Expected: FAIL — compilation error, `RunComparisonService` not defined.

- [ ] **Step 6: Create `RunComparisonService`**

`console/src/main/java/io/rampage/console/results/RunComparisonService.java`:

```java
package io.rampage.console.results;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Diffs two {@link StoredRun}s scenario-by-scenario into a {@link RunComparison}. */
@ApplicationScoped
public class RunComparisonService {

    @Inject
    StoredRunRepository repository;

    /** Compare two runs by id. Throws {@link IllegalArgumentException} when either is unknown. */
    public RunComparison compare(String idA, String idB) {
        StoredRun a = repository.findById(idA);
        StoredRun b = repository.findById(idB);
        if (a == null || b == null) {
            throw new IllegalArgumentException("Unknown run id in comparison");
        }
        return compare(a, b);
    }

    /** Compare two already-loaded runs. */
    public RunComparison compare(StoredRun a, StoredRun b) {
        Map<String, ScenarioStat> byNameA = indexByName(a);
        Map<String, ScenarioStat> byNameB = indexByName(b);
        TreeSet<String> names = new TreeSet<>();
        names.addAll(byNameA.keySet());
        names.addAll(byNameB.keySet());

        List<ScenarioComparison> scenarios = new ArrayList<>();
        for (String name : names) {
            scenarios.add(ScenarioComparison.of(name, byNameA.get(name), byNameB.get(name)));
        }
        return new RunComparison(a, b, scenarios);
    }

    private static Map<String, ScenarioStat> indexByName(StoredRun run) {
        Map<String, ScenarioStat> map = new LinkedHashMap<>();
        for (ScenarioStat s : run.scenarioStats) {
            if (s.scenarioName != null) {
                map.put(s.scenarioName, s);
            }
        }
        return map;
    }
}
```

- [ ] **Step 7: Run the test to verify it passes**

Run: `gradle21w :console:test --tests io.rampage.console.results.RunComparisonServiceTest`
Expected: PASS — all three tests green.

- [ ] **Step 8: Commit**

```bash
git add console/src/main/java/io/rampage/console/results/MetricRow.java console/src/main/java/io/rampage/console/results/ScenarioComparison.java console/src/main/java/io/rampage/console/results/RunComparison.java console/src/main/java/io/rampage/console/results/RunComparisonService.java console/src/test/java/io/rampage/console/results/RunComparisonServiceTest.java
git commit -m "feat: add RunComparisonService for scenario-level run diffs"
```

---

## Task 8: Console — database-backed history page with search and filters

Replace the filesystem-scan history page with a DB-backed, searchable, filterable list. This task removes the now-dead `listRecent`/`RunHistoryEntry` code.

**Files:**
- Create: `console/src/main/java/io/rampage/console/web/Formats.java`
- Rewrite: `console/src/main/java/io/rampage/console/web/HistoryResource.java`
- Rewrite: `console/src/main/resources/templates/History/index.html`
- Create: `console/src/main/resources/templates/History/rows.html`
- Modify: `console/src/main/java/io/rampage/console/history/RunHistoryService.java` (delete dead code)
- Delete: `console/src/main/java/io/rampage/console/history/RunHistoryEntry.java`
- Modify: `console/src/test/java/io/rampage/console/history/RunHistoryServiceTest.java` (drop dead tests)
- Modify: `console/src/main/resources/META-INF/resources/style.css` (append)

- [ ] **Step 1: Create the Qute formatting extension**

`console/src/main/java/io/rampage/console/web/Formats.java`:

```java
package io.rampage.console.web;

import io.quarkus.qute.TemplateExtension;

import java.util.Locale;

/** Qute namespace extension — call as {@code {fmt:ms(value)}}, {@code {fmt:pct(value)}}, etc. */
@TemplateExtension(namespace = "fmt")
public class Formats {

    public static String ms(Double v) {
        return v == null ? "—" : Math.round(v) + " ms";
    }

    public static String pct(Double v) {
        return v == null ? "—" : String.format(Locale.ROOT, "%.2f%%", v);
    }

    public static String num(Object v) {
        return v == null ? "—" : String.valueOf(v);
    }

    /** Signed, rounded delta — "+30", "-12", "0". */
    public static String signed(Double v) {
        if (v == null) {
            return "—";
        }
        long r = Math.round(v);
        return (r > 0 ? "+" : "") + r;
    }

    /** Signed percentage to one decimal — "+12.5%". */
    public static String signedPct(Double v) {
        if (v == null) {
            return "—";
        }
        return String.format(Locale.ROOT, "%+.1f%%", v);
    }
}
```

- [ ] **Step 2: Rewrite `HistoryResource` (history list + rows fragment + rescan)**

Replace the entire contents of `console/src/main/java/io/rampage/console/web/HistoryResource.java`:

```java
package io.rampage.console.web;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.rampage.console.results.RunResultIngestor;
import io.rampage.console.results.StoredRun;
import io.rampage.console.results.StoredRunRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/** The run-history pages, backed by the results store. */
@Path("/history")
public class HistoryResource {

    @CheckedTemplate(basePath = "History")
    static class Templates {
        public static native TemplateInstance index(List<StoredRun> runs, List<String> allTags,
                String query, String tag, String status);

        public static native TemplateInstance rows(List<StoredRun> runs);
    }

    @Inject
    StoredRunRepository repository;

    @Inject
    RunResultIngestor ingestor;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list(@QueryParam("q") String query,
                                 @QueryParam("tag") String tag,
                                 @QueryParam("status") String status) {
        List<StoredRun> runs = repository.search(query, tag, status);
        return Templates.index(runs, repository.distinctTags(), query, tag, status);
    }

    @GET
    @Path("/rows")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance rows(@QueryParam("q") String query,
                                 @QueryParam("tag") String tag,
                                 @QueryParam("status") String status) {
        return Templates.rows(repository.search(query, tag, status));
    }

    @POST
    @Path("/rescan")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance rescan() {
        ingestor.importFromFilesystem();
        return Templates.rows(repository.listNewestFirst());
    }
}
```

- [ ] **Step 3: Rewrite the history index template**

Replace the entire contents of `console/src/main/resources/templates/History/index.html`:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Rampage Console — History</title>
    <link rel="stylesheet" href="/style.css">
    <script src="https://unpkg.com/htmx.org@2.0.4" defer></script>
</head>
<body>
<header>
    <div>
        <h1>Run history</h1>
        <p class="subtitle">Every persisted Gatling run — console-launched and imported.</p>
    </div>
    <nav>
        <a href="/">Dashboard</a>
        <a href="/configs">Configs</a>
        <a href="/history">History</a>
        <a href="/history/compare">Compare</a>
        <a href="/history/trends">Trends</a>
    </nav>
</header>

<main>
    <section>
        <form class="filter-bar" hx-get="/history/rows" hx-target="#run-rows"
              hx-trigger="submit, change, keyup delay:300ms from:input[name='q']">
            <input type="text" name="q" placeholder="Search name, environment, commit…"
                   value="{query ?: ''}">
            <select name="tag">
                <option value="">All tags</option>
                {#for t in allTags}
                    <option value="{t}" {#if t == tag}selected{/if}>{t}</option>
                {/for}
            </select>
            <select name="status">
                <option value="">All statuses</option>
                <option value="COMPLETED" {#if status == 'COMPLETED'}selected{/if}>Completed</option>
                <option value="FAILED" {#if status == 'FAILED'}selected{/if}>Failed</option>
                <option value="KILLED" {#if status == 'KILLED'}selected{/if}>Killed</option>
            </select>
            <button type="submit">Filter</button>
        </form>

        <form hx-post="/history/rescan" hx-target="#run-rows" class="rescan-form">
            <button type="submit" class="verify-btn">Rescan reports directory</button>
        </form>

        <table class="history-table">
            <thead>
                <tr>
                    <th>Run</th>
                    <th>Environment</th>
                    <th>Status</th>
                    <th>Started</th>
                    <th>P95</th>
                    <th>Error %</th>
                    <th>Tags</th>
                    <th>Report</th>
                </tr>
            </thead>
            <tbody id="run-rows">
                {#include History/rows.html runs=runs /}
            </tbody>
        </table>
    </section>
</main>
</body>
</html>
```

- [ ] **Step 4: Create the rows fragment template**

`console/src/main/resources/templates/History/rows.html`:

```html
{#if runs.isEmpty}
    <tr><td colspan="8" class="muted">No runs match. Launch one from the dashboard,
        or rescan the reports directory.</td></tr>
{#else}
    {#for r in runs}
        <tr class="status-{r.status}">
            <td><a href="/history/{r.id}">{r.name ?: r.id}</a></td>
            <td>{r.environmentId ?: '—'}</td>
            <td><span class="status-badge">{r.status}</span></td>
            <td>{r.startedAt ?: '—'}</td>
            <td>{fmt:ms(r.worstP95)}</td>
            <td>{fmt:pct(r.worstErrorPercent)}</td>
            <td>
                {#for t in r.tags}<span class="tag-chip">{t}</span>{/for}
            </td>
            <td>
                {#if r.simulationDir}
                    <a href="/reports/{r.simulationDir}/index.html">Open</a>
                {#else}
                    <span class="muted">—</span>
                {/if}
            </td>
        </tr>
    {/for}
{/if}
```

- [ ] **Step 5: Delete the dead filesystem-list code**

(a) In `RunHistoryService.java`, delete the `listRecent(int limit)` method and the private `toEntry(...)` method. Keep `init()`, `scanSimulationDirs()`, `latestSimulationDirSince(...)`, `modifiedAtOrAfter(...)`, `resolveReport(...)` and `setReportsDir(...)`. Remove the now-unused import `import io.rampage.console.history.RunHistoryEntry;` if present, and any imports left unused (`Comparator` is still used by `scanSimulationDirs`; `Instant` still used).

(b) Delete the file `console/src/main/java/io/rampage/console/history/RunHistoryEntry.java`:

```bash
git rm console/src/main/java/io/rampage/console/history/RunHistoryEntry.java
```

(c) In `RunHistoryServiceTest.java`, delete the test methods that exercised `listRecent` / `RunHistoryEntry`: `listsOnlyDirectoriesWithIndexHtml`, `newestFirst`, `hasMetadataReflectsFilePresence`, `reportPathPointsAtIndexHtml`, `limitIsRespected`. Keep `missingReportsDirReturnsEmpty` (change its body to assert `empty.scanSimulationDirs()` is empty), `resolveRejectsPathTraversal`, and the two methods added in Task 4. Remove the now-unused `import io.rampage.console.history.RunHistoryEntry;` import. The `setUp()` fixture stays as-is.

- [ ] **Step 6: Append history-page styles**

Append to `console/src/main/resources/META-INF/resources/style.css`:

```css

/* --- Results store: history, compare, trends ----------------------------- */
.filter-bar {
    display: flex;
    gap: 0.5rem;
    align-items: center;
    margin-bottom: 0.75rem;
    flex-wrap: wrap;
}
.filter-bar input[type="text"] { flex: 1; min-width: 200px; margin-top: 0; }
.filter-bar select { width: auto; margin-top: 0; }
.rescan-form { margin-bottom: 1rem; }

.tag-chip {
    display: inline-block;
    background: var(--bg);
    border: 1px solid var(--border);
    border-radius: 999px;
    padding: 0.05rem 0.5rem;
    margin: 0 0.2rem 0.2rem 0;
    font-size: 0.7rem;
}
.tag-chip button {
    background: none;
    color: var(--muted);
    padding: 0;
    margin-left: 0.25rem;
    font-size: 0.7rem;
}

.delta-bad { color: var(--err); }
.delta-good { color: var(--ok); }
.delta-flat { color: var(--muted); }
.regression-banner {
    padding: 0.5rem 0.75rem;
    border-radius: 4px;
    margin-bottom: 1rem;
    font-size: 0.85rem;
}
.regression-banner.bad { background: rgba(248, 81, 73, 0.12); border-left: 3px solid var(--err); }
.regression-banner.ok { background: rgba(63, 185, 80, 0.12); border-left: 3px solid var(--ok); }

.detail-grid { display: grid; grid-template-columns: max-content 1fr; gap: 0.3rem 1rem; }
.detail-grid dt { color: var(--muted); }
.detail-grid dd { margin: 0; }

#trend-chart { margin-top: 1rem; }
.uplot { color: var(--fg); }
```

- [ ] **Step 7: Build and verify**

Run: `gradle21w :console:build`
Expected: BUILD SUCCESSFUL — Qute checked-template validation passes for `History/index.html` and `History/rows.html`, no compilation errors from the deleted `RunHistoryEntry`.

- [ ] **Step 8: Commit**

```bash
git add console/src/main/java/io/rampage/console/web/ console/src/main/resources/templates/History/ console/src/main/java/io/rampage/console/history/ console/src/test/java/io/rampage/console/history/ console/src/main/resources/META-INF/resources/style.css
git commit -m "feat: database-backed history page with search and filters"
```

---

## Task 9: Console — run tagging and notes

Add inline tag add/remove and a notes field, swapped via HTMX.

**Files:**
- Modify: `console/src/main/java/io/rampage/console/web/HistoryResource.java`
- Create: `console/src/main/resources/templates/History/tagCell.html`
- Test: `console/src/test/java/io/rampage/console/web/HistoryTaggingTest.java`

- [ ] **Step 1: Write the failing test**

`console/src/test/java/io/rampage/console/web/HistoryTaggingTest.java`:

```java
package io.rampage.console.web;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.rampage.console.orchestrator.RunStatus;
import io.rampage.console.results.RunSource;
import io.rampage.console.results.StoredRun;
import io.rampage.console.results.StoredRunRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class HistoryTaggingTest {

    @Inject
    StoredRunRepository repository;

    /** Seed must commit in its own transaction so the running app sees it. */
    void seed(String id) {
        QuarkusTransaction.requiringNew().run(() -> {
            StoredRun run = new StoredRun();
            run.id = id;
            run.name = "Seed";
            run.status = RunStatus.COMPLETED;
            run.source = RunSource.CONSOLE;
            run.startedAt = Instant.now();
            repository.persist(run);
        });
    }

    @Test
    void addThenRemoveTagUpdatesTheStoredRun() {
        seed("tag-run-1");

        given().formParam("tag", "nightly")
                .when().post("/history/tag-run-1/tags")
                .then().statusCode(200).body(containsString("nightly"));

        assertThat(reload("tag-run-1").tags).contains("nightly");

        given().when().delete("/history/tag-run-1/tags/nightly")
                .then().statusCode(200);

        assertThat(reload("tag-run-1").tags).doesNotContain("nightly");
    }

    @Test
    void savingNotesPersistsThem() {
        seed("note-run-1");

        given().formParam("notes", "Investigated the P95 spike")
                .when().post("/history/note-run-1/notes")
                .then().statusCode(200);

        assertThat(reload("note-run-1").notes).isEqualTo("Investigated the P95 spike");
    }

    StoredRun reload(String id) {
        return QuarkusTransaction.requiringNew().call(() -> repository.findById(id));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `gradle21w :console:test --tests io.rampage.console.web.HistoryTaggingTest`
Expected: FAIL — the tag/notes endpoints return 404.

- [ ] **Step 3: Add the tag-cell fragment template**

`console/src/main/resources/templates/History/tagCell.html`:

```html
{#for t in run.tags}
    <span class="tag-chip">{t}<button
        hx-delete="/history/{run.id}/tags/{t}"
        hx-target="closest .tag-cell" hx-swap="innerHTML">×</button></span>
{/for}
<form hx-post="/history/{run.id}/tags" hx-target="closest .tag-cell" hx-swap="innerHTML"
      class="tag-add">
    <input type="text" name="tag" placeholder="add tag…" required>
</form>
```

- [ ] **Step 4: Add the tag and notes endpoints**

In `HistoryResource.java`:

(a) Add imports:

```java
import io.quarkus.qute.TemplateInstance;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
```

(b) Add a checked-template method for the tag cell inside the `Templates` class:

```java
        public static native TemplateInstance tagCell(StoredRun run);
```

(c) Add these endpoint methods to the class:

```java
    @POST
    @Path("/{id}/tags")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance addTag(@PathParam("id") String id, @FormParam("tag") String tag) {
        StoredRun run = require(id);
        if (tag != null && !tag.isBlank()) {
            run.tags.add(tag.trim());
        }
        return Templates.tagCell(run);
    }

    @DELETE
    @Path("/{id}/tags/{tag}")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance removeTag(@PathParam("id") String id, @PathParam("tag") String tag) {
        StoredRun run = require(id);
        run.tags.remove(tag);
        return Templates.tagCell(run);
    }

    @POST
    @Path("/{id}/notes")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public String saveNotes(@PathParam("id") String id, @FormParam("notes") String notes) {
        StoredRun run = require(id);
        run.notes = notes;
        return "<span class=\"validation-ok\">Notes saved.</span>";
    }

    private StoredRun require(String id) {
        StoredRun run = repository.findById(id);
        if (run == null) {
            throw new WebApplicationException("Unknown run: " + id, 404);
        }
        return run;
    }
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `gradle21w :console:test --tests io.rampage.console.web.HistoryTaggingTest`
Expected: PASS — both tests green.

- [ ] **Step 6: Commit**

```bash
git add console/src/main/java/io/rampage/console/web/HistoryResource.java console/src/main/resources/templates/History/tagCell.html console/src/test/java/io/rampage/console/web/HistoryTaggingTest.java
git commit -m "feat: add run tagging and notes to the history page"
```

---

## Task 10: Console — run detail page

A per-run page: metadata, per-scenario stat table, editable tags and notes.

**Files:**
- Modify: `console/src/main/java/io/rampage/console/web/HistoryResource.java`
- Create: `console/src/main/resources/templates/History/detail.html`
- Test: `console/src/test/java/io/rampage/console/web/HistoryDetailTest.java`

- [ ] **Step 1: Write the failing test**

`console/src/test/java/io/rampage/console/web/HistoryDetailTest.java`:

```java
package io.rampage.console.web;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.rampage.console.orchestrator.RunStatus;
import io.rampage.console.results.RunSource;
import io.rampage.console.results.ScenarioStat;
import io.rampage.console.results.StoredRun;
import io.rampage.console.results.StoredRunRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class HistoryDetailTest {

    @Inject
    StoredRunRepository repository;

    void seed() {
        QuarkusTransaction.requiringNew().run(() -> {
            StoredRun run = new StoredRun();
            run.id = "detail-run-1";
            run.name = "Detail Run";
            run.environmentId = "local";
            run.status = RunStatus.COMPLETED;
            run.source = RunSource.IMPORTED;
            run.startedAt = Instant.now();
            ScenarioStat s = new ScenarioStat();
            s.scenarioName = "Quick GET";
            s.p95Ms = 142.0;
            s.requestCount = 300L;
            run.addScenarioStat(s);
            repository.persist(run);
        });
    }

    @Test
    void detailPageShowsRunNameAndScenarioStats() {
        seed();
        given().when().get("/history/detail-run-1")
                .then().statusCode(200)
                .body(containsString("Detail Run"))
                .body(containsString("Quick GET"));
    }

    @Test
    void unknownRunReturns404() {
        given().when().get("/history/no-such-run").then().statusCode(404);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `gradle21w :console:test --tests io.rampage.console.web.HistoryDetailTest`
Expected: FAIL — `GET /history/{id}` not defined (currently 404 even for the seeded run).

- [ ] **Step 3: Add the detail endpoint**

In `HistoryResource.java`, add a checked-template method inside `Templates`:

```java
        public static native TemplateInstance detail(StoredRun run);
```

Add this endpoint method:

```java
    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance detail(@PathParam("id") String id) {
        return Templates.detail(require(id));
    }
```

Note: place this method *after* the `/rows` and `/rescan` methods. JAX-RS matches the more specific literal paths first, so `/history/rows` is not captured by `/history/{id}`.

- [ ] **Step 4: Create the detail template**

`console/src/main/resources/templates/History/detail.html`:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Rampage Console — {run.name ?: run.id}</title>
    <link rel="stylesheet" href="/style.css">
    <script src="https://unpkg.com/htmx.org@2.0.4" defer></script>
</head>
<body>
<header>
    <div>
        <h1>{run.name ?: run.id}</h1>
        <p class="subtitle">Run detail</p>
    </div>
    <nav>
        <a href="/">Dashboard</a>
        <a href="/history">History</a>
        <a href="/history/trends?runConfigKey={run.runConfigKey ?: ''}">Trends for this config</a>
    </nav>
</header>

<main>
    <section>
        <h2>Summary</h2>
        <dl class="detail-grid">
            <dt>Status</dt><dd><span class="status-{run.status} status-badge">{run.status}</span></dd>
            <dt>Environment</dt><dd>{run.environmentId ?: '—'}</dd>
            <dt>Started</dt><dd>{run.startedAt ?: '—'}</dd>
            <dt>Finished</dt><dd>{run.finishedAt ?: '—'}</dd>
            <dt>Git</dt><dd>{run.gitBranch ?: '—'} @ {run.gitCommit ?: '—'}</dd>
            <dt>Source</dt><dd>{run.source}</dd>
            <dt>Assertions</dt><dd>{#if run.assertionsOk == true}Passed{#else if run.assertionsOk == false}Failed{#else}—{/if}</dd>
            <dt>Report</dt><dd>{#if run.simulationDir}<a href="/reports/{run.simulationDir}/index.html">Open Gatling report</a>{#else}—{/if}</dd>
        </dl>
    </section>

    <section>
        <h2>Tags</h2>
        <div class="tag-cell">
            {#include History/tagCell.html run=run /}
        </div>
    </section>

    <section>
        <h2>Notes</h2>
        <form hx-post="/history/{run.id}/notes" hx-target="#notes-feedback" hx-swap="innerHTML">
            <textarea name="notes" rows="3">{run.notes ?: ''}</textarea>
            <button type="submit">Save notes</button>
        </form>
        <div id="notes-feedback"></div>
    </section>

    <section>
        <h2>Scenario metrics</h2>
        {#if run.scenarioStats.isEmpty}
            <p class="muted">No metrics were parsed for this run.</p>
        {#else}
            <table class="history-table">
                <thead>
                    <tr><th>Scenario</th><th>Requests</th><th>Errors</th><th>RPS</th>
                        <th>Mean</th><th>P50</th><th>P95</th><th>P99</th></tr>
                </thead>
                <tbody>
                    {#for s in run.scenarioStats}
                        <tr>
                            <td>{s.scenarioName}</td>
                            <td>{fmt:num(s.requestCount)}</td>
                            <td>{fmt:pct(s.errorPercent)}</td>
                            <td>{fmt:num(s.requestsPerSecond)}</td>
                            <td>{fmt:ms(s.meanMs)}</td>
                            <td>{fmt:ms(s.p50Ms)}</td>
                            <td>{fmt:ms(s.p95Ms)}</td>
                            <td>{fmt:ms(s.p99Ms)}</td>
                        </tr>
                    {/for}
                </tbody>
            </table>
        {/if}
    </section>
</main>
</body>
</html>
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `gradle21w :console:test --tests io.rampage.console.web.HistoryDetailTest`
Expected: PASS — both tests green.

- [ ] **Step 6: Commit**

```bash
git add console/src/main/java/io/rampage/console/web/HistoryResource.java console/src/main/resources/templates/History/detail.html console/src/test/java/io/rampage/console/web/HistoryDetailTest.java
git commit -m "feat: add per-run detail page"
```

---

## Task 11: Console — run comparison page

Two run pickers; when both chosen, render the scenario-by-scenario diff.

**Files:**
- Modify: `console/src/main/java/io/rampage/console/web/HistoryResource.java`
- Create: `console/src/main/resources/templates/History/compare.html`
- Test: `console/src/test/java/io/rampage/console/web/HistoryCompareTest.java`

- [ ] **Step 1: Write the failing test**

`console/src/test/java/io/rampage/console/web/HistoryCompareTest.java`:

```java
package io.rampage.console.web;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.rampage.console.orchestrator.RunStatus;
import io.rampage.console.results.RunSource;
import io.rampage.console.results.ScenarioStat;
import io.rampage.console.results.StoredRun;
import io.rampage.console.results.StoredRunRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class HistoryCompareTest {

    @Inject
    StoredRunRepository repository;

    void seed(String id, double p95) {
        QuarkusTransaction.requiringNew().run(() -> {
            StoredRun run = new StoredRun();
            run.id = id;
            run.name = id;
            run.status = RunStatus.COMPLETED;
            run.source = RunSource.CONSOLE;
            run.startedAt = Instant.now();
            ScenarioStat s = new ScenarioStat();
            s.scenarioName = "Quick GET";
            s.p95Ms = p95;
            s.p99Ms = p95 * 1.2;
            s.meanMs = p95 * 0.6;
            s.errorPercent = 0.0;
            s.requestsPerSecond = 50.0;
            s.requestCount = 1000L;
            run.addScenarioStat(s);
            repository.persist(run);
        });
    }

    @Test
    void comparePageShowsBothPickersWithNoSelection() {
        given().when().get("/history/compare")
                .then().statusCode(200).body(containsString("Compare runs"));
    }

    @Test
    void comparePageRendersDiffAndRegressionVerdict() {
        seed("cmp-a", 100.0);
        seed("cmp-b", 150.0);

        given().queryParam("a", "cmp-a").queryParam("b", "cmp-b")
                .when().get("/history/compare")
                .then().statusCode(200)
                .body(containsString("Quick GET"))
                .body(containsString("Regression"));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `gradle21w :console:test --tests io.rampage.console.web.HistoryCompareTest`
Expected: FAIL — `GET /history/compare` not defined.

- [ ] **Step 3: Add the compare endpoint**

In `HistoryResource.java`:

(a) Add imports:

```java
import io.rampage.console.results.RunComparison;
import io.rampage.console.results.RunComparisonService;
```

(b) Inject the service — add a field:

```java
    @Inject
    RunComparisonService comparisonService;
```

(c) Add a checked-template method inside `Templates`:

```java
        public static native TemplateInstance compare(List<StoredRun> allRuns,
                String idA, String idB, RunComparison comparison);
```

(d) Add the endpoint method (place it *before* the `detail(@PathParam("id"))` method so the literal `/compare` path wins over `/{id}`):

```java
    @GET
    @Path("/compare")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance compare(@QueryParam("a") String idA, @QueryParam("b") String idB) {
        RunComparison comparison = null;
        if (idA != null && !idA.isBlank() && idB != null && !idB.isBlank()) {
            comparison = comparisonService.compare(idA, idB);
        }
        return Templates.compare(repository.listNewestFirst(), idA, idB, comparison);
    }
```

- [ ] **Step 4: Create the compare template**

`console/src/main/resources/templates/History/compare.html`:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Rampage Console — Compare runs</title>
    <link rel="stylesheet" href="/style.css">
</head>
<body>
<header>
    <div>
        <h1>Compare runs</h1>
        <p class="subtitle">Side-by-side scenario metrics for two runs.</p>
    </div>
    <nav>
        <a href="/">Dashboard</a>
        <a href="/history">History</a>
        <a href="/history/trends">Trends</a>
    </nav>
</header>

<main>
    <section>
        <form method="get" action="/history/compare" class="filter-bar">
            <select name="a">
                <option value="">Run A…</option>
                {#for r in allRuns}
                    <option value="{r.id}" {#if r.id == idA}selected{/if}>{r.name ?: r.id}</option>
                {/for}
            </select>
            <select name="b">
                <option value="">Run B…</option>
                {#for r in allRuns}
                    <option value="{r.id}" {#if r.id == idB}selected{/if}>{r.name ?: r.id}</option>
                {/for}
            </select>
            <button type="submit">Compare</button>
        </form>

        {#if comparison}
            {#if comparison.hasRegression}
                <div class="regression-banner bad">Regression detected — one or more
                    metrics worsened by more than the threshold.</div>
            {#else}
                <div class="regression-banner ok">No regression — run B holds up against run A.</div>
            {/if}

            {#for sc in comparison.scenarios}
                <h3>{sc.scenarioName}
                    {#if sc.presence.toString == 'ONLY_A'}<span class="muted">(only in A)</span>{/if}
                    {#if sc.presence.toString == 'ONLY_B'}<span class="muted">(only in B)</span>{/if}
                </h3>
                <table class="history-table">
                    <thead>
                        <tr><th>Metric</th><th>Run A</th><th>Run B</th><th>Δ</th><th>Change</th></tr>
                    </thead>
                    <tbody>
                        {#for m in sc.metrics}
                            <tr>
                                <td>{m.label}</td>
                                <td>{fmt:num(m.valueA)}</td>
                                <td>{fmt:num(m.valueB)}</td>
                                <td class="{#if m.regressed}delta-bad{#else}delta-flat{/if}">{fmt:signed(m.delta)}</td>
                                <td class="{#if m.regressed}delta-bad{#else}delta-good{/if}">{fmt:signedPct(m.pctChange)}</td>
                            </tr>
                        {/for}
                    </tbody>
                </table>
            {/for}
        {#else}
            <p class="muted">Pick two runs above to see the comparison.</p>
        {/if}
    </section>
</main>
</body>
</html>
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `gradle21w :console:test --tests io.rampage.console.web.HistoryCompareTest`
Expected: PASS — both tests green.

- [ ] **Step 6: Commit**

```bash
git add console/src/main/java/io/rampage/console/web/HistoryResource.java console/src/main/resources/templates/History/compare.html console/src/test/java/io/rampage/console/web/HistoryCompareTest.java
git commit -m "feat: add run comparison page"
```

---

## Task 12: Console — trends page with uPlot charts

For one run configuration, chart P95, RPS and error rate over time.

**Files:**
- Create: `console/src/main/java/io/rampage/console/web/TrendData.java`
- Modify: `console/src/main/java/io/rampage/console/web/HistoryResource.java`
- Create: `console/src/main/resources/templates/History/trends.html`
- Test: `console/src/test/java/io/rampage/console/web/HistoryTrendsTest.java`

- [ ] **Step 1: Create the trend data builder**

`console/src/main/java/io/rampage/console/web/TrendData.java`:

```java
package io.rampage.console.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rampage.console.results.StoredRun;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a time-ordered list of runs into the JSON array bundle uPlot expects:
 * {@code {"x":[epochSeconds...],"p95":[...],"rps":[...],"err":[...]}}.
 */
public final class TrendData {

    private static final ObjectMapper JSON = new ObjectMapper();

    private TrendData() {
    }

    /** Build the uPlot data island JSON for {@code runs} (assumed oldest-first). */
    public static String toJson(List<StoredRun> runs) {
        List<Long> x = new ArrayList<>();
        List<Double> p95 = new ArrayList<>();
        List<Double> rps = new ArrayList<>();
        List<Double> err = new ArrayList<>();
        for (StoredRun run : runs) {
            if (run.startedAt == null) {
                continue;
            }
            x.add(run.startedAt.getEpochSecond());
            p95.add(run.worstP95());
            rps.add(totalRps(run));
            err.add(run.worstErrorPercent());
        }
        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("x", x);
        bundle.put("p95", p95);
        bundle.put("rps", rps);
        bundle.put("err", err);
        try {
            return JSON.writeValueAsString(bundle);
        } catch (Exception e) {
            return "{\"x\":[],\"p95\":[],\"rps\":[],\"err\":[]}";
        }
    }

    private static Double totalRps(StoredRun run) {
        return run.scenarioStats.stream()
                .map(s -> s.requestsPerSecond)
                .filter(v -> v != null)
                .mapToDouble(Double::doubleValue)
                .sum();
    }
}
```

- [ ] **Step 2: Write the failing test**

`console/src/test/java/io/rampage/console/web/HistoryTrendsTest.java`:

```java
package io.rampage.console.web;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.rampage.console.orchestrator.RunStatus;
import io.rampage.console.results.RunSource;
import io.rampage.console.results.ScenarioStat;
import io.rampage.console.results.StoredRun;
import io.rampage.console.results.StoredRunRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class HistoryTrendsTest {

    @Inject
    StoredRunRepository repository;

    void seed(String id, Instant started, double p95) {
        QuarkusTransaction.requiringNew().run(() -> {
            StoredRun run = new StoredRun();
            run.id = id;
            run.name = id;
            run.status = RunStatus.COMPLETED;
            run.source = RunSource.CONSOLE;
            run.runConfigKey = "local::smoke";
            run.startedAt = started;
            ScenarioStat s = new ScenarioStat();
            s.scenarioName = "Quick GET";
            s.p95Ms = p95;
            s.errorPercent = 0.0;
            s.requestsPerSecond = 50.0;
            run.addScenarioStat(s);
            repository.persist(run);
        });
    }

    @Test
    void trendDataBuildsAJsonBundleInTimeOrder() {
        StoredRun a = new StoredRun();
        a.startedAt = Instant.parse("2026-05-10T10:00:00Z");
        ScenarioStat s = new ScenarioStat();
        s.p95Ms = 120.0;
        s.requestsPerSecond = 40.0;
        s.errorPercent = 1.0;
        a.addScenarioStat(s);

        String json = TrendData.toJson(List.of(a));

        assertThat(json).contains("\"p95\":[120.0]");
        assertThat(json).contains("\"x\":[" + Instant.parse("2026-05-10T10:00:00Z").getEpochSecond());
    }

    @Test
    void trendsPageRendersChartForAConfigKey() {
        seed("trend-a", Instant.parse("2026-05-09T10:00:00Z"), 110.0);
        seed("trend-b", Instant.parse("2026-05-11T10:00:00Z"), 140.0);

        given().queryParam("runConfigKey", "local::smoke")
                .when().get("/history/trends")
                .then().statusCode(200)
                .body(containsString("trend-chart"))
                .body(containsString("\"p95\""));
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `gradle21w :console:test --tests io.rampage.console.web.HistoryTrendsTest`
Expected: FAIL — compilation error (`TrendData` exists from Step 1, but `GET /history/trends` is not defined, so the second test 404s).

- [ ] **Step 4: Add the trends endpoint**

In `HistoryResource.java`:

(a) Add a checked-template method inside `Templates`:

```java
        public static native TemplateInstance trends(List<String> configKeys,
                String selectedKey, String chartJson, boolean hasData);
```

(b) Add the endpoint method (place it *before* the `detail(@PathParam("id"))` method, alongside `/compare`, so the literal path wins):

```java
    @GET
    @Path("/trends")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance trends(@QueryParam("runConfigKey") String runConfigKey) {
        List<StoredRun> series = (runConfigKey != null && !runConfigKey.isBlank())
                ? repository.byRunConfigKey(runConfigKey)
                : List.of();
        String chartJson = TrendData.toJson(series);
        return Templates.trends(repository.distinctRunConfigKeys(), runConfigKey,
                chartJson, !series.isEmpty());
    }
```

- [ ] **Step 5: Create the trends template**

`console/src/main/resources/templates/History/trends.html`:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Rampage Console — Trends</title>
    <link rel="stylesheet" href="/style.css">
    <link rel="stylesheet" href="https://unpkg.com/uplot@1.6.31/dist/uPlot.min.css">
    <script src="https://unpkg.com/uplot@1.6.31/dist/uPlot.iife.min.js"></script>
</head>
<body>
<header>
    <div>
        <h1>Trends</h1>
        <p class="subtitle">P95, throughput and error rate over time for one run configuration.</p>
    </div>
    <nav>
        <a href="/">Dashboard</a>
        <a href="/history">History</a>
        <a href="/history/compare">Compare</a>
    </nav>
</header>

<main>
    <section>
        <form method="get" action="/history/trends" class="filter-bar">
            <select name="runConfigKey">
                <option value="">Choose a run configuration…</option>
                {#for key in configKeys}
                    <option value="{key}" {#if key == selectedKey}selected{/if}>{key}</option>
                {/for}
            </select>
            <button type="submit">Show trend</button>
        </form>

        {#if hasData}
            <div id="trend-chart"></div>
            <script type="application/json" id="trend-data">{chartJson.raw}</script>
            <script>
                (function () {
                    var el = document.getElementById('trend-data');
                    var d = JSON.parse(el.textContent);
                    if (!d.x || !d.x.length) { return; }
                    var opts = {
                        title: 'P95 (ms) · RPS · Error %',
                        width: Math.min(900, window.innerWidth - 80),
                        height: 340,
                        scales: { x: { time: true } },
                        series: [
                            {},
                            { label: 'P95 ms', stroke: '#58a6ff' },
                            { label: 'RPS', stroke: '#3fb950' },
                            { label: 'Error %', stroke: '#f85149' }
                        ]
                    };
                    new uPlot(opts, [d.x, d.p95, d.rps, d.err],
                        document.getElementById('trend-chart'));
                })();
            </script>
        {#else}
            <p class="muted">Pick a run configuration above. Trends need at least one
                stored run for that configuration.</p>
        {/if}
    </section>
</main>
</body>
</html>
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `gradle21w :console:test --tests io.rampage.console.web.HistoryTrendsTest`
Expected: PASS — both tests green.

- [ ] **Step 7: Commit**

```bash
git add console/src/main/java/io/rampage/console/web/TrendData.java console/src/main/java/io/rampage/console/web/HistoryResource.java console/src/main/resources/templates/History/trends.html console/src/test/java/io/rampage/console/web/HistoryTrendsTest.java
git commit -m "feat: add trends page with uPlot time-series charts"
```

---

## Task 13: End-to-end test and full verification

A browser-driven test for the new history flow, then a clean full build.

**Files:**
- Create: `console/src/test/java/io/rampage/console/e2e/HistoryE2eTest.java`

- [ ] **Step 1: Write the e2e test**

`console/src/test/java/io/rampage/console/e2e/HistoryE2eTest.java`:

```java
package io.rampage.console.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.rampage.console.orchestrator.RunStatus;
import io.rampage.console.results.RunSource;
import io.rampage.console.results.ScenarioStat;
import io.rampage.console.results.StoredRun;
import io.rampage.console.results.StoredRunRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Browser-driven coverage of the results-store UI. Tagged {@code e2e}; runs via
 * {@code :console:e2eTest}. Seeds the database directly so it does not depend on
 * a working Gatling installation.
 */
@QuarkusTest
@Tag("e2e")
class HistoryE2eTest {

    private static Playwright playwright;
    private static Browser browser;

    @TestHTTPResource("/history")
    URL historyUrl;

    @Inject
    StoredRunRepository repository;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void shutdown() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    void seed(String id, String name) {
        QuarkusTransaction.requiringNew().run(() -> {
            if (repository.findById(id) != null) {
                return;
            }
            StoredRun run = new StoredRun();
            run.id = id;
            run.name = name;
            run.environmentId = "local";
            run.status = RunStatus.COMPLETED;
            run.source = RunSource.CONSOLE;
            run.runConfigKey = "local::smoke";
            run.startedAt = Instant.now();
            ScenarioStat s = new ScenarioStat();
            s.scenarioName = "Quick GET";
            s.p95Ms = 142.0;
            s.errorPercent = 0.0;
            s.requestsPerSecond = 50.0;
            s.requestCount = 300L;
            run.addScenarioStat(s);
            repository.persist(run);
        });
    }

    @Test
    void historyListsSeededRunAndDetailPageOpens() {
        seed("e2e-history-run", "E2E History Run");
        try (Page page = browser.newPage()) {
            page.navigate(historyUrl.toString());
            assertThat(page.locator("h1").textContent()).isEqualTo("Run history");
            assertThat(page.content()).contains("E2E History Run");

            page.getByRole(com.microsoft.playwright.options.AriaRole.LINK,
                    new Page.GetByRoleOptions().setName("E2E History Run")).click();
            page.waitForSelector("text=Scenario metrics");
            assertThat(page.content()).contains("Quick GET");
        }
    }

    @Test
    void trendsPageRendersChartForSeededConfig() {
        seed("e2e-trend-run", "E2E Trend Run");
        try (Page page = browser.newPage()) {
            String base = historyUrl.toString().replaceFirst("/history$", "");
            page.navigate(base + "/history/trends?runConfigKey=local::smoke");
            page.waitForSelector("#trend-chart canvas",
                    new Page.WaitForSelectorOptions().setTimeout(10_000));
            assertThat(page.locator("#trend-chart canvas").count()).isGreaterThan(0);
        }
    }
}
```

- [ ] **Step 2: Run the e2e test to verify it passes**

Run: `gradle21w :console:e2eTest --tests io.rampage.console.e2e.HistoryE2eTest`
Expected: PASS — uPlot renders a `<canvas>` inside `#trend-chart`; the detail page shows the scenario table. (First run downloads the Playwright browser — allow time.)

- [ ] **Step 3: Run the complete build**

Run: `gradle21w build test`
Expected: BUILD SUCCESSFUL — engine + console unit tests all green.

- [ ] **Step 4: Run the console e2e suite**

Run: `gradle21w :console:e2eTest`
Expected: BUILD SUCCESSFUL — the new `HistoryE2eTest` and the pre-existing `ConsoleE2eTest` all pass. Note `ConsoleE2eTest.historyPageRendersWithoutError` still asserts the `<h1>` is "Run history" — unchanged by this work.

- [ ] **Step 5: Manual smoke check**

Start the console: `gradle21w :console:quarkusDev` (run from the repo root). Then:
1. Open `http://localhost:8090/history` — pre-existing runs under `build/reports/gatling/` appear (startup backfill), or "No runs match" if there are none.
2. Click **Rescan reports directory** — confirms idempotent re-import.
3. Launch a run from the dashboard; when it finishes it appears in `/history` with P95 and error % populated.
4. Open a run, add a tag, save a note; reload — both persist.
5. `/history/compare` — pick two runs, confirm the diff table and regression banner.
6. `/history/trends` — pick a configuration with ≥2 runs, confirm the chart draws.
7. Stop and restart the console — history survives (the `data/rampage.mv.db` file persists).

- [ ] **Step 6: Commit**

```bash
git add console/src/test/java/io/rampage/console/e2e/HistoryE2eTest.java
git commit -m "test: add end-to-end coverage for the results-store UI"
```

---

## Self-review notes (verification of this plan against the spec)

- **Persist every run, survives restart** — Tasks 3 (entities/H2 file DB), 5 (ingestor), 6 (orchestrator hook). H2 file mode persists across restarts (Task 2 config).
- **CLI-only runs captured** — Task 5 `importFromFilesystem()` + startup `@Observes StartupEvent`.
- **Searchable, taggable history** — Task 8 (search/filter), Task 9 (tags + notes).
- **Run comparison** — Tasks 7 (service) + 11 (page).
- **Trend charts** — Task 12 (uPlot).
- **Engine touch-point** — Task 1 (`RunSummaryGenerator.summarise`).
- **Build/config/.gitignore** — Task 2.
- **Tests** — every task is TDD; e2e in Task 13.

**Implementation note (deviation from the spec):** the spec described the H2 file path resolving via `PathResolver` relative to the repo root. Quarkus resolves the datasource JDBC URL before CDI beans run, so Task 2 uses a working-directory-relative path (`./data/rampage`) instead and documents launching the console from the repo root. This is the only deviation; behaviour is otherwise as specified.

**Roadmap doc:** the "Platform evolution" section has already been added to `src/docs/modules/ROOT/pages/reference/roadmap.adoc` (and the `docs/roadmap/roadmap.md` source) — it is not a task in this plan.
