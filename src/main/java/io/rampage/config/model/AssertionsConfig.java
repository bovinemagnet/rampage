package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class AssertionsConfig {
    @JsonProperty("global")
    private GlobalAssertionConfig global;

    @JsonProperty("scenarios")
    private Map<String, ScenarioAssertionConfig> scenarios;

    public AssertionsConfig() {}

    public GlobalAssertionConfig getGlobal() { return global; }
    public void setGlobal(GlobalAssertionConfig global) { this.global = global; }
    public Map<String, ScenarioAssertionConfig> getScenarios() { return scenarios; }
    public void setScenarios(Map<String, ScenarioAssertionConfig> scenarios) { this.scenarios = scenarios; }
}
