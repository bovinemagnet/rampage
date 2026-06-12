package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Threshold configuration for global Gatling assertions applied across all scenarios.
 *
 * <p>Bound to the {@code assertions} key in {@code run.yaml} via {@code AssertionsConfig}.
 * The thresholds declared here are translated into Gatling global assertions by the simulation.
 * Only the three properties below are currently wired through; scenario-level assertion config
 * exists in the model but is not yet translated.</p>
 */
public class GlobalAssertionConfig {
    @JsonProperty("maxResponseTimeP95Millis")
    private long maxResponseTimeP95Millis;

    @JsonProperty("maxResponseTimeP99Millis")
    private long maxResponseTimeP99Millis;

    @JsonProperty("maxErrorPercentage")
    private double maxErrorPercentage;

    /**
     * Constructs a {@code GlobalAssertionConfig} with all thresholds initialised to zero.
     */
    public GlobalAssertionConfig() {}

    /**
     * Returns the maximum acceptable 95th-percentile response time in milliseconds.
     *
     * @return the P95 response-time threshold in milliseconds
     */
    public long getMaxResponseTimeP95Millis() { return maxResponseTimeP95Millis; }

    /**
     * Sets the maximum acceptable 95th-percentile response time in milliseconds.
     * Bound to the {@code maxResponseTimeP95Millis} key.
     *
     * @param maxResponseTimeP95Millis the P95 threshold in milliseconds
     */
    public void setMaxResponseTimeP95Millis(long maxResponseTimeP95Millis) { this.maxResponseTimeP95Millis = maxResponseTimeP95Millis; }

    /**
     * Returns the maximum acceptable 99th-percentile response time in milliseconds.
     *
     * @return the P99 response-time threshold in milliseconds
     */
    public long getMaxResponseTimeP99Millis() { return maxResponseTimeP99Millis; }

    /**
     * Sets the maximum acceptable 99th-percentile response time in milliseconds.
     * Bound to the {@code maxResponseTimeP99Millis} key.
     *
     * @param maxResponseTimeP99Millis the P99 threshold in milliseconds
     */
    public void setMaxResponseTimeP99Millis(long maxResponseTimeP99Millis) { this.maxResponseTimeP99Millis = maxResponseTimeP99Millis; }

    /**
     * Returns the maximum acceptable error percentage across all requests.
     *
     * @return the error-rate threshold as a percentage (0–100)
     */
    public double getMaxErrorPercentage() { return maxErrorPercentage; }

    /**
     * Sets the maximum acceptable error percentage across all requests.
     * Bound to the {@code maxErrorPercentage} key.
     *
     * @param maxErrorPercentage the error-rate threshold as a percentage (0–100)
     */
    public void setMaxErrorPercentage(double maxErrorPercentage) { this.maxErrorPercentage = maxErrorPercentage; }
}
