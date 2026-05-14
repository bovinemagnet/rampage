package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

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

    public ChecksConfig() {}

    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
    public List<JsonPathCheck> getJsonPath() { return jsonPath; }
    public void setJsonPath(List<JsonPathCheck> jsonPath) { this.jsonPath = jsonPath; }
    public List<RegexCheck> getRegex() { return regex; }
    public void setRegex(List<RegexCheck> regex) { this.regex = regex; }
    public List<HeaderCheck> getHeader() { return header; }
    public void setHeader(List<HeaderCheck> header) { this.header = header; }
    public Long getResponseTimeMillis() { return responseTimeMillis; }
    public void setResponseTimeMillis(Long responseTimeMillis) { this.responseTimeMillis = responseTimeMillis; }
    public List<String> getBodyContains() { return bodyContains; }
    public void setBodyContains(List<String> bodyContains) { this.bodyContains = bodyContains; }
}
