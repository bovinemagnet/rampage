package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CheckConfig {
    @JsonProperty("jsonPath")
    private String jsonPath;

    @JsonProperty("exists")
    private Boolean exists;

    @JsonProperty("notExists")
    private Boolean notExists;

    public CheckConfig() {}

    public String getJsonPath() { return jsonPath; }
    public void setJsonPath(String jsonPath) { this.jsonPath = jsonPath; }
    public Boolean getExists() { return exists; }
    public void setExists(Boolean exists) { this.exists = exists; }
    public Boolean getNotExists() { return notExists; }
    public void setNotExists(Boolean notExists) { this.notExists = notExists; }
}
