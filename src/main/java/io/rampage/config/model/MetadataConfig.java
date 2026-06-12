package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Optional descriptive metadata attached to a run or scenario configuration.
 *
 * <p>Bound to the {@code metadata} key in {@code run.yaml} and optionally in scenario YAML files.
 * These fields are informational and may be written to the run report when
 * {@code reporting.writeRunMetadata} is {@code true}. No field is mandatory.</p>
 */
public class MetadataConfig {
    @JsonProperty("owner")
    private String owner;

    @JsonProperty("application")
    private String application;

    @JsonProperty("service")
    private String service;

    @JsonProperty("changeReference")
    private String changeReference;

    @JsonProperty("description")
    private String description;

    /**
     * Constructs a {@code MetadataConfig} with all fields uninitialised.
     */
    public MetadataConfig() {}

    /**
     * Returns the name or team identifier of the person or group responsible for this run.
     *
     * @return the owner, or {@code null} if not set
     */
    public String getOwner() { return owner; }

    /**
     * Sets the name or team identifier of the person or group responsible for this run.
     * Bound to the {@code owner} key.
     *
     * @param owner the owner identifier
     */
    public void setOwner(String owner) { this.owner = owner; }

    /**
     * Returns the application name under test.
     *
     * @return the application name, or {@code null} if not set
     */
    public String getApplication() { return application; }

    /**
     * Sets the application name under test.
     * Bound to the {@code application} key.
     *
     * @param application the application name
     */
    public void setApplication(String application) { this.application = application; }

    /**
     * Returns the service name or component under test.
     *
     * @return the service name, or {@code null} if not set
     */
    public String getService() { return service; }

    /**
     * Sets the service name or component under test.
     * Bound to the {@code service} key.
     *
     * @param service the service name
     */
    public void setService(String service) { this.service = service; }

    /**
     * Returns a change reference (for example a ticket or pull-request identifier) associated with this run.
     *
     * @return the change reference, or {@code null} if not set
     */
    public String getChangeReference() { return changeReference; }

    /**
     * Sets a change reference associated with this run.
     * Bound to the {@code changeReference} key.
     *
     * @param changeReference the change reference identifier
     */
    public void setChangeReference(String changeReference) { this.changeReference = changeReference; }

    /**
     * Returns a human-readable description of the purpose of this run.
     *
     * @return the description, or {@code null} if not set
     */
    public String getDescription() { return description; }

    /**
     * Sets a human-readable description of the purpose of this run.
     * Bound to the {@code description} key.
     *
     * @param description the description text
     */
    public void setDescription(String description) { this.description = description; }
}
