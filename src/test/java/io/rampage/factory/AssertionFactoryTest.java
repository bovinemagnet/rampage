package io.rampage.factory;

import io.gatling.javaapi.core.Assertion;
import io.rampage.config.model.AssertionsConfig;
import io.rampage.config.model.GlobalAssertionConfig;
import io.rampage.config.model.ScenarioAssertionConfig;
import io.rampage.config.model.ScenarioConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AssertionFactoryTest {

    private ScenarioConfig scenario(String id) {
        ScenarioConfig sc = new ScenarioConfig();
        sc.setId(id);
        sc.setName(id);
        return sc;
    }

    @Test
    void buildGlobalAssertions_emitsP95AndErrorRate() {
        AssertionsConfig a = new AssertionsConfig();
        GlobalAssertionConfig g = new GlobalAssertionConfig();
        g.setMaxResponseTimeP95Millis(2000);
        g.setMaxErrorPercentage(5.0);
        a.setGlobal(g);

        List<Assertion> assertions = AssertionFactory.buildGlobalAssertions(a);
        assertEquals(2, assertions.size());
    }

    @Test
    void buildScenarioAssertions_skipsScenariosWithoutConfig() {
        AssertionsConfig a = new AssertionsConfig();
        ScenarioAssertionConfig sa = new ScenarioAssertionConfig();
        sa.setMaxResponseTimeP95Millis(500);
        a.setScenarios(Map.of("only-this-one", sa));

        List<Assertion> assertions = AssertionFactory.buildScenarioAssertions(a,
            List.of(scenario("only-this-one"), scenario("other")));
        // Should produce one assertion for "only-this-one" (p95) only.
        assertEquals(1, assertions.size());
    }

    @Test
    void buildScenarioAssertions_returnsEmptyForNullConfig() {
        assertTrue(AssertionFactory.buildScenarioAssertions(null, List.of(scenario("a"))).isEmpty());
    }

    @Test
    void validateUnknownScenarios_reportsMissingScenarios() {
        AssertionsConfig a = new AssertionsConfig();
        ScenarioAssertionConfig sa = new ScenarioAssertionConfig();
        sa.setMaxErrorPercentage(1.0);
        a.setScenarios(Map.of("missing", sa));

        List<String> errors = AssertionFactory.validateUnknownScenarios(a, List.of(scenario("present")));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("missing"));
    }

    @Test
    void validateUnknownScenarios_passesWhenAllPresent() {
        AssertionsConfig a = new AssertionsConfig();
        ScenarioAssertionConfig sa = new ScenarioAssertionConfig();
        sa.setMaxErrorPercentage(1.0);
        a.setScenarios(Map.of("present", sa));

        assertTrue(AssertionFactory.validateUnknownScenarios(a, List.of(scenario("present"))).isEmpty());
    }
}
