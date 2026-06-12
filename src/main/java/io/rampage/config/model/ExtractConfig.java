package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Captures a value from the response into a Gatling session key for use in subsequent
 * steps via {@code ${session:sessionKey}} placeholders.
 */
public class ExtractConfig {
    @JsonProperty("type")
    private String type;

    @JsonProperty("path")
    private String path;

    @JsonProperty("sessionKey")
    private String sessionKey;

    @JsonProperty("defaultValue")
    private String defaultValue;

    /**
     * Constructs an {@code ExtractConfig} with all fields at their default values.
     */
    public ExtractConfig() {}

    /**
     * Returns the extraction type (for example, {@code "jsonPath"} or {@code "regex"}).
     *
     * @return the extraction type, or {@code null} if not configured
     */
    public String getType() { return type; }

    /**
     * Sets the extraction type.
     *
     * @param type the extraction type to use
     */
    public void setType(String type) { this.type = type; }

    /**
     * Returns the expression used to locate the value in the response
     * (a JSONPath expression or regex pattern, depending on {@code type}).
     *
     * @return the path expression, or {@code null} if not configured
     */
    public String getPath() { return path; }

    /**
     * Sets the path expression used to locate the value in the response.
     *
     * @param path the path expression to use
     */
    public void setPath(String path) { this.path = path; }

    /**
     * Returns the Gatling session key under which the extracted value will be stored.
     *
     * @return the session key, or {@code null} if not configured
     */
    public String getSessionKey() { return sessionKey; }

    /**
     * Sets the Gatling session key under which the extracted value will be stored.
     *
     * @param sessionKey the session key to use
     */
    public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }

    /**
     * Returns the fallback value to use when the extraction expression matches nothing.
     *
     * @return the default value, or {@code null} if not configured
     */
    public String getDefaultValue() { return defaultValue; }

    /**
     * Sets the fallback value to use when the extraction expression matches nothing.
     *
     * @param defaultValue the default value to use
     */
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
}
