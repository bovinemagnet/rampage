package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Top-level configuration for a Rampage load-test run.
 *
 * <p>Bound to the root of {@code run.yaml} (or the file supplied via the
 * {@code loadtest.run} system property). This is the central object that drives
 * the simulation: it references the scenarios to execute, the execution workload,
 * global assertions, reporting options, and safety guards.</p>
 *
 * <p>{@link ScenarioRef} entries in {@code scenarios} must each have an {@code id}
 * that matches a loaded scenario configuration; {@code ConfigValidator} enforces
 * this constraint.</p>
 */
public class RunConfig {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("version")
    private int version = 1;

    @JsonProperty("environment")
    private String environment;

    @JsonProperty("metadata")
    private MetadataConfig metadata;

    @JsonProperty("scenarios")
    private List<ScenarioRef> scenarios;

    @JsonProperty("execution")
    private ExecutionConfig execution;

    @JsonProperty("assertions")
    private AssertionsConfig assertions;

    @JsonProperty("reporting")
    private ReportingConfig reporting;

    @JsonProperty("safety")
    private RunSafetyConfig safety;

    @JsonProperty("headers")
    private Map<String, String> headers;

    /**
     * Constructs a {@code RunConfig} with {@code version} defaulting to 1.
     */
    public RunConfig() {}

    /**
     * Returns the unique identifier for this run configuration.
     *
     * @return the run ID, or {@code null} if not set
     */
    public String getId() { return id; }

    /**
     * Sets the unique identifier for this run configuration.
     * Bound to the {@code id} key.
     *
     * @param id the run ID
     */
    public void setId(String id) { this.id = id; }

    /**
     * Returns the human-readable name for this run.
     *
     * @return the run name, or {@code null} if not set
     */
    public String getName() { return name; }

    /**
     * Sets the human-readable name for this run.
     * Bound to the {@code name} key.
     *
     * @param name the run name
     */
    public void setName(String name) { this.name = name; }

    /**
     * Returns the schema version of this run configuration file.
     *
     * @return the version number; default is 1
     */
    public int getVersion() { return version; }

    /**
     * Sets the schema version of this run configuration file.
     * Bound to the {@code version} key.
     *
     * @param version the version number
     */
    public void setVersion(int version) { this.version = version; }

    /**
     * Returns the logical environment identifier used to select the appropriate {@code EnvironmentConfig}.
     *
     * @return the environment identifier, or {@code null} if not set
     */
    public String getEnvironment() { return environment; }

    /**
     * Sets the logical environment identifier.
     * Bound to the {@code environment} key.
     *
     * @param environment the environment identifier
     */
    public void setEnvironment(String environment) { this.environment = environment; }

    /**
     * Returns the optional metadata attached to this run.
     *
     * @return the metadata, or {@code null} if not configured
     */
    public MetadataConfig getMetadata() { return metadata; }

    /**
     * Sets the optional metadata attached to this run.
     * Bound to the {@code metadata} key.
     *
     * @param metadata the metadata configuration
     */
    public void setMetadata(MetadataConfig metadata) { this.metadata = metadata; }

    /**
     * Returns the list of scenario references to execute in this run.
     *
     * <p>Each entry must contain an {@code id} matching a loaded scenario configuration.
     * Bound to the {@code scenarios} key.</p>
     *
     * @return the scenario reference list, or {@code null} if not set
     */
    public List<ScenarioRef> getScenarios() { return scenarios; }

    /**
     * Sets the list of scenario references to execute in this run.
     * Bound to the {@code scenarios} key.
     *
     * @param scenarios the scenario reference list
     */
    public void setScenarios(List<ScenarioRef> scenarios) { this.scenarios = scenarios; }

    /**
     * Returns the execution configuration defining the workload mode for this run.
     *
     * @return the execution configuration, or {@code null} if not configured
     */
    public ExecutionConfig getExecution() { return execution; }

    /**
     * Sets the execution configuration defining the workload mode for this run.
     * Bound to the {@code execution} key.
     *
     * @param execution the execution configuration
     */
    public void setExecution(ExecutionConfig execution) { this.execution = execution; }

    /**
     * Returns the global assertion thresholds for this run.
     *
     * @return the assertions configuration, or {@code null} if not configured
     */
    public AssertionsConfig getAssertions() { return assertions; }

    /**
     * Sets the global assertion thresholds for this run.
     * Bound to the {@code assertions} key.
     *
     * @param assertions the assertions configuration
     */
    public void setAssertions(AssertionsConfig assertions) { this.assertions = assertions; }

    /**
     * Returns the reporting configuration controlling how the run report is produced.
     *
     * @return the reporting configuration, or {@code null} if not configured
     */
    public ReportingConfig getReporting() { return reporting; }

    /**
     * Sets the reporting configuration controlling how the run report is produced.
     * Bound to the {@code reporting} key.
     *
     * @param reporting the reporting configuration
     */
    public void setReporting(ReportingConfig reporting) { this.reporting = reporting; }

    /**
     * Returns the safety configuration that guards against accidentally running against production targets.
     *
     * @return the safety configuration, or {@code null} if not configured
     */
    public RunSafetyConfig getSafety() { return safety; }

    /**
     * Sets the safety configuration that guards against accidentally running against production targets.
     * Bound to the {@code safety} key.
     *
     * @param safety the safety configuration
     */
    public void setSafety(RunSafetyConfig safety) { this.safety = safety; }

    /**
     * Returns additional HTTP headers applied to every request in this run, keyed by header name.
     *
     * @return the headers map, or {@code null} if not configured
     */
    public Map<String, String> getHeaders() { return headers; }

    /**
     * Sets additional HTTP headers applied to every request in this run.
     * Bound to the {@code headers} key.
     *
     * @param headers the headers map keyed by header name
     */
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
}
