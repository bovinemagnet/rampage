package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GlobalAssertionConfig {
    @JsonProperty("maxResponseTimeP95Millis")
    private long maxResponseTimeP95Millis;

    @JsonProperty("maxResponseTimeP99Millis")
    private long maxResponseTimeP99Millis;

    @JsonProperty("maxErrorPercentage")
    private double maxErrorPercentage;

    public GlobalAssertionConfig() {}

    public long getMaxResponseTimeP95Millis() { return maxResponseTimeP95Millis; }
    public void setMaxResponseTimeP95Millis(long maxResponseTimeP95Millis) { this.maxResponseTimeP95Millis = maxResponseTimeP95Millis; }
    public long getMaxResponseTimeP99Millis() { return maxResponseTimeP99Millis; }
    public void setMaxResponseTimeP99Millis(long maxResponseTimeP99Millis) { this.maxResponseTimeP99Millis = maxResponseTimeP99Millis; }
    public double getMaxErrorPercentage() { return maxErrorPercentage; }
    public void setMaxErrorPercentage(double maxErrorPercentage) { this.maxErrorPercentage = maxErrorPercentage; }
}
