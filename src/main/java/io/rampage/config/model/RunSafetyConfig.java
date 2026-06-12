package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Run-level safety guards bound from the {@code safety} block inside {@code run.yaml}.
 *
 * <p>These flags control behaviours that are evaluated before a load test is allowed to
 * execute, such as dry-run mode and confirmation requirements for mutating requests.</p>
 */
public class RunSafetyConfig {
    @JsonProperty("dryRun")
    private boolean dryRun = false;

    @JsonProperty("requireConfirmation")
    private boolean requireConfirmation = false;

    @JsonProperty("failIfEnvironmentAllowsProduction")
    private boolean failIfEnvironmentAllowsProduction = false;

    @JsonProperty("approveMutatingRequests")
    private boolean approveMutatingRequests = false;

    /**
     * Constructs a {@code RunSafetyConfig} with all fields at their defaults.
     */
    public RunSafetyConfig() {}

    /**
     * Returns whether the run should execute in dry-run mode, producing no real traffic.
     *
     * @return {@code true} if dry-run mode is enabled; {@code false} by default
     */
    public boolean isDryRun() { return dryRun; }

    /**
     * Sets whether the run executes in dry-run mode.
     *
     * @param dryRun {@code true} to enable dry-run mode
     */
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }

    /**
     * Returns whether a manual confirmation is required before the run starts.
     *
     * @return {@code true} if confirmation is required; {@code false} by default
     */
    public boolean isRequireConfirmation() { return requireConfirmation; }

    /**
     * Sets whether a manual confirmation is required before the run starts.
     *
     * @param requireConfirmation {@code true} to require confirmation
     */
    public void setRequireConfirmation(boolean requireConfirmation) { this.requireConfirmation = requireConfirmation; }

    /**
     * Returns whether the run should abort when the target environment permits production traffic.
     *
     * @return {@code true} if the run fails when {@code allowProduction} is set on the environment
     */
    public boolean isFailIfEnvironmentAllowsProduction() { return failIfEnvironmentAllowsProduction; }

    /**
     * Sets whether the run aborts when the target environment permits production traffic.
     *
     * @param failIfEnvironmentAllowsProduction {@code true} to abort on a production-enabled environment
     */
    public void setFailIfEnvironmentAllowsProduction(boolean failIfEnvironmentAllowsProduction) { this.failIfEnvironmentAllowsProduction = failIfEnvironmentAllowsProduction; }

    /**
     * Returns whether mutating requests must be explicitly approved before execution.
     *
     * @return {@code true} if mutating-request approval is required; {@code false} by default
     */
    public boolean isApproveMutatingRequests() { return approveMutatingRequests; }

    /**
     * Sets whether mutating requests must be explicitly approved before execution.
     *
     * @param approveMutatingRequests {@code true} to require approval for mutating requests
     */
    public void setApproveMutatingRequests(boolean approveMutatingRequests) { this.approveMutatingRequests = approveMutatingRequests; }
}
