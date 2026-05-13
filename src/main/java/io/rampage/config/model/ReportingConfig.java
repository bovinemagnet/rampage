package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReportingConfig {
    @JsonProperty("outputDirectory")
    private String outputDirectory;

    @JsonProperty("writeRunMetadata")
    private boolean writeRunMetadata = false;

    @JsonProperty("redactSecrets")
    private boolean redactSecrets = true;

    public ReportingConfig() {}

    public String getOutputDirectory() { return outputDirectory; }
    public void setOutputDirectory(String outputDirectory) { this.outputDirectory = outputDirectory; }
    public boolean isWriteRunMetadata() { return writeRunMetadata; }
    public void setWriteRunMetadata(boolean writeRunMetadata) { this.writeRunMetadata = writeRunMetadata; }
    public boolean isRedactSecrets() { return redactSecrets; }
    public void setRedactSecrets(boolean redactSecrets) { this.redactSecrets = redactSecrets; }
}
