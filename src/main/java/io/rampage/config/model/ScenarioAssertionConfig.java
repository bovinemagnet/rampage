package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Per-scenario assertion thresholds bound from the {@code assertions} block inside a
 * scenario YAML file.
 *
 * <p>These values constrain the 95th-percentile response time and error percentage for an
 * individual scenario. Note that scenario-level assertions are present in the model but are
 * not yet wired through to the Gatling simulation; only global assertions are currently
 * applied.</p>
 */
public class ScenarioAssertionConfig {
    @JsonProperty("maxResponseTimeP95Millis")
    private long maxResponseTimeP95Millis;

    @JsonProperty("maxErrorPercentage")
    private double maxErrorPercentage;

    /**
     * Constructs a {@code ScenarioAssertionConfig} with all fields at their defaults.
     */
    public ScenarioAssertionConfig() {}

    /**
     * Returns the maximum acceptable 95th-percentile response time in milliseconds.
     *
     * @return the P95 response-time threshold in milliseconds
     */
    public long getMaxResponseTimeP95Millis() { return maxResponseTimeP95Millis; }

    /**
     * Sets the maximum acceptable 95th-percentile response time in milliseconds.
     *
     * @param maxResponseTimeP95Millis the P95 response-time threshold in milliseconds
     */
    public void setMaxResponseTimeP95Millis(long maxResponseTimeP95Millis) { this.maxResponseTimeP95Millis = maxResponseTimeP95Millis; }

    /**
     * Returns the maximum acceptable error percentage for this scenario.
     *
     * @return the error percentage threshold (0–100)
     */
    public double getMaxErrorPercentage() { return maxErrorPercentage; }

    /**
     * Sets the maximum acceptable error percentage for this scenario.
     *
     * @param maxErrorPercentage the error percentage threshold (0–100)
     */
    public void setMaxErrorPercentage(double maxErrorPercentage) { this.maxErrorPercentage = maxErrorPercentage; }
}
