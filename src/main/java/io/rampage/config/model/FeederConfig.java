package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FeederConfig {
    @JsonProperty("type")
    private String type;

    @JsonProperty("query")
    private String query;

    @JsonProperty("preload")
    private int preload = 100;

    public FeederConfig() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public int getPreload() { return preload; }
    public void setPreload(int preload) { this.preload = preload; }
}
