package io.rampage.factory;

import io.rampage.config.model.EnvironmentConfig;
import io.rampage.config.model.RunConfig;
import io.rampage.config.model.ScenarioConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {
    private ConfigLoader configLoader;

    @BeforeEach
    void setUp() {
        configLoader = new ConfigLoader();
    }

    @Test
    void loadEnvironment_parsesCorrectly() {
        EnvironmentConfig config = configLoader.loadEnvironment("test-environment.yaml");
        assertNotNull(config);
        assertNotNull(config.getEnvironment());
        assertEquals("test", config.getEnvironment().getName());
        assertEquals("http://localhost:9090", config.getEnvironment().getBaseUrl());
    }

    @Test
    void loadEnvironment_hasTimeouts() {
        EnvironmentConfig config = configLoader.loadEnvironment("test-environment.yaml");
        assertNotNull(config.getEnvironment().getTimeouts());
        assertEquals(1000L, config.getEnvironment().getTimeouts().getConnectionTimeoutMs());
        assertEquals(5000L, config.getEnvironment().getTimeouts().getReadTimeoutMs());
    }

    @Test
    void loadEnvironment_hasSafety() {
        EnvironmentConfig config = configLoader.loadEnvironment("test-environment.yaml");
        assertNotNull(config.getEnvironment().getSafety());
        assertTrue(config.getEnvironment().getSafety().isEnabled());
        assertEquals(50.0, config.getEnvironment().getSafety().getMaxUsersPerSecond());
    }

    @Test
    void loadRun_parsesCorrectly() {
        RunConfig config = configLoader.loadRun("test-run.yaml");
        assertNotNull(config);
        assertNotNull(config.getRun());
        assertEquals("test-run", config.getRun().getName());
        assertFalse(config.getRun().getScenarios().isEmpty());
        assertEquals("test-scenario", config.getRun().getScenarios().get(0));
    }

    @Test
    void loadRun_hasWorkload() {
        RunConfig config = configLoader.loadRun("test-run.yaml");
        assertNotNull(config.getRun().getWorkload());
        assertEquals("ramp-and-hold", config.getRun().getWorkload().getModel());
        assertEquals(5.0, config.getRun().getWorkload().getTargetCallsPerSecond());
    }

    @Test
    void loadScenario_parsesCorrectly() {
        ScenarioConfig config = configLoader.loadScenario("scenarios/test-scenario.yaml");
        assertNotNull(config);
        assertNotNull(config.getScenario());
        assertEquals("test-scenario", config.getScenario().getName());
    }

    @Test
    void loadScenario_hasGraphql() {
        ScenarioConfig config = configLoader.loadScenario("scenarios/test-scenario.yaml");
        assertNotNull(config.getScenario().getGraphql());
        assertEquals("/graphql", config.getScenario().getGraphql().getEndpoint());
        assertEquals("queries/get-user.graphql", config.getScenario().getGraphql().getQueryFile());
    }

    @Test
    void loadScenario_hasFeeder() {
        ScenarioConfig config = configLoader.loadScenario("scenarios/test-scenario.yaml");
        assertNotNull(config.getScenario().getFeeder());
        assertEquals("sql", config.getScenario().getFeeder().getType());
        assertEquals(10, config.getScenario().getFeeder().getPreload());
    }

    @Test
    void loadEnvironment_throwsOnMissingFile() {
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> configLoader.loadEnvironment("nonexistent.yaml"));
        assertTrue(ex.getMessage().contains("nonexistent.yaml"));
    }

    @Test
    void loadRun_throwsOnMissingFile() {
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> configLoader.loadRun("nonexistent-run.yaml"));
        assertTrue(ex.getMessage().contains("nonexistent-run.yaml"));
    }
}
