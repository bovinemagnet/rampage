package io.rampage.reporting;

import io.rampage.config.model.EnvironmentConfig;
import io.rampage.config.model.ExecutionConfig;
import io.rampage.config.model.RateConfig;
import io.rampage.config.model.RunConfig;
import io.rampage.config.model.ScenarioConfig;
import io.rampage.config.model.ScenarioRef;
import io.rampage.config.model.ScenarioWorkloadConfig;
import io.rampage.config.model.WorkloadConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DryRunSummaryWriterTest {
    private final DryRunSummaryWriter writer = new DryRunSummaryWriter();

    private RunConfig runWithRampAndHold() {
        RunConfig run = new RunConfig();
        run.setId("test-run");
        run.setName("Test Run");
        ExecutionConfig exec = new ExecutionConfig();
        WorkloadConfig wc = new WorkloadConfig();
        wc.setType("ramp-and-hold");
        RateConfig rate = new RateConfig();
        rate.setFrom(0);
        rate.setTo(10);
        wc.setRate(rate);
        wc.setRampUp("60s");
        wc.setHoldFor("5m");
        exec.setWorkload(wc);
        run.setExecution(exec);
        ScenarioRef ref = new ScenarioRef();
        ref.setId("sc-1");
        ref.setEnabled(true);
        run.setScenarios(List.of(ref));
        return run;
    }

    @Test
    void buildSummary_includesRunAndEnvIds() {
        EnvironmentConfig env = new EnvironmentConfig();
        env.setId("test");

        ScenarioConfig sc = new ScenarioConfig();
        sc.setId("sc-1");
        sc.setName("Scenario 1");

        Map<String, Object> summary = writer.buildSummary(env, runWithRampAndHold(), List.of(sc));

        assertEquals("dry-run", summary.get("mode"));
        assertEquals("test", summary.get("environment"));
        assertEquals("test-run", summary.get("runId"));
    }

    @Test
    void buildSummary_includesEffectiveWorkloadFromRun() {
        EnvironmentConfig env = new EnvironmentConfig();
        env.setId("test");
        ScenarioConfig sc = new ScenarioConfig();
        sc.setId("sc-1");

        Map<String, Object> summary = writer.buildSummary(env, runWithRampAndHold(), List.of(sc));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> scenarios = (List<Map<String, Object>>) summary.get("scenarios");
        assertEquals(1, scenarios.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> workload = (Map<String, Object>) scenarios.get(0).get("effectiveWorkload");
        assertEquals("ramp-and-hold", workload.get("type"));
        assertEquals("60s", workload.get("rampUp"));
        assertEquals("5m", workload.get("holdFor"));
        assertEquals("run", workload.get("source"));
    }

    @Test
    void buildSummary_marksScenarioOverrideSource() {
        EnvironmentConfig env = new EnvironmentConfig();
        env.setId("test");

        ScenarioConfig sc = new ScenarioConfig();
        sc.setId("sc-1");
        ScenarioWorkloadConfig swc = new ScenarioWorkloadConfig();
        swc.setInheritFromRun(false);
        swc.setType("constant");
        swc.setHoldFor("30s");
        RateConfig rate = new RateConfig();
        rate.setTo(2.0);
        swc.setRate(rate);
        sc.setWorkload(swc);

        Map<String, Object> summary = writer.buildSummary(env, runWithRampAndHold(), List.of(sc));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> scenarios = (List<Map<String, Object>>) summary.get("scenarios");
        @SuppressWarnings("unchecked")
        Map<String, Object> workload = (Map<String, Object>) scenarios.get(0).get("effectiveWorkload");
        assertEquals("constant", workload.get("type"));
        assertEquals("scenario-override", workload.get("source"));
    }

    @Test
    void write_createsDryRunSummaryFile(@TempDir Path tempDir) throws IOException {
        EnvironmentConfig env = new EnvironmentConfig();
        env.setId("test");
        ScenarioConfig sc = new ScenarioConfig();
        sc.setId("sc-1");
        sc.setName("Scenario 1");

        writer.write(env, runWithRampAndHold(), List.of(sc), tempDir.toString());

        Path output = tempDir.resolve("dry-run-summary.json");
        assertTrue(Files.exists(output));
        String content = Files.readString(output);
        assertTrue(content.contains("dry-run"));
        assertTrue(content.contains("ramp-and-hold"));
    }

    @Test
    void write_propagatesIOExceptionWhenOutputPathUnwritable(@TempDir Path tempDir) throws IOException {
        EnvironmentConfig env = new EnvironmentConfig();
        env.setId("test");
        ScenarioConfig sc = new ScenarioConfig();
        sc.setId("sc-1");

        // A regular file where a directory is expected makes Files.createDirectories fail. The
        // writer must propagate rather than swallow, so the dry-run caller can exit non-zero
        // instead of reporting success with a missing artefact.
        Path fileNotDir = Files.createFile(tempDir.resolve("not-a-dir"));
        String badOutputDir = fileNotDir.resolve("sub").toString();

        assertThrows(IOException.class,
            () -> writer.write(env, runWithRampAndHold(), List.of(sc), badOutputDir));
    }
}
