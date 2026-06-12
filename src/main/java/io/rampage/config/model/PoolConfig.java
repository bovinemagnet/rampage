package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JDBC connection-pool settings for a feeder database.
 *
 * <p>Bound to the {@code pool} key within a {@code DatabaseConfig} entry in
 * {@code environment.yaml}. These values are used by {@code FeederFactory} when
 * establishing a connection pool to load feeder data via SQL.
 * Defaults: {@code maximumPoolSize} = 5, {@code connectionTimeoutMillis} = 5000,
 * {@code idleTimeoutMillis} = 600000.</p>
 */
public class PoolConfig {
    @JsonProperty("maximumPoolSize")
    private int maximumPoolSize = 5;

    @JsonProperty("connectionTimeoutMillis")
    private long connectionTimeoutMillis = 5000;

    @JsonProperty("idleTimeoutMillis")
    private long idleTimeoutMillis = 600000;

    /**
     * Constructs a {@code PoolConfig} with default pool size and timeout values.
     */
    public PoolConfig() {}

    /**
     * Returns the maximum number of connections the pool may hold.
     *
     * @return the maximum pool size; default is 5
     */
    public int getMaximumPoolSize() { return maximumPoolSize; }

    /**
     * Sets the maximum number of connections the pool may hold.
     * Bound to the {@code maximumPoolSize} key.
     *
     * @param maximumPoolSize the maximum pool size
     */
    public void setMaximumPoolSize(int maximumPoolSize) { this.maximumPoolSize = maximumPoolSize; }

    /**
     * Returns the maximum time in milliseconds to wait for a connection from the pool before timing out.
     *
     * @return the connection-acquisition timeout in milliseconds; default is 5000
     */
    public long getConnectionTimeoutMillis() { return connectionTimeoutMillis; }

    /**
     * Sets the maximum time in milliseconds to wait for a connection from the pool before timing out.
     * Bound to the {@code connectionTimeoutMillis} key.
     *
     * @param connectionTimeoutMillis the connection-acquisition timeout in milliseconds
     */
    public void setConnectionTimeoutMillis(long connectionTimeoutMillis) { this.connectionTimeoutMillis = connectionTimeoutMillis; }

    /**
     * Returns the time in milliseconds after which an idle connection is eligible for removal from the pool.
     *
     * @return the idle-connection timeout in milliseconds; default is 600000
     */
    public long getIdleTimeoutMillis() { return idleTimeoutMillis; }

    /**
     * Sets the time in milliseconds after which an idle connection is eligible for removal from the pool.
     * Bound to the {@code idleTimeoutMillis} key.
     *
     * @param idleTimeoutMillis the idle-connection timeout in milliseconds
     */
    public void setIdleTimeoutMillis(long idleTimeoutMillis) { this.idleTimeoutMillis = idleTimeoutMillis; }
}
