package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class EnvironmentConfig {
    @JsonProperty("environment")
    private Environment environment;

    public EnvironmentConfig() {}

    public Environment getEnvironment() { return environment; }
    public void setEnvironment(Environment environment) { this.environment = environment; }

    public static class Environment {
        @JsonProperty("name")
        private String name;

        @JsonProperty("baseUrl")
        private String baseUrl;

        @JsonProperty("httpHeaders")
        private Map<String, String> httpHeaders;

        @JsonProperty("auth")
        private AuthConfig auth;

        @JsonProperty("database")
        private DatabaseConfig database;

        @JsonProperty("timeouts")
        private TimeoutConfig timeouts;

        @JsonProperty("safety")
        private SafetyConfig safety;

        public Environment() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public Map<String, String> getHttpHeaders() { return httpHeaders; }
        public void setHttpHeaders(Map<String, String> httpHeaders) { this.httpHeaders = httpHeaders; }
        public AuthConfig getAuth() { return auth; }
        public void setAuth(AuthConfig auth) { this.auth = auth; }
        public DatabaseConfig getDatabase() { return database; }
        public void setDatabase(DatabaseConfig database) { this.database = database; }
        public TimeoutConfig getTimeouts() { return timeouts; }
        public void setTimeouts(TimeoutConfig timeouts) { this.timeouts = timeouts; }
        public SafetyConfig getSafety() { return safety; }
        public void setSafety(SafetyConfig safety) { this.safety = safety; }
    }
}
