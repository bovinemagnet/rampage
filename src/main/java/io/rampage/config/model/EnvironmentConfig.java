package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class EnvironmentConfig {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("baseUrls")
    private Map<String, String> baseUrls;

    @JsonProperty("http")
    private HttpConfig http;

    @JsonProperty("security")
    private SecurityConfig security;

    @JsonProperty("databases")
    private Map<String, DatabaseConfig> databases;

    @JsonProperty("observability")
    private ObservabilityConfig observability;

    @JsonProperty("safety")
    private SafetyConfig safety;

    public EnvironmentConfig() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Map<String, String> getBaseUrls() { return baseUrls; }
    public void setBaseUrls(Map<String, String> baseUrls) { this.baseUrls = baseUrls; }
    public HttpConfig getHttp() { return http; }
    public void setHttp(HttpConfig http) { this.http = http; }
    public SecurityConfig getSecurity() { return security; }
    public void setSecurity(SecurityConfig security) { this.security = security; }
    public Map<String, DatabaseConfig> getDatabases() { return databases; }
    public void setDatabases(Map<String, DatabaseConfig> databases) { this.databases = databases; }
    public ObservabilityConfig getObservability() { return observability; }
    public void setObservability(ObservabilityConfig observability) { this.observability = observability; }
    public SafetyConfig getSafety() { return safety; }
    public void setSafety(SafetyConfig safety) { this.safety = safety; }
}
