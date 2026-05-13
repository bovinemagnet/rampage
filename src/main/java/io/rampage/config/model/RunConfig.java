package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class RunConfig {
    @JsonProperty("run")
    private Run run;

    public RunConfig() {}

    public Run getRun() { return run; }
    public void setRun(Run run) { this.run = run; }

    public static class Run {
        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        @JsonProperty("scenarios")
        private List<String> scenarios;

        @JsonProperty("workload")
        private WorkloadConfig workload;

        @JsonProperty("assertions")
        private AssertionConfig assertions;

        @JsonProperty("reporting")
        private ReportingConfig reporting;

        public Run() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getScenarios() { return scenarios; }
        public void setScenarios(List<String> scenarios) { this.scenarios = scenarios; }
        public WorkloadConfig getWorkload() { return workload; }
        public void setWorkload(WorkloadConfig workload) { this.workload = workload; }
        public AssertionConfig getAssertions() { return assertions; }
        public void setAssertions(AssertionConfig assertions) { this.assertions = assertions; }
        public ReportingConfig getReporting() { return reporting; }
        public void setReporting(ReportingConfig reporting) { this.reporting = reporting; }
    }
}
