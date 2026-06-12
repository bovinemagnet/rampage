package io.rampage.console.metrics;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stateful aggregator for Carbon-protocol lines emitted by Gatling's
 * GraphiteDataWriter. Carbon line format is
 *
 *   {@code <metric.path> <value> <unix-timestamp>\n}
 *
 * Gatling 3.15 emits one batch of lines per write period (default 1 s) all
 * sharing the same timestamp. The aggregator keeps the latest value per
 * metric path and yields a {@link MetricSnapshot} every time a complete tick
 * is detected (i.e. the first line at a new timestamp arrives).
 *
 * Not thread-safe — wrap in a single-threaded executor or use Vert.x's
 * event-loop guarantees.
 */
public final class MetricsAggregator {

    /**
     * Creates a new {@code MetricsAggregator} with no accumulated state.
     */
    public MetricsAggregator() {}

    private static final String SUFFIX_USERS_ACTIVE = ".users.allUsers.active";
    private static final String SUFFIX_REQ_COUNT    = ".allRequests.all.count";
    private static final String SUFFIX_REQ_KO       = ".allRequests.ko.count";
    private static final String SUFFIX_REQ_P50      = ".allRequests.all.percentiles50";
    private static final String SUFFIX_REQ_P95      = ".allRequests.all.percentiles95";

    private final Map<String, Double> latest = new LinkedHashMap<>();
    private long currentTick = -1;

    /**
     * Feeds one Carbon line into the aggregator.
     *
     * @param line a single Carbon plain-text protocol line ({@code <path> <value> <timestamp>}).
     * @return a completed {@link MetricSnapshot} when a tick boundary has been crossed,
     *         or {@code null} if this line belongs to the current tick.
     */
    public MetricSnapshot ingest(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length < 3) {
            return null;
        }
        double value;
        long tick;
        try {
            value = Double.parseDouble(parts[1]);
            tick = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }

        MetricSnapshot completed = null;
        if (currentTick != -1 && tick != currentTick) {
            completed = buildSnapshot();
        }
        currentTick = tick;
        latest.put(parts[0], value);
        return completed;
    }

    /**
     * Forces a snapshot of the current state, e.g. when the simulation
     * terminates and no further ticks will arrive to flush the buffer.
     *
     * @return a {@link MetricSnapshot} for the last tick, or {@code null} if no
     *         lines have been ingested.
     */
    public MetricSnapshot flush() {
        if (currentTick == -1) {
            return null;
        }
        return buildSnapshot();
    }

    private MetricSnapshot buildSnapshot() {
        long active = (long) findBySuffix(SUFFIX_USERS_ACTIVE, 0.0);
        long reqs   = (long) findBySuffix(SUFFIX_REQ_COUNT, 0.0);
        long errs   = (long) findBySuffix(SUFFIX_REQ_KO, 0.0);
        double p50  = findBySuffix(SUFFIX_REQ_P50, 0.0);
        double p95  = findBySuffix(SUFFIX_REQ_P95, 0.0);
        return new MetricSnapshot(
                Instant.now(),
                currentTick,
                active,
                reqs,
                errs,
                p50,
                p95,
                new HashMap<>(latest));
    }

    private double findBySuffix(String suffix, double fallback) {
        for (Map.Entry<String, Double> e : latest.entrySet()) {
            if (e.getKey().endsWith(suffix)) {
                return e.getValue();
            }
        }
        return fallback;
    }
}
