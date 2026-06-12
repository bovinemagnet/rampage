package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Metadata for a single column returned by a feeder SQL query.
 *
 * <p>Bound to entries in the {@code columns} map inside a {@code feeder} block
 * in a scenario YAML file. Describes the expected type of the column value,
 * whether it is mandatory, and how it should be stored in the Gatling session.</p>
 */
public class ColumnConfig {
    @JsonProperty("type")
    private String type;

    @JsonProperty("required")
    private boolean required = false;

    @JsonProperty("sessionKey")
    private String sessionKey;

    @JsonProperty("sensitive")
    private boolean sensitive = false;

    /**
     * Constructs a {@code ColumnConfig} with all fields at their default values.
     */
    public ColumnConfig() {}

    /**
     * Returns the expected data type of the column value (for example, {@code "string"} or {@code "integer"}).
     *
     * @return the type name, or {@code null} if not specified
     */
    public String getType() { return type; }

    /**
     * Sets the expected data type of the column value.
     *
     * @param type the type name to use
     */
    public void setType(String type) { this.type = type; }

    /**
     * Returns whether the column value must be present for each feeder row.
     *
     * @return {@code true} if the column is required; {@code false} otherwise
     */
    public boolean isRequired() { return required; }

    /**
     * Sets whether the column value is required for each feeder row.
     *
     * @param required {@code true} to mark the column as required
     */
    public void setRequired(boolean required) { this.required = required; }

    /**
     * Returns the Gatling session key under which the column value will be stored.
     *
     * @return the session key, or {@code null} if not configured
     */
    public String getSessionKey() { return sessionKey; }

    /**
     * Sets the Gatling session key under which the column value will be stored.
     *
     * @param sessionKey the session key to use
     */
    public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }

    /**
     * Returns whether the column value is sensitive and should be masked in logs.
     *
     * @return {@code true} if the value is sensitive; {@code false} otherwise
     */
    public boolean isSensitive() { return sensitive; }

    /**
     * Sets whether the column value is sensitive and should be masked in logs.
     *
     * @param sensitive {@code true} to mark the value as sensitive
     */
    public void setSensitive(boolean sensitive) { this.sensitive = sensitive; }
}
