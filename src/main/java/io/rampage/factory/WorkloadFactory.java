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
        String type = workload.getType();
        log.info("Building workload injection for type: {}", type);

        if ("ramp-and-hold".equals(type)) {
            double fromRate = workload.getRate() != null ? workload.getRate().getFrom() : 0.0;
            double toRate = workload.getRate() != null ? workload.getRate().getTo() : 10.0;
            Duration rampDuration = parseDuration(workload.getRampUp(), Duration.ofSeconds(60));
            Duration holdDuration = parseDuration(workload.getHoldFor(), Duration.ofSeconds(300));
            return new OpenInjectionStep[]{
                rampUsersPerSec(fromRate).to(toRate).during(rampDuration),
                constantUsersPerSec(toRate).during(holdDuration)
            };
        }

        if ("smoke".equals(type)) {
            int users = workload.getUsers() > 0 ? workload.getUsers() : 1;
            return new OpenInjectionStep[]{
                atOnceUsers(users)
            };
        }

        if ("soak".equals(type)) {
            double rate = workload.getRate() != null ? workload.getRate().getTo() : 1.0;
            Duration duration = parseDuration(workload.getHoldFor(), Duration.ofHours(1));
            return new OpenInjectionStep[]{
                constantUsersPerSec(rate).during(duration)
            };
        }

        if ("constant".equals(type)) {
            double rate = workload.getRate() != null ? workload.getRate().getTo() : 1.0;
            Duration duration = parseDuration(workload.getHoldFor(), Duration.ofSeconds(60));
            return new OpenInjectionStep[]{
                constantUsersPerSec(rate).during(duration)
            };
        }

        log.warn("Unknown workload type '{}', falling back to single user", type);
        return new OpenInjectionStep[]{
            atOnceUsers(1)
        };
    }

    public static Duration parseDuration(String durationStr, Duration defaultDuration) {
        if (durationStr == null || durationStr.isBlank()) {
            return defaultDuration;
        }
        String s = durationStr.trim().toLowerCase();
        try {
            if (s.endsWith("ms")) {
                return Duration.ofMillis(Long.parseLong(s.substring(0, s.length() - 2).trim()));
            } else if (s.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(s.substring(0, s.length() - 1).trim()));
            } else if (s.endsWith("m")) {
                return Duration.ofMinutes(Long.parseLong(s.substring(0, s.length() - 1).trim()));
            } else if (s.endsWith("h")) {
                return Duration.ofHours(Long.parseLong(s.substring(0, s.length() - 1).trim()));
            } else {
                return Duration.ofSeconds(Long.parseLong(s));
            }
        } catch (NumberFormatException e) {
            log.warn("Could not parse duration '{}', using default: {}", durationStr, defaultDuration);
            return defaultDuration;
        }
    }
}
