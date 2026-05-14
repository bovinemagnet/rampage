package io.rampage.factory;

import io.gatling.javaapi.core.ClosedInjectionStep;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.rampage.config.model.RunConfig;
import io.rampage.config.model.ScenarioConfig;
import io.rampage.config.model.ScenarioWorkloadConfig;
import io.rampage.config.model.WorkloadConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;

public class WorkloadFactory {
    private static final Logger log = LoggerFactory.getLogger(WorkloadFactory.class);

    public static WorkloadConfig effectiveWorkload(RunConfig runConfig, ScenarioConfig scenarioCfg) {
        ScenarioWorkloadConfig scenarioWorkload = scenarioCfg != null ? scenarioCfg.getWorkload() : null;
        if (scenarioWorkload != null && !scenarioWorkload.isInheritFromRun()) {
            return fromScenarioOverride(scenarioWorkload);
        }
        if (runConfig != null && runConfig.getExecution() != null
            && runConfig.getExecution().getWorkload() != null) {
            return runConfig.getExecution().getWorkload();
        }
        WorkloadConfig fallback = new WorkloadConfig();
        fallback.setType("smoke");
        fallback.setUsers(1);
        return fallback;
    }

    static WorkloadConfig fromScenarioOverride(ScenarioWorkloadConfig override) {
        WorkloadConfig wc = new WorkloadConfig();
        wc.setType(override.getType());
        wc.setRate(override.getRate());
        wc.setRampUp(override.getRampUp());
        wc.setHoldFor(override.getHoldFor());
        return wc;
    }

    public ClosedInjectionStep[] buildClosedInjection(WorkloadConfig workload) {
        String type = workload.getType();
        log.info("Building closed-model injection for type: {}", type);

        if ("smoke".equals(type)) {
            int users = workload.getUsers() > 0 ? workload.getUsers() : 1;
            Duration duration = parseDuration(workload.getDuration(), Duration.ofSeconds(30));
            return new ClosedInjectionStep[]{
                constantConcurrentUsers(users).during(duration)
            };
        }

        if ("ramp-and-hold".equals(type)) {
            int from = workload.getRate() != null ? (int) Math.round(workload.getRate().getFrom()) : 1;
            int to = workload.getRate() != null ? (int) Math.round(workload.getRate().getTo()) : 10;
            Duration rampDuration = parseDuration(workload.getRampUp(), Duration.ofSeconds(60));
            Duration holdDuration = parseDuration(workload.getHoldFor(), Duration.ofSeconds(300));
            return new ClosedInjectionStep[]{
                rampConcurrentUsers(from).to(to).during(rampDuration),
                constantConcurrentUsers(to).during(holdDuration)
            };
        }

        if ("constant".equals(type) || "baseline".equals(type) || "soak".equals(type)) {
            int users = workload.getRate() != null ? (int) Math.round(workload.getRate().getTo()) : 1;
            Duration duration = parseDuration(workload.getHoldFor(),
                "soak".equals(type) ? Duration.ofHours(1) : Duration.ofSeconds(60));
            return new ClosedInjectionStep[]{
                constantConcurrentUsers(users).during(duration)
            };
        }

        throw new IllegalArgumentException("Workload type '" + type
            + "' is not supported in closed model. Use 'smoke', 'baseline', 'constant', 'soak', or 'ramp-and-hold'.");
    }

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

        if ("constant".equals(type) || "baseline".equals(type)) {
            double rate = workload.getRate() != null ? workload.getRate().getTo() : 1.0;
            Duration duration = parseDuration(workload.getHoldFor(), Duration.ofSeconds(60));
            return new OpenInjectionStep[]{
                constantUsersPerSec(rate).during(duration)
            };
        }

        if ("spike".equals(type)) {
            double peak = workload.getRate() != null ? workload.getRate().getTo() : 10.0;
            double baseline = workload.getBaselineRate() != null ? workload.getBaselineRate()
                : (workload.getRate() != null ? workload.getRate().getFrom() : 1.0);
            Duration warmUp = parseDuration(workload.getRampUp(), Duration.ofSeconds(30));
            Duration spike = parseDuration(workload.getSpikeDuration(), Duration.ofSeconds(1));
            Duration hold = parseDuration(workload.getHoldFor(), Duration.ofSeconds(60));
            return new OpenInjectionStep[]{
                constantUsersPerSec(baseline).during(warmUp),
                rampUsersPerSec(baseline).to(peak).during(spike),
                constantUsersPerSec(peak).during(hold),
                rampUsersPerSec(peak).to(baseline).during(spike)
            };
        }

        if ("stress".equals(type)) {
            double start = workload.getRate() != null ? workload.getRate().getFrom() : 1.0;
            double step = workload.getStepRate() != null ? workload.getStepRate() : 1.0;
            double max = workload.getMaxRate() != null ? workload.getMaxRate()
                : (workload.getRate() != null ? workload.getRate().getTo() : 10.0);
            Duration stepDur = parseDuration(workload.getStepDuration(), Duration.ofSeconds(30));
            if (step <= 0) {
                throw new IllegalArgumentException("stress workload requires stepRate > 0");
            }
            int steps = (int) Math.ceil((max - start) / step) + 1;
            if (steps < 1) steps = 1;
            OpenInjectionStep[] result = new OpenInjectionStep[steps];
            for (int i = 0; i < steps; i++) {
                double rate = Math.min(start + i * step, max);
                result[i] = constantUsersPerSec(rate).during(stepDur);
            }
            return result;
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
        try {
            return parseDurationStrict(durationStr);
        } catch (IllegalArgumentException e) {
            log.warn("Could not parse duration '{}', using default: {}", durationStr, defaultDuration);
            return defaultDuration;
        }
    }

    public static Duration parseDurationStrict(String durationStr) {
        if (durationStr == null || durationStr.isBlank()) {
            throw new IllegalArgumentException("Duration must not be null or blank");
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
            throw new IllegalArgumentException("Invalid duration: '" + durationStr
                + "'. Expected e.g. '5s', '2m', '1h', '500ms', or a bare number of seconds.", e);
        }
    }
}
