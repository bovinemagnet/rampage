package io.rampage.factory;

import io.rampage.config.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    void loadFromSql_throwsWhenEmptyAndFailIfEmpty(@TempDir Path tempDir) throws IOException {
        FeederConfig feeder = new FeederConfig();
        feeder.setType("jdbc");
        feeder.setSqlFile(writeSql(tempDir, "SELECT id FROM load_test_users WHERE 1 = 0").toString());
        feeder.setFailIfEmpty(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> feederFactory.loadFromSql(testDb(), feeder, secretResolver));
        assertTrue(ex.getMessage().contains("no rows"));
    }

    @Test
    void loadFromSql_throwsOnSqlError(@TempDir Path tempDir) throws IOException {
        FeederConfig feeder = new FeederConfig();
        feeder.setType("jdbc");
        feeder.setSqlFile(writeSql(tempDir, "SELECT * FROM table_that_does_not_exist").toString());

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> feederFactory.loadFromSql(testDb(), feeder, secretResolver));
        assertTrue(ex.getMessage().toLowerCase().contains("failed to load feeder data"));
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

    @Test
    void loadFromSql_failsWhenDeclaredColumnAbsent() {
        FeederConfig feeder = new FeederConfig();
        feeder.setType("jdbc");
        ColumnConfig col = new ColumnConfig();
        col.setRequired(true);
        feeder.setColumns(Map.of("not_a_real_column", col));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> feederFactory.loadFromSql(testDb(), feeder, secretResolver));
        assertTrue(ex.getMessage().contains("not_a_real_column"));
    }

    private Path writeSql(Path tempDir, String sql) throws IOException {
        Path file = tempDir.resolve("query-" + System.nanoTime() + ".sql");
        Files.writeString(file, sql);
        return file;
    }

    @Test
    void loadFromSql_remapsSessionKey(@TempDir Path tempDir) throws IOException {
        FeederConfig feeder = new FeederConfig();
        feeder.setType("jdbc");
        feeder.setSqlFile(writeSql(tempDir, "SELECT id FROM load_test_users").toString());
        ColumnConfig col = new ColumnConfig();
        col.setSessionKey("userId");
        feeder.setColumns(Map.of("id", col));

        List<Map<String, Object>> rows = feederFactory.loadFromSql(testDb(), feeder, secretResolver);

        assertFalse(rows.isEmpty());
        assertTrue(rows.get(0).containsKey("userId"));
        assertFalse(rows.get(0).containsKey("ID"));
        assertFalse(rows.get(0).containsKey("id"));
    }

    @Test
    void loadFromSql_capsAtMaxRows(@TempDir Path tempDir) throws IOException {
        FeederConfig feeder = new FeederConfig();
        feeder.setType("jdbc");
        feeder.setSqlFile(writeSql(tempDir, "SELECT id FROM load_test_users").toString());
        feeder.setMaxRows(1);

        List<Map<String, Object>> rows = feederFactory.loadFromSql(testDb(), feeder, secretResolver);
        assertEquals(1, rows.size());
    }

    @Test
    void streamFromSql_iteratesAllRowsLazily(@TempDir Path tempDir) throws IOException {
        FeederConfig feeder = new FeederConfig();
        feeder.setType("jdbc");
        feeder.setPreload(false);
        feeder.setSqlFile(writeSql(tempDir, "SELECT id FROM load_test_users ORDER BY id").toString());

        try (FeederFactory.StreamingFeeder stream = feederFactory.streamFromSql(
                new DataSourceRegistry(secretResolver).getOrCreate("test", testDb()), feeder)) {
            int count = 0;
            while (stream.hasNext()) {
                stream.next();
                count++;
                if (count > 10) break;
            }
            assertEquals(2, count);
        }
    }

    @Test
    void streamFromSql_appliesMaxRowsCap(@TempDir Path tempDir) throws IOException {
        FeederConfig feeder = new FeederConfig();
        feeder.setType("jdbc");
        feeder.setPreload(false);
        feeder.setMaxRows(1);
        feeder.setSqlFile(writeSql(tempDir, "SELECT id FROM load_test_users ORDER BY id").toString());

        try (FeederFactory.StreamingFeeder stream = feederFactory.streamFromSql(
                new DataSourceRegistry(secretResolver).getOrCreate("test", testDb()), feeder)) {
            int count = 0;
            while (stream.hasNext()) {
                stream.next();
                count++;
            }
            assertEquals(1, count);
        }
    }

    @Test
    void loadFromSql_failsWhenOverLimitAndFlagSet(@TempDir Path tempDir) throws IOException {
        FeederConfig feeder = new FeederConfig();
        feeder.setType("jdbc");
        feeder.setSqlFile(writeSql(tempDir, "SELECT id FROM load_test_users").toString());
        feeder.setMaxRows(1);
        feeder.setFailIfOverLimit(true);

        assertThrows(RuntimeException.class,
            () -> feederFactory.loadFromSql(testDb(), feeder, secretResolver));
    }
}
