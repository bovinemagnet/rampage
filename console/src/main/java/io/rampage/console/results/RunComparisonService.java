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

    /** Compare two runs by id. Throws {@link IllegalArgumentException} when either is unknown. */
    public RunComparison compare(String idA, String idB) {
        StoredRun a = repository.findById(idA);
        StoredRun b = repository.findById(idB);
        if (a == null || b == null) {
            throw new IllegalArgumentException("Unknown run id in comparison");
        }
        return compare(a, b);
    }

    /** Compare two already-loaded runs. */
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
