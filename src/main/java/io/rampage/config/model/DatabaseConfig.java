package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Connection configuration for a JDBC data source used by a feeder.
 *
 * <p>Bound to entries in the {@code databases} map in {@code environment.yaml}.
 * Scenarios reference a database by the key under which it is registered in that
 * map. Credentials are resolved at runtime via the {@code username} and
 * {@code password} {@code CredentialConfig} objects.</p>
 */
public class DatabaseConfig {
    @JsonProperty("driverClassName")
    private String driverClassName;

    @JsonProperty("jdbcUrl")
    private String jdbcUrl;

    @JsonProperty("username")
    private CredentialConfig username;

    @JsonProperty("password")
    private CredentialConfig password;

    @JsonProperty("pool")
    private PoolConfig pool;

    /**
     * Constructs a {@code DatabaseConfig} with all fields at their default values.
     */
    public DatabaseConfig() {}

    /**
     * Returns the fully qualified JDBC driver class name
     * (for example, {@code "org.postgresql.Driver"}).
     *
     * @return the driver class name, or {@code null} if not configured
     */
    public String getDriverClassName() { return driverClassName; }

    /**
     * Sets the fully qualified JDBC driver class name.
     *
     * @param driverClassName the driver class name to use
     */
    public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }

    /**
     * Returns the JDBC connection URL for the database.
     *
     * @return the JDBC URL, or {@code null} if not configured
     */
    public String getJdbcUrl() { return jdbcUrl; }

    /**
     * Sets the JDBC connection URL.
     *
     * @param jdbcUrl the JDBC URL to use
     */
    public void setJdbcUrl(String jdbcUrl) { this.jdbcUrl = jdbcUrl; }

    /**
     * Returns the credential configuration used to resolve the database username.
     *
     * @return the username credential configuration, or {@code null} if not configured
     */
    public CredentialConfig getUsername() { return username; }

    /**
     * Sets the credential configuration for the database username.
     *
     * @param username the username credential configuration to use
     */
    public void setUsername(CredentialConfig username) { this.username = username; }

    /**
     * Returns the credential configuration used to resolve the database password.
     *
     * @return the password credential configuration, or {@code null} if not configured
     */
    public CredentialConfig getPassword() { return password; }

    /**
     * Sets the credential configuration for the database password.
     *
     * @param password the password credential configuration to use
     */
    public void setPassword(CredentialConfig password) { this.password = password; }

    /**
     * Returns the connection pool configuration for this database.
     *
     * @return the pool configuration, or {@code null} if not configured
     */
    public PoolConfig getPool() { return pool; }

    /**
     * Sets the connection pool configuration for this database.
     *
     * @param pool the pool configuration to use
     */
    public void setPool(PoolConfig pool) { this.pool = pool; }
}
