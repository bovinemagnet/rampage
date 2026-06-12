package io.rampage.console.results;

/**
 * One metric compared across two runs. {@code delta} = B &minus; A; {@code pctChange}
 * is that delta as a percentage of A. {@code regressed} is true when the change
 * exceeds the threshold in the worsening direction.
 *
 * @param label      human-readable metric name shown in the comparison table
 * @param valueA     metric value from run A, or {@code null} when unavailable
 * @param valueB     metric value from run B, or {@code null} when unavailable
 * @param delta      absolute difference (B &minus; A), or {@code null} when either value is absent
 * @param pctChange  percentage change relative to A, or {@code null} when A is absent or zero
 * @param regressed  {@code true} when the change exceeds the regression threshold in the
 *                   worsening direction for the metric's polarity
 */
public record MetricRow(
        String label,
        Double valueA,
        Double valueB,
        Double delta,
        Double pctChange,
        boolean regressed) {

    /**
     * Constructs a {@code MetricRow} from the raw values of two runs, computing the delta,
     * percentage change, and regression flag automatically.
     *
     * @param label                  human-readable metric name
     * @param a                      metric value from run A, or {@code null} when unavailable
     * @param b                      metric value from run B, or {@code null} when unavailable
     * @param lowerIsBetter          {@code true} for latency/error metrics (a rise is bad)
     * @param regressionThresholdPct percentage change beyond which a move counts as a regression
     * @return a fully populated {@code MetricRow}
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
        } else if (a != null && b != null && a == 0.0 && b != 0.0) {
            // Zero baseline: percentage change is undefined, but a move away
            // from zero is still a regression for lower-is-better metrics
            // (e.g. error rate 0% -> 5%).
            regressed = lowerIsBetter ? b > 0.0 : b < 0.0;
        }
        return new MetricRow(label, a, b, delta, pct, regressed);
    }
}
