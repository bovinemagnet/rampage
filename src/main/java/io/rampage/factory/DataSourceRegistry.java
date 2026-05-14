package io.rampage.factory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.rampage.config.model.DatabaseConfig;
import io.rampage.config.model.PoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataSourceRegistry implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(DataSourceRegistry.class);

    private final SecretResolver secretResolver;
    private final Map<String, HikariDataSource> pools = new ConcurrentHashMap<>();

    public DataSourceRegistry(SecretResolver secretResolver) {
        this.secretResolver = secretResolver;
    }

    public DataSource getOrCreate(String name, DatabaseConfig db) {
        return pools.computeIfAbsent(name, n -> buildPool(n, db));
    }

    private HikariDataSource buildPool(String name, DatabaseConfig db) {
        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName(name);
        cfg.setJdbcUrl(db.getJdbcUrl());
        if (db.getDriverClassName() != null && !db.getDriverClassName().isBlank()) {
            cfg.setDriverClassName(db.getDriverClassName());
        }
        String username = secretResolver.resolveCredential(db.getUsername(), "databases." + name + ".username");
        String password = secretResolver.resolveCredential(db.getPassword(), "databases." + name + ".password");
        if (username != null) cfg.setUsername(username);
        if (password != null) cfg.setPassword(password);

        PoolConfig pool = db.getPool();
        if (pool != null) {
            if (pool.getMaximumPoolSize() > 0) cfg.setMaximumPoolSize(pool.getMaximumPoolSize());
            if (pool.getConnectionTimeoutMillis() > 0) cfg.setConnectionTimeout(pool.getConnectionTimeoutMillis());
            if (pool.getIdleTimeoutMillis() > 0) cfg.setIdleTimeout(pool.getIdleTimeoutMillis());
        }
        // Read-only is a sensible default for feeder data sources; users can override per scenario later.
        cfg.setReadOnly(true);

        HikariDataSource ds = new HikariDataSource(cfg);
        log.info("Created HikariCP pool '{}' for {} (max={}, connTimeoutMs={})",
            name, db.getJdbcUrl(),
            ds.getMaximumPoolSize(), ds.getConnectionTimeout());
        return ds;
    }

    public void logStats() {
        pools.forEach((name, ds) -> log.info(
            "Pool '{}': active={}, idle={}, awaiting={}, total={}",
            name,
            ds.getHikariPoolMXBean().getActiveConnections(),
            ds.getHikariPoolMXBean().getIdleConnections(),
            ds.getHikariPoolMXBean().getThreadsAwaitingConnection(),
            ds.getHikariPoolMXBean().getTotalConnections()));
    }

    @Override
    public void close() {
        pools.forEach((name, ds) -> {
            try {
                if (!ds.isClosed()) {
                    ds.close();
                    log.info("Closed HikariCP pool '{}'", name);
                }
            } catch (Exception e) {
                log.warn("Error closing pool '{}': {}", name, e.getMessage());
            }
        });
        pools.clear();
    }

    int size() {
        return pools.size();
    }
}
