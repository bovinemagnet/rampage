package io.rampage.console.results;

import java.util.List;

/**
 * The metric rows for one scenario, present in run A, run B, or both.
 *
 * @param scenarioName the Gatling request/scenario name identifying this row
 * @param presence     whether the scenario appeared in both runs or only one
 * @param metrics      ordered list of per-metric comparison rows for this scenario
 */
public record ScenarioComparison(
        String scenarioName,
        Presence presence,
        List<MetricRow> metrics) {

    /**
     * Indicates which of the two compared runs contains this scenario.
     */
    public enum Presence {
        /** The scenario is present in both run A and run B. */
        BOTH,
        /** The scenario is present only in run A (the baseline). */
        ONLY_A,
        /** The scenario is present only in run B (the candidate). */
        ONLY_B
    }

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

    /**
     * Returns {@code true} when any metric in this scenario regressed.
     *
     * @return {@code true} if at least one metric row is flagged as regressed
     */
    public boolean hasRegression() {
        return metrics.stream().anyMatch(MetricRow::regressed);
    }

    private static Double asDouble(Long v) {
        return v == null ? null : v.doubleValue();
    }
}
