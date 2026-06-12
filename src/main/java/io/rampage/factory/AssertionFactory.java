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

/**
 * Translates {@code AssertionsConfig} into Gatling {@code Assertion} objects for use in a
 * simulation's {@code assertions()} block.
 *
 * <p>Both global assertions (applied across all requests) and per-scenario assertions
 * (scoped to a named scenario's details) are supported. Only thresholds configured with a
 * value greater than zero are emitted; a value of zero means "not set" and is silently
 * skipped.
 */
public final class AssertionFactory {

    /** Prevents instantiation of this utility class. */
    private AssertionFactory() {}

    /**
     * Builds all global and per-scenario assertions from the supplied configuration.
     *
     * @param assertionsConfig the top-level assertions configuration; may be {@code null}
     * @param scenarios        the loaded scenario configurations used to resolve scenario names
     * @return a combined list of all applicable {@code Assertion} objects; never {@code null}
     */
    public static List<Assertion> buildAll(AssertionsConfig assertionsConfig, List<ScenarioConfig> scenarios) {
        List<Assertion> all = new ArrayList<>();
        all.addAll(buildGlobalAssertions(assertionsConfig));
        all.addAll(buildScenarioAssertions(assertionsConfig, scenarios));
        return all;
    }

    /**
     * Builds global assertions from the {@code global} block of the assertions configuration.
     *
     * <p>Emits assertions for {@code maxResponseTimeP95Millis}, {@code maxResponseTimeP99Millis},
     * and {@code maxErrorPercentage} when each is configured with a positive value.
     *
     * @param assertionsConfig the top-level assertions configuration; may be {@code null}
     * @return a list of global {@code Assertion} objects; never {@code null}
     */
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

    /**
     * Builds per-scenario assertions scoped to the Gatling {@code details(name)} scope.
     *
     * <p>For each scenario that has a matching entry in {@code assertionsConfig.scenarios},
     * assertions are emitted for {@code maxResponseTimeP95Millis} and {@code maxErrorPercentage}
     * when each is configured with a positive value. The scenario's {@code name} field is used
     * as the Gatling detail scope; if {@code name} is absent, {@code id} is used instead.
     *
     * @param assertionsConfig the top-level assertions configuration; may be {@code null}
     * @param scenarios        the loaded scenario configurations; may be {@code null}
     * @return a list of scenario-scoped {@code Assertion} objects; never {@code null}
     */
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
     *
     * @param assertionsConfig the top-level assertions configuration; may be {@code null}
     * @param scenarios        the loaded scenario configurations used to derive known ids;
     *                         may be {@code null}
     * @return a list of error messages, one per unknown scenario reference; empty if none found
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
