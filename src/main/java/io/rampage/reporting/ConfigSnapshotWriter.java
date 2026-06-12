package io.rampage.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.rampage.config.model.EnvironmentConfig;
import io.rampage.config.model.RunConfig;
import io.rampage.config.model.ScenarioConfig;
import io.rampage.config.model.WorkloadConfig;
import io.rampage.factory.SecretResolver;
import io.rampage.factory.WorkloadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Writes the merged, resolved env/run/scenario configuration as a YAML snapshot for
 * post-run inspection. Secret-sourced values present in {@code SecretResolver} are
 * replaced with {@code ***REDACTED***} before serialisation.
 */
public class ConfigSnapshotWriter {
    private static final Logger log = LoggerFactory.getLogger(ConfigSnapshotWriter.class);
    private static final String REDACTED = "***REDACTED***";

    private final ObjectMapper mapper;

    /**
     * Constructs a {@code ConfigSnapshotWriter} with a YAML mapper configured to suppress
     * the document-start marker and null map values.
     */
    public ConfigSnapshotWriter() {
        YAMLFactory yaml = YAMLFactory.builder()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .build();
        this.mapper = new ObjectMapper(yaml);
        this.mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
        this.mapper.findAndRegisterModules();
    }

    /**
     * Builds the merged configuration snapshot as an ordered map suitable for
     * serialisation to YAML. Each scenario entry includes the scenario config and
     * its effective workload as resolved by {@code WorkloadFactory}.
     *
     * @param env       the environment configuration; may be null
     * @param run       the run configuration; may be null
     * @param scenarios the resolved scenario configurations; may be null or empty
     * @return an ordered map with keys {@code environment}, {@code run}, and {@code scenarios}
     */
    public Map<String, Object> buildSnapshot(EnvironmentConfig env, RunConfig run, List<ScenarioConfig> scenarios) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("environment", env);
        snapshot.put("run", run);

        List<Map<String, Object>> sc = new ArrayList<>();
        if (scenarios != null) {
            for (ScenarioConfig s : scenarios) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("config", s);
                if (run != null) {
                    WorkloadConfig effective = WorkloadFactory.effectiveWorkload(run, s);
                    entry.put("effectiveWorkload", effective);
                }
                sc.add(entry);
            }
        }
        snapshot.put("scenarios", sc);
        return snapshot;
    }

    /**
     * Serialises the configuration snapshot to {@code config-snapshot.yaml} inside
     * {@code outputDir}, optionally replacing sensitive values with {@code ***REDACTED***}.
     *
     * @param env             the environment configuration
     * @param run             the run configuration
     * @param scenarios       the resolved scenario configurations
     * @param outputDir       the directory path into which the snapshot file is written;
     *                        the directory is created if it does not exist
     * @param secretResolver  the resolver whose sensitive values should be redacted;
     *                        ignored when {@code redactSecrets} is false or this is null
     * @param redactSecrets   when true, any sensitive value known to {@code secretResolver}
     *                        is replaced with {@code ***REDACTED***} in the output
     */
    public void write(EnvironmentConfig env, RunConfig run, List<ScenarioConfig> scenarios,
                      String outputDir, SecretResolver secretResolver, boolean redactSecrets) {
        try {
            Path dir = Path.of(outputDir);
            Files.createDirectories(dir);
            Path output = dir.resolve("config-snapshot.yaml");
            String yaml = mapper.writeValueAsString(buildSnapshot(env, run, scenarios));
            if (redactSecrets && secretResolver != null) {
                yaml = redact(yaml, secretResolver.getSensitiveValues());
            }
            Files.writeString(output, yaml);
            log.info("Config snapshot written to: {}", output);
        } catch (IOException e) {
            log.error("Failed to write config snapshot: {}", e.getMessage());
        }
    }

    static String redact(String content, Set<String> sensitiveValues) {
        if (sensitiveValues == null || sensitiveValues.isEmpty()) return content;
        String result = content;
        // Replace longest-first to avoid partial overlaps.
        List<String> ordered = new ArrayList<>(sensitiveValues);
        ordered.sort((a, b) -> Integer.compare(b.length(), a.length()));
        for (String value : ordered) {
            if (value == null || value.isEmpty()) continue;
            result = result.replace(value, REDACTED);
        }
        return result;
    }
}
