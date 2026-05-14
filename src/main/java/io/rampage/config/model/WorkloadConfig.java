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

    @JsonProperty("baselineRate")
    private Double baselineRate;

    @JsonProperty("spikeDuration")
    private String spikeDuration;

    @JsonProperty("stepRate")
    private Double stepRate;

    @JsonProperty("stepDuration")
    private String stepDuration;

    @JsonProperty("maxRate")
    private Double maxRate;

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
    public Double getBaselineRate() { return baselineRate; }
    public void setBaselineRate(Double baselineRate) { this.baselineRate = baselineRate; }
    public String getSpikeDuration() { return spikeDuration; }
    public void setSpikeDuration(String spikeDuration) { this.spikeDuration = spikeDuration; }
    public Double getStepRate() { return stepRate; }
    public void setStepRate(Double stepRate) { this.stepRate = stepRate; }
    public String getStepDuration() { return stepDuration; }
    public void setStepDuration(String stepDuration) { this.stepDuration = stepDuration; }
    public Double getMaxRate() { return maxRate; }
    public void setMaxRate(Double maxRate) { this.maxRate = maxRate; }
}
