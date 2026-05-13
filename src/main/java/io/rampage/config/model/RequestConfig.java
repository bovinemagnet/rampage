package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class RequestConfig {
    @JsonProperty("graphqlQueryFile")
    private String graphqlQueryFile;

    @JsonProperty("variables")
    private Map<String, String> variables;

    @JsonProperty("bodyTemplate")
    private String bodyTemplate;

    public RequestConfig() {}

    public String getGraphqlQueryFile() { return graphqlQueryFile; }
    public void setGraphqlQueryFile(String graphqlQueryFile) { this.graphqlQueryFile = graphqlQueryFile; }
    public Map<String, String> getVariables() { return variables; }
    public void setVariables(Map<String, String> variables) { this.variables = variables; }
    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }
}
