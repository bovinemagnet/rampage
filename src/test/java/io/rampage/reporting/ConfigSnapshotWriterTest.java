package io.rampage.reporting;

import io.rampage.config.model.CredentialConfig;
import io.rampage.config.model.DatabaseConfig;
import io.rampage.config.model.EnvironmentConfig;
import io.rampage.config.model.ExecutionConfig;
import io.rampage.config.model.RateConfig;
import io.rampage.config.model.RunConfig;
import io.rampage.config.model.ScenarioConfig;
import io.rampage.config.model.WorkloadConfig;
import io.rampage.factory.SecretResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConfigSnapshotWriterTest {
    private final ConfigSnapshotWriter writer = new ConfigSnapshotWriter();

    private RunConfig run() {
        RunConfig run = new RunConfig();
        run.setId("test-run");
        run.setName("Test Run");
        ExecutionConfig exec = new ExecutionConfig();
        WorkloadConfig wc = new WorkloadConfig();
        wc.setType("ramp-and-hold");
        RateConfig r = new RateConfig();
        r.setFrom(0);
        r.setTo(10);
        wc.setRate(r);
        wc.setRampUp("30s");
        wc.setHoldFor("60s");
        exec.setWorkload(wc);
        run.setExecution(exec);
        return run;
    }

    @Test
    void buildSnapshot_includesEnvRunAndScenarios() {
        EnvironmentConfig env = new EnvironmentConfig();
        env.setId("local");
        ScenarioConfig sc = new ScenarioConfig();
        sc.setId("sc-1");

        Map<String, Object> snapshot = writer.buildSnapshot(env, run(), List.of(sc));

        assertSame(env, snapshot.get("environment"));
        assertNotNull(snapshot.get("run"));
        assertEquals(1, ((List<?>) snapshot.get("scenarios")).size());
    }

    @Test
    void buildSnapshot_includesEffectiveWorkloadPerScenario() {
        EnvironmentConfig env = new EnvironmentConfig();
        env.setId("local");
        ScenarioConfig sc = new ScenarioConfig();
        sc.setId("sc-1");

        Map<String, Object> snapshot = writer.buildSnapshot(env, run(), List.of(sc));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> scenarios = (List<Map<String, Object>>) snapshot.get("scenarios");
        Map<String, Object> entry = scenarios.get(0);
        assertNotNull(entry.get("effectiveWorkload"));
        WorkloadConfig wl = (WorkloadConfig) entry.get("effectiveWorkload");
        assertEquals("ramp-and-hold", wl.getType());
    }

    @Test
    void redact_replacesSensitiveValues() {
        String input = "password: my-secret-token\nusername: user1";
        String redacted = ConfigSnapshotWriter.redact(input, Set.of("my-secret-token"));

        assertTrue(redacted.contains("***REDACTED***"));
        assertFalse(redacted.contains("my-secret-token"));
    }

    @Test
    void redact_preservesNonSensitiveContent() {
        String input = "key: value\nother: stuff";
        String redacted = ConfigSnapshotWriter.redact(input, Set.of());
        assertEquals(input, redacted);
    }

    @Test
    void write_producesYamlFile(@TempDir Path tempDir) throws IOException {
        EnvironmentConfig env = new EnvironmentConfig();
        env.setId("test");
        DatabaseConfig db = new DatabaseConfig();
        db.setJdbcUrl("jdbc:h2:mem:t");
        CredentialConfig pwd = new CredentialConfig();
        pwd.setSource("plain");
        pwd.setValue("my-secret-pwd");
        db.setPassword(pwd);
        env.setDatabases(Map.of("source", db));

        ScenarioConfig sc = new ScenarioConfig();
        sc.setId("s1");

        SecretResolver resolver = new SecretResolver();
        // Trigger tracking of the sensitive value
        resolver.resolveCredential(pwd, "test.password");

        writer.write(env, run(), List.of(sc), tempDir.toString(), resolver, true);

        Path output = tempDir.resolve("config-snapshot.yaml");
        assertTrue(Files.exists(output));
        String content = Files.readString(output);
        assertTrue(content.contains("***REDACTED***"));
        assertFalse(content.contains("my-secret-pwd"));
    }

    @Test
    void write_doesNotRedactWhenDisabled(@TempDir Path tempDir) throws IOException {
        EnvironmentConfig env = new EnvironmentConfig();
        env.setId("test");
        DatabaseConfig db = new DatabaseConfig();
        db.setJdbcUrl("jdbc:h2:mem:t");
        CredentialConfig pwd = new CredentialConfig();
        pwd.setSource("plain");
        pwd.setValue("plain-pwd");
        db.setPassword(pwd);
        env.setDatabases(Map.of("source", db));

        SecretResolver resolver = new SecretResolver();
        resolver.resolveCredential(pwd, "test.password");

        writer.write(env, run(), List.of(), tempDir.toString(), resolver, false);

        String content = Files.readString(tempDir.resolve("config-snapshot.yaml"));
        assertTrue(content.contains("plain-pwd"), "When redaction disabled, secret values appear verbatim");
    }
}
