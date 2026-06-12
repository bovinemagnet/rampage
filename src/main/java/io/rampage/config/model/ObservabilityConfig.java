package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for observability headers injected into every HTTP request.
 *
 * <p>Bound to the {@code observability} key in {@code environment.yaml}.
 * When {@code correlationIdHeader} is set, {@code HttpProtocolFactory} adds a header with
 * that name and a Gatling EL value of {@code #{correlationId}}. Note that no session setup
 * currently populates the {@code correlationId} session attribute, so this feature requires
 * additional wiring before it produces meaningful values.</p>
 *
 * <p>When {@code includeRunMetadataHeaders} is {@code true}, run metadata (owner, application,
 * service, change reference) is intended to be propagated as request headers, though the exact
 * wiring depends on the factory implementation.</p>
 */
public class ObservabilityConfig {
    @JsonProperty("correlationIdHeader")
    private String correlationIdHeader;

    @JsonProperty("includeRunMetadataHeaders")
    private boolean includeRunMetadataHeaders = false;

    /**
     * Constructs an {@code ObservabilityConfig} with {@code includeRunMetadataHeaders} defaulting to {@code false}.
     */
    public ObservabilityConfig() {}

    /**
     * Returns the HTTP header name used to carry a per-request correlation identifier.
     *
     * @return the correlation ID header name, or {@code null} if not configured
     */
    public String getCorrelationIdHeader() { return correlationIdHeader; }

    /**
     * Sets the HTTP header name used to carry a per-request correlation identifier.
     * Bound to the {@code correlationIdHeader} key.
     *
     * @param correlationIdHeader the header name
     */
    public void setCorrelationIdHeader(String correlationIdHeader) { this.correlationIdHeader = correlationIdHeader; }

    /**
     * Returns whether run metadata fields are propagated as request headers.
     *
     * @return {@code true} if run metadata headers are included; {@code false} by default
     */
    public boolean isIncludeRunMetadataHeaders() { return includeRunMetadataHeaders; }

    /**
     * Sets whether run metadata fields are propagated as request headers.
     * Bound to the {@code includeRunMetadataHeaders} key.
     *
     * @param includeRunMetadataHeaders {@code true} to include run metadata headers
     */
    public void setIncludeRunMetadataHeaders(boolean includeRunMetadataHeaders) { this.includeRunMetadataHeaders = includeRunMetadataHeaders; }
}
