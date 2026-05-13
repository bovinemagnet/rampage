package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

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

    public MetadataConfig() {}

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getApplication() { return application; }
    public void setApplication(String application) { this.application = application; }
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    public String getChangeReference() { return changeReference; }
    public void setChangeReference(String changeReference) { this.changeReference = changeReference; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
