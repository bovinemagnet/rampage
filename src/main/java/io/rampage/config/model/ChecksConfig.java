package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response-check configuration for a scenario.
 *
 * <p>Bound to the {@code checks} key in a scenario YAML file. Each field
 * represents a distinct check type that Gatling will evaluate after the
 * HTTP response is received. All fields are optional; omitting a field
 * means that check is not applied.</p>
 */
public class ChecksConfig {
    @JsonProperty("httpStatus")
    private Integer httpStatus;

    @JsonProperty("jsonPath")
    private List<JsonPathCheck> jsonPath;

    @JsonProperty("regex")
    private List<RegexCheck> regex;

    @JsonProperty("header")
    private List<HeaderCheck> header;

    @JsonProperty("responseTimeMillis")
    private Long responseTimeMillis;

    @JsonProperty("bodyContains")
    private List<String> bodyContains;

    /**
     * Constructs a {@code ChecksConfig} with all fields at their default values.
     */
    public ChecksConfig() {}

    /**
     * Returns the expected HTTP status code.
     *
     * @return the expected status code, or {@code null} if not configured
     */
    public Integer getHttpStatus() { return httpStatus; }

    /**
     * Sets the expected HTTP status code.
     *
     * @param httpStatus the expected status code
     */
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }

    /**
     * Returns the list of JSONPath checks to apply to the response body.
     *
     * @return the JSONPath checks, or {@code null} if not configured
     */
    public List<JsonPathCheck> getJsonPath() { return jsonPath; }

    /**
     * Sets the list of JSONPath checks to apply to the response body.
     *
     * @param jsonPath the JSONPath checks to use
     */
    public void setJsonPath(List<JsonPathCheck> jsonPath) { this.jsonPath = jsonPath; }

    /**
     * Returns the list of regex checks to apply to the response body.
     *
     * @return the regex checks, or {@code null} if not configured
     */
    public List<RegexCheck> getRegex() { return regex; }

    /**
     * Sets the list of regex checks to apply to the response body.
     *
     * @param regex the regex checks to use
     */
    public void setRegex(List<RegexCheck> regex) { this.regex = regex; }

    /**
     * Returns the list of response header checks.
     *
     * @return the header checks, or {@code null} if not configured
     */
    public List<HeaderCheck> getHeader() { return header; }

    /**
     * Sets the list of response header checks.
     *
     * @param header the header checks to use
     */
    public void setHeader(List<HeaderCheck> header) { this.header = header; }

    /**
     * Returns the maximum allowable response time in milliseconds.
     *
     * @return the response time threshold in milliseconds, or {@code null} if not configured
     */
    public Long getResponseTimeMillis() { return responseTimeMillis; }

    /**
     * Sets the maximum allowable response time in milliseconds.
     *
     * @param responseTimeMillis the response time threshold in milliseconds
     */
    public void setResponseTimeMillis(Long responseTimeMillis) { this.responseTimeMillis = responseTimeMillis; }

    /**
     * Returns the list of substrings that must be present in the response body.
     *
     * @return the body-contains strings, or {@code null} if not configured
     */
    public List<String> getBodyContains() { return bodyContains; }

    /**
     * Sets the list of substrings that must be present in the response body.
     *
     * @param bodyContains the required substrings
     */
    public void setBodyContains(List<String> bodyContains) { this.bodyContains = bodyContains; }
}
