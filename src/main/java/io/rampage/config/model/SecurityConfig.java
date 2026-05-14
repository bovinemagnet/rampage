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

    @JsonProperty("tokenUrl")
    private String tokenUrl;

    @JsonProperty("clientId")
    private CredentialConfig clientId;

    @JsonProperty("clientSecret")
    private CredentialConfig clientSecret;

    @JsonProperty("scope")
    private String scope;

    @JsonProperty("audience")
    private String audience;

    @JsonProperty("refreshIntervalSeconds")
    private Long refreshIntervalSeconds;

    @JsonProperty("onRefreshFailure")
    private String onRefreshFailure = "continue";

    public SecurityConfig() {}

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public TokenConfig getToken() { return token; }
    public void setToken(TokenConfig token) { this.token = token; }
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public String getTokenUrl() { return tokenUrl; }
    public void setTokenUrl(String tokenUrl) { this.tokenUrl = tokenUrl; }
    public CredentialConfig getClientId() { return clientId; }
    public void setClientId(CredentialConfig clientId) { this.clientId = clientId; }
    public CredentialConfig getClientSecret() { return clientSecret; }
    public void setClientSecret(CredentialConfig clientSecret) { this.clientSecret = clientSecret; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
    public Long getRefreshIntervalSeconds() { return refreshIntervalSeconds; }
    public void setRefreshIntervalSeconds(Long refreshIntervalSeconds) { this.refreshIntervalSeconds = refreshIntervalSeconds; }
    public String getOnRefreshFailure() { return onRefreshFailure; }
    public void setOnRefreshFailure(String onRefreshFailure) { this.onRefreshFailure = onRefreshFailure; }
}

