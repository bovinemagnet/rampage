package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WorkloadConfig {
    @JsonProperty("model")
    private String model;

    @JsonProperty("rampDurationSeconds")
    private long rampDurationSeconds;

    @JsonProperty("holdDurationSeconds")
    private long holdDurationSeconds;

    @JsonProperty("targetCallsPerSecond")
    private double targetCallsPerSecond;

    public WorkloadConfig() {}

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public long getRampDurationSeconds() { return rampDurationSeconds; }
    public void setRampDurationSeconds(long rampDurationSeconds) { this.rampDurationSeconds = rampDurationSeconds; }
    public long getHoldDurationSeconds() { return holdDurationSeconds; }
    public void setHoldDurationSeconds(long holdDurationSeconds) { this.holdDurationSeconds = holdDurationSeconds; }
    public double getTargetCallsPerSecond() { return targetCallsPerSecond; }
    public void setTargetCallsPerSecond(double targetCallsPerSecond) { this.targetCallsPerSecond = targetCallsPerSecond; }
}
