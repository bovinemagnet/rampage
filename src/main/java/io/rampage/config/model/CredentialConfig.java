package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

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

    public CredentialConfig() {}

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getEnvVar() { return envVar; }
    public void setEnvVar(String envVar) { this.envVar = envVar; }
    public String getSecretPath() { return secretPath; }
    public void setSecretPath(String secretPath) { this.secretPath = secretPath; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
}
