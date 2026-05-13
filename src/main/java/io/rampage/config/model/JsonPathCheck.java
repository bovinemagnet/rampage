package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonPathCheck {
    @JsonProperty("path")
    private String path;

    @JsonProperty("expectation")
    private String expectation;

    @JsonProperty("sessionKey")
    private String sessionKey;

    public JsonPathCheck() {}

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getExpectation() { return expectation; }
    public void setExpectation(String expectation) { this.expectation = expectation; }
    public String getSessionKey() { return sessionKey; }
    public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }
}
