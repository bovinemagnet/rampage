package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PausesConfig {
    @JsonProperty("beforeRequestMillis")
    private long beforeRequestMillis;

    @JsonProperty("afterRequest")
    private AfterRequestPause afterRequest;

    public PausesConfig() {}

    public long getBeforeRequestMillis() { return beforeRequestMillis; }
    public void setBeforeRequestMillis(long beforeRequestMillis) { this.beforeRequestMillis = beforeRequestMillis; }
    public AfterRequestPause getAfterRequest() { return afterRequest; }
    public void setAfterRequest(AfterRequestPause afterRequest) { this.afterRequest = afterRequest; }
}
