package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RunSafetyConfig {
    @JsonProperty("dryRun")
    private boolean dryRun = false;

    @JsonProperty("requireConfirmation")
    private boolean requireConfirmation = false;

    @JsonProperty("failIfEnvironmentAllowsProduction")
    private boolean failIfEnvironmentAllowsProduction = false;

    public RunSafetyConfig() {}

    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }
    public boolean isRequireConfirmation() { return requireConfirmation; }
    public void setRequireConfirmation(boolean requireConfirmation) { this.requireConfirmation = requireConfirmation; }
    public boolean isFailIfEnvironmentAllowsProduction() { return failIfEnvironmentAllowsProduction; }
    public void setFailIfEnvironmentAllowsProduction(boolean failIfEnvironmentAllowsProduction) { this.failIfEnvironmentAllowsProduction = failIfEnvironmentAllowsProduction; }
}
