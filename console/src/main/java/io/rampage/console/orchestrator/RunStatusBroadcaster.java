package io.rampage.console.orchestrator;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RunStatusBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(RunStatusBroadcaster.class);

    private final BroadcastProcessor<RunStatusEvent> processor = BroadcastProcessor.create();

    public void publish(RunStatusEvent event) {
        try {
            processor.onNext(event);
        } catch (BackPressureFailure e) {
            log.debug("Dropped status event for slow subscriber: {}", e.getMessage());
        }
    }

    public Multi<RunStatusEvent> stream() {
        return processor;
    }
}
