package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Full configuration for a single load-test scenario, bound from a scenario YAML file.
 *
 * <p>Each scenario file is identified by its {@code id}, which must match a
 * {@code ScenarioRef} entry in {@code run.yaml}. The scenario describes the protocol,
 * target endpoint, GraphQL request, feeder, response checks, workload override,
 * and safety/security constraints for one logical user journey.</p>
 *
 * <p>A scenario with no {@code steps} list is treated by the simulation as a single
 * synthesised step using the top-level {@code request} and {@code checks} fields.
 * When a {@code steps} list is present, those top-level fields are ignored.</p>
 */
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

    @JsonProperty("steps")
    private List<StepConfig> steps;

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

    /**
     * Constructs a {@code ScenarioConfig} with all fields uninitialised.
     */
    public ScenarioConfig() {}

    /**
     * Returns the unique identifier for this scenario, used to match against
     * {@code ScenarioRef} entries in {@code run.yaml}.
     *
     * @return the scenario id, or {@code null} if not set
     */
    public String getId() { return id; }

    /**
     * Sets the unique identifier for this scenario.
     *
     * @param id the scenario id
     */
    public void setId(String id) { this.id = id; }

    /**
     * Returns the human-readable display name for this scenario.
     *
     * @return the scenario name, or {@code null} if not set
     */
    public String getName() { return name; }

    /**
     * Sets the human-readable display name for this scenario.
     *
     * @param name the scenario name
     */
    public void setName(String name) { this.name = name; }

    /**
     * Returns the protocol identifier (e.g. {@code graphql}, {@code rest}).
     *
     * <p>This field is informational; only GraphQL is wired through the simulation.</p>
     *
     * @return the protocol string, or {@code null} if not set
     */
    public String getProtocol() { return protocol; }

    /**
     * Sets the protocol identifier.
     *
     * @param protocol the protocol string
     */
    public void setProtocol(String protocol) { this.protocol = protocol; }

    /**
     * Returns the key used to look up the base URL in the environment's {@code baseUrls} map.
     *
     * @return the endpoint reference key, or {@code null} if not set
     */
    public String getEndpointRef() { return endpointRef; }

    /**
     * Sets the key used to look up the base URL in the environment's {@code baseUrls} map.
     *
     * @param endpointRef the endpoint reference key
     */
    public void setEndpointRef(String endpointRef) { this.endpointRef = endpointRef; }

    /**
     * Returns the GraphQL operation name sent in request bodies.
     *
     * @return the operation name, or {@code null} if not set
     */
    public String getOperationName() { return operationName; }

    /**
     * Sets the GraphQL operation name sent in request bodies.
     *
     * @param operationName the operation name
     */
    public void setOperationName(String operationName) { this.operationName = operationName; }

    /**
     * Returns a free-text description of the scenario.
     *
     * @return the description, or {@code null} if not set
     */
    public String getDescription() { return description; }

    /**
     * Sets a free-text description of the scenario.
     *
     * @param description the description text
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Returns the scenario-level HTTP headers added to every request in this scenario.
     *
     * @return a map of header name to value, or {@code null} if none are configured
     */
    public Map<String, String> getHeaders() { return headers; }

    /**
     * Sets the scenario-level HTTP headers added to every request.
     *
     * @param headers a map of header name to value
     */
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }

    /**
     * Returns the request configuration for a single-step scenario.
     *
     * @return the request configuration, or {@code null} if using a {@code steps} list
     */
    public RequestConfig getRequest() { return request; }

    /**
     * Sets the request configuration for a single-step scenario.
     *
     * @param request the request configuration
     */
    public void setRequest(RequestConfig request) { this.request = request; }

    /**
     * Returns the feeder configuration that supplies per-virtual-user data.
     *
     * @return the feeder configuration, or {@code null} if not set
     */
    public FeederConfig getFeeder() { return feeder; }

    /**
     * Sets the feeder configuration that supplies per-virtual-user data.
     *
     * @param feeder the feeder configuration
     */
    public void setFeeder(FeederConfig feeder) { this.feeder = feeder; }

    /**
     * Returns the response check configuration for a single-step scenario.
     *
     * @return the checks configuration, or {@code null} if not set
     */
    public ChecksConfig getChecks() { return checks; }

    /**
     * Sets the response check configuration for a single-step scenario.
     *
     * @param checks the checks configuration
     */
    public void setChecks(ChecksConfig checks) { this.checks = checks; }

    /**
     * Returns the ordered list of steps for a multi-step scenario.
     *
     * <p>When present, the top-level {@code request} and {@code checks} fields are ignored.</p>
     *
     * @return the list of step configurations, or {@code null} for a single-step scenario
     */
    public List<StepConfig> getSteps() { return steps; }

    /**
     * Sets the ordered list of steps for a multi-step scenario.
     *
     * @param steps the list of step configurations
     */
    public void setSteps(List<StepConfig> steps) { this.steps = steps; }

    /**
     * Returns the scenario-level workload override.
     *
     * <p>Applied only when {@code inheritFromRun} is {@code false}; otherwise the
     * run-level workload is used.</p>
     *
     * @return the scenario workload configuration, or {@code null} if not set
     */
    public ScenarioWorkloadConfig getWorkload() { return workload; }

    /**
     * Sets the scenario-level workload override.
     *
     * @param workload the scenario workload configuration
     */
    public void setWorkload(ScenarioWorkloadConfig workload) { this.workload = workload; }

    /**
     * Returns the pause configuration applied between steps or requests.
     *
     * @return the pauses configuration, or {@code null} if not set
     */
    public PausesConfig getPauses() { return pauses; }

    /**
     * Sets the pause configuration applied between steps or requests.
     *
     * @param pauses the pauses configuration
     */
    public void setPauses(PausesConfig pauses) { this.pauses = pauses; }

    /**
     * Returns the list of tags associated with this scenario, used for grouping and filtering.
     *
     * @return the tag list, or {@code null} if none are configured
     */
    public List<String> getTags() { return tags; }

    /**
     * Sets the list of tags associated with this scenario.
     *
     * @param tags the tag list
     */
    public void setTags(List<String> tags) { this.tags = tags; }

    /**
     * Returns the safety classification for this scenario.
     *
     * @return the safety configuration, or {@code null} if not set
     */
    public ScenarioSafetyConfig getSafety() { return safety; }

    /**
     * Sets the safety classification for this scenario.
     *
     * @param safety the safety configuration
     */
    public void setSafety(ScenarioSafetyConfig safety) { this.safety = safety; }

    /**
     * Returns the security settings specific to this scenario.
     *
     * @return the security configuration, or {@code null} if not set
     */
    public ScenarioSecurityConfig getSecurity() { return security; }

    /**
     * Sets the security settings specific to this scenario.
     *
     * @param security the security configuration
     */
    public void setSecurity(ScenarioSecurityConfig security) { this.security = security; }
}
