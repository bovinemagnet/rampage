package io.rampage.factory;

import io.rampage.config.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigValidatorTest {
    private ConfigValidator validator;
    private ConfigLoader configLoader;

    @BeforeEach
    void setUp() {
        validator = new ConfigValidator();
        configLoader = new ConfigLoader();
    }

    private EnvironmentConfig validEnv() {
        EnvironmentConfig envConfig = new EnvironmentConfig();
        EnvironmentConfig.Environment env = new EnvironmentConfig.Environment();
        env.setBaseUrl("http://localhost:8080");
        TimeoutConfig timeouts = new TimeoutConfig();
        timeouts.setConnectionTimeoutMs(5000);
        timeouts.setReadTimeoutMs(30000);
        env.setTimeouts(timeouts);
        SafetyConfig safety = new SafetyConfig();
        safety.setMaxUsersPerSecond(100);
        safety.setEnabled(true);
        env.setSafety(safety);
        envConfig.setEnvironment(env);
        return envConfig;
    }

    private RunConfig validRun(double targetCPS) {
        RunConfig runConfig = new RunConfig();
        RunConfig.Run run = new RunConfig.Run();
        run.setName("test-run");
        run.setScenarios(List.of("test-scenario"));
        WorkloadConfig workload = new WorkloadConfig();
        workload.setModel("ramp-and-hold");
        workload.setTargetCallsPerSecond(targetCPS);
        workload.setRampDurationSeconds(10);
        workload.setHoldDurationSeconds(30);
        run.setWorkload(workload);
        runConfig.setRun(run);
        return runConfig;
    }

    private ScenarioConfig validScenario(String name) {
        ScenarioConfig sc = new ScenarioConfig();
        ScenarioConfig.Scenario scenario = new ScenarioConfig.Scenario();
        scenario.setName(name);
        sc.setScenario(scenario);
        return sc;
    }

    @Test
    void validate_passesForValidConfig() {
        assertDoesNotThrow(() ->
            validator.validate(validEnv(), validRun(10), List.of(validScenario("test-scenario"))));
    }

    @Test
    void validate_failsWhenBaseUrlMissing() {
        EnvironmentConfig env = validEnv();
        env.getEnvironment().setBaseUrl(null);
        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(env, validRun(10), List.of(validScenario("test-scenario"))));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("baseUrl")));
    }

    @Test
    void validate_failsWhenBaseUrlEmpty() {
        EnvironmentConfig env = validEnv();
        env.getEnvironment().setBaseUrl("   ");
        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(env, validRun(10), List.of(validScenario("test-scenario"))));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("baseUrl")));
    }

    @Test
    void validate_failsWhenScenariosEmpty() {
        RunConfig run = validRun(10);
        run.getRun().setScenarios(List.of());
        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(validEnv(), run, List.of()));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("scenarios")));
    }

    @Test
    void validate_failsWhenTargetCPSExceedsSafetyLimit() {
        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(validEnv(), validRun(200), List.of(validScenario("test-scenario"))));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("maxUsersPerSecond")));
    }

    @Test
    void validate_failsWhenScenarioNotLoaded() {
        RunConfig run = validRun(10);
        run.getRun().setScenarios(List.of("missing-scenario"));
        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(validEnv(), run, List.of()));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("missing-scenario")));
    }

    @Test
    void validate_listsAllErrors() {
        EnvironmentConfig env = validEnv();
        env.getEnvironment().setBaseUrl(null);
        RunConfig run = validRun(200); // exceeds limit
        run.getRun().setScenarios(List.of("missing-scenario"));
        ConfigValidator.ConfigValidationException ex = assertThrows(
            ConfigValidator.ConfigValidationException.class,
            () -> validator.validate(env, run, List.of()));
        assertTrue(ex.getErrors().size() >= 2, "Should have multiple errors");
    }
}
