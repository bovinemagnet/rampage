package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

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

    public DatabaseConfig() {}

    public String getDriverClassName() { return driverClassName; }
    public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }
    public String getJdbcUrl() { return jdbcUrl; }
    public void setJdbcUrl(String jdbcUrl) { this.jdbcUrl = jdbcUrl; }
    public CredentialConfig getUsername() { return username; }
    public void setUsername(CredentialConfig username) { this.username = username; }
    public CredentialConfig getPassword() { return password; }
    public void setPassword(CredentialConfig password) { this.password = password; }
    public PoolConfig getPool() { return pool; }
    public void setPool(PoolConfig pool) { this.pool = pool; }
}
