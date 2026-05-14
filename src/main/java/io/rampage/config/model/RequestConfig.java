package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class RequestConfig {
    @JsonProperty("method")
    private String method;

    @JsonProperty("path")
    private String path;

    @JsonProperty("bodyType")
    private String bodyType;

    @JsonProperty("body")
    private String body;

    @JsonProperty("bodyFile")
    private String bodyFile;

    @JsonProperty("queryParams")
    private Map<String, String> queryParams;

    @JsonProperty("formParams")
    private Map<String, String> formParams;

    @JsonProperty("graphqlQueryFile")
    private String graphqlQueryFile;

    @JsonProperty("variables")
    private Map<String, Object> variables;

    @JsonProperty("bodyTemplate")
    private String bodyTemplate;

    public RequestConfig() {}

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getBodyType() { return bodyType; }
    public void setBodyType(String bodyType) { this.bodyType = bodyType; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getBodyFile() { return bodyFile; }
    public void setBodyFile(String bodyFile) { this.bodyFile = bodyFile; }
    public Map<String, String> getQueryParams() { return queryParams; }
    public void setQueryParams(Map<String, String> queryParams) { this.queryParams = queryParams; }
    public Map<String, String> getFormParams() { return formParams; }
    public void setFormParams(Map<String, String> formParams) { this.formParams = formParams; }
    public String getGraphqlQueryFile() { return graphqlQueryFile; }
    public void setGraphqlQueryFile(String graphqlQueryFile) { this.graphqlQueryFile = graphqlQueryFile; }
    public Map<String, Object> getVariables() { return variables; }
    public void setVariables(Map<String, Object> variables) { this.variables = variables; }
    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }
}
