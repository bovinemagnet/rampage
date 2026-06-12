/**
 * Persistent storage and comparison of completed load-test runs.
 *
 * <p>{@code RunResultIngestor} imports finished Gatling runs into the database as
 * {@code StoredRun} entities with per-scenario {@code ScenarioStat} rows, accessed via
 * {@code StoredRunRepository}. {@code RunComparisonService} builds side-by-side
 * comparisons ({@code RunComparison}, {@code ScenarioComparison}, {@code MetricRow})
 * that flag regressions between two runs. {@code RunSource} records how a run entered
 * the store.</p>
 */
package io.rampage.console.results;
