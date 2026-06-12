package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Run-level workload profile bound from the {@code execution.workload} block in
 * {@code run.yaml}.
 *
 * <p>The {@code type} field selects the injection strategy. Supported types are
 * {@code smoke}, {@code constant}, {@code soak}, and {@code ramp-and-hold}. Unknown
 * types fall back to a single-user smoke test.</p>
 *
 * <p>Duration fields ({@code rampUp}, {@code holdFor}, {@code duration},
 * {@code spikeDuration}, {@code stepDuration}) accept a numeric string with an optional
 * suffix: {@code ms}, {@code s}, {@code m}, or {@code h}. A bare number is interpreted as
 * seconds.</p>
 */
public class WorkloadConfig {
    @JsonProperty("type")
    private String type;

    @JsonProperty("rampUp")
    private String rampUp;

    @JsonProperty("holdFor")
    private String holdFor;

    @JsonProperty("duration")
    private String duration;

    @JsonProperty("rate")
    private RateConfig rate;

    @JsonProperty("users")
    private int users = 1;

    @JsonProperty("baselineRate")
    private Double baselineRate;

    @JsonProperty("spikeDuration")
    private String spikeDuration;

    @JsonProperty("stepRate")
    private Double stepRate;

    @JsonProperty("stepDuration")
    private String stepDuration;

    @JsonProperty("maxRate")
    private Double maxRate;

    /**
     * Constructs a {@code WorkloadConfig} with all fields at their defaults.
     */
    public WorkloadConfig() {}

    /**
     * Returns the workload type (e.g. {@code smoke}, {@code constant}, {@code soak},
     * {@code ramp-and-hold}).
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
     * Returns the duration over which virtual users or the request rate ramps up.
     *
     * @return the ramp-up duration string (e.g. {@code "60s"}), or {@code null} if not set
     */
    public String getRampUp() { return rampUp; }

    /**
     * Sets the duration over which virtual users or the request rate ramps up.
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

    /**
     * Returns the total run duration used by workload types that do not split into
     * ramp-up and hold phases.
     *
     * @return the duration string, or {@code null} if not set
     */
    public String getDuration() { return duration; }

    /**
     * Sets the total run duration.
     *
     * @param duration the duration string
     */
    public void setDuration(String duration) { this.duration = duration; }

    /**
     * Returns the target request rate configuration.
     *
     * @return the rate configuration, or {@code null} if not set
     */
    public RateConfig getRate() { return rate; }

    /**
     * Sets the target request rate configuration.
     *
     * @param rate the rate configuration
     */
    public void setRate(RateConfig rate) { this.rate = rate; }

    /**
     * Returns the number of concurrent virtual users for user-based workload types.
     *
     * @return the user count; defaults to {@code 1}
     */
    public int getUsers() { return users; }

    /**
     * Sets the number of concurrent virtual users.
     *
     * @param users the user count
     */
    public void setUsers(int users) { this.users = users; }

    /**
     * Returns the baseline request rate applied during the normal phase of a spike workload.
     *
     * @return the baseline rate in requests per second, or {@code null} if not set
     */
    public Double getBaselineRate() { return baselineRate; }

    /**
     * Sets the baseline request rate for a spike workload.
     *
     * @param baselineRate the baseline rate in requests per second
     */
    public void setBaselineRate(Double baselineRate) { this.baselineRate = baselineRate; }

    /**
     * Returns the duration of the spike phase in a spike workload.
     *
     * @return the spike duration string, or {@code null} if not set
     */
    public String getSpikeDuration() { return spikeDuration; }

    /**
     * Sets the duration of the spike phase.
     *
     * @param spikeDuration the spike duration string
     */
    public void setSpikeDuration(String spikeDuration) { this.spikeDuration = spikeDuration; }

    /**
     * Returns the rate increment applied at each step in a stepped workload.
     *
     * @return the step rate in requests per second, or {@code null} if not set
     */
    public Double getStepRate() { return stepRate; }

    /**
     * Sets the rate increment per step.
     *
     * @param stepRate the step rate in requests per second
     */
    public void setStepRate(Double stepRate) { this.stepRate = stepRate; }

    /**
     * Returns the duration of each step in a stepped workload.
     *
     * @return the step duration string, or {@code null} if not set
     */
    public String getStepDuration() { return stepDuration; }

    /**
     * Sets the duration of each step.
     *
     * @param stepDuration the step duration string
     */
    public void setStepDuration(String stepDuration) { this.stepDuration = stepDuration; }

    /**
     * Returns the maximum request rate cap for workload types that ramp or step upward.
     *
     * @return the maximum rate in requests per second, or {@code null} if not set
     */
    public Double getMaxRate() { return maxRate; }

    /**
     * Sets the maximum request rate cap.
     *
     * @param maxRate the maximum rate in requests per second
     */
    public void setMaxRate(Double maxRate) { this.maxRate = maxRate; }
}
