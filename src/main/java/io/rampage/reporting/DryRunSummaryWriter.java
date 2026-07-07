package io.rampage.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.rampage.config.model.EnvironmentConfig;
import io.rampage.config.model.RunConfig;
import io.rampage.config.model.ScenarioConfig;
import io.rampage.config.model.ScenarioRef;
import io.rampage.config.model.WorkloadConfig;
import io.rampage.factory.WorkloadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds and writes a dry-run summary JSON that describes what a Gatling run
 * <em>would</em> execute without actually triggering load. The summary captures
 * effective workloads, global assertions, and enabled/disabled scenario counts
 * derived from the resolved configuration.
 */
public class DryRunSummaryWriter {
    private static final Logger log = LoggerFactory.getLogger(DryRunSummaryWriter.class);
    private final ObjectMapper mapper;

    /**
     * Constructs a {@code DryRunSummaryWriter} with indented JSON output enabled.
     */
    public DryRunSummaryWriter() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Builds the dry-run summary as an ordered map. The map includes mode, generation
     * timestamp, environment and run identifiers, per-scenario effective workloads, global
     * assertions, and scenario enabled/disabled counts.
     *
     * @param env       the environment configuration; may be null
     * @param run       the run configuration; may be null
     * @param scenarios the resolved scenario configurations; may be null
     * @return an ordered map representing the dry-run summary
     */
    public Map<String, Object> buildSummary(EnvironmentConfig env, RunConfig run, List<ScenarioConfig> scenarios) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("mode", "dry-run");
        summary.put("generatedAt", Instant.now().toString());
        summary.put("environment", env != null ? env.getId() : null);
        summary.put("runId", run != null ? run.getId() : null);
        summary.put("runName", run != null ? run.getName() : null);

        List<Map<String, Object>> scenarioSummaries = new ArrayList<>();
        if (scenarios != null && run != null) {
            for (ScenarioConfig sc : scenarios) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", sc.getId());
                entry.put("name", sc.getName());
                entry.put("tags", sc.getTags());

                WorkloadConfig effective = WorkloadFactory.effectiveWorkload(run, sc);
                Map<String, Object> workload = new LinkedHashMap<>();
                workload.put("type", effective.getType());
                workload.put("rampUp", effective.getRampUp());
                workload.put("holdFor", effective.getHoldFor());
                workload.put("duration", effective.getDuration());
                if (effective.getRate() != null) {
                    Map<String, Object> rate = new LinkedHashMap<>();
                    rate.put("unit", effective.getRate().getUnit());
                    rate.put("from", effective.getRate().getFrom());
                    rate.put("to", effective.getRate().getTo());
                    workload.put("rate", rate);
                }
                workload.put("users", effective.getUsers());
                workload.put("source", scenarioWorkloadOverridden(sc) ? "scenario-override" : "run");
                entry.put("effectiveWorkload", workload);

                scenarioSummaries.add(entry);
            }
        }
        summary.put("scenarios", scenarioSummaries);

        Map<String, Object> assertions = new LinkedHashMap<>();
        if (run != null && run.getAssertions() != null && run.getAssertions().getGlobal() != null) {
            assertions.put("maxResponseTimeP95Millis", run.getAssertions().getGlobal().getMaxResponseTimeP95Millis());
            assertions.put("maxResponseTimeP99Millis", run.getAssertions().getGlobal().getMaxResponseTimeP99Millis());
            assertions.put("maxErrorPercentage", run.getAssertions().getGlobal().getMaxErrorPercentage());
        }
        summary.put("globalAssertions", assertions);

        if (run != null && run.getScenarios() != null) {
            int enabled = (int) run.getScenarios().stream().filter(ScenarioRef::isEnabled).count();
            int disabled = run.getScenarios().size() - enabled;
            Map<String, Object> counts = new LinkedHashMap<>();
            counts.put("enabled", enabled);
            counts.put("disabled", disabled);
            summary.put("scenarioCounts", counts);
        }

        return summary;
    }

    /**
     * Writes the dry-run summary to {@code dry-run-summary.json} inside {@code outputDir}.
     * The directory is created if it does not exist.
     *
     * @param env       the environment configuration
     * @param run       the run configuration
     * @param scenarios the resolved scenario configurations
     * @param outputDir the directory path into which the summary file is written
     * @throws IOException if the output directory or summary file cannot be written;
     *                     propagated so the dry-run caller can fail rather than
     *                     reporting success with a missing artefact
     */
    public void write(EnvironmentConfig env, RunConfig run, List<ScenarioConfig> scenarios, String outputDir)
            throws IOException {
        Map<String, Object> summary = buildSummary(env, run, scenarios);
        Path dir = Path.of(outputDir);
        Files.createDirectories(dir);
        Path output = dir.resolve("dry-run-summary.json");
        mapper.writeValue(output.toFile(), summary);
        log.info("Dry-run summary written to: {}", output);
    }

    private static boolean scenarioWorkloadOverridden(ScenarioConfig sc) {
        return sc != null && sc.getWorkload() != null && !sc.getWorkload().isInheritFromRun();
    }
}
