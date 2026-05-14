package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RegexCheck {
    @JsonProperty("pattern")
    private String pattern;

    @JsonProperty("expectation")
    private String expectation;

    @JsonProperty("sessionKey")
    private String sessionKey;

    public RegexCheck() {}

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
    public String getExpectation() { return expectation; }
    public void setExpectation(String expectation) { this.expectation = expectation; }
    public String getSessionKey() { return sessionKey; }
    public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }
}
