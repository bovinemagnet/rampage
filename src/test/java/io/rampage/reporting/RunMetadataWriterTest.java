package io.rampage.reporting;

import io.rampage.config.model.*;
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
}
