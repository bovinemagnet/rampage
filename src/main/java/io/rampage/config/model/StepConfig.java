package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * One ordered step in a multi-step scenario. A scenario with no {@code steps} list is
 * treated as a single synthesised step from its top-level {@code request}/{@code checks}.
 */
public class StepConfig {
    @JsonProperty("name")
    private String name;

    @JsonProperty("endpointRef")
    private String endpointRef;

    @JsonProperty("request")
    private RequestConfig request;

    @JsonProperty("checks")
    private ChecksConfig checks;

    @JsonProperty("extract")
    private List<ExtractConfig> extract;

    @JsonProperty("pauseAfter")
    private AfterRequestPause pauseAfter;

    /**
     * Constructs a {@code StepConfig} with all fields uninitialised.
     */
    public StepConfig() {}

    /**
     * Returns the display name for this step, used in Gatling reports.
     *
     * @return the step name, or {@code null} if not set
     */
    public String getName() { return name; }

    /**
     * Sets the display name for this step.
     *
     * @param name the step name
     */
    public void setName(String name) { this.name = name; }

    /**
     * Returns the key used to look up the base URL in the environment's {@code baseUrls} map,
     * overriding the scenario-level {@code endpointRef} for this step.
     *
     * @return the endpoint reference key, or {@code null} to use the scenario-level value
     */
    public String getEndpointRef() { return endpointRef; }

    /**
     * Sets the endpoint reference key for this step.
     *
     * @param endpointRef the endpoint reference key
     */
    public void setEndpointRef(String endpointRef) { this.endpointRef = endpointRef; }

    /**
     * Returns the request configuration for this step.
     *
     * @return the request configuration, or {@code null} if not set
     */
    public RequestConfig getRequest() { return request; }

    /**
     * Sets the request configuration for this step.
     *
     * @param request the request configuration
     */
    public void setRequest(RequestConfig request) { this.request = request; }

    /**
     * Returns the response check configuration for this step.
     *
     * @return the checks configuration, or {@code null} if not set
     */
    public ChecksConfig getChecks() { return checks; }

    /**
     * Sets the response check configuration for this step.
     *
     * @param checks the checks configuration
     */
    public void setChecks(ChecksConfig checks) { this.checks = checks; }

    /**
     * Returns the list of response-extraction rules that save values into the Gatling session
     * for use in subsequent steps.
     *
     * @return the extract configuration list, or {@code null} if none are configured
     */
    public List<ExtractConfig> getExtract() { return extract; }

    /**
     * Sets the list of response-extraction rules.
     *
     * @param extract the extract configuration list
     */
    public void setExtract(List<ExtractConfig> extract) { this.extract = extract; }

    /**
     * Returns the pause applied after this step completes.
     *
     * @return the after-request pause configuration, or {@code null} if not set
     */
    public AfterRequestPause getPauseAfter() { return pauseAfter; }

    /**
     * Sets the pause applied after this step completes.
     *
     * @param pauseAfter the after-request pause configuration
     */
    public void setPauseAfter(AfterRequestPause pauseAfter) { this.pauseAfter = pauseAfter; }
}
