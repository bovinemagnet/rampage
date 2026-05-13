package io.rampage.factory;

import io.rampage.config.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ConfigValidator {
    private static final Logger log = LoggerFactory.getLogger(ConfigValidator.class);

    public void validate(EnvironmentConfig env, RunConfig run, List<ScenarioConfig> scenarios) {
        List<String> errors = new ArrayList<>();

        if (env == null) {
            errors.add("Environment config is null");
        } else {
            if (env.getBaseUrls() == null || env.getBaseUrls().isEmpty()) {
                errors.add("environment.baseUrls must not be null or empty");
            }
            if (env.getHttp() != null) {
                if (env.getHttp().getConnectTimeoutMillis() <= 0) {
                    errors.add("environment.http.connectTimeoutMillis must be > 0");
                }
                if (env.getHttp().getRequestTimeoutMillis() <= 0) {
                    errors.add("environment.http.requestTimeoutMillis must be > 0");
                }
            }
            if (env.getSafety() != null && !env.getSafety().isAllowProduction()) {
                String envId = env.getId() != null ? env.getId() : "";
                if (envId.contains("prod")) {
                    errors.add("Environment '" + envId + "' appears to be production but safety.allowProduction is false");
                }
            }
        }

        if (run == null) {
            errors.add("Run config is null");
        } else {
            if (run.getName() == null || run.getName().isBlank()) {
                errors.add("run.name must not be null or empty");
            }
            if (run.getScenarios() == null || run.getScenarios().isEmpty()) {
                errors.add("run.scenarios must not be empty");
            } else {
                Set<String> loadedIds = scenarios.stream()
                    .filter(s -> s.getId() != null)
                    .map(ScenarioConfig::getId)
                    .collect(Collectors.toSet());

                for (ScenarioRef ref : run.getScenarios()) {
                    if (ref.isEnabled() && !loadedIds.contains(ref.getId())) {
                        errors.add("Scenario '" + ref.getId() + "' listed in run.scenarios has no corresponding ScenarioConfig loaded");
                    }
                }
            }

            if (run.getExecution() != null && run.getExecution().getWorkload() != null) {
                WorkloadConfig workload = run.getExecution().getWorkload();
                if (workload.getType() == null || workload.getType().isBlank()) {
                    errors.add("run.execution.workload.type must not be blank");
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
