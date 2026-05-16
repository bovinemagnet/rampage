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
        assertThat(comparison.scenarios().get(0).presence())
                .isEqualTo(ScenarioComparison.Presence.BOTH);
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

    @Test
    void flagsRegressionWhenErrorRateRisesFromZeroBaseline() {
        StoredRun a = runWith("a", stat("Quick GET", 100.0, 0.0));
        StoredRun b = runWith("b", stat("Quick GET", 100.0, 5.0));

        RunComparison comparison = service.compare(a, b);

        assertThat(comparison.hasRegression()).isTrue();
        MetricRow error = comparison.scenarios().get(0).metrics().stream()
                .filter(m -> m.label().equals("Error %")).findFirst().orElseThrow();
        assertThat(error.regressed()).isTrue();
    }
}
