package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Specifies how a single credential (username or password) is resolved at runtime.
 *
 * <p>Bound to credential fields such as {@code username} and {@code password} within
 * a {@code DatabaseConfig} block in {@code environment.yaml}. The {@code source}
 * field determines the resolution strategy: {@code "env"} reads an environment
 * variable named by {@code envVar}; {@code "secret-manager"} (or {@code "sm"})
 * reads from a secret manager at {@code secretPath} (not yet implemented); and
 * an inline {@code value} may be provided directly.</p>
 */
public class CredentialConfig {
    @JsonProperty("source")
    private String source;

    @JsonProperty("envVar")
    private String envVar;

    @JsonProperty("secretPath")
    private String secretPath;

    @JsonProperty("value")
    private String value;

    @JsonProperty("required")
    private boolean required = true;

    /**
     * Constructs a {@code CredentialConfig} with {@code required} defaulting to {@code true}.
     */
    public CredentialConfig() {}

    /**
     * Returns the credential source type (for example, {@code "env"} or {@code "secret-manager"}).
     *
     * @return the source type, or {@code null} if not configured
     */
    public String getSource() { return source; }

    /**
     * Sets the credential source type.
     *
     * @param source the source type to use
     */
    public void setSource(String source) { this.source = source; }

    /**
     * Returns the name of the environment variable from which the credential is read
     * when {@code source} is {@code "env"}.
     *
     * @return the environment variable name, or {@code null} if not configured
     */
    public String getEnvVar() { return envVar; }

    /**
     * Sets the name of the environment variable from which the credential is read.
     *
     * @param envVar the environment variable name
     */
    public void setEnvVar(String envVar) { this.envVar = envVar; }

    /**
     * Returns the secret manager path from which the credential is read
     * when {@code source} is {@code "secret-manager"} or {@code "sm"}.
     *
     * @return the secret manager path, or {@code null} if not configured
     */
    public String getSecretPath() { return secretPath; }

    /**
     * Sets the secret manager path.
     *
     * @param secretPath the secret manager path to use
     */
    public void setSecretPath(String secretPath) { this.secretPath = secretPath; }

    /**
     * Returns the inline credential value when no external source is used.
     *
     * @return the inline value, or {@code null} if not configured
     */
    public String getValue() { return value; }

    /**
     * Sets the inline credential value.
     *
     * @param value the inline value to use
     */
    public void setValue(String value) { this.value = value; }

    /**
     * Returns whether the credential must be resolved successfully at startup.
     *
     * @return {@code true} if the credential is required; {@code false} otherwise
     */
    public boolean isRequired() { return required; }

    /**
     * Sets whether the credential must be resolved successfully at startup.
     *
     * @param required {@code true} to treat a missing credential as a fatal error
     */
    public void setRequired(boolean required) { this.required = required; }
}
