package io.rampage.console.metrics;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application-scoped Mutiny broadcast bus for {@link MetricSnapshot} events
 * produced by the {@link CarbonReceiver}. All console SSE streams that display
 * live metrics subscribe to this broadcaster.
 *
 * <p>Back-pressure failures from slow subscribers are silently dropped so that
 * the Carbon receiver thread is never blocked or terminated.</p>
 */
@ApplicationScoped
public class MetricsBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(MetricsBroadcaster.class);

    private final BroadcastProcessor<MetricSnapshot> processor = BroadcastProcessor.create();

    /**
     * Creates a new {@code MetricsBroadcaster} instance.
     * The CDI container calls this constructor; no initialisation is required beyond
     * what the field initialisers provide.
     */
    public MetricsBroadcaster() {}

    /**
     * Publishes a metric snapshot to all current subscribers, silently dropping
     * the event for any subscriber that cannot keep up.
     *
     * @param snapshot the snapshot to broadcast.
     */
    public void publish(MetricSnapshot snapshot) {
        try {
            processor.onNext(snapshot);
        } catch (BackPressureFailure e) {
            log.debug("Dropped metric snapshot for slow subscriber: {}", e.getMessage());
        }
    }

    /**
     * Returns a {@code Multi} that emits every {@link MetricSnapshot} published to
     * this broadcaster.
     *
     * @return a hot {@code Multi} of metric snapshots; never null.
     */
    public Multi<MetricSnapshot> stream() {
        return processor;
    }
}
