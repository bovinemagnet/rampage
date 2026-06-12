package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configures a pause inserted after each request in a scenario.
 *
 * <p>Bound to the {@code afterRequestPause} key in a scenario YAML file.
 * The pause duration is chosen according to {@code strategy}; {@code minMillis}
 * and {@code maxMillis} define the boundaries when a random strategy is used.</p>
 */
public class AfterRequestPause {
    @JsonProperty("strategy")
    private String strategy;

    @JsonProperty("minMillis")
    private long minMillis;

    @JsonProperty("maxMillis")
    private long maxMillis;

    /**
     * Constructs an {@code AfterRequestPause} with all fields at their default values.
     */
    public AfterRequestPause() {}

    /**
     * Returns the pause strategy (for example, {@code "fixed"} or {@code "uniform"}).
     *
     * @return the strategy name, or {@code null} if not configured
     */
    public String getStrategy() { return strategy; }

    /**
     * Sets the pause strategy.
     *
     * @param strategy the strategy name to use
     */
    public void setStrategy(String strategy) { this.strategy = strategy; }

    /**
     * Returns the minimum pause duration in milliseconds.
     *
     * @return the minimum pause in milliseconds
     */
    public long getMinMillis() { return minMillis; }

    /**
     * Sets the minimum pause duration in milliseconds.
     *
     * @param minMillis the minimum pause in milliseconds
     */
    public void setMinMillis(long minMillis) { this.minMillis = minMillis; }

    /**
     * Returns the maximum pause duration in milliseconds.
     *
     * @return the maximum pause in milliseconds
     */
    public long getMaxMillis() { return maxMillis; }

    /**
     * Sets the maximum pause duration in milliseconds.
     *
     * @param maxMillis the maximum pause in milliseconds
     */
    public void setMaxMillis(long maxMillis) { this.maxMillis = maxMillis; }
}
