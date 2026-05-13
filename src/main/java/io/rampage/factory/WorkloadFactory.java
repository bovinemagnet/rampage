package io.rampage.factory;

import io.gatling.javaapi.core.OpenInjectionStep;
import io.rampage.config.model.WorkloadConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;

public class WorkloadFactory {
    private static final Logger log = LoggerFactory.getLogger(WorkloadFactory.class);

    public OpenInjectionStep[] buildInjection(WorkloadConfig workload) {
        log.info("Building workload injection for model: {}", workload.getModel());

        if ("ramp-and-hold".equals(workload.getModel())) {
            return new OpenInjectionStep[]{
                rampUsersPerSec(0.0).to(workload.getTargetCallsPerSecond())
                    .during(Duration.ofSeconds(workload.getRampDurationSeconds())),
                constantUsersPerSec(workload.getTargetCallsPerSecond())
                    .during(Duration.ofSeconds(workload.getHoldDurationSeconds()))
            };
        }

        log.warn("Unknown workload model '{}', falling back to constant load", workload.getModel());
        return new OpenInjectionStep[]{
            constantUsersPerSec(workload.getTargetCallsPerSecond())
                .during(Duration.ofSeconds(workload.getHoldDurationSeconds()))
        };
    }
}
