package io.rampage.reporting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the parser against a real Gatling 3.15 {@code index.html} captured during
 * development. The fixture lives at {@code src/test/resources/gatling-report-index.html}
 * and was produced by a 5-second constant-rate run with two configured assertions.
 */
class RunSummaryGeneratorTest {

    private String loadFixture() throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("gatling-report-index.html")) {
            assertNotNull(is, "gatling-report-index.html missing from test resources");
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void parseRequests_findsGlobalAndPerRequestRows() throws Exception {
        List<Map<String, Object>> rows = RunSummaryGenerator.parseRequests(loadFixture());
        assertFalse(rows.isEmpty(), "expected at least the ROOT row");
        Map<String, Object> root = rows.stream()
            .filter(r -> "ROOT".equals(r.get("id"))).findFirst().orElseThrow();
        assertEquals("All Requests", root.get("name"));
        assertEquals(300L, root.get("total"));
        assertEquals(300L, root.get("ok"));
        assertEquals(0L, root.get("ko"));
        assertNotNull(root.get("p95"), "p95 must be populated");
        assertNotNull(root.get("p99"), "p99 must be populated");
        assertNotNull(root.get("rps"), "throughput must be populated");
    }

    @Test
    void parseRequests_alsoIncludesNamedRequestRow() throws Exception {
        List<Map<String, Object>> rows = RunSummaryGenerator.parseRequests(loadFixture());
        boolean hasQuickGet = rows.stream().anyMatch(r -> "Quick GET".equals(r.get("name")));
        assertTrue(hasQuickGet, "expected the per-request row 'Quick GET'");
    }

    @Test
    void parseAssertions_capturesBothConfiguredAssertionsWithCleanText() throws Exception {
        List<Map<String, Object>> assertions = RunSummaryGenerator.parseAssertions(loadFixture());
        assertEquals(2, assertions.size(), "fixture has two assertions");
        for (Map<String, Object> a : assertions) {
            assertEquals("OK", a.get("result"));
        }
        // The fixture has a hidden <span style="display:none">0</span> after the first
        // assertion text and "1" after the second. Either should be stripped, not appended
        // to the description.
        assertEquals("Global: 95th percentile of response time is less than 1000.0",
            assertions.get(0).get("description"));
        assertEquals("Global: percentage of failed events is less than 5.0",
            assertions.get(1).get("description"));
    }

    @Test
    void generate_writesSummaryWithStatusPass(@TempDir Path tmp) throws Exception {
        Path reportRoot = tmp.resolve("reports");
        Path simDir = reportRoot.resolve("rampagesimulation-20260515000946259");
        Files.createDirectories(simDir);
        Files.writeString(simDir.resolve("index.html"), loadFixture());

        Path output = tmp.resolve("run-summary.json");
        Map<String, Object> summary = RunSummaryGenerator.generate(reportRoot, output);

        assertTrue(Files.isRegularFile(output));
        assertEquals("PASS", summary.get("status"));
        assertEquals("rampagesimulation-20260515000946259", summary.get("simulationDir"));
    }

    @Test
    void generate_throwsWhenNoSimulationDirectory(@TempDir Path tmp) {
        Path reportRoot = tmp.resolve("empty");
        try {
            Files.createDirectories(reportRoot);
        } catch (IOException e) {
            fail(e);
        }
        IOException ex = assertThrows(IOException.class,
            () -> RunSummaryGenerator.generate(reportRoot, tmp.resolve("out.json")));
        assertTrue(ex.getMessage().contains("rampagesimulation-"));
    }

    @Test
    void summarise_parsesNamedSimulationDirectoryWithoutWritingJson(@TempDir Path tmp) throws Exception {
        Path simDir = tmp.resolve("rampagesimulation-20260515000946259");
        Files.createDirectories(simDir);
        Files.writeString(simDir.resolve("index.html"), loadFixture());

        Map<String, Object> summary = RunSummaryGenerator.summarise(simDir);

        assertEquals("PASS", summary.get("status"));
        assertEquals("rampagesimulation-20260515000946259", summary.get("simulationDir"));
        assertFalse(((List<?>) summary.get("requests")).isEmpty());
        assertEquals(2, ((List<?>) summary.get("assertions")).size());
    }

    @Test
    void summarise_throwsWhenIndexHtmlMissing(@TempDir Path tmp) throws Exception {
        Path simDir = tmp.resolve("rampagesimulation-empty");
        Files.createDirectories(simDir);
        IOException ex = assertThrows(IOException.class, () -> RunSummaryGenerator.summarise(simDir));
        assertTrue(ex.getMessage().contains("index.html"));
    }

    @Test
    void findLatestSimulationDir_picksMostRecentByName() throws Exception {
        Path tmp = Files.createTempDirectory("sim");
        try {
            Files.createDirectory(tmp.resolve("rampagesimulation-20260101000000000"));
            Files.createDirectory(tmp.resolve("rampagesimulation-20260601000000000"));
            Files.createDirectory(tmp.resolve("rampagesimulation-20260301000000000"));
            var latest = RunSummaryGenerator.findLatestSimulationDir(tmp.toFile());
            assertNotNull(latest);
            assertEquals("rampagesimulation-20260601000000000", latest.getName());
        } finally {
            try (var stream = Files.walk(tmp)) {
                stream.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }
}
