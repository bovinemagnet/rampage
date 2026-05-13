package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WorkloadConfig {
    @JsonProperty("type")
    private String type;

    @JsonProperty("rampUp")
    private String rampUp;

    @JsonProperty("holdFor")
    private String holdFor;

    @JsonProperty("duration")
    private String duration;

    @JsonProperty("rate")
    private RateConfig rate;

    @JsonProperty("users")
    private int users = 1;

    public WorkloadConfig() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getRampUp() { return rampUp; }
    public void setRampUp(String rampUp) { this.rampUp = rampUp; }
    public String getHoldFor() { return holdFor; }
    public void setHoldFor(String holdFor) { this.holdFor = holdFor; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public RateConfig getRate() { return rate; }
    public void setRate(RateConfig rate) { this.rate = rate; }
    public int getUsers() { return users; }
    public void setUsers(int users) { this.users = users; }
}
