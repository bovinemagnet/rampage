package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

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

    public RunConfig() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public MetadataConfig getMetadata() { return metadata; }
    public void setMetadata(MetadataConfig metadata) { this.metadata = metadata; }
    public List<ScenarioRef> getScenarios() { return scenarios; }
    public void setScenarios(List<ScenarioRef> scenarios) { this.scenarios = scenarios; }
    public ExecutionConfig getExecution() { return execution; }
    public void setExecution(ExecutionConfig execution) { this.execution = execution; }
    public AssertionsConfig getAssertions() { return assertions; }
    public void setAssertions(AssertionsConfig assertions) { this.assertions = assertions; }
    public ReportingConfig getReporting() { return reporting; }
    public void setReporting(ReportingConfig reporting) { this.reporting = reporting; }
    public RunSafetyConfig getSafety() { return safety; }
    public void setSafety(RunSafetyConfig safety) { this.safety = safety; }
}
