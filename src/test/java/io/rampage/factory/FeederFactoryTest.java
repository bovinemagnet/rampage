package io.rampage.factory;

import io.rampage.config.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FeederFactoryTest {
    private FeederFactory feederFactory;
    private SecretResolver secretResolver;

    @BeforeEach
    void setUp() throws Exception {
        feederFactory = new FeederFactory();
        secretResolver = new SecretResolver();
        Class.forName("org.h2.Driver");
        try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:feedertest;DB_CLOSE_DELAY=-1", "sa", "")) {
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS load_test_users (id VARCHAR(36), active BOOLEAN)");
            conn.createStatement().execute(
                "DELETE FROM load_test_users");
            conn.createStatement().execute(
                "INSERT INTO load_test_users VALUES ('user-1', true)");
            conn.createStatement().execute(
                "INSERT INTO load_test_users VALUES ('user-2', true)");
        }
    }

    private DatabaseConfig testDb() {
        DatabaseConfig db = new DatabaseConfig();
        db.setDriverClassName("org.h2.Driver");
        db.setJdbcUrl("jdbc:h2:mem:feedertest;DB_CLOSE_DELAY=-1");
        CredentialConfig user = new CredentialConfig();
        user.setSource("plain");
        user.setValue("sa");
        db.setUsername(user);
        CredentialConfig pass = new CredentialConfig();
        pass.setSource("plain");
        pass.setValue("");
        db.setPassword(pass);
        return db;
    }

    @Test
    void loadFromSql_returnsRows() {
        FeederConfig feeder = new FeederConfig();
        feeder.setType("jdbc");
        feeder.setFailIfEmpty(false);
        List<Map<String, Object>> rows = feederFactory.loadFromSql(testDb(), feeder, secretResolver);
        assertFalse(rows.isEmpty());
    }

    @Test
    void loadFromSql_failIfEmpty_isTrueWhenSet() {
        FeederConfig feeder = new FeederConfig();
        feeder.setType("jdbc");
        feeder.setFailIfEmpty(true);
        assertTrue(feeder.isFailIfEmpty());
    }

    @Test
    void createFeeder_circular() {
        List<Map<String, Object>> data = List.of(
            Map.of("userId", "1"),
            Map.of("userId", "2")
        );
        var iter = feederFactory.createFeeder(data);
        assertTrue(iter.hasNext());
        assertEquals("1", iter.next().get("userId"));
        assertEquals("2", iter.next().get("userId"));
        assertEquals("1", iter.next().get("userId")); // wraps around
    }
}
