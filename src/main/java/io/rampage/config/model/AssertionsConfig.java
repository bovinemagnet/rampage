package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Top-level assertions configuration for a run.
 *
 * <p>Bound to the {@code assertions} key in {@code run.yaml}. Contains a
 * {@code global} block that applies across all scenarios, and an optional
 * {@code scenarios} map that allows per-scenario thresholds to be specified
 * (not yet wired into the simulation engine).</p>
 */
public class AssertionsConfig {
    @JsonProperty("global")
    private GlobalAssertionConfig global;

    @JsonProperty("scenarios")
    private Map<String, ScenarioAssertionConfig> scenarios;

    /**
     * Constructs an {@code AssertionsConfig} with all fields at their default values.
     */
    public AssertionsConfig() {}

    /**
     * Returns the global assertion thresholds applied across all scenarios.
     *
     * @return the global assertion configuration, or {@code null} if not configured
     */
    public GlobalAssertionConfig getGlobal() { return global; }

    /**
     * Sets the global assertion thresholds.
     *
     * @param global the global assertion configuration to use
     */
    public void setGlobal(GlobalAssertionConfig global) { this.global = global; }

    /**
     * Returns the per-scenario assertion thresholds, keyed by scenario ID.
     *
     * @return a map of scenario IDs to their assertion configurations, or {@code null} if not configured
     */
    public Map<String, ScenarioAssertionConfig> getScenarios() { return scenarios; }

    /**
     * Sets the per-scenario assertion thresholds.
     *
     * @param scenarios a map of scenario IDs to their assertion configurations
     */
    public void setScenarios(Map<String, ScenarioAssertionConfig> scenarios) { this.scenarios = scenarios; }
}
