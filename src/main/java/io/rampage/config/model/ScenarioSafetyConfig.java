package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScenarioSafetyConfig {
    @JsonProperty("mutating")
    private boolean mutating = false;

    @JsonProperty("idempotent")
    private boolean idempotent = true;

    public ScenarioSafetyConfig() {}

    public boolean isMutating() { return mutating; }
    public void setMutating(boolean mutating) { this.mutating = mutating; }
    public boolean isIdempotent() { return idempotent; }
    public void setIdempotent(boolean idempotent) { this.idempotent = idempotent; }
}
