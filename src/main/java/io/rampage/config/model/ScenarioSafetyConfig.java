package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Safety classification for a scenario, bound from the {@code safety} block in a scenario
 * YAML file.
 *
 * <p>These flags indicate whether the scenario sends write requests, allowing the safety
 * framework to enforce approval policies before mutating requests reach a protected
 * environment.</p>
 */
public class ScenarioSafetyConfig {
    @JsonProperty("mutating")
    private boolean mutating = false;

    @JsonProperty("idempotent")
    private boolean idempotent = true;

    /**
     * Constructs a {@code ScenarioSafetyConfig} with all fields at their defaults.
     */
    public ScenarioSafetyConfig() {}

    /**
     * Returns whether this scenario sends mutating (write) requests.
     *
     * @return {@code true} if the scenario modifies state; {@code false} by default
     */
    public boolean isMutating() { return mutating; }

    /**
     * Sets whether this scenario sends mutating (write) requests.
     *
     * @param mutating {@code true} if the scenario modifies state
     */
    public void setMutating(boolean mutating) { this.mutating = mutating; }

    /**
     * Returns whether repeated executions of this scenario produce the same result.
     *
     * @return {@code true} if the scenario is idempotent (the default); {@code false} otherwise
     */
    public boolean isIdempotent() { return idempotent; }

    /**
     * Sets whether repeated executions of this scenario produce the same result.
     *
     * @param idempotent {@code true} if the scenario is idempotent
     */
    public void setIdempotent(boolean idempotent) { this.idempotent = idempotent; }
}
