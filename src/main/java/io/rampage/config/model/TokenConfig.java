package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for obtaining a bearer token, bound from the {@code token} block inside
 * a {@code security} configuration.
 *
 * <p>The {@code source} field determines how the token value is retrieved: {@code env}
 * reads from an environment variable named by {@code envVar}, while
 * {@code secret-manager} (or {@code sm}) resolves the value via the path in
 * {@code secretPath} (Secret Manager integration is not yet implemented and returns a
 * placeholder).</p>
 */
public class TokenConfig {
    @JsonProperty("source")
    private String source;

    @JsonProperty("envVar")
    private String envVar;

    @JsonProperty("secretPath")
    private String secretPath;

    @JsonProperty("required")
    private boolean required = true;

    /**
     * Constructs a {@code TokenConfig} with all fields at their defaults.
     */
    public TokenConfig() {}

    /**
     * Returns the token source type ({@code env} or {@code secret-manager}/{@code sm}).
     *
     * @return the source type string, or {@code null} if not set
     */
    public String getSource() { return source; }

    /**
     * Sets the token source type.
     *
     * @param source the source type string
     */
    public void setSource(String source) { this.source = source; }

    /**
     * Returns the name of the environment variable from which the token is read when
     * {@code source} is {@code env}.
     *
     * @return the environment variable name, or {@code null} if not applicable
     */
    public String getEnvVar() { return envVar; }

    /**
     * Sets the name of the environment variable from which the token is read.
     *
     * @param envVar the environment variable name
     */
    public void setEnvVar(String envVar) { this.envVar = envVar; }

    /**
     * Returns the secret manager path from which the token is retrieved when {@code source}
     * is {@code secret-manager} or {@code sm}.
     *
     * @return the secret path, or {@code null} if not applicable
     */
    public String getSecretPath() { return secretPath; }

    /**
     * Sets the secret manager path.
     *
     * @param secretPath the secret manager path
     */
    public void setSecretPath(String secretPath) { this.secretPath = secretPath; }

    /**
     * Returns whether the token is required; when {@code true} a missing or empty token
     * causes an error at startup.
     *
     * @return {@code true} if the token is required (the default); {@code false} to allow
     *         an absent token
     */
    public boolean isRequired() { return required; }

    /**
     * Sets whether the token is required.
     *
     * @param required {@code true} if the token must be present
     */
    public void setRequired(boolean required) { this.required = required; }
}
