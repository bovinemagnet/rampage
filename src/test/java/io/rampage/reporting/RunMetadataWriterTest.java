package io.rampage.reporting;

import io.rampage.config.model.EnvironmentConfig;
import io.rampage.config.model.ReportingConfig;
import io.rampage.config.model.RunConfig;
import io.rampage.config.model.ScenarioConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RunMetadataWriterTest {
    private final RunMetadataWriter writer = new RunMetadataWriter();

    @Test
    void write_createsJsonFile(@TempDir Path tempDir) throws IOException {
        RunConfig run = new RunConfig();
        run.setId("test-run");
        run.setName("Test Run");

        EnvironmentConfig env = new EnvironmentConfig();
        env.setId("test");
        env.setName("Test");

        ScenarioConfig sc = new ScenarioConfig();
        sc.setId("sc-1");
        sc.setName("Scenario 1");

        writer.write(run, env, List.of(sc), tempDir.toString());

        Path output = tempDir.resolve("run-metadata.json");
        assertTrue(Files.exists(output));
        String content = Files.readString(output);
        assertTrue(content.contains("test-run"));
        assertTrue(content.contains("test"));
    }

    @Test
    void write_includesGitBranchAndCommit(@TempDir Path tempDir) throws IOException {
        RunConfig run = new RunConfig();
        run.setId("r1");
        run.setName("r1");
        EnvironmentConfig env = new EnvironmentConfig();
        env.setId("test");

        writer.write(run, env, List.of(), tempDir.toString());

        String content = Files.readString(tempDir.resolve("run-metadata.json"));
        assertTrue(content.contains("gitCommit"));
        assertTrue(content.contains("gitBranch"));
    }

    @Test
    void write_reflectsRedactFlag(@TempDir Path tempDir) throws IOException {
        RunConfig run = new RunConfig();
        run.setId("r1");
        run.setName("r1");
        ReportingConfig reporting = new ReportingConfig();
        reporting.setRedactSecrets(false);
        run.setReporting(reporting);

        EnvironmentConfig env = new EnvironmentConfig();
        env.setId("test");

        writer.write(run, env, List.of(), tempDir.toString());

        String content = Files.readString(tempDir.resolve("run-metadata.json"));
        assertTrue(content.contains("\"redactSecretsEnabled\" : false"),
            "Should reflect the flag honestly. Got: " + content);
    }

    @Test
    void write_includesScenarioTags(@TempDir Path tempDir) throws IOException {
        RunConfig run = new RunConfig();
        run.setId("r1");
        run.setName("r1");
        EnvironmentConfig env = new EnvironmentConfig();
        env.setId("test");
        ScenarioConfig sc = new ScenarioConfig();
        sc.setId("sc-1");
        sc.setName("Scenario 1");
        sc.setTags(List.of("graphql", "read-only"));

        writer.write(run, env, List.of(sc), tempDir.toString());

        String content = Files.readString(tempDir.resolve("run-metadata.json"));
        assertTrue(content.contains("graphql"));
        assertTrue(content.contains("read-only"));
    }
}
