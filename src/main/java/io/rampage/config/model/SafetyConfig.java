package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SafetyConfig {
    @JsonProperty("maxUsersPerSecond")
    private double maxUsersPerSecond;

    @JsonProperty("maxDurationSeconds")
    private long maxDurationSeconds;

    @JsonProperty("enabled")
    private boolean enabled = true;

    public SafetyConfig() {}

    public double getMaxUsersPerSecond() { return maxUsersPerSecond; }
    public void setMaxUsersPerSecond(double maxUsersPerSecond) { this.maxUsersPerSecond = maxUsersPerSecond; }
    public long getMaxDurationSeconds() { return maxDurationSeconds; }
    public void setMaxDurationSeconds(long maxDurationSeconds) { this.maxDurationSeconds = maxDurationSeconds; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
