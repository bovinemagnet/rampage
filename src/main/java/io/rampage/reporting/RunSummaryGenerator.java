package io.rampage.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a Gatling 3.15 simulation report and emits a structured {@code run-summary.json}.
 *
 * <p>Gatling's machine-readable simulation log is a binary format with no public parser,
 * but the rendered {@code index.html} contains stable, well-structured tables for both
 * the per-request statistics and the assertion outcomes. This class extracts the data
 * we care about — request counts, response-time percentiles, throughput, and assertion
 * pass/fail — without pulling in an HTML parser dependency.
 *
 * <p>The output JSON is the canonical artefact for downstream baseline comparison and
 * PR-comment rendering (see {@link RunSummaryComparator}).
 */
public final class RunSummaryGenerator {

    private static final Logger log = LoggerFactory.getLogger(RunSummaryGenerator.class);

    /** Maps Gatling's stats-table column index to the metric we expose in JSON. */
    private static final String[] COLUMN_KEYS = {
        null, null, "total", "ok", "ko", "koPct", "rps",
        "min", "p50", "p75", "p95", "p99", "max", "mean", "stddev"
    };

    private static final Pattern NAME_SPAN = Pattern.compile(
        "<span[^>]*id=\"stats-table-([^\"]+)\"[^>]*class=\"ellipsed-name\"[^>]*>([^<]+)</span>");

    private static final Pattern STAT_CELL = Pattern.compile(
        "<td[^>]*class=\"value\\s+(?:ok|ko|total)\\s+col-(\\d+)\"[^>]*>\\s*([^<]+?)\\s*</td>");

    private static final Pattern ASSERTION_DESC_CELL = Pattern.compile(
        "<td[^>]*class=\"error-col-1\\s+(?:ok|ko)\\s+total\"[^>]*>(.*?)</td>",
        Pattern.DOTALL);

    private static final Pattern ASSERTION_RESULT_CELL = Pattern.compile(
        "<td[^>]*class=\"error-col-2[^\"]*\"[^>]*>\\s*(OK|KO)\\s*</td>",
        Pattern.DOTALL);

    private static final Pattern INLINE_TAG = Pattern.compile("<[^>]+>");

    private static final Pattern HIDDEN_SPAN = Pattern.compile(
        "<span[^>]*style=\"display:none\"[^>]*>.*?</span>",
        Pattern.DOTALL);

    private static final ObjectMapper JSON = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    private RunSummaryGenerator() {}

    /**
     * Parses a single Gatling simulation directory's {@code index.html} into a
     * structured summary map containing request stats, assertion outcomes, and an overall
     * PASS/FAIL status. No file is written by this method.
     *
     * @param simulationDir a {@code rampagesimulation-*} directory that contains
     *                      {@code index.html}
     * @return an ordered map with keys {@code generatedAt}, {@code simulationDir},
     *         {@code simulationPath}, {@code requests}, {@code assertions}, and {@code status}
     * @throws IOException if {@code simulationDir} is not a directory, {@code index.html}
     *                     is missing, or the file cannot be read
     */
    public static Map<String, Object> summarise(Path simulationDir) throws IOException {
        File simDir = simulationDir.toFile();
        if (!simDir.isDirectory()) {
            throw new IOException("Not a simulation directory: " + simulationDir);
        }
        File index = new File(simDir, "index.html");
        if (!index.isFile()) {
            throw new IOException("Gatling report missing index.html: " + index);
        }
        String html = Files.readString(index.toPath(), StandardCharsets.UTF_8);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("generatedAt", Instant.now().toString());
        summary.put("simulationDir", simDir.getName());
        summary.put("simulationPath", simDir.getAbsolutePath());
        summary.put("requests", parseRequests(html));
        summary.put("assertions", parseAssertions(html));

        boolean allAssertionsPassed = ((List<?>) summary.get("assertions")).stream()
            .map(a -> ((Map<?, ?>) a).get("result"))
            .allMatch(r -> "OK".equals(r));
        summary.put("status", allAssertionsPassed ? "PASS" : "FAIL");
        return summary;
    }

    /**
     * Generates a {@code run-summary.json} from the most recent Gatling report directory
     * inside {@code reportRoot} and writes it to {@code outputFile}.
     *
     * <p>Delegates the actual report parsing to {@link #summarise(Path)}.
     *
     * @param reportRoot the Gatling reports root directory (e.g. {@code build/reports/gatling});
     *                   the most recently named {@code rampagesimulation-*} subdirectory is used
     * @param outputFile the path to write the resulting {@code run-summary.json}; parent
     *                   directories are created if they do not exist
     * @return the parsed summary as a map, for callers that want to inspect or post-process it
     * @throws IOException if no simulation directory is found, or reading/writing fails
     * @see #summarise(Path)
     */
    public static Map<String, Object> generate(Path reportRoot, Path outputFile) throws IOException {
        File simDir = findLatestSimulationDir(reportRoot.toFile());
        if (simDir == null) {
            throw new IOException("No Gatling simulation directory found under " + reportRoot
                + " (expected a 'rampagesimulation-*' subdirectory)");
        }
        Map<String, Object> summary = summarise(simDir.toPath());
        Files.createDirectories(outputFile.getParent());
        JSON.writeValue(outputFile.toFile(), summary);
        log.info("Wrote run summary to {} (status={}, requestRows={}, assertions={})",
            outputFile, summary.get("status"),
            ((List<?>) summary.get("requests")).size(),
            ((List<?>) summary.get("assertions")).size());
        return summary;
    }

    /**
     * Returns the alphabetically last {@code rampagesimulation-*} subdirectory of
     * {@code reportRoot}, or null if none exists.
     *
     * @param reportRoot the directory to search; may be null
     * @return the most recently named simulation directory, or null if not found
     */
    static File findLatestSimulationDir(File reportRoot) {
        if (reportRoot == null || !reportRoot.isDirectory()) return null;
        File[] candidates = reportRoot.listFiles((dir, name) -> name.startsWith("rampagesimulation-"));
        if (candidates == null || candidates.length == 0) return null;
        Arrays.sort(candidates, Comparator.comparing(File::getName).reversed());
        return candidates[0];
    }

    /**
     * Extracts per-request statistics from the Gatling {@code index.html} content.
     * Each entry in the returned list corresponds to one row in the stats table and
     * includes keys such as {@code id}, {@code name}, {@code total}, {@code ok},
     * {@code ko}, {@code koPct}, {@code rps}, and the percentile/mean columns.
     *
     * @param html the full content of Gatling's {@code index.html}
     * @return an ordered list of request-stat maps; never null, may be empty
     */
    static List<Map<String, Object>> parseRequests(String html) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Matcher names = NAME_SPAN.matcher(html);
        while (names.find()) {
            String id = names.group(1);
            String displayName = names.group(2).trim();
            // Within ~3000 chars after the name span, harvest the col-2..col-14 cells.
            int from = names.end();
            int to = Math.min(html.length(), from + 3000);
            Map<Integer, String> cols = new LinkedHashMap<>();
            Matcher cells = STAT_CELL.matcher(html).region(from, to);
            while (cells.find()) {
                int col = Integer.parseInt(cells.group(1));
                cols.putIfAbsent(col, cells.group(2));
                if (col >= 14) break;
            }
            if (cols.isEmpty()) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", id);
            row.put("name", displayName);
            for (int i = 2; i < COLUMN_KEYS.length; i++) {
                String key = COLUMN_KEYS[i];
                if (key == null) continue;
                String raw = cols.get(i);
                row.put(key, parseNumericIfPossible(raw));
            }
            rows.add(row);
        }
        return rows;
    }

    /**
     * Extracts assertion results from the Gatling {@code index.html} content.
     * Each entry in the returned list has keys {@code description} and {@code result}
     * ({@code "OK"} or {@code "KO"}).
     *
     * @param html the full content of Gatling's {@code index.html}
     * @return an ordered list of assertion-result maps; never null, may be empty
     */
    static List<Map<String, Object>> parseAssertions(String html) {
        List<Map<String, Object>> result = new ArrayList<>();
        Matcher desc = ASSERTION_DESC_CELL.matcher(html);
        while (desc.find()) {
            String inner = HIDDEN_SPAN.matcher(desc.group(1)).replaceAll("");
            String description = normaliseWhitespace(INLINE_TAG.matcher(inner).replaceAll(""));
            if (description.isEmpty()) continue;
            Matcher res = ASSERTION_RESULT_CELL.matcher(html).region(desc.end(), html.length());
            if (!res.find()) break;
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("description", description);
            a.put("result", res.group(1));
            result.add(a);
        }
        return result;
    }

    private static String normaliseWhitespace(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    private static Object parseNumericIfPossible(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || "-".equals(trimmed)) return null;
        try {
            if (trimmed.contains(".")) return Double.parseDouble(trimmed);
            return Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            return trimmed;
        }
    }
}
