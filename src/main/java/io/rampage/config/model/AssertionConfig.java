package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AssertionConfig {
    @JsonProperty("globalResponseTimeMaxMs")
    private long globalResponseTimeMaxMs;

    @JsonProperty("globalResponseTimeMeanMs")
    private long globalResponseTimeMeanMs;

    @JsonProperty("globalErrorRatePercent")
    private double globalErrorRatePercent;

    public AssertionConfig() {}

    public long getGlobalResponseTimeMaxMs() { return globalResponseTimeMaxMs; }
    public void setGlobalResponseTimeMaxMs(long globalResponseTimeMaxMs) { this.globalResponseTimeMaxMs = globalResponseTimeMaxMs; }
    public long getGlobalResponseTimeMeanMs() { return globalResponseTimeMeanMs; }
    public void setGlobalResponseTimeMeanMs(long globalResponseTimeMeanMs) { this.globalResponseTimeMeanMs = globalResponseTimeMeanMs; }
    public double getGlobalErrorRatePercent() { return globalErrorRatePercent; }
    public void setGlobalErrorRatePercent(double globalErrorRatePercent) { this.globalErrorRatePercent = globalErrorRatePercent; }
}
