package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HttpConfig {
    @JsonProperty("connectTimeoutMillis")
    private long connectTimeoutMillis = 5000;

    @JsonProperty("requestTimeoutMillis")
    private long requestTimeoutMillis = 30000;

    @JsonProperty("followRedirects")
    private boolean followRedirects = false;

    @JsonProperty("acceptHeader")
    private String acceptHeader;

    @JsonProperty("contentTypeHeader")
    private String contentTypeHeader;

    public HttpConfig() {}

    public long getConnectTimeoutMillis() { return connectTimeoutMillis; }
    public void setConnectTimeoutMillis(long connectTimeoutMillis) { this.connectTimeoutMillis = connectTimeoutMillis; }
    public long getRequestTimeoutMillis() { return requestTimeoutMillis; }
    public void setRequestTimeoutMillis(long requestTimeoutMillis) { this.requestTimeoutMillis = requestTimeoutMillis; }
    public boolean isFollowRedirects() { return followRedirects; }
    public void setFollowRedirects(boolean followRedirects) { this.followRedirects = followRedirects; }
    public String getAcceptHeader() { return acceptHeader; }
    public void setAcceptHeader(String acceptHeader) { this.acceptHeader = acceptHeader; }
    public String getContentTypeHeader() { return contentTypeHeader; }
    public void setContentTypeHeader(String contentTypeHeader) { this.contentTypeHeader = contentTypeHeader; }
}
