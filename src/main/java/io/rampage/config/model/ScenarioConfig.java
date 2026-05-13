package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class ScenarioConfig {
    @JsonProperty("scenario")
    private Scenario scenario;

    public ScenarioConfig() {}

    public Scenario getScenario() { return scenario; }
    public void setScenario(Scenario scenario) { this.scenario = scenario; }

    public static class Scenario {
        @JsonProperty("name")
        private String name;

        @JsonProperty("graphql")
        private GraphQLConfig graphql;

        @JsonProperty("feeder")
        private FeederConfig feeder;

        @JsonProperty("headers")
        private Map<String, String> headers;

        @JsonProperty("checks")
        private List<CheckConfig> checks;

        @JsonProperty("tags")
        private Map<String, String> tags;

        @JsonProperty("workload")
        private WorkloadConfig workload;

        public Scenario() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public GraphQLConfig getGraphql() { return graphql; }
        public void setGraphql(GraphQLConfig graphql) { this.graphql = graphql; }
        public FeederConfig getFeeder() { return feeder; }
        public void setFeeder(FeederConfig feeder) { this.feeder = feeder; }
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        public List<CheckConfig> getChecks() { return checks; }
        public void setChecks(List<CheckConfig> checks) { this.checks = checks; }
        public Map<String, String> getTags() { return tags; }
        public void setTags(Map<String, String> tags) { this.tags = tags; }
        public WorkloadConfig getWorkload() { return workload; }
        public void setWorkload(WorkloadConfig workload) { this.workload = workload; }
    }
}
