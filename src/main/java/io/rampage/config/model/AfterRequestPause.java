package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AfterRequestPause {
    @JsonProperty("strategy")
    private String strategy;

    @JsonProperty("minMillis")
    private long minMillis;

    @JsonProperty("maxMillis")
    private long maxMillis;

    public AfterRequestPause() {}

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
    public long getMinMillis() { return minMillis; }
    public void setMinMillis(long minMillis) { this.minMillis = minMillis; }
    public long getMaxMillis() { return maxMillis; }
    public void setMaxMillis(long maxMillis) { this.maxMillis = maxMillis; }
}
