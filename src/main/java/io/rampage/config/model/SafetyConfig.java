package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Environment-level safety settings bound from the {@code safety} block inside
 * {@code environment.yaml}.
 *
 * <p>These flags describe the nature of the target environment and impose guardrails
 * that prevent accidental production load tests or unapproved mutating requests.</p>
 */
public class SafetyConfig {
    @JsonProperty("allowProduction")
    private boolean allowProduction = false;

    @JsonProperty("requireApprovalForMutatingRequests")
    private boolean requireApprovalForMutatingRequests = false;

    @JsonProperty("isProduction")
    private boolean isProduction = false;

    /**
     * Constructs a {@code SafetyConfig} with all fields at their defaults.
     */
    public SafetyConfig() {}

    /**
     * Returns whether load tests are permitted to target this environment even if it is
     * flagged as a production environment.
     *
     * @return {@code true} if production targeting is allowed; {@code false} by default
     */
    public boolean isAllowProduction() { return allowProduction; }

    /**
     * Sets whether load tests are permitted to target this production environment.
     *
     * @param allowProduction {@code true} to permit production targeting
     */
    public void setAllowProduction(boolean allowProduction) { this.allowProduction = allowProduction; }

    /**
     * Returns whether mutating requests require explicit approval before being sent to this
     * environment.
     *
     * @return {@code true} if approval is required for mutating requests; {@code false} by default
     */
    public boolean isRequireApprovalForMutatingRequests() { return requireApprovalForMutatingRequests; }

    /**
     * Sets whether mutating requests require explicit approval before being sent to this
     * environment.
     *
     * @param requireApprovalForMutatingRequests {@code true} to require approval
     */
    public void setRequireApprovalForMutatingRequests(boolean requireApprovalForMutatingRequests) {
        this.requireApprovalForMutatingRequests = requireApprovalForMutatingRequests;
    }

    /**
     * Returns whether this environment is classified as a production environment.
     *
     * @return {@code true} if the environment is production; {@code false} by default
     */
    public boolean isProduction() { return isProduction; }

    /**
     * Sets whether this environment is classified as a production environment.
     *
     * @param isProduction {@code true} to mark the environment as production
     */
    public void setProduction(boolean isProduction) { this.isProduction = isProduction; }
}
