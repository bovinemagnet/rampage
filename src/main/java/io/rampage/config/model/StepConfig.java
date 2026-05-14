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

    public StepConfig() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEndpointRef() { return endpointRef; }
    public void setEndpointRef(String endpointRef) { this.endpointRef = endpointRef; }
    public RequestConfig getRequest() { return request; }
    public void setRequest(RequestConfig request) { this.request = request; }
    public ChecksConfig getChecks() { return checks; }
    public void setChecks(ChecksConfig checks) { this.checks = checks; }
    public List<ExtractConfig> getExtract() { return extract; }
    public void setExtract(List<ExtractConfig> extract) { this.extract = extract; }
    public AfterRequestPause getPauseAfter() { return pauseAfter; }
    public void setPauseAfter(AfterRequestPause pauseAfter) { this.pauseAfter = pauseAfter; }
}
