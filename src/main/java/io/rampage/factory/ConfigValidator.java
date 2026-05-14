package io.rampage.factory;

import io.rampage.config.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class ConfigValidator {
    private static final Logger log = LoggerFactory.getLogger(ConfigValidator.class);

    private static final Set<String> KNOWN_WORKLOAD_TYPES = Set.of(
        "smoke", "baseline", "ramp-and-hold", "spike", "stress", "soak", "constant");

    private final SecretResolver secretResolver;

    public ConfigValidator() {
        this(new SecretResolver());
    }

    public ConfigValidator(SecretResolver secretResolver) {
        this.secretResolver = secretResolver;
    }

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
            if (env.getSafety() != null) {
                if (env.getSafety().isProduction() && !env.getSafety().isAllowProduction()) {
                    errors.add("environment.safety.isProduction=true but safety.allowProduction=false");
                } else if (!env.getSafety().isAllowProduction()) {
                    String envId = env.getId() != null ? env.getId() : "";
                    if (envId.contains("prod")) {
                        errors.add("Environment '" + envId + "' appears to be production but safety.allowProduction is false");
                    }
                }
            }
            validateSecrets(env, errors);
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
                validateWorkload(run.getExecution().getWorkload(), "run.execution.workload", errors);
            }

            if (env != null && run.getSafety() != null
                && run.getSafety().isFailIfEnvironmentAllowsProduction()
                && env.getSafety() != null && env.getSafety().isAllowProduction()) {
                errors.add("run.safety.failIfEnvironmentAllowsProduction=true but environment.safety.allowProduction=true");
            }
        }

        if (env != null && scenarios != null) {
            for (ScenarioConfig sc : scenarios) {
                validateScenarioReferences(env, sc, errors);
                validateMutatingApproval(env, run, sc, errors);
                errors.addAll(HeaderResolver.validateOverrides(env, sc));
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

    private void validateSecrets(EnvironmentConfig env, List<String> errors) {
        if (env.getSecurity() != null && env.getSecurity().getToken() != null) {
            try {
                secretResolver.resolveToken(env.getSecurity().getToken(), "environment.security.token");
            } catch (SecretResolutionException e) {
                errors.add(e.getMessage());
            }
        }
        if (env.getDatabases() != null) {
            env.getDatabases().forEach((name, db) -> {
                if (db == null) return;
                String base = "environment.databases." + name;
                try {
                    secretResolver.resolveCredential(db.getUsername(), base + ".username");
                } catch (SecretResolutionException e) {
                    errors.add(e.getMessage());
                }
                try {
                    secretResolver.resolveCredential(db.getPassword(), base + ".password");
                } catch (SecretResolutionException e) {
                    errors.add(e.getMessage());
                }
            });
        }
    }

    private void validateWorkload(WorkloadConfig workload, String path, List<String> errors) {
        String type = workload.getType();
        if (type == null || type.isBlank()) {
            errors.add(path + ".type must not be blank");
        } else if (!KNOWN_WORKLOAD_TYPES.contains(type)) {
            errors.add(path + ".type '" + type + "' is not one of " + KNOWN_WORKLOAD_TYPES);
        }
        validateDurationField(workload.getRampUp(), path + ".rampUp", errors);
        validateDurationField(workload.getHoldFor(), path + ".holdFor", errors);
        validateDurationField(workload.getDuration(), path + ".duration", errors);
    }

    private void validateDurationField(String value, String path, List<String> errors) {
        if (value == null || value.isBlank()) return;
        try {
            WorkloadFactory.parseDurationStrict(value);
        } catch (IllegalArgumentException e) {
            errors.add(path + ": " + e.getMessage());
        }
    }

    private void validateScenarioReferences(EnvironmentConfig env, ScenarioConfig sc, List<String> errors) {
        if (sc == null) return;
        String scPath = "scenario." + (sc.getId() != null ? sc.getId() : "?");

        if (sc.getRequest() != null && sc.getRequest().getGraphqlQueryFile() != null) {
            String queryFile = sc.getRequest().getGraphqlQueryFile();
            if (!resourceExists(queryFile)) {
                errors.add(scPath + ".request.graphqlQueryFile '" + queryFile + "' not found on filesystem or classpath");
            }
        }

        FeederConfig feeder = sc.getFeeder();
        if (feeder != null) {
            if (feeder.getSqlFile() != null && !feeder.getSqlFile().isBlank()
                && !resourceExists(feeder.getSqlFile())) {
                errors.add(scPath + ".feeder.sqlFile '" + feeder.getSqlFile() + "' not found on filesystem or classpath");
            }
            String dbRef = feeder.getDatabaseRef();
            if (dbRef != null && !dbRef.isBlank()) {
                if (env.getDatabases() == null || !env.getDatabases().containsKey(dbRef)) {
                    errors.add(scPath + ".feeder.databaseRef '" + dbRef + "' is not defined in environment.databases");
                }
            }
        }

        if (sc.getWorkload() != null && !sc.getWorkload().isInheritFromRun()) {
            validateScenarioWorkload(sc.getWorkload(), scPath + ".workload", errors);
        }
    }

    private void validateScenarioWorkload(ScenarioWorkloadConfig workload, String path, List<String> errors) {
        if (workload.getType() != null && !KNOWN_WORKLOAD_TYPES.contains(workload.getType())) {
            errors.add(path + ".type '" + workload.getType() + "' is not one of " + KNOWN_WORKLOAD_TYPES);
        }
        validateDurationField(workload.getRampUp(), path + ".rampUp", errors);
        validateDurationField(workload.getHoldFor(), path + ".holdFor", errors);
    }

    private void validateMutatingApproval(EnvironmentConfig env, RunConfig run, ScenarioConfig sc, List<String> errors) {
        if (sc == null || sc.getSafety() == null || !sc.getSafety().isMutating()) return;
        if (env.getSafety() == null || !env.getSafety().isRequireApprovalForMutatingRequests()) return;
        boolean approved = run != null && run.getSafety() != null && run.getSafety().isApproveMutatingRequests();
        if (!approved) {
            errors.add("scenario." + sc.getId() + " is mutating and environment requires approval; "
                + "set run.safety.approveMutatingRequests=true to acknowledge");
        }
    }

    private boolean resourceExists(String path) {
        if (path == null || path.isBlank()) return false;
        File fsFile = new File(path);
        if (fsFile.exists() && fsFile.isFile()) return true;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            return is != null;
        } catch (Exception e) {
            return false;
        }
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
