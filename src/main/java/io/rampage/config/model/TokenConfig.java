package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenConfig {
    @JsonProperty("source")
    private String source;

    @JsonProperty("envVar")
    private String envVar;

    @JsonProperty("secretPath")
    private String secretPath;

    public TokenConfig() {}

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getEnvVar() { return envVar; }
    public void setEnvVar(String envVar) { this.envVar = envVar; }
    public String getSecretPath() { return secretPath; }
    public void setSecretPath(String secretPath) { this.secretPath = secretPath; }
}
