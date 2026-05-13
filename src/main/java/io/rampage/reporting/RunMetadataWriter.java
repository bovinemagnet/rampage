package io.rampage.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.rampage.config.model.EnvironmentConfig;
import io.rampage.config.model.RunConfig;
import io.rampage.config.model.ScenarioConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

public class RunMetadataWriter {
    private static final Logger log = LoggerFactory.getLogger(RunMetadataWriter.class);
    private final ObjectMapper mapper;

    public RunMetadataWriter() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void write(RunConfig run, EnvironmentConfig env, List<ScenarioConfig> scenarios, String outputDir) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("runId", run.getId());
        metadata.put("runName", run.getName());
        metadata.put("environment", env.getId());
        metadata.put("startedAt", Instant.now().toString());
        metadata.put("gitCommit", getGitCommit());
        metadata.put("redacted", true);

        List<Map<String, String>> scenarioList = new ArrayList<>();
        for (ScenarioConfig sc : scenarios) {
            Map<String, String> s = new LinkedHashMap<>();
            s.put("id", sc.getId());
            s.put("name", sc.getName());
            scenarioList.add(s);
        }
        metadata.put("scenarios", scenarioList);

        if (run.getMetadata() != null) {
            Map<String, Object> runMeta = new LinkedHashMap<>();
            runMeta.put("owner", run.getMetadata().getOwner());
            runMeta.put("application", run.getMetadata().getApplication());
            runMeta.put("service", run.getMetadata().getService());
            runMeta.put("description", run.getMetadata().getDescription());
            metadata.put("runMetadata", runMeta);
        }

        try {
            Path dir = Path.of(outputDir);
            Files.createDirectories(dir);
            Path outputFile = dir.resolve("run-metadata.json");
            mapper.writeValue(outputFile.toFile(), metadata);
            log.info("Run metadata written to: {}", outputFile);
        } catch (IOException e) {
            log.error("Failed to write run metadata: {}", e.getMessage());
        }
    }

    private String getGitCommit() {
        try {
            Process process = new ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                .redirectErrorStream(true)
                .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String commit = reader.readLine();
                return commit != null ? commit.trim() : "unknown";
            }
        } catch (IOException e) {
            log.debug("Could not get git commit: {}", e.getMessage());
            return "unknown";
        }
    }
}
