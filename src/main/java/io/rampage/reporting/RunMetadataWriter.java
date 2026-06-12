package io.rampage.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.rampage.config.model.AssertionsConfig;
import io.rampage.config.model.EnvironmentConfig;
import io.rampage.config.model.RateConfig;
import io.rampage.config.model.RunConfig;
import io.rampage.config.model.ScenarioAssertionConfig;
import io.rampage.config.model.ScenarioConfig;
import io.rampage.config.model.ScenarioRef;
import io.rampage.config.model.WorkloadConfig;
import io.rampage.factory.WorkloadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/**
 * Writes a {@code run-metadata.json} file capturing the resolved configuration,
 * effective workloads, feeder row counts, git provenance, and assertion configuration
 * for a Gatling run. The file is written to a caller-supplied output directory and is
 * intended to be consumed by the console ingestor after metadata promotion.
 */
public class RunMetadataWriter {
    private static final Logger log = LoggerFactory.getLogger(RunMetadataWriter.class);
    private final ObjectMapper mapper;
    private final String gitCommit;
    private final String gitBranch;

    /**
     * Constructs a {@code RunMetadataWriter}, capturing the current git short commit hash
     * and branch name by invoking {@code git} as a subprocess. If git is unavailable, both
     * values default to {@code "unknown"}.
     */
    public RunMetadataWriter() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.gitCommit = readGit("rev-parse", "--short", "HEAD");
        this.gitBranch = readGit("rev-parse", "--abbrev-ref", "HEAD");
    }

    /**
     * Writes run metadata using the current instant as the start time and an empty
     * feeder row counts map. Delegates to
     * {@link #write(RunConfig, EnvironmentConfig, List, String, Instant, Map)}.
     *
     * @param run       the run configuration
     * @param env       the environment configuration
     * @param scenarios the resolved scenario configurations
     * @param outputDir the directory path into which {@code run-metadata.json} is written
     */
    public void write(RunConfig run, EnvironmentConfig env, List<ScenarioConfig> scenarios, String outputDir) {
        write(run, env, scenarios, outputDir, Instant.now(), Map.of());
    }

    /**
     * Writes run metadata using the supplied start time and an empty feeder row counts map.
     * Delegates to {@link #write(RunConfig, EnvironmentConfig, List, String, Instant, Map)}.
     *
     * @param run        the run configuration
     * @param env        the environment configuration
     * @param scenarios  the resolved scenario configurations
     * @param outputDir  the directory path into which {@code run-metadata.json} is written
     * @param startedAt  the instant the run was started
     */
    public void write(RunConfig run, EnvironmentConfig env, List<ScenarioConfig> scenarios,
                      String outputDir, Instant startedAt) {
        write(run, env, scenarios, outputDir, startedAt, Map.of());
    }

    /**
     * Writes {@code run-metadata.json} to {@code outputDir}, combining run/environment
     * identifiers, git provenance, per-scenario effective workloads, feeder row counts,
     * and effective assertion configuration.
     *
     * @param run              the run configuration
     * @param env              the environment configuration
     * @param scenarios        the resolved scenario configurations
     * @param outputDir        the directory path into which the file is written;
     *                         the directory is created if it does not exist
     * @param startedAt        the instant the run was started
     * @param feederRowCounts  a map from scenario id to the number of feeder rows loaded;
     *                         entries are included only for scenarios present in the map
     */
    public void write(RunConfig run, EnvironmentConfig env, List<ScenarioConfig> scenarios,
                      String outputDir, Instant startedAt, Map<String, Object> feederRowCounts) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("runId", run.getId());
        metadata.put("runName", run.getName());
        metadata.put("environment", env.getId());
        metadata.put("startedAt", startedAt.toString());
        metadata.put("gitCommit", gitCommit);
        metadata.put("gitBranch", gitBranch);
        boolean redactEnabled = run.getReporting() == null || run.getReporting().isRedactSecrets();
        metadata.put("redactSecretsEnabled", redactEnabled);

        double totalRate = 0;
        int totalUsers = 0;
        List<Map<String, Object>> scenarioList = new ArrayList<>();
        for (ScenarioConfig sc : scenarios) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("id", sc.getId());
            s.put("name", sc.getName());
            if (sc.getTags() != null) {
                s.put("tags", sc.getTags());
            }
            WorkloadConfig effective = WorkloadFactory.effectiveWorkload(run, sc);
            s.put("effectiveWorkload", workloadSummary(effective, sc));
            if (feederRowCounts != null && feederRowCounts.containsKey(sc.getId())) {
                s.put("feederRowCount", feederRowCounts.get(sc.getId()));
            }
            Map<String, Object> sa = scenarioAssertionSummary(run.getAssertions(), sc.getId());
            if (sa != null) s.put("effectiveAssertions", sa);
            if (effective.getRate() != null) {
                totalRate += effective.getRate().getTo();
            }
            if (effective.getUsers() > 0) {
                totalUsers += effective.getUsers();
            }
            scenarioList.add(s);
        }
        metadata.put("scenarios", scenarioList);

        Map<String, Object> counts = new LinkedHashMap<>();
        int enabled = 0;
        int disabled = 0;
        if (run.getScenarios() != null) {
            for (ScenarioRef ref : run.getScenarios()) {
                if (ref.isEnabled()) enabled++; else disabled++;
            }
        }
        counts.put("enabled", enabled);
        counts.put("disabled", disabled);
        counts.put("totalRate", totalRate);
        counts.put("totalUsers", totalUsers);
        metadata.put("scenarioCounts", counts);

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

    private static Map<String, Object> workloadSummary(WorkloadConfig wc, ScenarioConfig sc) {
        if (wc == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", wc.getType());
        map.put("rampUp", wc.getRampUp());
        map.put("holdFor", wc.getHoldFor());
        map.put("duration", wc.getDuration());
        if (wc.getRate() != null) {
            Map<String, Object> rate = new LinkedHashMap<>();
            RateConfig r = wc.getRate();
            rate.put("unit", r.getUnit());
            rate.put("from", r.getFrom());
            rate.put("to", r.getTo());
            map.put("rate", rate);
        }
        if (wc.getUsers() > 0) {
            map.put("users", wc.getUsers());
        }
        boolean overridden = sc.getWorkload() != null && !sc.getWorkload().isInheritFromRun();
        map.put("source", overridden ? "scenario-override" : "run");
        return map;
    }

    private static Map<String, Object> scenarioAssertionSummary(AssertionsConfig assertions, String scenarioId) {
        if (assertions == null || assertions.getScenarios() == null || scenarioId == null) return null;
        ScenarioAssertionConfig sa = assertions.getScenarios().get(scenarioId);
        if (sa == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("maxResponseTimeP95Millis", sa.getMaxResponseTimeP95Millis());
        map.put("maxErrorPercentage", sa.getMaxErrorPercentage());
        return map;
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
