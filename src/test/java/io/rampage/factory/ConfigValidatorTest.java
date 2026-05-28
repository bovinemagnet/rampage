package io.rampage.factory;

import io.rampage.config.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigValidatorTest {
    private ConfigValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ConfigValidator();
    }

    private EnvironmentConfig validEnv() {
        EnvironmentConfig env = new EnvironmentConfig();
        env.setId("test");
        env.setName("Test Environment");
        env.setBaseUrls(Map.of("rest", "http://localhost:8080"));
        HttpConfig http = new HttpConfig();
        http.setConnectTimeoutMillis(5000);
        http.setRequestTimeoutMillis(30000);
        env.setHttp(http);
        SafetyConfig safety = new SafetyConfig();
        safety.setAllowProduction(false);
        env.setSafety(safety);
        return env;
    }

    private RunConfig validRun() {
        RunConfig run = new RunConfig();
        run.setId("test-run-id");
        run.setName("test-run");
        ScenarioRef ref = new ScenarioRef();
        ref.setId("test-scenario");
        ref.setEnabled(true);
        run.setScenarios(List.of(ref));
        ExecutionConfig exec = new ExecutionConfig();
        WorkloadConfig wc = new WorkloadConfig();
        wc.setType("ramp-and-hold");
        RateConfig rate = new RateConfig();
        rate.setFrom(0);
        rate.setTo(10);
        wc.setRate(rate);
        wc.setRampUp("10s");
        wc.setHoldFor("30s");
        exec.setWorkload(wc);
        run.setExecution(exec);
        return run;
    }

    private ScenarioConfig validScenario(String id) {
        ScenarioConfig sc = new ScenarioConfig();
        sc.setId(id);
        sc.setName(id);
        return sc;
    }

    @Test
    void validate_passesForValidConfig() {
        assertDoesNotThrow(() ->
            validator.validate(validEnv(), validRun(), List.of(validScenario("test-scenario"))));
    }

    @Test
    void validate_failsWhenBaseUrlsMissing() {
        EnvironmentConfig env = validEnv();
        env.setBaseUrls(null);
        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(env, validRun(), List.of(validScenario("test-scenario"))));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("baseUrls")));
    }

    @Test
    void validate_failsWhenBaseUrlsEmpty() {
        EnvironmentConfig env = validEnv();
        env.setBaseUrls(Map.of());
        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(env, validRun(), List.of(validScenario("test-scenario"))));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("baseUrls")));
    }

    @Test
    void validate_failsWhenScenariosEmpty() {
        RunConfig run = validRun();
        run.setScenarios(List.of());
        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(validEnv(), run, List.of()));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("scenarios")));
    }

    @Test
    void validate_failsWhenScenarioNotLoaded() {
        RunConfig run = validRun();
        ScenarioRef ref = new ScenarioRef();
        ref.setId("missing-scenario");
        ref.setEnabled(true);
        run.setScenarios(List.of(ref));
        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(validEnv(), run, List.of()));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("missing-scenario")));
    }

    @Test
    void validate_failsWhenRunNameBlank() {
        RunConfig run = validRun();
        run.setName("   ");
        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(validEnv(), run, List.of(validScenario("test-scenario"))));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("run.name")));
    }

    @Test
    void validate_failsForProductionEnvironmentWhenNotAllowed() {
        EnvironmentConfig env = validEnv();
        env.setId("prod-eu-west");
        SafetyConfig safety = new SafetyConfig();
        safety.setAllowProduction(false);
        env.setSafety(safety);
        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(env, validRun(), List.of(validScenario("test-scenario"))));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("prod")));
    }

    @Test
    void validate_listsAllErrors() {
        EnvironmentConfig env = validEnv();
        env.setBaseUrls(null);
        RunConfig run = validRun();
        run.setName(null);
        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(env, run, List.of(validScenario("test-scenario"))));
        assertTrue(ex.getErrors().size() >= 2, "Should have multiple errors");
    }

    @Test
    void validate_failsWhenRequiredSecretEnvVarUnset() {
        EnvironmentConfig env = validEnv();
        SecurityConfig sec = new SecurityConfig();
        sec.setMode("bearer-token");
        TokenConfig token = new TokenConfig();
        token.setSource("env");
        token.setEnvVar("RAMPAGE_TEST_UNSET_TOKEN_QQQ");
        sec.setToken(token);
        env.setSecurity(sec);

        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(env, validRun(), List.of(validScenario("test-scenario"))));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("RAMPAGE_TEST_UNSET_TOKEN_QQQ")),
            "Expected error to mention the missing env var: " + ex.getErrors());
    }

    @Test
    void validate_passesWhenOptionalSecretEnvVarUnset() {
        EnvironmentConfig env = validEnv();
        SecurityConfig sec = new SecurityConfig();
        sec.setMode("bearer-token");
        TokenConfig token = new TokenConfig();
        token.setSource("env");
        token.setEnvVar("RAMPAGE_TEST_UNSET_TOKEN_QQQ");
        token.setRequired(false);
        sec.setToken(token);
        env.setSecurity(sec);

        assertDoesNotThrow(() ->
            validator.validate(env, validRun(), List.of(validScenario("test-scenario"))));
    }

    @Test
    void validate_failsWhenGraphqlQueryFileMissing() {
        ScenarioConfig sc = validScenario("test-scenario");
        RequestConfig req = new RequestConfig();
        req.setGraphqlQueryFile("/no/such/path/query.graphql");
        sc.setRequest(req);

        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(validEnv(), validRun(), List.of(sc)));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("graphqlQueryFile")),
            "Expected graphqlQueryFile error in: " + ex.getErrors());
    }

    @Test
    void validate_failsWhenSqlFileMissing() {
        ScenarioConfig sc = validScenario("test-scenario");
        FeederConfig feeder = new FeederConfig();
        feeder.setSqlFile("/no/such/path/feeder.sql");
        sc.setFeeder(feeder);

        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(validEnv(), validRun(), List.of(sc)));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("sqlFile")));
    }

    @Test
    void validate_failsWhenDatabaseRefMissing() {
        ScenarioConfig sc = validScenario("test-scenario");
        FeederConfig feeder = new FeederConfig();
        feeder.setDatabaseRef("nonexistent");
        sc.setFeeder(feeder);

        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(validEnv(), validRun(), List.of(sc)));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("databaseRef")));
    }

    @Test
    void validate_passesWhenDatabaseRefDefined() {
        EnvironmentConfig env = validEnv();
        DatabaseConfig db = new DatabaseConfig();
        db.setJdbcUrl("jdbc:h2:mem:test");
        env.setDatabases(Map.of("sourceData", db));

        ScenarioConfig sc = validScenario("test-scenario");
        FeederConfig feeder = new FeederConfig();
        feeder.setDatabaseRef("sourceData");
        sc.setFeeder(feeder);

        assertDoesNotThrow(() -> validator.validate(env, validRun(), List.of(sc)));
    }

    @Test
    void validate_failsForUnknownWorkloadType() {
        RunConfig run = validRun();
        run.getExecution().getWorkload().setType("teleport-storm");

        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(validEnv(), run, List.of(validScenario("test-scenario"))));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("teleport-storm")));
    }

    @Test
    void validate_failsForMalformedDuration() {
        RunConfig run = validRun();
        run.getExecution().getWorkload().setRampUp("forever");

        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(validEnv(), run, List.of(validScenario("test-scenario"))));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("rampUp")
            && e.contains("Invalid duration")));
    }

    @Test
    void validate_failsWhenIsProductionAndAllowProductionFalse() {
        EnvironmentConfig env = validEnv();
        SafetyConfig safety = new SafetyConfig();
        safety.setAllowProduction(false);
        safety.setProduction(true);
        env.setSafety(safety);

        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(env, validRun(), List.of(validScenario("test-scenario"))));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("isProduction=true")));
    }

    @Test
    void validate_passesWhenIsProductionAndAllowProductionTrue() {
        EnvironmentConfig env = validEnv();
        env.setId("safe-prod");
        SafetyConfig safety = new SafetyConfig();
        safety.setAllowProduction(true);
        safety.setProduction(true);
        env.setSafety(safety);

        assertDoesNotThrow(() ->
            validator.validate(env, validRun(), List.of(validScenario("test-scenario"))));
    }

    @Test
    void validate_failsForMutatingScenarioWithoutApproval() {
        EnvironmentConfig env = validEnv();
        env.getSafety().setRequireApprovalForMutatingRequests(true);

        ScenarioConfig sc = validScenario("mutating-scn");
        ScenarioSafetyConfig scSafety = new ScenarioSafetyConfig();
        scSafety.setMutating(true);
        sc.setSafety(scSafety);

        RunConfig run = validRun();
        ScenarioRef ref = new ScenarioRef();
        ref.setId("mutating-scn");
        ref.setEnabled(true);
        run.setScenarios(List.of(ref));

        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(env, run, List.of(sc)));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("mutating-scn") && e.contains("approval")));
    }

    @Test
    void validate_passesForMutatingScenarioWithApproval() {
        EnvironmentConfig env = validEnv();
        env.getSafety().setRequireApprovalForMutatingRequests(true);

        ScenarioConfig sc = validScenario("mutating-scn");
        ScenarioSafetyConfig scSafety = new ScenarioSafetyConfig();
        scSafety.setMutating(true);
        sc.setSafety(scSafety);

        RunConfig run = validRun();
        ScenarioRef ref = new ScenarioRef();
        ref.setId("mutating-scn");
        ref.setEnabled(true);
        run.setScenarios(List.of(ref));
        RunSafetyConfig safety = new RunSafetyConfig();
        safety.setApproveMutatingRequests(true);
        run.setSafety(safety);

        assertDoesNotThrow(() -> validator.validate(env, run, List.of(sc)));
    }

    @Test
    void validate_failsWhenFailIfEnvAllowsProductionAndEnvAllows() {
        EnvironmentConfig env = validEnv();
        env.getSafety().setAllowProduction(true);

        RunConfig run = validRun();
        RunSafetyConfig safety = new RunSafetyConfig();
        safety.setFailIfEnvironmentAllowsProduction(true);
        run.setSafety(safety);

        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(env, run, List.of(validScenario("test-scenario"))));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("failIfEnvironmentAllowsProduction")));
    }

    @Test
    void validate_failsWhenScenarioOverridesAuthorizationWithoutPermission() {
        ScenarioConfig sc = validScenario("test-scenario");
        sc.setHeaders(Map.of("Authorization", "Bearer custom"));

        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(validEnv(), validRun(), List.of(sc)));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("Authorization")));
    }

    @Test
    void validate_failsWhenScenarioEndpointRefNotInBaseUrls() {
        ScenarioConfig sc = validScenario("test-scenario");
        sc.setEndpointRef("unknown-service");

        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(validEnv(), validRun(), List.of(sc)));
        assertTrue(ex.getErrors().stream().anyMatch(
            e -> e.contains("endpointRef") && e.contains("unknown-service")),
            "Expected unknown endpointRef error: " + ex.getErrors());
    }

    @Test
    void validate_passesWhenScenarioEndpointRefMatchesBaseUrls() {
        EnvironmentConfig env = validEnv();
        env.setBaseUrls(Map.of("rest", "http://localhost:8080", "graphql", "http://localhost:9090"));
        ScenarioConfig sc = validScenario("test-scenario");
        sc.setEndpointRef("graphql");

        assertDoesNotThrow(() -> validator.validate(env, validRun(), List.of(sc)));
    }

    @Test
    void validate_failsWhenStepEndpointRefNotInBaseUrls() {
        ScenarioConfig sc = validScenario("test-scenario");
        StepConfig step = new StepConfig();
        step.setName("call-other-service");
        step.setEndpointRef("unknown-service");
        sc.setSteps(List.of(step));

        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(validEnv(), validRun(), List.of(sc)));
        assertTrue(ex.getErrors().stream().anyMatch(
            e -> e.contains("endpointRef") && e.contains("unknown-service")),
            "Expected unknown step endpointRef error: " + ex.getErrors());
    }

    @Test
    void validate_reportsUnresolvedDatabaseCredentials() {
        EnvironmentConfig env = validEnv();
        DatabaseConfig db = new DatabaseConfig();
        db.setJdbcUrl("jdbc:h2:mem:test");
        CredentialConfig user = new CredentialConfig();
        user.setSource("env");
        user.setEnvVar("RAMPAGE_TEST_UNSET_USER_QQQ");
        db.setUsername(user);
        CredentialConfig pwd = new CredentialConfig();
        pwd.setSource("env");
        pwd.setEnvVar("RAMPAGE_TEST_UNSET_PWD_QQQ");
        db.setPassword(pwd);
        env.setDatabases(Map.of("sourceData", db));

        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(env, validRun(), List.of(validScenario("test-scenario"))));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("environment.databases.sourceData.username")));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("environment.databases.sourceData.password")));
    }
}
