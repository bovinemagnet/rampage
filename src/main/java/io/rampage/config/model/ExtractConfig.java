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

    public ExtractConfig() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getSessionKey() { return sessionKey; }
    public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
}
