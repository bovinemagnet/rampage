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

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** The run that owns this stat; never {@code null}. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "run_id")
    public StoredRun run;

    /** The Gatling stats-table request name (e.g. "All Requests", "Quick GET"). */
    public String scenarioName;

    /** Rampage scenario id, when the request name matches a configured scenario; otherwise null. */
    public String scenarioId;

    /** Total number of requests sent in this scenario. */
    public Long requestCount;

    /** Number of requests that completed successfully. */
    public Long okCount;

    /** Number of requests that failed. */
    public Long koCount;

    /** Percentage of failed requests (0–100). */
    public Double errorPercent;

    /** Mean response time in milliseconds. */
    public Double meanMs;

    /** 50th-percentile response time in milliseconds. */
    public Double p50Ms;

    /** 75th-percentile response time in milliseconds. */
    public Double p75Ms;

    /** 95th-percentile response time in milliseconds. */
    public Double p95Ms;

    /** 99th-percentile response time in milliseconds. */
    public Double p99Ms;

    /** Maximum observed response time in milliseconds. */
    public Double maxMs;

    /** Mean requests per second over the scenario duration. */
    public Double requestsPerSecond;

    /**
     * Creates a default {@code ScenarioStat} instance.
     * Fields are populated by the ingestor after construction.
     */
    public ScenarioStat() {
    }
}
