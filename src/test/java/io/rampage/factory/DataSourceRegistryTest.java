package io.rampage.factory;

import com.zaxxer.hikari.HikariDataSource;
import io.rampage.config.model.CredentialConfig;
import io.rampage.config.model.DatabaseConfig;
import io.rampage.config.model.PoolConfig;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

class DataSourceRegistryTest {

    private DatabaseConfig h2() {
        DatabaseConfig db = new DatabaseConfig();
        db.setDriverClassName("org.h2.Driver");
        db.setJdbcUrl("jdbc:h2:mem:test-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        CredentialConfig user = new CredentialConfig();
        user.setSource("plain");
        user.setValue("sa");
        db.setUsername(user);
        CredentialConfig pwd = new CredentialConfig();
        pwd.setSource("plain");
        pwd.setValue("");
        db.setPassword(pwd);
        return db;
    }

    @Test
    void getOrCreate_returnsSameDataSourceForSameName() {
        try (DataSourceRegistry reg = new DataSourceRegistry(new SecretResolver())) {
            DataSource a = reg.getOrCreate("source", h2());
            DataSource b = reg.getOrCreate("source", h2());
            assertSame(a, b, "Same name should return cached pool");
        }
    }

    @Test
    void getOrCreate_appliesPoolConfig() {
        DatabaseConfig db = h2();
        PoolConfig pool = new PoolConfig();
        pool.setMaximumPoolSize(7);
        pool.setConnectionTimeoutMillis(1500);
        pool.setIdleTimeoutMillis(15000);
        db.setPool(pool);

        try (DataSourceRegistry reg = new DataSourceRegistry(new SecretResolver())) {
            HikariDataSource ds = (HikariDataSource) reg.getOrCreate("source", db);
            assertEquals(7, ds.getMaximumPoolSize());
            assertEquals(1500, ds.getConnectionTimeout());
            assertEquals(15000, ds.getIdleTimeout());
        }
    }

    @Test
    void close_closesAllPools() {
        DataSourceRegistry reg = new DataSourceRegistry(new SecretResolver());
        HikariDataSource ds = (HikariDataSource) reg.getOrCreate("a", h2());
        reg.close();
        assertTrue(ds.isClosed());
        assertEquals(0, reg.size());
    }

    @Test
    void close_isIdempotent() {
        DataSourceRegistry reg = new DataSourceRegistry(new SecretResolver());
        reg.getOrCreate("a", h2());
        reg.close();
        assertDoesNotThrow(reg::close);
    }
}
