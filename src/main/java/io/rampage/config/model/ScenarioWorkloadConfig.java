package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScenarioWorkloadConfig {
    @JsonProperty("inheritFromRun")
    private boolean inheritFromRun = true;

    @JsonProperty("type")
    private String type;

    @JsonProperty("rate")
    private RateConfig rate;

    @JsonProperty("rampUp")
    private String rampUp;

    @JsonProperty("holdFor")
    private String holdFor;

    public ScenarioWorkloadConfig() {}

    public boolean isInheritFromRun() { return inheritFromRun; }
    public void setInheritFromRun(boolean inheritFromRun) { this.inheritFromRun = inheritFromRun; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public RateConfig getRate() { return rate; }
    public void setRate(RateConfig rate) { this.rate = rate; }
    public String getRampUp() { return rampUp; }
    public void setRampUp(String rampUp) { this.rampUp = rampUp; }
    public String getHoldFor() { return holdFor; }
    public void setHoldFor(String holdFor) { this.holdFor = holdFor; }
}
