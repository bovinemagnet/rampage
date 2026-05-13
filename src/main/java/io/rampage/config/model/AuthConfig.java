package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthConfig {
    @JsonProperty("mode")
    private String mode;

    @JsonProperty("tokenRef")
    private String tokenRef;

    public AuthConfig() {}

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getTokenRef() { return tokenRef; }
    public void setTokenRef(String tokenRef) { this.tokenRef = tokenRef; }
}
