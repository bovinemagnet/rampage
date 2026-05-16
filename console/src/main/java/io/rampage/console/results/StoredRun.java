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
