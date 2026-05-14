package io.rampage.factory;

import io.rampage.config.model.EnvironmentConfig;
import io.rampage.config.model.RunConfig;
import io.rampage.config.model.ScenarioConfig;
import io.rampage.config.model.ScenarioRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
        assertEquals("test", config.getId());
        assertEquals("Test Environment", config.getName());
    }

    @Test
    void loadEnvironment_hasBaseUrls() {
        EnvironmentConfig config = configLoader.loadEnvironment("test-environment.yaml");
        assertNotNull(config.getBaseUrls());
        assertEquals("http://localhost:9090", config.getBaseUrls().get("rest"));
    }

    @Test
    void loadEnvironment_hasHttpConfig() {
        EnvironmentConfig config = configLoader.loadEnvironment("test-environment.yaml");
        assertNotNull(config.getHttp());
        assertEquals(1000L, config.getHttp().getConnectTimeoutMillis());
        assertEquals(5000L, config.getHttp().getRequestTimeoutMillis());
    }

    @Test
    void loadEnvironment_hasSafety() {
        EnvironmentConfig config = configLoader.loadEnvironment("test-environment.yaml");
        assertNotNull(config.getSafety());
        assertFalse(config.getSafety().isAllowProduction());
    }

    @Test
    void loadRun_parsesCorrectly() {
        RunConfig config = configLoader.loadRun("test-run.yaml");
        assertNotNull(config);
        assertEquals("test-run", config.getName());
        assertFalse(config.getScenarios().isEmpty());
        assertEquals("test-scenario", config.getScenarios().get(0).getId());
    }

    @Test
    void loadRun_hasWorkload() {
        RunConfig config = configLoader.loadRun("test-run.yaml");
        assertNotNull(config.getExecution());
        assertNotNull(config.getExecution().getWorkload());
        assertEquals("ramp-and-hold", config.getExecution().getWorkload().getType());
        assertEquals(5.0, config.getExecution().getWorkload().getRate().getTo());
    }

    @Test
    void loadScenario_parsesCorrectly() {
        ScenarioConfig config = configLoader.loadScenario("scenarios/test-scenario.yaml");
        assertNotNull(config);
        assertEquals("test-scenario", config.getId());
        assertEquals("test-scenario", config.getName());
    }

    @Test
    void loadScenario_hasRequest() {
        ScenarioConfig config = configLoader.loadScenario("scenarios/test-scenario.yaml");
        assertNotNull(config.getRequest());
        assertEquals("queries/get-user.graphql", config.getRequest().getGraphqlQueryFile());
    }

    @Test
    void loadScenario_hasFeeder() {
        ScenarioConfig config = configLoader.loadScenario("scenarios/test-scenario.yaml");
        assertNotNull(config.getFeeder());
        assertEquals("jdbc", config.getFeeder().getType());
        assertTrue(config.getFeeder().isPreload());
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

    @Test
    void loadScenario_byRef_resolvesFromFilesystem(@TempDir Path tempDir) throws IOException {
        Path scenarioFile = tempDir.resolve("custom-scenario.yaml");
        Files.writeString(scenarioFile, "id: fs-scenario\nname: From Filesystem\nprotocol: graphql\n");
        ScenarioRef ref = new ScenarioRef();
        ref.setId("fs-scenario");
        ref.setFile(scenarioFile.toString());

        ScenarioConfig config = configLoader.loadScenario(ref);

        assertEquals("fs-scenario", config.getId());
        assertEquals("From Filesystem", config.getName());
    }

    @Test
    void loadScenario_byRef_resolvesFileAsClasspath() {
        ScenarioRef ref = new ScenarioRef();
        ref.setId("test-scenario");
        ref.setFile("scenarios/test-scenario.yaml");

        ScenarioConfig config = configLoader.loadScenario(ref);

        assertEquals("test-scenario", config.getId());
    }

    @Test
    void loadScenario_byRef_fallsBackToIdConventionOnClasspath() {
        ScenarioRef ref = new ScenarioRef();
        ref.setId("test-scenario");
        // No file set — should resolve scenarios/test-scenario.yaml from classpath

        ScenarioConfig config = configLoader.loadScenario(ref);

        assertEquals("test-scenario", config.getId());
    }

    @Test
    void loadScenario_byRef_throwsWithAttemptsWhenUnresolvable() {
        ScenarioRef ref = new ScenarioRef();
        ref.setId("does-not-exist");
        ref.setFile("/nonexistent/path/scenario.yaml");

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> configLoader.loadScenario(ref));

        // The message should include both attempted paths so users can debug
        assertTrue(ex.getMessage().contains("/nonexistent/path/scenario.yaml"),
            "Should mention filesystem path: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("does-not-exist"),
            "Should mention id-based fallback: " + ex.getMessage());
    }
}
