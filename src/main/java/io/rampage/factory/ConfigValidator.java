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

    private static final Set<String> KNOWN_HTTP_METHODS = Set.of(
        "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS");

    private static final Set<String> KNOWN_BODY_TYPES = Set.of(
        "graphql", "json", "form", "text", "none");

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

            errors.addAll(AssertionFactory.validateUnknownScenarios(run.getAssertions(), scenarios));

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

        if (sc.getRequest() != null) {
            validateRequest(sc.getRequest(), scPath + ".request", errors);
        }

        validateSteps(sc, scPath, errors);

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

    private void validateSteps(ScenarioConfig sc, String scPath, List<String> errors) {
        if (sc.getSteps() == null || sc.getSteps().isEmpty()) return;
        Set<String> namesSeen = new java.util.LinkedHashSet<>();
        Set<String> sessionKeysAvailable = new java.util.LinkedHashSet<>();
        // Feeder columns are also valid session keys at runtime.
        if (sc.getFeeder() != null && sc.getFeeder().getColumns() != null) {
            sc.getFeeder().getColumns().forEach((columnName, col) -> {
                String key = (col != null && col.getSessionKey() != null) ? col.getSessionKey() : columnName;
                if (key != null) sessionKeysAvailable.add(key);
            });
        }
        for (int i = 0; i < sc.getSteps().size(); i++) {
            StepConfig step = sc.getSteps().get(i);
            String stepPath = scPath + ".steps[" + i + "]";
            if (step == null) {
                errors.add(stepPath + " must not be null");
                continue;
            }
            if (step.getName() == null || step.getName().isBlank()) {
                errors.add(stepPath + ".name must not be blank");
            } else if (!namesSeen.add(step.getName())) {
                errors.add(stepPath + ".name '" + step.getName() + "' is duplicated within scenario");
            }
            if (step.getRequest() != null) {
                validateRequest(step.getRequest(), stepPath + ".request", errors);
                validateSessionReferences(step.getRequest(), sessionKeysAvailable, stepPath, errors);
            }
            validateExtracts(step.getExtract(), stepPath + ".extract", sessionKeysAvailable, errors);
        }
    }

    private void validateRequest(RequestConfig request, String path, List<String> errors) {
        if (request == null) return;
        if (request.getMethod() != null && !request.getMethod().isBlank()
            && !KNOWN_HTTP_METHODS.contains(request.getMethod().toUpperCase(java.util.Locale.ROOT))) {
            errors.add(path + ".method '" + request.getMethod() + "' is not one of " + KNOWN_HTTP_METHODS);
        }
        if (request.getBodyType() != null && !request.getBodyType().isBlank()
            && !KNOWN_BODY_TYPES.contains(request.getBodyType().toLowerCase(java.util.Locale.ROOT))) {
            errors.add(path + ".bodyType '" + request.getBodyType() + "' is not one of " + KNOWN_BODY_TYPES);
        }
        String bodyType = request.getBodyType() != null ? request.getBodyType().toLowerCase(java.util.Locale.ROOT) : null;
        if ("graphql".equals(bodyType) && (request.getGraphqlQueryFile() == null || request.getGraphqlQueryFile().isBlank())) {
            errors.add(path + ".bodyType=graphql requires .graphqlQueryFile");
        }
        if (("json".equals(bodyType) || "text".equals(bodyType))
            && (request.getBody() == null || request.getBody().isBlank())
            && (request.getBodyFile() == null || request.getBodyFile().isBlank())) {
            errors.add(path + ".bodyType=" + bodyType + " requires .body or .bodyFile");
        }
        if ("form".equals(bodyType) && (request.getFormParams() == null || request.getFormParams().isEmpty())) {
            errors.add(path + ".bodyType=form requires .formParams");
        }
        if (request.getBodyFile() != null && !request.getBodyFile().isBlank()
            && !resourceExists(request.getBodyFile())) {
            errors.add(path + ".bodyFile '" + request.getBodyFile() + "' not found on filesystem or classpath");
        }
    }

    private void validateExtracts(List<ExtractConfig> extracts, String path,
                                   Set<String> sessionKeysAvailable, List<String> errors) {
        if (extracts == null) return;
        for (int i = 0; i < extracts.size(); i++) {
            ExtractConfig e = extracts.get(i);
            if (e == null) continue;
            String ePath = path + "[" + i + "]";
            if (e.getSessionKey() == null || e.getSessionKey().isBlank()) {
                errors.add(ePath + ".sessionKey must not be blank");
                continue;
            }
            String type = e.getType() != null ? e.getType().toLowerCase(java.util.Locale.ROOT) : "jsonpath";
            if (!"body".equals(type) && (e.getPath() == null || e.getPath().isBlank())) {
                errors.add(ePath + ".path must not be blank for type=" + type);
            }
            sessionKeysAvailable.add(e.getSessionKey());
        }
    }

    private void validateSessionReferences(RequestConfig request, Set<String> available,
                                            String stepPath, List<String> errors) {
        if (request == null) return;
        java.util.function.Consumer<String> check = s -> {
            for (String key : PlaceholderRewriter.referencedSessionKeys(s)) {
                if (!available.contains(key)) {
                    errors.add(stepPath + " references session key '" + key
                        + "' that no earlier step extracts and no feeder column provides");
                }
            }
        };
        check.accept(request.getPath());
        check.accept(request.getBody());
        if (request.getQueryParams() != null) request.getQueryParams().values().forEach(check);
        if (request.getFormParams() != null) request.getFormParams().values().forEach(check);
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
