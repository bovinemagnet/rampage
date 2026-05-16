package io.rampage.console.results;

import java.util.List;

/** The result of comparing run A against run B. */
public record RunComparison(
        StoredRun runA,
        StoredRun runB,
        List<ScenarioComparison> scenarios) {

    public RunComparison {
        scenarios = List.copyOf(scenarios);
    }

    /** True when any scenario regressed — the headline verdict for the page. */
    public boolean hasRegression() {
        return scenarios.stream().anyMatch(ScenarioComparison::hasRegression);
    }
}
