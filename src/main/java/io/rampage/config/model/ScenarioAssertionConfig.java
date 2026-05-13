package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScenarioAssertionConfig {
    @JsonProperty("maxResponseTimeP95Millis")
    private long maxResponseTimeP95Millis;

    @JsonProperty("maxErrorPercentage")
    private double maxErrorPercentage;

    public ScenarioAssertionConfig() {}

    public long getMaxResponseTimeP95Millis() { return maxResponseTimeP95Millis; }
    public void setMaxResponseTimeP95Millis(long maxResponseTimeP95Millis) { this.maxResponseTimeP95Millis = maxResponseTimeP95Millis; }
    public double getMaxErrorPercentage() { return maxErrorPercentage; }
    public void setMaxErrorPercentage(double maxErrorPercentage) { this.maxErrorPercentage = maxErrorPercentage; }
}
