package io.rampage.console.metrics;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable point-in-time view of the metrics being streamed by Gatling's
 * stock Graphite (Carbon) writer. Values reflect the most recent
 * Carbon-protocol tick (typically one second) received over the wire.
 *
 * @param at                Wall-clock instant the snapshot was assembled.
 * @param tick              Carbon tick timestamp (Unix seconds) the snapshot is keyed to.
 * @param activeUsers       Concurrently active virtual users.
 * @param requestsPerTick   Requests completed within the tick.
 * @param errorsPerTick     Failed requests within the tick.
 * @param p50ResponseMs     50th-percentile response time across the tick (ms).
 * @param p95ResponseMs     95th-percentile response time across the tick (ms).
 * @param raw               Full map of metric path → latest value, for debugging.
 */
public record MetricSnapshot(
        Instant at,
        long tick,
        long activeUsers,
        long requestsPerTick,
        long errorsPerTick,
        double p50ResponseMs,
        double p95ResponseMs,
        Map<String, Double> raw) {

    /**
     * Returns a zero-valued snapshot timestamped to the current instant.
     * Useful as a safe initial state before the first Carbon tick arrives.
     *
     * @return an empty {@code MetricSnapshot} with all numeric fields set to zero.
     */
    public static MetricSnapshot empty() {
        return new MetricSnapshot(Instant.now(), 0L, 0L, 0L, 0L, 0.0, 0.0, Map.of());
    }
}
