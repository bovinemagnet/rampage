package io.rampage.console.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.rampage.console.results.StoredRun;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a time-ordered list of runs into the JSON array bundle uPlot expects:
 * {@code {"x":[epochSeconds...],"p95":[...],"rps":[...],"err":[...]}}.
 */
public final class TrendData {

    private static final ObjectMapper JSON = new ObjectMapper();

    private TrendData() {
    }

    /** Build the uPlot data island JSON for {@code runs} (assumed oldest-first). */
    public static String toJson(List<StoredRun> runs) {
        List<Long> x = new ArrayList<>();
        List<Double> p95 = new ArrayList<>();
        List<Double> rps = new ArrayList<>();
        List<Double> err = new ArrayList<>();
        for (StoredRun run : runs) {
            if (run.startedAt == null) {
                continue;
            }
            x.add(run.startedAt.getEpochSecond());
            p95.add(run.worstP95());
            rps.add(totalRps(run));
            err.add(run.worstErrorPercent());
        }
        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("x", x);
        bundle.put("p95", p95);
        bundle.put("rps", rps);
        bundle.put("err", err);
        try {
            return JSON.writeValueAsString(bundle);
        } catch (Exception e) {
            return "{\"x\":[],\"p95\":[],\"rps\":[],\"err\":[]}";
        }
    }

    /**
     * Total throughput across a run's scenarios. Returns {@code null} (not 0.0)
     * when the run has no scenario stats, so a run with no data shows as a chart
     * gap rather than a misleading zero — consistent with worstP95/worstErrorPercent.
     */
    private static Double totalRps(StoredRun run) {
        boolean hasData = run.scenarioStats.stream()
                .anyMatch(s -> s.requestsPerSecond != null);
        if (!hasData) {
            return null;
        }
        return run.scenarioStats.stream()
                .map(s -> s.requestsPerSecond)
                .filter(v -> v != null)
                .mapToDouble(Double::doubleValue)
                .sum();
    }
}
