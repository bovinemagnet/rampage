package io.rampage.reporting;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Compares a current {@link RunSummaryGenerator} JSON output against an optional
 * baseline and renders a Markdown summary suitable for posting as a PR comment.
 *
 * <p>For each request row in the current run we look up the same row by {@code name}
 * in the baseline and compute deltas for the headline metrics (P95, P99, error %,
 * throughput). Rows that don't exist in one side are flagged as added or removed.
 *
 * <p>The Markdown is intentionally compact and uses GitHub-flavoured table syntax
 * with simple emoji indicators (no images, no JS). It can be piped to
 * {@code gh pr comment --body-file -} unchanged.
 */
public final class RunSummaryComparator {

    private static final ObjectMapper JSON = new ObjectMapper();

    private RunSummaryComparator() {}

    /** Render the comparison into Markdown. {@code baselinePath} may be null. */
    public static String renderMarkdown(Path currentPath, Path baselinePath) throws IOException {
        Map<String, Object> current = readJson(currentPath);
        Map<String, Object> baseline = (baselinePath != null && Files.isRegularFile(baselinePath))
            ? readJson(baselinePath) : null;
        return renderMarkdown(current, baseline);
    }

    static String renderMarkdown(Map<String, Object> current, Map<String, Object> baseline) {
        StringBuilder out = new StringBuilder();
        String status = String.valueOf(current.get("status"));
        out.append("## Rampage load test ").append(statusBadge(status)).append("\n\n");

        out.append("Simulation: `").append(current.getOrDefault("simulationDir", "?")).append("`  \n");
        out.append("Generated: `").append(current.getOrDefault("generatedAt", "?")).append("`");
        if (baseline != null) {
            out.append("  \nBaseline: `").append(baseline.getOrDefault("simulationDir", "?")).append("`");
        } else {
            out.append("  \nBaseline: _(none — first run on this branch)_");
        }
        out.append("\n\n");

        renderAssertions(out, current);
        renderRequestTable(out, current, baseline);

        return out.toString();
    }

    private static String statusBadge(String status) {
        return switch (status == null ? "" : status) {
            case "PASS" -> "✅ PASS";
            case "FAIL" -> "❌ FAIL";
            default -> "⚠️ " + status;
        };
    }

    @SuppressWarnings("unchecked")
    private static void renderAssertions(StringBuilder out, Map<String, Object> current) {
        List<Map<String, Object>> assertions = (List<Map<String, Object>>) current.getOrDefault(
            "assertions", List.of());
        if (assertions.isEmpty()) {
            out.append("_No assertions configured for this run._\n\n");
            return;
        }
        out.append("### Assertions\n\n");
        out.append("| Result | Assertion |\n");
        out.append("|---|---|\n");
        for (Map<String, Object> a : assertions) {
            String result = String.valueOf(a.get("result"));
            String desc = String.valueOf(a.get("description"));
            out.append("| ").append("OK".equals(result) ? "✅" : "❌").append(" | ")
                .append(escapePipes(desc)).append(" |\n");
        }
        out.append('\n');
    }

    @SuppressWarnings("unchecked")
    private static void renderRequestTable(StringBuilder out, Map<String, Object> current,
                                            Map<String, Object> baseline) {
        List<Map<String, Object>> currentRows = (List<Map<String, Object>>) current.getOrDefault(
            "requests", List.of());
        Map<String, Map<String, Object>> baselineByName = indexByName(baseline);
        if (currentRows.isEmpty()) {
            out.append("_No request data captured._\n");
            return;
        }
        out.append("### Request stats\n\n");
        out.append("| Request | Count | Errors | RPS | P50 | P95 (Δ) | P99 (Δ) | Mean |\n");
        out.append("|---|---:|---:|---:|---:|---:|---:|---:|\n");
        for (Map<String, Object> row : currentRows) {
            String name = String.valueOf(row.get("name"));
            Map<String, Object> b = baselineByName.get(name);
            out.append("| ").append(escapePipes(name)).append(" | ")
                .append(formatNumber(row.get("total"))).append(" | ")
                .append(formatErrors(row)).append(" | ")
                .append(formatNumber(row.get("rps"))).append(" | ")
                .append(formatNumber(row.get("p50"))).append(" | ")
                .append(formatWithDelta(row.get("p95"), b == null ? null : b.get("p95"), false)).append(" | ")
                .append(formatWithDelta(row.get("p99"), b == null ? null : b.get("p99"), false)).append(" | ")
                .append(formatNumber(row.get("mean"))).append(" |\n");
        }
        out.append('\n');
        out.append("_Δ shown when a baseline is available. Positive Δ = slower than baseline._\n");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> indexByName(Map<String, Object> summary) {
        Map<String, Map<String, Object>> map = new LinkedHashMap<>();
        if (summary == null) return map;
        List<Map<String, Object>> rows = (List<Map<String, Object>>) summary.getOrDefault(
            "requests", List.of());
        for (Map<String, Object> r : rows) {
            Object name = r.get("name");
            if (name != null) map.put(name.toString(), r);
        }
        return map;
    }

    private static String formatErrors(Map<String, Object> row) {
        Object ko = row.get("ko");
        Object pct = row.get("koPct");
        if (ko == null) return "0";
        if (pct == null) return formatNumber(ko);
        return formatNumber(ko) + " (" + formatNumber(pct) + "%)";
    }

    /**
     * Render a number with an optional delta vs. baseline. {@code lowerIsBetter=false} means
     * Δ is shown without judgement — we only flag direction. Latency metrics: positive Δ is bad.
     */
    static String formatWithDelta(Object current, Object baseline, boolean higherIsBetter) {
        if (current == null) return "—";
        String currentStr = formatNumber(current);
        if (baseline == null || !(current instanceof Number) || !(baseline instanceof Number)) {
            return currentStr;
        }
        double c = ((Number) current).doubleValue();
        double b = ((Number) baseline).doubleValue();
        double delta = c - b;
        if (Math.abs(delta) < 0.5) return currentStr;
        boolean isImprovement = higherIsBetter ? delta > 0 : delta < 0;
        String arrow = isImprovement ? "🟢" : "🔴";
        String sign = delta >= 0 ? "+" : "";
        return currentStr + " " + arrow + " " + sign + formatNumber(delta);
    }

    static String formatNumber(Object value) {
        if (value == null) return "—";
        if (value instanceof Long || value instanceof Integer) return value.toString();
        if (value instanceof Number n) {
            double d = n.doubleValue();
            if (d == Math.floor(d)) return String.valueOf((long) d);
            return String.format(Locale.ROOT, "%.1f", d);
        }
        return value.toString();
    }

    private static String escapePipes(String s) {
        return s == null ? "" : s.replace("|", "\\|");
    }

    private static Map<String, Object> readJson(Path p) throws IOException {
        byte[] bytes = Files.readAllBytes(p);
        return JSON.readValue(new String(bytes, StandardCharsets.UTF_8),
            new TypeReference<Map<String, Object>>() {});
    }
}
