package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class ScenarioConfig {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("protocol")
    private String protocol;

    @JsonProperty("endpointRef")
    private String endpointRef;

    @JsonProperty("operationName")
    private String operationName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("headers")
    private Map<String, String> headers;

    @JsonProperty("request")
    private RequestConfig request;

    @JsonProperty("feeder")
    private FeederConfig feeder;

    @JsonProperty("checks")
    private ChecksConfig checks;

    @JsonProperty("workload")
    private ScenarioWorkloadConfig workload;

    @JsonProperty("pauses")
    private PausesConfig pauses;

    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("safety")
    private ScenarioSafetyConfig safety;

    @JsonProperty("security")
    private ScenarioSecurityConfig security;

    public ScenarioConfig() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public String getEndpointRef() { return endpointRef; }
    public void setEndpointRef(String endpointRef) { this.endpointRef = endpointRef; }
    public String getOperationName() { return operationName; }
    public void setOperationName(String operationName) { this.operationName = operationName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public RequestConfig getRequest() { return request; }
    public void setRequest(RequestConfig request) { this.request = request; }
    public FeederConfig getFeeder() { return feeder; }
    public void setFeeder(FeederConfig feeder) { this.feeder = feeder; }
    public ChecksConfig getChecks() { return checks; }
    public void setChecks(ChecksConfig checks) { this.checks = checks; }
    public ScenarioWorkloadConfig getWorkload() { return workload; }
    public void setWorkload(ScenarioWorkloadConfig workload) { this.workload = workload; }
    public PausesConfig getPauses() { return pauses; }
    public void setPauses(PausesConfig pauses) { this.pauses = pauses; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public ScenarioSafetyConfig getSafety() { return safety; }
    public void setSafety(ScenarioSafetyConfig safety) { this.safety = safety; }
    public ScenarioSecurityConfig getSecurity() { return security; }
    public void setSecurity(ScenarioSecurityConfig security) { this.security = security; }
}
