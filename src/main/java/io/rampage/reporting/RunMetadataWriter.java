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
    private final String gitCommit;
    private final String gitBranch;

    public RunMetadataWriter() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.gitCommit = readGit("rev-parse", "--short", "HEAD");
        this.gitBranch = readGit("rev-parse", "--abbrev-ref", "HEAD");
    }

    public void write(RunConfig run, EnvironmentConfig env, List<ScenarioConfig> scenarios, String outputDir) {
        write(run, env, scenarios, outputDir, Instant.now());
    }

    public void write(RunConfig run, EnvironmentConfig env, List<ScenarioConfig> scenarios,
                      String outputDir, Instant startedAt) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("runId", run.getId());
        metadata.put("runName", run.getName());
        metadata.put("environment", env.getId());
        metadata.put("startedAt", startedAt.toString());
        metadata.put("gitCommit", gitCommit);
        metadata.put("gitBranch", gitBranch);
        boolean redactEnabled = run.getReporting() == null || run.getReporting().isRedactSecrets();
        metadata.put("redactSecretsEnabled", redactEnabled);

        List<Map<String, Object>> scenarioList = new ArrayList<>();
        for (ScenarioConfig sc : scenarios) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("id", sc.getId());
            s.put("name", sc.getName());
            if (sc.getTags() != null) {
                s.put("tags", sc.getTags());
            }
            scenarioList.add(s);
        }
        metadata.put("scenarios", scenarioList);

        if (run.getMetadata() != null) {
            Map<String, Object> runMeta = new LinkedHashMap<>();
            runMeta.put("owner", run.getMetadata().getOwner());
            runMeta.put("application", run.getMetadata().getApplication());
            runMeta.put("service", run.getMetadata().getService());
            runMeta.put("changeReference", run.getMetadata().getChangeReference());
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

    private static String readGit(String... args) {
        try {
            List<String> command = new ArrayList<>(args.length + 1);
            command.add("git");
            command.addAll(Arrays.asList(args));
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String value = reader.readLine();
                return value != null ? value.trim() : "unknown";
            }
        } catch (IOException e) {
            log.debug("Could not run git {}: {}", String.join(" ", args), e.getMessage());
            return "unknown";
        }
    }
}
