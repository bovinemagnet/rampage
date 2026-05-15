package io.rampage.reporting;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RunSummaryComparatorTest {

    private Map<String, Object> summary(String status, Map<String, Object>... rows) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("status", status);
        s.put("simulationDir", "rampagesimulation-20260515000000000");
        s.put("generatedAt", "2026-05-15T00:00:00Z");
        s.put("assertions", List.of(
            Map.of("description", "Global: percentage of failed events is less than 5.0", "result",
                "FAIL".equals(status) ? "KO" : "OK")));
        s.put("requests", List.of(rows));
        return s;
    }

    private Map<String, Object> row(String name, long total, long ko, double koPct,
                                     long p95, long p99, double rps) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("name", name);
        r.put("total", total);
        r.put("ko", ko);
        r.put("koPct", koPct);
        r.put("p50", 20L);
        r.put("p75", 25L);
        r.put("p95", p95);
        r.put("p99", p99);
        r.put("mean", 22L);
        r.put("rps", rps);
        return r;
    }

    @Test
    void render_passStatusUsesGreenTick() {
        var current = summary("PASS", row("All Requests", 100, 0, 0, 30, 35, 5.0));
        String md = RunSummaryComparator.renderMarkdown(current, null);
        assertTrue(md.startsWith("## Rampage load test ✅ PASS"));
        assertTrue(md.contains("first run on this branch"));
    }

    @Test
    void render_failStatusUsesRedCross() {
        var current = summary("FAIL", row("All Requests", 100, 12, 12.5, 30, 35, 5.0));
        String md = RunSummaryComparator.renderMarkdown(current, null);
        assertTrue(md.contains("❌ FAIL"));
        // koPct=12.5 renders as "12.5"; whole-number percentages render without decimals.
        assertTrue(md.contains("12 (12.5%)"),
            "error count and percentage should appear; got:\n" + md);
    }

    @Test
    void render_withSlowerBaselineShowsRegressionArrows() {
        var baseline = summary("PASS", row("All Requests", 100, 0, 0, 25, 30, 5.0));
        var current = summary("PASS", row("All Requests", 100, 0, 0, 40, 45, 5.0));
        String md = RunSummaryComparator.renderMarkdown(current, baseline);
        assertTrue(md.contains("🔴"), "regression should mark with red indicator");
        assertTrue(md.contains("+15"), "P95 delta should be +15");
    }

    @Test
    void render_withFasterBaselineShowsImprovementArrows() {
        var baseline = summary("PASS", row("All Requests", 100, 0, 0, 50, 60, 5.0));
        var current = summary("PASS", row("All Requests", 100, 0, 0, 30, 35, 5.0));
        String md = RunSummaryComparator.renderMarkdown(current, baseline);
        assertTrue(md.contains("🟢"), "improvement should mark with green indicator");
        assertTrue(md.contains("-20"), "P95 delta should be -20");
    }

    @Test
    void render_smallDeltasAreSuppressed() {
        var baseline = summary("PASS", row("All Requests", 100, 0, 0, 30, 35, 5.0));
        var current = summary("PASS", row("All Requests", 100, 0, 0, 30, 35, 5.0));
        String md = RunSummaryComparator.renderMarkdown(current, baseline);
        // No arrows when both metrics match exactly (delta < 0.5 → suppressed).
        assertFalse(md.contains("🔴"), "should not mark identical values as regression");
        assertFalse(md.contains("🟢"), "should not mark identical values as improvement");
    }

    @Test
    void formatWithDelta_roundTripsNumeric() {
        // current=100, baseline=80, latency: positive delta is bad → 🔴
        String result = RunSummaryComparator.formatWithDelta(100L, 80L, false);
        assertTrue(result.contains("🔴"));
        assertTrue(result.contains("+20"));
    }

    @Test
    void formatWithDelta_handlesNullBaseline() {
        assertEquals("100", RunSummaryComparator.formatWithDelta(100L, null, false));
    }

    @Test
    void formatNumber_dashForNull() {
        assertEquals("—", RunSummaryComparator.formatNumber(null));
        assertEquals("100", RunSummaryComparator.formatNumber(100L));
        assertEquals("5", RunSummaryComparator.formatNumber(5.0));
        assertEquals("5.5", RunSummaryComparator.formatNumber(5.5));
    }
}
