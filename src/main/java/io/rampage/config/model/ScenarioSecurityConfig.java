package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScenarioSecurityConfig {
    @JsonProperty("allowAuthOverride")
    private boolean allowAuthOverride = false;

    public ScenarioSecurityConfig() {}

    public boolean isAllowAuthOverride() { return allowAuthOverride; }
    public void setAllowAuthOverride(boolean allowAuthOverride) { this.allowAuthOverride = allowAuthOverride; }
}
