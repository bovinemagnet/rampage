/**
 * Live metric ingestion and aggregation during load-test runs.
 *
 * <p>{@code CarbonReceiver} listens for Graphite plaintext-protocol metrics emitted by
 * Gatling, {@code MetricsAggregator} folds the raw lines into periodic
 * {@code MetricSnapshot} records, and {@code MetricsBroadcaster} fans the snapshots
 * out to the console's server-sent-event subscribers.</p>
 */
package io.rampage.console.metrics;
