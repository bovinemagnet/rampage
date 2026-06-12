package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Environment-level security and authentication settings bound from the {@code security}
 * block inside {@code environment.yaml}.
 *
 * <p>The {@code mode} field controls how authentication is applied. The {@code bearer}
 * mode injects the resolved token as an {@code Authorization: Bearer} header. OAuth
 * client-credentials flow fields ({@code tokenUrl}, {@code clientId}, {@code clientSecret},
 * {@code scope}, {@code audience}) are used when the mode requires token acquisition.
 * Static headers can be added directly via the {@code headers} map.</p>
 */
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

    /**
     * Constructs a {@code SecurityConfig} with all fields at their defaults.
     */
    public SecurityConfig() {}

    /**
     * Returns the authentication mode (e.g. {@code bearer}, {@code oauth2}).
     *
     * @return the authentication mode string, or {@code null} if not set
     */
    public String getMode() { return mode; }

    /**
     * Sets the authentication mode.
     *
     * @param mode the authentication mode string
     */
    public void setMode(String mode) { this.mode = mode; }

    /**
     * Returns the static token configuration used in {@code bearer} mode.
     *
     * @return the token configuration, or {@code null} if not set
     */
    public TokenConfig getToken() { return token; }

    /**
     * Sets the static token configuration.
     *
     * @param token the token configuration
     */
    public void setToken(TokenConfig token) { this.token = token; }

    /**
     * Returns additional static HTTP headers to include with every request.
     *
     * @return a map of header name to value, or {@code null} if none are configured
     */
    public Map<String, String> getHeaders() { return headers; }

    /**
     * Sets additional static HTTP headers to include with every request.
     *
     * @param headers a map of header name to value
     */
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }

    /**
     * Returns the OAuth 2.0 token endpoint URL used when acquiring access tokens.
     *
     * @return the token URL, or {@code null} if not set
     */
    public String getTokenUrl() { return tokenUrl; }

    /**
     * Sets the OAuth 2.0 token endpoint URL.
     *
     * @param tokenUrl the token URL
     */
    public void setTokenUrl(String tokenUrl) { this.tokenUrl = tokenUrl; }

    /**
     * Returns the OAuth 2.0 client identifier credential configuration.
     *
     * @return the client-id credential configuration, or {@code null} if not set
     */
    public CredentialConfig getClientId() { return clientId; }

    /**
     * Sets the OAuth 2.0 client identifier credential configuration.
     *
     * @param clientId the client-id credential configuration
     */
    public void setClientId(CredentialConfig clientId) { this.clientId = clientId; }

    /**
     * Returns the OAuth 2.0 client secret credential configuration.
     *
     * @return the client-secret credential configuration, or {@code null} if not set
     */
    public CredentialConfig getClientSecret() { return clientSecret; }

    /**
     * Sets the OAuth 2.0 client secret credential configuration.
     *
     * @param clientSecret the client-secret credential configuration
     */
    public void setClientSecret(CredentialConfig clientSecret) { this.clientSecret = clientSecret; }

    /**
     * Returns the OAuth 2.0 scope requested when acquiring an access token.
     *
     * @return the scope string, or {@code null} if not set
     */
    public String getScope() { return scope; }

    /**
     * Sets the OAuth 2.0 scope requested when acquiring an access token.
     *
     * @param scope the scope string
     */
    public void setScope(String scope) { this.scope = scope; }

    /**
     * Returns the OAuth 2.0 audience parameter sent in token requests.
     *
     * @return the audience string, or {@code null} if not set
     */
    public String getAudience() { return audience; }

    /**
     * Sets the OAuth 2.0 audience parameter.
     *
     * @param audience the audience string
     */
    public void setAudience(String audience) { this.audience = audience; }

    /**
     * Returns the interval in seconds at which the access token is proactively refreshed.
     *
     * @return the refresh interval in seconds, or {@code null} to disable proactive refresh
     */
    public Long getRefreshIntervalSeconds() { return refreshIntervalSeconds; }

    /**
     * Sets the interval in seconds at which the access token is proactively refreshed.
     *
     * @param refreshIntervalSeconds the refresh interval in seconds
     */
    public void setRefreshIntervalSeconds(Long refreshIntervalSeconds) { this.refreshIntervalSeconds = refreshIntervalSeconds; }

    /**
     * Returns the behaviour when a token refresh fails ({@code "continue"} or {@code "fail"}).
     *
     * @return the on-refresh-failure action string; defaults to {@code "continue"}
     */
    public String getOnRefreshFailure() { return onRefreshFailure; }

    /**
     * Sets the behaviour when a token refresh fails.
     *
     * @param onRefreshFailure {@code "continue"} to keep the stale token, or {@code "fail"} to abort
     */
    public void setOnRefreshFailure(String onRefreshFailure) { this.onRefreshFailure = onRefreshFailure; }
}
