package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HeaderCheck {
    @JsonProperty("name")
    private String name;

    @JsonProperty("expectation")
    private String expectation;

    @JsonProperty("value")
    private String value;

    @JsonProperty("sessionKey")
    private String sessionKey;

    public HeaderCheck() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getExpectation() { return expectation; }
    public void setExpectation(String expectation) { this.expectation = expectation; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getSessionKey() { return sessionKey; }
    public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }
}
