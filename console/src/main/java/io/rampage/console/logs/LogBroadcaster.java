package io.rampage.console.logs;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application-scoped Mutiny broadcast bus for {@link LogLine} events captured
 * from Gatling process output. All console SSE streams that display live log
 * output subscribe to this broadcaster.
 *
 * <p>Back-pressure failures from slow subscribers are silently dropped so that
 * the stdout pump thread is never blocked or terminated.</p>
 */
@ApplicationScoped
public class LogBroadcaster {

    /**
     * Creates a new {@code LogBroadcaster} instance.
     * The CDI container calls this constructor; no initialisation is required beyond
     * what the field initialisers provide.
     */
    public LogBroadcaster() {}

    private static final Logger log = LoggerFactory.getLogger(LogBroadcaster.class);

    private final BroadcastProcessor<LogLine> processor = BroadcastProcessor.create();

    /**
     * Forwards a log line to every subscriber. Slow subscribers can cause
     * {@link BackPressureFailure} to bubble out of {@code processor.onNext} —
     * we catch and drop those so the producer (e.g. the stdout pump thread)
     * is never killed by a lagging browser. Subscribers themselves should
     * still apply {@code onOverflow().drop()} for proper handling on their
     * end (see {@code StreamResource}).
     *
     * @param line the log line to broadcast; a {@code null} value is forwarded
     *             as-is and may cause {@code BackPressureFailure} to be swallowed.
     */
    public void publish(LogLine line) {
        try {
            processor.onNext(line);
        } catch (BackPressureFailure e) {
            log.debug("Dropped log line for slow subscriber: {}", e.getMessage());
        }
    }

    /**
     * Returns a {@code Multi} that emits every {@link LogLine} published to this broadcaster.
     *
     * @return a hot {@code Multi} of log lines; never null.
     */
    public Multi<LogLine> stream() {
        return processor;
    }
}
