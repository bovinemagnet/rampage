package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SafetyConfig {
    @JsonProperty("allowProduction")
    private boolean allowProduction = false;

    @JsonProperty("requireApprovalForMutatingRequests")
    private boolean requireApprovalForMutatingRequests = false;

    public SafetyConfig() {}

    public boolean isAllowProduction() { return allowProduction; }
    public void setAllowProduction(boolean allowProduction) { this.allowProduction = allowProduction; }
    public boolean isRequireApprovalForMutatingRequests() { return requireApprovalForMutatingRequests; }
    public void setRequireApprovalForMutatingRequests(boolean requireApprovalForMutatingRequests) {
        this.requireApprovalForMutatingRequests = requireApprovalForMutatingRequests;
    }
}
