package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request-rate range used by ramping workload types.
 *
 * <p>Bound to the {@code rate} key within a {@code WorkloadConfig} entry.
 * {@code from} and {@code to} define the start and end of a rate ramp, and
 * {@code unit} specifies the time unit (for example {@code "per_second"} or
 * {@code "per_minute"}). Used by {@code WorkloadFactory} when building
 * {@code ramp-and-hold} and similar injection profiles.</p>
 */
public class RateConfig {
    @JsonProperty("unit")
    private String unit;

    @JsonProperty("from")
    private double from;

    @JsonProperty("to")
    private double to;

    /**
     * Constructs a {@code RateConfig} with all fields uninitialised.
     */
    public RateConfig() {}

    /**
     * Returns the time unit for the rate values (for example {@code "per_second"}).
     *
     * @return the rate unit, or {@code null} if not set
     */
    public String getUnit() { return unit; }

    /**
     * Sets the time unit for the rate values.
     * Bound to the {@code unit} key.
     *
     * @param unit the rate unit
     */
    public void setUnit(String unit) { this.unit = unit; }

    /**
     * Returns the starting rate at the beginning of the ramp.
     *
     * @return the starting rate
     */
    public double getFrom() { return from; }

    /**
     * Sets the starting rate at the beginning of the ramp.
     * Bound to the {@code from} key.
     *
     * @param from the starting rate
     */
    public void setFrom(double from) { this.from = from; }

    /**
     * Returns the target rate at the end of the ramp.
     *
     * @return the target rate
     */
    public double getTo() { return to; }

    /**
     * Sets the target rate at the end of the ramp.
     * Bound to the {@code to} key.
     *
     * @param to the target rate
     */
    public void setTo(double to) { this.to = to; }
}
