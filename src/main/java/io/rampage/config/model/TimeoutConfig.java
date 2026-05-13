package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TimeoutConfig {
    @JsonProperty("connectionTimeoutMs")
    private long connectionTimeoutMs = 5000;

    @JsonProperty("readTimeoutMs")
    private long readTimeoutMs = 30000;

    public TimeoutConfig() {}

    public long getConnectionTimeoutMs() { return connectionTimeoutMs; }
    public void setConnectionTimeoutMs(long connectionTimeoutMs) { this.connectionTimeoutMs = connectionTimeoutMs; }
    public long getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(long readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
}
