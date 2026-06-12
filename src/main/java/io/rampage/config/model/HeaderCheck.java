package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for a Gatling check that inspects a named HTTP response header.
 *
 * <p>Bound to entries in the {@code checks.headers} list within a scenario YAML file.
 * The {@code expectation} field controls the type of assertion applied (for example
 * {@code "exists"}, {@code "is"}, or {@code "saveAs"}). When the expectation is
 * {@code "is"}, the {@code value} field supplies the expected literal value. When the
 * expectation involves session storage, {@code sessionKey} names the Gatling session
 * attribute to use.</p>
 */
public class HeaderCheck {
    @JsonProperty("name")
    private String name;

    @JsonProperty("expectation")
    private String expectation;

    @JsonProperty("value")
    private String value;

    @JsonProperty("sessionKey")
    private String sessionKey;

    /**
     * Constructs a {@code HeaderCheck} with all fields uninitialised.
     */
    public HeaderCheck() {}

    /**
     * Returns the name of the HTTP response header to inspect.
     *
     * @return the header name, or {@code null} if not set
     */
    public String getName() { return name; }

    /**
     * Sets the name of the HTTP response header to inspect.
     * Bound to the {@code name} key.
     *
     * @param name the header name
     */
    public void setName(String name) { this.name = name; }

    /**
     * Returns the expectation type applied to the header value (for example {@code "exists"} or {@code "is"}).
     *
     * @return the expectation type, or {@code null} if not set
     */
    public String getExpectation() { return expectation; }

    /**
     * Sets the expectation type applied to the header value.
     * Bound to the {@code expectation} key.
     *
     * @param expectation the expectation type
     */
    public void setExpectation(String expectation) { this.expectation = expectation; }

    /**
     * Returns the expected literal value used when the expectation is {@code "is"}.
     *
     * @return the expected header value, or {@code null} if not applicable
     */
    public String getValue() { return value; }

    /**
     * Sets the expected literal value for an {@code "is"} expectation.
     * Bound to the {@code value} key.
     *
     * @param value the expected header value
     */
    public void setValue(String value) { this.value = value; }

    /**
     * Returns the Gatling session key used when saving or comparing the header value against a session attribute.
     *
     * @return the session attribute name, or {@code null} if not applicable
     */
    public String getSessionKey() { return sessionKey; }

    /**
     * Sets the Gatling session key used when saving or comparing the header value.
     * Bound to the {@code sessionKey} key.
     *
     * @param sessionKey the session attribute name
     */
    public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }
}
