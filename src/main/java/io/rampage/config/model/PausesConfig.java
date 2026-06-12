package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for think-time pauses inserted around each request in a scenario.
 *
 * <p>Bound to the {@code pauses} key within a scenario YAML file.
 * {@code beforeRequestMillis} introduces a fixed pause before every request.
 * {@code afterRequest} supplies more detailed control over the pause that follows each
 * request and is described by {@code AfterRequestPause}.</p>
 */
public class PausesConfig {
    @JsonProperty("beforeRequestMillis")
    private long beforeRequestMillis;

    @JsonProperty("afterRequest")
    private AfterRequestPause afterRequest;

    /**
     * Constructs a {@code PausesConfig} with all fields uninitialised.
     */
    public PausesConfig() {}

    /**
     * Returns the fixed pause duration in milliseconds inserted before each request.
     *
     * @return the pre-request pause in milliseconds; zero if not configured
     */
    public long getBeforeRequestMillis() { return beforeRequestMillis; }

    /**
     * Sets the fixed pause duration in milliseconds inserted before each request.
     * Bound to the {@code beforeRequestMillis} key.
     *
     * @param beforeRequestMillis the pre-request pause in milliseconds
     */
    public void setBeforeRequestMillis(long beforeRequestMillis) { this.beforeRequestMillis = beforeRequestMillis; }

    /**
     * Returns the post-request pause configuration.
     *
     * @return the after-request pause configuration, or {@code null} if not set
     */
    public AfterRequestPause getAfterRequest() { return afterRequest; }

    /**
     * Sets the post-request pause configuration.
     * Bound to the {@code afterRequest} key.
     *
     * @param afterRequest the after-request pause configuration
     */
    public void setAfterRequest(AfterRequestPause afterRequest) { this.afterRequest = afterRequest; }
}
