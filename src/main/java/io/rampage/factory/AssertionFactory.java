package io.rampage.factory;

import io.gatling.javaapi.core.Assertion;
import io.rampage.config.model.AssertionsConfig;
import io.rampage.config.model.GlobalAssertionConfig;
import io.rampage.config.model.ScenarioAssertionConfig;
import io.rampage.config.model.ScenarioConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.global;

public final class AssertionFactory {

    private AssertionFactory() {}

    public static List<Assertion> buildAll(AssertionsConfig assertionsConfig, List<ScenarioConfig> scenarios) {
        List<Assertion> all = new ArrayList<>();
        all.addAll(buildGlobalAssertions(assertionsConfig));
        all.addAll(buildScenarioAssertions(assertionsConfig, scenarios));
        return all;
    }

    public static List<Assertion> buildGlobalAssertions(AssertionsConfig assertionsConfig) {
        List<Assertion> assertions = new ArrayList<>();
        if (assertionsConfig == null || assertionsConfig.getGlobal() == null) return assertions;
        GlobalAssertionConfig g = assertionsConfig.getGlobal();
        if (g.getMaxResponseTimeP95Millis() > 0) {
            assertions.add(global().responseTime().percentile(95).lt((int) g.getMaxResponseTimeP95Millis()));
        }
        if (g.getMaxResponseTimeP99Millis() > 0) {
            assertions.add(global().responseTime().percentile(99).lt((int) g.getMaxResponseTimeP99Millis()));
        }
        if (g.getMaxErrorPercentage() > 0) {
            assertions.add(global().failedRequests().percent().lt(g.getMaxErrorPercentage()));
        }
        return assertions;
    }

    public static List<Assertion> buildScenarioAssertions(AssertionsConfig assertionsConfig, List<ScenarioConfig> scenarios) {
        List<Assertion> assertions = new ArrayList<>();
        if (assertionsConfig == null || assertionsConfig.getScenarios() == null
            || assertionsConfig.getScenarios().isEmpty() || scenarios == null) {
            return assertions;
        }
        for (ScenarioConfig sc : scenarios) {
            if (sc.getId() == null) continue;
            ScenarioAssertionConfig cfg = assertionsConfig.getScenarios().get(sc.getId());
            if (cfg == null) continue;
            String name = sc.getName() != null ? sc.getName() : sc.getId();
            if (cfg.getMaxResponseTimeP95Millis() > 0) {
                assertions.add(details(name).responseTime().percentile(95).lt((int) cfg.getMaxResponseTimeP95Millis()));
            }
            if (cfg.getMaxErrorPercentage() > 0) {
                assertions.add(details(name).failedRequests().percent().lt(cfg.getMaxErrorPercentage()));
            }
        }
        return assertions;
    }

    /**
     * Returns errors for any scenario-level assertion that references a scenario id not
     * present in the loaded scenarios.
     */
    public static List<String> validateUnknownScenarios(AssertionsConfig assertionsConfig, List<ScenarioConfig> scenarios) {
        List<String> errors = new ArrayList<>();
        if (assertionsConfig == null || assertionsConfig.getScenarios() == null) return errors;
        java.util.Set<String> known = new java.util.HashSet<>();
        if (scenarios != null) {
            for (ScenarioConfig sc : scenarios) {
                if (sc.getId() != null) known.add(sc.getId());
            }
        }
        for (Map.Entry<String, ScenarioAssertionConfig> e : assertionsConfig.getScenarios().entrySet()) {
            if (!known.contains(e.getKey())) {
                errors.add("assertions.scenarios.'" + e.getKey() + "' refers to a scenario not in run.scenarios");
            }
        }
        return errors;
    }
}
