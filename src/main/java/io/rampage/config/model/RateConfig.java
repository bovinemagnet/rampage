package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RateConfig {
    @JsonProperty("unit")
    private String unit;

    @JsonProperty("from")
    private double from;

    @JsonProperty("to")
    private double to;

    public RateConfig() {}

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public double getFrom() { return from; }
    public void setFrom(double from) { this.from = from; }
    public double getTo() { return to; }
    public void setTo(double to) { this.to = to; }
}
