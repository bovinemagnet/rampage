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
        } else if (a != null && b != null && a == 0.0 && b != 0.0) {
            // Zero baseline: percentage change is undefined, but a move away
            // from zero is still a regression for lower-is-better metrics
            // (e.g. error rate 0% -> 5%).
            regressed = lowerIsBetter ? b > 0.0 : b < 0.0;
        }
        return new MetricRow(label, a, b, delta, pct, regressed);
    }
}
