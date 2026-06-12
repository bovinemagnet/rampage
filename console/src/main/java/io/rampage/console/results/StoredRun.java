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

    /** Human-readable name derived from the run-config filename or the run-metadata.json {@code runName} field. */
    public String name;

    /** Filesystem path of the environment YAML used for this run. */
    public String environmentPath;

    /** Filesystem path of the run YAML used for this run. */
    public String runPath;

    /** Environment identifier read from run-metadata.json; may be null for console runs without metadata. */
    public String environmentId;

    /** Stable grouping key for trend charts — environment + run identity. */
    public String runConfigKey;

    /** Terminal status of the run (e.g. COMPLETED, FAILED). */
    @Enumerated(EnumType.STRING)
    public RunStatus status;

    /** Timestamp at which the Gatling process was started. */
    public Instant startedAt;

    /** Timestamp at which the Gatling process finished; approximated by the report directory mtime for imported runs. */
    public Instant finishedAt;

    /** OS exit code of the Gatling process; null for imported runs. */
    public Integer exitCode;

    /** Git commit SHA recorded in run-metadata.json; null when not present. */
    public String gitCommit;

    /** Git branch name recorded in run-metadata.json; null when not present. */
    public String gitBranch;

    /** Gatling output directory name — links the run to its HTML report. */
    public String simulationDir;

    /** {@code true} if all Gatling assertions passed; {@code false} on failure; null when not evaluated. */
    public Boolean assertionsOk;

    /** How this run entered the results store. */
    @Enumerated(EnumType.STRING)
    public RunSource source;

    /** Free-text notes attached to this run by a user; null when none. */
    @Column(length = 4000)
    public String notes;

    /** User-defined tags for filtering and grouping runs. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "stored_run_tag", joinColumns = @JoinColumn(name = "run_id"))
    @Column(name = "tag")
    public Set<String> tags = new LinkedHashSet<>();

    /** Per-request aggregate statistics parsed from the Gatling report; empty when no report was found. */
    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    public List<ScenarioStat> scenarioStats = new ArrayList<>();

    /**
     * Creates a default {@code StoredRun} instance.
     * Fields are populated by the ingestor after construction.
     */
    public StoredRun() {
    }

    /**
     * Attaches a scenario stat to this run and sets its back-reference.
     *
     * @param stat the scenario stat to attach; its {@code run} field will be set to this instance
     */
    public void addScenarioStat(ScenarioStat stat) {
        stat.run = this;
        scenarioStats.add(stat);
    }

    /**
     * Returns the worst (highest) P95 response time across all scenarios in this run.
     *
     * @return the maximum P95 in milliseconds, or {@code null} when no stats were parsed
     */
    public Double worstP95() {
        return scenarioStats.stream()
            .map(s -> s.p95Ms)
            .filter(Objects::nonNull)
            .max(Double::compareTo)
            .orElse(null);
    }

    /**
     * Returns the worst (highest) error percentage across all scenarios in this run.
     *
     * @return the maximum error percentage (0–100), or {@code null} when no stats were parsed
     */
    public Double worstErrorPercent() {
        return scenarioStats.stream()
            .map(s -> s.errorPercent)
            .filter(Objects::nonNull)
            .max(Double::compareTo)
            .orElse(null);
    }
}
