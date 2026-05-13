package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DatabaseConfig {
    @JsonProperty("url")
    private String url;

    @JsonProperty("usernameRef")
    private String usernameRef;

    @JsonProperty("passwordRef")
    private String passwordRef;

    @JsonProperty("driver")
    private String driver;

    public DatabaseConfig() {}

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getUsernameRef() { return usernameRef; }
    public void setUsernameRef(String usernameRef) { this.usernameRef = usernameRef; }
    public String getPasswordRef() { return passwordRef; }
    public void setPasswordRef(String passwordRef) { this.passwordRef = passwordRef; }
    public String getDriver() { return driver; }
    public void setDriver(String driver) { this.driver = driver; }
}
