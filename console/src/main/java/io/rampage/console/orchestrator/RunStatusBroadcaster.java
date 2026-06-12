package io.rampage.console.orchestrator;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application-scoped broadcaster that fans out {@link RunStatusEvent}s to all
 * active SSE subscribers.
 *
 * <p>Events are published on the orchestrator dispatcher thread.  Slow
 * subscribers that cannot keep up with back-pressure are silently dropped
 * (the event is logged at {@code DEBUG} level rather than propagating an
 * exception).</p>
 */
@ApplicationScoped
public class RunStatusBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(RunStatusBroadcaster.class);

    private final BroadcastProcessor<RunStatusEvent> processor = BroadcastProcessor.create();

    /**
     * Constructs a new {@code RunStatusBroadcaster}.
     * Instances are managed by the CDI container.
     */
    public RunStatusBroadcaster() {
    }

    /**
     * Publishes a status event to all current subscribers.
     *
     * <p>If a subscriber's buffer is full (back-pressure failure), the event
     * is dropped for that subscriber and a {@code DEBUG} log entry is written.</p>
     *
     * @param event the status event to publish; must not be {@code null}
     */
    public void publish(RunStatusEvent event) {
        try {
            processor.onNext(event);
        } catch (BackPressureFailure e) {
            log.debug("Dropped status event for slow subscriber: {}", e.getMessage());
        }
    }

    /**
     * Returns a {@link Multi} that emits all future {@link RunStatusEvent}s.
     * Callers may subscribe to this stream to receive live status updates.
     *
     * @return the event stream; never {@code null}
     */
    public Multi<RunStatusEvent> stream() {
        return processor;
    }
}
