package io.rampage.console.results;

import java.util.List;

/**
 * The result of comparing run A against run B.
 *
 * @param runA      the baseline run
 * @param runB      the candidate run being compared against the baseline
 * @param scenarios per-scenario comparison results, one entry per scenario name
 *                  appearing in either run; immutable
 */
public record RunComparison(
        StoredRun runA,
        StoredRun runB,
        List<ScenarioComparison> scenarios) {

    /**
     * Compact constructor — defensive copy ensures the list is immutable.
     */
    public RunComparison {
        scenarios = List.copyOf(scenarios);
    }

    /**
     * Returns {@code true} when any scenario regressed — the headline verdict for the
     * comparison page.
     *
     * @return {@code true} if at least one scenario contains a regressed metric
     */
    public boolean hasRegression() {
        return scenarios.stream().anyMatch(ScenarioComparison::hasRegression);
    }
}
