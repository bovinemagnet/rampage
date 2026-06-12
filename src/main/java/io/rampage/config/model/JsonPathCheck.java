package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for a Gatling check that evaluates a JSONPath expression against the response body.
 *
 * <p>Bound to entries in the {@code checks.jsonPath} list within a scenario YAML file.
 * The {@code path} field is a JSONPath expression (for example {@code "$.data.userId"}).
 * The {@code expectation} field controls the type of assertion: {@code "exists"} asserts the
 * path resolves to a value; {@code "is"} compares the extracted value against {@code equalsValue};
 * {@code "saveAs"} stores the extracted value in the Gatling session under the key named by
 * {@code sessionKey} or {@code saveAs}. {@code sessionKey} may be used when comparing the
 * extracted value against an existing session attribute.</p>
 */
public class JsonPathCheck {
    @JsonProperty("path")
    private String path;

    @JsonProperty("expectation")
    private String expectation;

    @JsonProperty("sessionKey")
    private String sessionKey;

    @JsonProperty("equalsValue")
    private String equalsValue;

    @JsonProperty("saveAs")
    private String saveAs;

    /**
     * Constructs a {@code JsonPathCheck} with all fields uninitialised.
     */
    public JsonPathCheck() {}

    /**
     * Returns the JSONPath expression used to extract a value from the response body.
     *
     * @return the JSONPath expression, or {@code null} if not set
     */
    public String getPath() { return path; }

    /**
     * Sets the JSONPath expression used to extract a value from the response body.
     * Bound to the {@code path} key.
     *
     * @param path the JSONPath expression
     */
    public void setPath(String path) { this.path = path; }

    /**
     * Returns the expectation type applied to the extracted value (for example {@code "exists"}, {@code "is"}, or {@code "saveAs"}).
     *
     * @return the expectation type, or {@code null} if not set
     */
    public String getExpectation() { return expectation; }

    /**
     * Sets the expectation type applied to the extracted value.
     * Bound to the {@code expectation} key.
     *
     * @param expectation the expectation type
     */
    public void setExpectation(String expectation) { this.expectation = expectation; }

    /**
     * Returns the Gatling session key used when comparing the extracted value against a session attribute.
     *
     * @return the session attribute name, or {@code null} if not applicable
     */
    public String getSessionKey() { return sessionKey; }

    /**
     * Sets the Gatling session key used when comparing the extracted value against a session attribute.
     * Bound to the {@code sessionKey} key.
     *
     * @param sessionKey the session attribute name
     */
    public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }

    /**
     * Returns the literal value the extracted JSONPath result must equal.
     *
     * @return the expected value, or {@code null} if not applicable
     */
    public String getEqualsValue() { return equalsValue; }

    /**
     * Sets the literal value the extracted JSONPath result must equal.
     * Bound to the {@code equalsValue} key.
     *
     * @param equalsValue the expected value
     */
    public void setEqualsValue(String equalsValue) { this.equalsValue = equalsValue; }

    /**
     * Returns the Gatling session attribute name under which the extracted value is saved.
     *
     * @return the session attribute name for storage, or {@code null} if not applicable
     */
    public String getSaveAs() { return saveAs; }

    /**
     * Sets the Gatling session attribute name under which the extracted value is saved.
     * Bound to the {@code saveAs} key.
     *
     * @param saveAs the session attribute name for storage
     */
    public void setSaveAs(String saveAs) { this.saveAs = saveAs; }
}
