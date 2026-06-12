package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * HTTP client defaults applied to every request in the simulation.
 *
 * <p>Bound to the {@code http} key in {@code environment.yaml}. These values are
 * passed to the Gatling HTTP protocol builder by {@code HttpProtocolFactory}.
 * Defaults: {@code connectTimeoutMillis} = 5000, {@code requestTimeoutMillis} = 30000,
 * {@code followRedirects} = {@code false}.</p>
 */
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

    /**
     * Constructs an {@code HttpConfig} with default timeout and redirect values.
     */
    public HttpConfig() {}

    /**
     * Returns the TCP connection timeout in milliseconds.
     *
     * @return the connection timeout in milliseconds; default is 5000
     */
    public long getConnectTimeoutMillis() { return connectTimeoutMillis; }

    /**
     * Sets the TCP connection timeout in milliseconds.
     * Bound to the {@code connectTimeoutMillis} key.
     *
     * @param connectTimeoutMillis the connection timeout in milliseconds
     */
    public void setConnectTimeoutMillis(long connectTimeoutMillis) { this.connectTimeoutMillis = connectTimeoutMillis; }

    /**
     * Returns the per-request response timeout in milliseconds.
     *
     * @return the request timeout in milliseconds; default is 30000
     */
    public long getRequestTimeoutMillis() { return requestTimeoutMillis; }

    /**
     * Sets the per-request response timeout in milliseconds.
     * Bound to the {@code requestTimeoutMillis} key.
     *
     * @param requestTimeoutMillis the request timeout in milliseconds
     */
    public void setRequestTimeoutMillis(long requestTimeoutMillis) { this.requestTimeoutMillis = requestTimeoutMillis; }

    /**
     * Returns whether HTTP redirects are followed automatically.
     *
     * @return {@code true} if redirects are followed; {@code false} by default
     */
    public boolean isFollowRedirects() { return followRedirects; }

    /**
     * Sets whether HTTP redirects are followed automatically.
     * Bound to the {@code followRedirects} key.
     *
     * @param followRedirects {@code true} to follow redirects automatically
     */
    public void setFollowRedirects(boolean followRedirects) { this.followRedirects = followRedirects; }

    /**
     * Returns the value sent as the {@code Accept} header on every request.
     *
     * @return the Accept header value, or {@code null} if not configured
     */
    public String getAcceptHeader() { return acceptHeader; }

    /**
     * Sets the value sent as the {@code Accept} header on every request.
     * Bound to the {@code acceptHeader} key.
     *
     * @param acceptHeader the Accept header value
     */
    public void setAcceptHeader(String acceptHeader) { this.acceptHeader = acceptHeader; }

    /**
     * Returns the value sent as the {@code Content-Type} header on every request.
     *
     * @return the Content-Type header value, or {@code null} if not configured
     */
    public String getContentTypeHeader() { return contentTypeHeader; }

    /**
     * Sets the value sent as the {@code Content-Type} header on every request.
     * Bound to the {@code contentTypeHeader} key.
     *
     * @param contentTypeHeader the Content-Type header value
     */
    public void setContentTypeHeader(String contentTypeHeader) { this.contentTypeHeader = contentTypeHeader; }
}
