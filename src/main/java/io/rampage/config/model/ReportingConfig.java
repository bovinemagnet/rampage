package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReportingConfig {
    @JsonProperty("outputDir")
    private String outputDir;

    public ReportingConfig() {}

    public String getOutputDir() { return outputDir; }
    public void setOutputDir(String outputDir) { this.outputDir = outputDir; }
}
