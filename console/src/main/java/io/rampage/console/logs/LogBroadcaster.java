package io.rampage.console.logs;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class LogBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(LogBroadcaster.class);

    private final BroadcastProcessor<LogLine> processor = BroadcastProcessor.create();

    /**
     * Forwards a log line to every subscriber. Slow subscribers can cause
     * {@link BackPressureFailure} to bubble out of {@code processor.onNext} —
     * we catch and drop those so the producer (e.g. the stdout pump thread)
     * is never killed by a lagging browser. Subscribers themselves should
     * still apply {@code onOverflow().drop()} for proper handling on their
     * end (see {@code StreamResource}).
     */
    public void publish(LogLine line) {
        try {
            processor.onNext(line);
        } catch (BackPressureFailure e) {
            log.debug("Dropped log line for slow subscriber: {}", e.getMessage());
        }
    }

    public Multi<LogLine> stream() {
        return processor;
    }
}
