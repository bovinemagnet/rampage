package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ColumnConfig {
    @JsonProperty("type")
    private String type;

    @JsonProperty("required")
    private boolean required = false;

    @JsonProperty("sessionKey")
    private String sessionKey;

    public ColumnConfig() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public String getSessionKey() { return sessionKey; }
    public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }
}
