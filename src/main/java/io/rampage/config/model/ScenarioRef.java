package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScenarioRef {
    @JsonProperty("id")
    private String id;

    @JsonProperty("file")
    private String file;

    @JsonProperty("enabled")
    private boolean enabled = true;

    @JsonProperty("weight")
    private int weight = 100;

    public ScenarioRef() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFile() { return file; }
    public void setFile(String file) { this.file = file; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }
}
