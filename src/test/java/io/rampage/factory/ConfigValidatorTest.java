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
}
