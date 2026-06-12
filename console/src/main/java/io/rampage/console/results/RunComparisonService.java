package io.rampage.console.results;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Diffs two {@link StoredRun}s scenario-by-scenario into a {@link RunComparison}. */
@ApplicationScoped
public class RunComparisonService {

    @Inject
    StoredRunRepository repository;

    /**
     * Creates a default {@code RunComparisonService} instance.
     * Dependencies are injected by the CDI container.
     */
    public RunComparisonService() {
    }

    /**
     * Compares two runs identified by their string ids.
     *
     * @param idA id of the baseline run
     * @param idB id of the candidate run
     * @return a {@link RunComparison} containing per-scenario metric diffs
     * @throws IllegalArgumentException when either id does not correspond to a stored run
     */
    public RunComparison compare(String idA, String idB) {
        StoredRun a = repository.findById(idA);
        StoredRun b = repository.findById(idB);
        if (a == null || b == null) {
            throw new IllegalArgumentException(
                "Unknown run id in comparison: idA=" + idA + " idB=" + idB);
        }
        return compare(a, b);
    }

    /**
     * Compares two already-loaded runs, building per-scenario metric diffs.
     *
     * @param a the baseline run
     * @param b the candidate run
     * @return a {@link RunComparison} containing per-scenario metric diffs
     */
    public RunComparison compare(StoredRun a, StoredRun b) {
        Map<String, ScenarioStat> byNameA = indexByName(a);
        Map<String, ScenarioStat> byNameB = indexByName(b);
        TreeSet<String> names = new TreeSet<>();
        names.addAll(byNameA.keySet());
        names.addAll(byNameB.keySet());

        List<ScenarioComparison> scenarios = new ArrayList<>();
        for (String name : names) {
            scenarios.add(ScenarioComparison.of(name, byNameA.get(name), byNameB.get(name)));
        }
        return new RunComparison(a, b, scenarios);
    }

    private static Map<String, ScenarioStat> indexByName(StoredRun run) {
        Map<String, ScenarioStat> map = new LinkedHashMap<>();
        for (ScenarioStat s : run.scenarioStats) {
            if (s.scenarioName != null) {
                map.put(s.scenarioName, s);
            }
        }
        return map;
    }
}
