package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Per-scenario security settings bound from the {@code security} block in a scenario
 * YAML file.
 *
 * <p>These settings allow a scenario to override the environment-level authentication
 * and to declare which response fields contain sensitive data that should be redacted
 * from logs and reports.</p>
 */
public class ScenarioSecurityConfig {
    @JsonProperty("allowAuthOverride")
    private boolean allowAuthOverride = false;

    @JsonProperty("sensitiveFields")
    private List<String> sensitiveFields;

    /**
     * Constructs a {@code ScenarioSecurityConfig} with all fields at their defaults.
     */
    public ScenarioSecurityConfig() {}

    /**
     * Returns whether this scenario is permitted to override the environment-level
     * authentication configuration.
     *
     * @return {@code true} if auth override is allowed; {@code false} by default
     */
    public boolean isAllowAuthOverride() { return allowAuthOverride; }

    /**
     * Sets whether this scenario is permitted to override the environment-level
     * authentication configuration.
     *
     * @param allowAuthOverride {@code true} to permit auth override
     */
    public void setAllowAuthOverride(boolean allowAuthOverride) { this.allowAuthOverride = allowAuthOverride; }

    /**
     * Returns the list of response-field names whose values should be redacted in logs and
     * reports.
     *
     * @return the list of sensitive field names, or {@code null} if none are declared
     */
    public List<String> getSensitiveFields() { return sensitiveFields; }

    /**
     * Sets the list of response-field names whose values should be redacted.
     *
     * @param sensitiveFields the list of sensitive field names
     */
    public void setSensitiveFields(List<String> sensitiveFields) { this.sensitiveFields = sensitiveFields; }
}
