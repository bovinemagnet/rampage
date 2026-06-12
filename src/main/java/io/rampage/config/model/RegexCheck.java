package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for a Gatling check that applies a regular-expression pattern to the response body.
 *
 * <p>Bound to entries in the {@code checks.regex} list within a scenario YAML file.
 * The {@code pattern} field is a Java regular expression. The {@code expectation} field
 * controls the type of assertion applied (for example {@code "exists"} or {@code "saveAs"}).
 * When the expectation involves session storage, {@code sessionKey} names the Gatling session
 * attribute to use.</p>
 */
public class RegexCheck {
    @JsonProperty("pattern")
    private String pattern;

    @JsonProperty("expectation")
    private String expectation;

    @JsonProperty("sessionKey")
    private String sessionKey;

    /**
     * Constructs a {@code RegexCheck} with all fields uninitialised.
     */
    public RegexCheck() {}

    /**
     * Returns the Java regular-expression pattern applied to the response body.
     *
     * @return the regex pattern, or {@code null} if not set
     */
    public String getPattern() { return pattern; }

    /**
     * Sets the Java regular-expression pattern applied to the response body.
     * Bound to the {@code pattern} key.
     *
     * @param pattern the regex pattern
     */
    public void setPattern(String pattern) { this.pattern = pattern; }

    /**
     * Returns the expectation type applied to the pattern match (for example {@code "exists"} or {@code "saveAs"}).
     *
     * @return the expectation type, or {@code null} if not set
     */
    public String getExpectation() { return expectation; }

    /**
     * Sets the expectation type applied to the pattern match.
     * Bound to the {@code expectation} key.
     *
     * @param expectation the expectation type
     */
    public void setExpectation(String expectation) { this.expectation = expectation; }

    /**
     * Returns the Gatling session key used when saving or comparing the matched value against a session attribute.
     *
     * @return the session attribute name, or {@code null} if not applicable
     */
    public String getSessionKey() { return sessionKey; }

    /**
     * Sets the Gatling session key used when saving or comparing the matched value.
     * Bound to the {@code sessionKey} key.
     *
     * @param sessionKey the session attribute name
     */
    public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }
}
