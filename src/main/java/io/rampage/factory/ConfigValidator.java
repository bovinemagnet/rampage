package io.rampage.factory;

import io.rampage.config.model.EnvironmentConfig;
import io.rampage.config.model.RunConfig;
import io.rampage.config.model.ScenarioConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ConfigValidator {
    private static final Logger log = LoggerFactory.getLogger(ConfigValidator.class);

    public void validate(EnvironmentConfig env, RunConfig run, List<ScenarioConfig> scenarios) {
        List<String> errors = new ArrayList<>();

        // Validate environment
        if (env == null || env.getEnvironment() == null) {
            errors.add("Environment config is null or missing 'environment' key");
        } else {
            EnvironmentConfig.Environment environment = env.getEnvironment();
            if (environment.getBaseUrl() == null || environment.getBaseUrl().isBlank()) {
                errors.add("environment.baseUrl must not be null or empty");
            }
            if (environment.getTimeouts() != null) {
                if (environment.getTimeouts().getConnectionTimeoutMs() <= 0) {
                    errors.add("environment.timeouts.connectionTimeoutMs must be > 0");
                }
                if (environment.getTimeouts().getReadTimeoutMs() <= 0) {
                    errors.add("environment.timeouts.readTimeoutMs must be > 0");
                }
            }
        }

        // Validate run
        if (run == null || run.getRun() == null) {
            errors.add("Run config is null or missing 'run' key");
        } else {
            RunConfig.Run runInner = run.getRun();
            if (runInner.getScenarios() == null || runInner.getScenarios().isEmpty()) {
                errors.add("run.scenarios must not be empty");
            } else {
                Set<String> loadedNames = scenarios.stream()
                    .filter(s -> s.getScenario() != null)
                    .map(s -> s.getScenario().getName())
                    .collect(Collectors.toSet());

                for (String scenarioName : runInner.getScenarios()) {
                    if (!loadedNames.contains(scenarioName)) {
                        errors.add("Scenario '" + scenarioName + "' listed in run.scenarios has no corresponding ScenarioConfig loaded");
                    }
                }
            }

            // Validate workload
            if (runInner.getWorkload() != null) {
                double targetCPS = runInner.getWorkload().getTargetCallsPerSecond();
                if (targetCPS <= 0) {
                    errors.add("run.workload.targetCallsPerSecond must be > 0");
                }
                if (env != null && env.getEnvironment() != null
                        && env.getEnvironment().getSafety() != null
                        && env.getEnvironment().getSafety().isEnabled()) {
                    double maxCPS = env.getEnvironment().getSafety().getMaxUsersPerSecond();
                    if (targetCPS > maxCPS) {
                        errors.add("run.workload.targetCallsPerSecond (" + targetCPS
                            + ") exceeds safety.maxUsersPerSecond (" + maxCPS + ")");
                    }
                }
            }
        }

        if (!errors.isEmpty()) {
            String message = "Configuration validation failed with " + errors.size() + " error(s):\n"
                + String.join("\n", errors);
            log.error(message);
            throw new ConfigValidationException(message, errors);
        }

        log.info("Configuration validation passed");
    }

    public static class ConfigValidationException extends RuntimeException {
        private final List<String> errors;

        public ConfigValidationException(String message, List<String> errors) {
            super(message);
            this.errors = List.copyOf(errors);
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
