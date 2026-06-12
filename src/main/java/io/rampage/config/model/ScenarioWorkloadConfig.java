package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Per-scenario workload settings bound from the {@code workload} block in a scenario
 * YAML file.
 *
 * <p>When {@code inheritFromRun} is {@code true} (the default), the run-level workload
 * configuration is used and the remaining fields here are ignored. Set
 * {@code inheritFromRun: false} to apply a workload profile specific to this scenario.</p>
 *
 * <p>Duration values ({@code rampUp}, {@code holdFor}) accept a numeric string with an
 * optional suffix: {@code ms}, {@code s}, {@code m}, or {@code h}. A bare number is
 * interpreted as seconds.</p>
 */
public class ScenarioWorkloadConfig {
    @JsonProperty("inheritFromRun")
    private boolean inheritFromRun = true;

    @JsonProperty("type")
    private String type;

    @JsonProperty("rate")
    private RateConfig rate;

    @JsonProperty("rampUp")
    private String rampUp;

    @JsonProperty("holdFor")
    private String holdFor;

    /**
     * Constructs a {@code ScenarioWorkloadConfig} with all fields at their defaults.
     */
    public ScenarioWorkloadConfig() {}

    /**
     * Returns whether this scenario should inherit the run-level workload profile.
     *
     * @return {@code true} to use the run-level workload (the default); {@code false} to
     *         apply the scenario-level settings below
     */
    public boolean isInheritFromRun() { return inheritFromRun; }

    /**
     * Sets whether this scenario should inherit the run-level workload profile.
     *
     * @param inheritFromRun {@code false} to apply the scenario-level workload settings
     */
    public void setInheritFromRun(boolean inheritFromRun) { this.inheritFromRun = inheritFromRun; }

    /**
     * Returns the workload type (e.g. {@code constant}, {@code ramp-and-hold}, {@code soak}).
     *
     * @return the workload type string, or {@code null} if not set
     */
    public String getType() { return type; }

    /**
     * Sets the workload type.
     *
     * @param type the workload type string
     */
    public void setType(String type) { this.type = type; }

    /**
     * Returns the target request rate for this scenario.
     *
     * @return the rate configuration, or {@code null} if not set
     */
    public RateConfig getRate() { return rate; }

    /**
     * Sets the target request rate for this scenario.
     *
     * @param rate the rate configuration
     */
    public void setRate(RateConfig rate) { this.rate = rate; }

    /**
     * Returns the duration over which virtual users or requests ramp up to the target rate.
     *
     * @return the ramp-up duration string (e.g. {@code "60s"}), or {@code null} if not set
     */
    public String getRampUp() { return rampUp; }

    /**
     * Sets the duration over which virtual users or requests ramp up to the target rate.
     *
     * @param rampUp the ramp-up duration string
     */
    public void setRampUp(String rampUp) { this.rampUp = rampUp; }

    /**
     * Returns the duration for which the target rate is maintained after the ramp-up phase.
     *
     * @return the hold duration string (e.g. {@code "5m"}), or {@code null} if not set
     */
    public String getHoldFor() { return holdFor; }

    /**
     * Sets the duration for which the target rate is maintained after the ramp-up phase.
     *
     * @param holdFor the hold duration string
     */
    public void setHoldFor(String holdFor) { this.holdFor = holdFor; }
}
