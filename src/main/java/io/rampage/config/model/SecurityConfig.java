package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class SecurityConfig {
    @JsonProperty("mode")
    private String mode;

    @JsonProperty("token")
    private TokenConfig token;

    @JsonProperty("headers")
    private Map<String, String> headers;

    public SecurityConfig() {}

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public TokenConfig getToken() { return token; }
    public void setToken(TokenConfig token) { this.token = token; }
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
}
