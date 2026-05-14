package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ScenarioSecurityConfig {
    @JsonProperty("allowAuthOverride")
    private boolean allowAuthOverride = false;

    @JsonProperty("sensitiveFields")
    private List<String> sensitiveFields;

    public ScenarioSecurityConfig() {}

    public boolean isAllowAuthOverride() { return allowAuthOverride; }
    public void setAllowAuthOverride(boolean allowAuthOverride) { this.allowAuthOverride = allowAuthOverride; }
    public List<String> getSensitiveFields() { return sensitiveFields; }
    public void setSensitiveFields(List<String> sensitiveFields) { this.sensitiveFields = sensitiveFields; }
}
