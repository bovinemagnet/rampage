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
