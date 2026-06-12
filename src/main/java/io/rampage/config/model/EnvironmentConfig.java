package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Top-level model for {@code environment.yaml}.
 *
 * <p>Describes the target system under test: base URLs, HTTP defaults,
 * authentication/security settings, JDBC databases used by feeders,
 * observability headers, and safety flags. An instance is loaded by
 * {@code ConfigLoader} and validated by {@code ConfigValidator} before
 * the simulation begins.</p>
 *
 * <p>{@code HttpProtocolFactory} reads {@code baseUrls}, {@code http}, and
 * {@code security} to build the Gatling HTTP protocol. Feeder loading reads
 * the {@code databases} map.</p>
 */
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

    /**
     * Constructs an {@code EnvironmentConfig} with all fields at their default values.
     */
    public EnvironmentConfig() {}

    /**
     * Returns the unique identifier for this environment.
     *
     * @return the environment ID, or {@code null} if not configured
     */
    public String getId() { return id; }

    /**
     * Sets the unique identifier for this environment.
     *
     * @param id the environment ID to use
     */
    public void setId(String id) { this.id = id; }

    /**
     * Returns the human-readable name of this environment.
     *
     * @return the environment name, or {@code null} if not configured
     */
    public String getName() { return name; }

    /**
     * Sets the human-readable name of this environment.
     *
     * @param name the environment name to use
     */
    public void setName(String name) { this.name = name; }

    /**
     * Returns the map of named base URLs available to scenarios.
     *
     * <p>Keys are endpoint reference names (for example, {@code "rest"});
     * values are the corresponding base URL strings.
     * Bound to the {@code baseUrls} key in {@code environment.yaml}.</p>
     *
     * @return the base URL map, or {@code null} if not configured
     */
    public Map<String, String> getBaseUrls() { return baseUrls; }

    /**
     * Sets the map of named base URLs.
     *
     * @param baseUrls a map of endpoint reference names to base URL strings
     */
    public void setBaseUrls(Map<String, String> baseUrls) { this.baseUrls = baseUrls; }

    /**
     * Returns the HTTP defaults applied to all requests in this environment.
     *
     * @return the HTTP configuration, or {@code null} if not configured
     */
    public HttpConfig getHttp() { return http; }

    /**
     * Sets the HTTP defaults for this environment.
     *
     * @param http the HTTP configuration to use
     */
    public void setHttp(HttpConfig http) { this.http = http; }

    /**
     * Returns the security configuration (authentication, bearer tokens, etc.).
     *
     * @return the security configuration, or {@code null} if not configured
     */
    public SecurityConfig getSecurity() { return security; }

    /**
     * Sets the security configuration.
     *
     * @param security the security configuration to use
     */
    public void setSecurity(SecurityConfig security) { this.security = security; }

    /**
     * Returns the map of named JDBC database configurations used by feeders.
     *
     * <p>Keys match the {@code databaseRef} values in scenario feeder configs.
     * Bound to the {@code databases} key in {@code environment.yaml}.</p>
     *
     * @return the databases map, or {@code null} if not configured
     */
    public Map<String, DatabaseConfig> getDatabases() { return databases; }

    /**
     * Sets the map of named JDBC database configurations.
     *
     * @param databases a map of database reference names to their configurations
     */
    public void setDatabases(Map<String, DatabaseConfig> databases) { this.databases = databases; }

    /**
     * Returns the observability configuration (correlation and trace headers).
     *
     * @return the observability configuration, or {@code null} if not configured
     */
    public ObservabilityConfig getObservability() { return observability; }

    /**
     * Sets the observability configuration.
     *
     * @param observability the observability configuration to use
     */
    public void setObservability(ObservabilityConfig observability) { this.observability = observability; }

    /**
     * Returns the safety configuration (rate limits, dry-run flags, etc.).
     *
     * @return the safety configuration, or {@code null} if not configured
     */
    public SafetyConfig getSafety() { return safety; }

    /**
     * Sets the safety configuration.
     *
     * @param safety the safety configuration to use
     */
    public void setSafety(SafetyConfig safety) { this.safety = safety; }
}
