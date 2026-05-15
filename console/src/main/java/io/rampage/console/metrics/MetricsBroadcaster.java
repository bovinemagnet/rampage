package io.rampage.console.metrics;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MetricsBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(MetricsBroadcaster.class);

    private final BroadcastProcessor<MetricSnapshot> processor = BroadcastProcessor.create();

    public void publish(MetricSnapshot snapshot) {
        try {
            processor.onNext(snapshot);
        } catch (BackPressureFailure e) {
            log.debug("Dropped metric snapshot for slow subscriber: {}", e.getMessage());
        }
    }

    public Multi<MetricSnapshot> stream() {
        return processor;
    }
}
