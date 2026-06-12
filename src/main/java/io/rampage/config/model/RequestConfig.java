package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Configuration describing the HTTP request sent by a scenario.
 *
 * <p>Bound to the {@code request} key within a scenario YAML file.
 * Only GraphQL POST requests are currently wired through by {@code ScenarioFactory}: the
 * {@code graphqlQueryFile} field names the file containing the GraphQL query, and
 * {@code variables} supplies the variable map (Gatling EL expressions of the form
 * {@code ${feeder:key}} are rewritten to {@code #{key}} for session lookup).
 * Other fields ({@code method}, {@code path}, {@code bodyType}, {@code body},
 * {@code bodyFile}, {@code bodyTemplate}) are present in the model for future use
 * but are not currently translated into Gatling request builders.</p>
 */
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

    /**
     * Constructs a {@code RequestConfig} with all fields uninitialised.
     */
    public RequestConfig() {}

    /**
     * Returns the HTTP method for the request (for example {@code "POST"} or {@code "GET"}).
     *
     * @return the HTTP method, or {@code null} if not set
     */
    public String getMethod() { return method; }

    /**
     * Sets the HTTP method for the request.
     * Bound to the {@code method} key.
     *
     * @param method the HTTP method
     */
    public void setMethod(String method) { this.method = method; }

    /**
     * Returns the URL path appended to the base URL for this request.
     *
     * @return the path, or {@code null} if not set
     */
    public String getPath() { return path; }

    /**
     * Sets the URL path appended to the base URL for this request.
     * Bound to the {@code path} key.
     *
     * @param path the URL path
     */
    public void setPath(String path) { this.path = path; }

    /**
     * Returns the body type identifier (for example {@code "json"} or {@code "graphql"}).
     *
     * @return the body type, or {@code null} if not set
     */
    public String getBodyType() { return bodyType; }

    /**
     * Sets the body type identifier.
     * Bound to the {@code bodyType} key.
     *
     * @param bodyType the body type identifier
     */
    public void setBodyType(String bodyType) { this.bodyType = bodyType; }

    /**
     * Returns the raw request body string.
     *
     * @return the request body, or {@code null} if not set
     */
    public String getBody() { return body; }

    /**
     * Sets the raw request body string.
     * Bound to the {@code body} key.
     *
     * @param body the request body
     */
    public void setBody(String body) { this.body = body; }

    /**
     * Returns the path to a file whose contents are used as the request body.
     *
     * @return the body file path, or {@code null} if not set
     */
    public String getBodyFile() { return bodyFile; }

    /**
     * Sets the path to a file whose contents are used as the request body.
     * Bound to the {@code bodyFile} key.
     *
     * @param bodyFile the body file path
     */
    public void setBodyFile(String bodyFile) { this.bodyFile = bodyFile; }

    /**
     * Returns URL query parameters appended to the request URL.
     *
     * @return the query parameter map, or {@code null} if not set
     */
    public Map<String, String> getQueryParams() { return queryParams; }

    /**
     * Sets URL query parameters appended to the request URL.
     * Bound to the {@code queryParams} key.
     *
     * @param queryParams the query parameter map
     */
    public void setQueryParams(Map<String, String> queryParams) { this.queryParams = queryParams; }

    /**
     * Returns form parameters sent as a URL-encoded body.
     *
     * @return the form parameter map, or {@code null} if not set
     */
    public Map<String, String> getFormParams() { return formParams; }

    /**
     * Sets form parameters sent as a URL-encoded body.
     * Bound to the {@code formParams} key.
     *
     * @param formParams the form parameter map
     */
    public void setFormParams(Map<String, String> formParams) { this.formParams = formParams; }

    /**
     * Returns the path to the file containing the GraphQL query document.
     *
     * <p>This is the primary field used by {@code ScenarioFactory} when building GraphQL requests.
     * The path is resolved from the filesystem first, then from the classpath.</p>
     *
     * @return the GraphQL query file path, or {@code null} if not set
     */
    public String getGraphqlQueryFile() { return graphqlQueryFile; }

    /**
     * Sets the path to the file containing the GraphQL query document.
     * Bound to the {@code graphqlQueryFile} key.
     *
     * @param graphqlQueryFile the GraphQL query file path
     */
    public void setGraphqlQueryFile(String graphqlQueryFile) { this.graphqlQueryFile = graphqlQueryFile; }

    /**
     * Returns the GraphQL variable map sent alongside the query.
     *
     * <p>Values may use Gatling EL syntax in the form {@code ${feeder:key}},
     * which {@code ScenarioFactory} rewrites to {@code #{key}} for session lookup.</p>
     *
     * @return the variables map, or {@code null} if not set
     */
    public Map<String, Object> getVariables() { return variables; }

    /**
     * Sets the GraphQL variable map sent alongside the query.
     * Bound to the {@code variables} key.
     *
     * @param variables the variables map
     */
    public void setVariables(Map<String, Object> variables) { this.variables = variables; }

    /**
     * Returns a body template string used for parameterised request bodies.
     *
     * @return the body template, or {@code null} if not set
     */
    public String getBodyTemplate() { return bodyTemplate; }

    /**
     * Sets a body template string used for parameterised request bodies.
     * Bound to the {@code bodyTemplate} key.
     *
     * @param bodyTemplate the body template
     */
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }
}
