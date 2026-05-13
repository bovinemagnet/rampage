package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExecutionConfig {
    @JsonProperty("mode")
    private String mode;

    @JsonProperty("workload")
    private WorkloadConfig workload;

    public ExecutionConfig() {}

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public WorkloadConfig getWorkload() { return workload; }
    public void setWorkload(WorkloadConfig workload) { this.workload = workload; }
}
