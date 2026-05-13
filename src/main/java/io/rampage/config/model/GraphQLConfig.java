package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class GraphQLConfig {
    @JsonProperty("endpoint")
    private String endpoint;

    @JsonProperty("queryFile")
    private String queryFile;

    @JsonProperty("variables")
    private Map<String, String> variables;

    public GraphQLConfig() {}

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getQueryFile() { return queryFile; }
    public void setQueryFile(String queryFile) { this.queryFile = queryFile; }
    public Map<String, String> getVariables() { return variables; }
    public void setVariables(Map<String, String> variables) { this.variables = variables; }
}
