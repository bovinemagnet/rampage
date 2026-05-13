package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PoolConfig {
    @JsonProperty("maximumPoolSize")
    private int maximumPoolSize = 5;

    @JsonProperty("connectionTimeoutMillis")
    private long connectionTimeoutMillis = 5000;

    @JsonProperty("idleTimeoutMillis")
    private long idleTimeoutMillis = 600000;

    public PoolConfig() {}

    public int getMaximumPoolSize() { return maximumPoolSize; }
    public void setMaximumPoolSize(int maximumPoolSize) { this.maximumPoolSize = maximumPoolSize; }
    public long getConnectionTimeoutMillis() { return connectionTimeoutMillis; }
    public void setConnectionTimeoutMillis(long connectionTimeoutMillis) { this.connectionTimeoutMillis = connectionTimeoutMillis; }
    public long getIdleTimeoutMillis() { return idleTimeoutMillis; }
    public void setIdleTimeoutMillis(long idleTimeoutMillis) { this.idleTimeoutMillis = idleTimeoutMillis; }
}
