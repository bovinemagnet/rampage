package io.rampage.factory;

import io.gatling.javaapi.core.ClosedInjectionStep;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.rampage.config.model.RateConfig;
import io.rampage.config.model.RunConfig;
import io.rampage.config.model.ScenarioConfig;
import io.rampage.config.model.ScenarioWorkloadConfig;
import io.rampage.config.model.WorkloadConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;

/**
 * Translates {@code WorkloadConfig} descriptors into Gatling injection steps.
 *
 * <p>Two injection models are supported:
 * <ul>
 *   <li><b>Open model</b> ({@link #buildInjection}) — injects users at a
 *       configurable arrival rate. Supports {@code smoke}, {@code ramp-and-hold},
 *       {@code soak}, {@code constant}, {@code baseline}, {@code spike}, and
 *       {@code stress} types. Unknown types fall back to a single-user
 *       smoke injection rather than throwing.</li>
 *   <li><b>Closed model</b> ({@link #buildClosedInjection}) — maintains a
 *       target number of concurrently active users. Supports {@code smoke},
 *       {@code ramp-and-hold}, {@code constant}, {@code baseline}, and
 *       {@code soak}.</li>
 * </ul>
 *
 * <p>Duration strings are parsed by {@link #parseDuration(String, Duration)},
 * which accepts {@code ms}, {@code s}, {@code m}, and {@code h} suffixes, or
 * a bare number interpreted as seconds.
 */
public class WorkloadFactory {
    private static final Logger log = LoggerFactory.getLogger(WorkloadFactory.class);

    /**
     * Constructs a new {@code WorkloadFactory}.
     */
    public WorkloadFactory() {
    }

    /**
     * Determines the effective workload configuration for a scenario.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>The scenario-level workload override, if present and
     *       {@code inheritFromRun} is {@code false}.</li>
     *   <li>The run-level workload from {@code run.execution.workload}.</li>
     *   <li>A default smoke workload of 1 user.</li>
     * </ol>
     *
     * @param runConfig    the run configuration; may be {@code null}
     * @param scenarioCfg  the scenario configuration; may be {@code null}
     * @return the effective workload configuration; never {@code null}
     */
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

    /**
     * Returns a scaled copy of the workload with rate/users multiplied by {@code scale}.
     * Used to apply scenario-weighting when several scenarios share a run-level workload.
     *
     * @param source the workload configuration to scale; may be {@code null},
     *               in which case {@code null} is returned
     * @param scale  the multiplicative factor to apply to user counts and rates;
     *               user counts are rounded to the nearest integer with a minimum
     *               of 1
     * @return a new {@code WorkloadConfig} with scaled values, or {@code null}
     *         if {@code source} is {@code null}
     */
    public static WorkloadConfig scaleWorkload(WorkloadConfig source, double scale) {
        if (source == null) return null;
        WorkloadConfig scaled = new WorkloadConfig();
        scaled.setType(source.getType());
        scaled.setRampUp(source.getRampUp());
        scaled.setHoldFor(source.getHoldFor());
        scaled.setDuration(source.getDuration());
        scaled.setSpikeDuration(source.getSpikeDuration());
        scaled.setStepDuration(source.getStepDuration());
        scaled.setUsers((int) Math.max(1, Math.round(source.getUsers() * scale)));
        if (source.getRate() != null) {
            RateConfig r = new RateConfig();
            r.setUnit(source.getRate().getUnit());
            r.setFrom(source.getRate().getFrom() * scale);
            r.setTo(source.getRate().getTo() * scale);
            scaled.setRate(r);
        }
        if (source.getBaselineRate() != null) {
            scaled.setBaselineRate(source.getBaselineRate() * scale);
        }
        if (source.getStepRate() != null) {
            scaled.setStepRate(source.getStepRate() * scale);
        }
        if (source.getMaxRate() != null) {
            scaled.setMaxRate(source.getMaxRate() * scale);
        }
        return scaled;
    }

    static WorkloadConfig fromScenarioOverride(ScenarioWorkloadConfig override) {
        WorkloadConfig wc = new WorkloadConfig();
        wc.setType(override.getType());
        wc.setRate(override.getRate());
        wc.setRampUp(override.getRampUp());
        wc.setHoldFor(override.getHoldFor());
        return wc;
    }

    /**
     * Builds Gatling closed-model injection steps from the given workload
     * configuration.
     *
     * <p>Supported types: {@code smoke}, {@code ramp-and-hold}, {@code constant},
     * {@code baseline}, and {@code soak}. Unsupported types throw
     * {@link IllegalArgumentException}.
     *
     * @param workload the workload configuration describing the injection profile
     * @return an array of one or more closed-model injection steps
     * @throws IllegalArgumentException if the workload type is not supported by
     *                                  the closed model
     */
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

    /**
     * Builds Gatling open-model injection steps from the given workload
     * configuration.
     *
     * <p>Supported types: {@code ramp-and-hold}, {@code smoke}, {@code soak},
     * {@code constant}, {@code baseline}, {@code spike}, and {@code stress}.
     * Unknown types log a warning and fall back to a single {@code atOnceUsers(1)}
     * step rather than throwing.
     *
     * @param workload the workload configuration describing the injection profile
     * @return an array of one or more open-model injection steps; never empty
     */
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
            Duration duration = resolveHoldDuration(workload, Duration.ofHours(1));
            return new OpenInjectionStep[]{
                constantUsersPerSec(rate).during(duration)
            };
        }

        if ("constant".equals(type) || "baseline".equals(type)) {
            double rate = workload.getRate() != null ? workload.getRate().getTo() : 1.0;
            Duration duration = resolveHoldDuration(workload, Duration.ofSeconds(60));
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

    /**
     * Resolves the hold duration for an open-model workload, preferring the explicit
     * {@code duration} field, falling back to {@code holdFor}, then the type default.
     */
    static Duration resolveHoldDuration(WorkloadConfig workload, Duration typeDefault) {
        if (workload.getDuration() != null && !workload.getDuration().isBlank()) {
            return parseDuration(workload.getDuration(),
                parseDuration(workload.getHoldFor(), typeDefault));
        }
        return parseDuration(workload.getHoldFor(), typeDefault);
    }

    /**
     * Parses a duration string, returning {@code defaultDuration} when the
     * input is blank or cannot be parsed.
     *
     * <p>Supported formats: {@code 500ms}, {@code 30s}, {@code 5m}, {@code 2h},
     * or a bare integer treated as seconds.
     *
     * @param durationStr     the string to parse; may be {@code null} or blank
     * @param defaultDuration the fallback duration returned when parsing fails
     * @return the parsed duration, or {@code defaultDuration} on failure
     */
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

    /**
     * Parses a duration string, throwing if the input is blank or uses an
     * unrecognised format.
     *
     * <p>Supported formats: {@code 500ms}, {@code 30s}, {@code 5m}, {@code 2h},
     * or a bare integer treated as seconds.
     *
     * @param durationStr the string to parse; must not be {@code null} or blank
     * @return the parsed {@code Duration}
     * @throws IllegalArgumentException if the string is blank or cannot be parsed
     */
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
