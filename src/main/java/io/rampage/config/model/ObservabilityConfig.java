package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ObservabilityConfig {
    @JsonProperty("correlationIdHeader")
    private String correlationIdHeader;

    @JsonProperty("includeRunMetadataHeaders")
    private boolean includeRunMetadataHeaders = false;

    public ObservabilityConfig() {}

    public String getCorrelationIdHeader() { return correlationIdHeader; }
    public void setCorrelationIdHeader(String correlationIdHeader) { this.correlationIdHeader = correlationIdHeader; }
    public boolean isIncludeRunMetadataHeaders() { return includeRunMetadataHeaders; }
    public void setIncludeRunMetadataHeaders(boolean includeRunMetadataHeaders) { this.includeRunMetadataHeaders = includeRunMetadataHeaders; }
}
