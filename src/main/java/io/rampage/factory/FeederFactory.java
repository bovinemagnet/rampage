package io.rampage.factory;

import io.rampage.config.model.DatabaseConfig;
import io.rampage.config.model.FeederConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

public class FeederFactory {
    private static final Logger log = LoggerFactory.getLogger(FeederFactory.class);

    public List<Map<String, Object>> loadFromSql(DatabaseConfig db, FeederConfig feeder, SecretResolver secretResolver) {
        String url = db.getJdbcUrl();
        String username = secretResolver.resolveCredential(db.getUsername());
        String password = secretResolver.resolveCredential(db.getPassword());

        String sql = resolveSql(feeder);
        log.info("Loading feeder data from SQL, preload={}", feeder.isPreload());

        if (db.getDriverClassName() != null) {
            try {
                Class.forName(db.getDriverClassName());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("JDBC driver not found: " + db.getDriverClassName(), e);
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, username, password);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }

            log.info("Loaded {} feeder rows from SQL", rows.size());

            if (rows.isEmpty() && feeder.isFailIfEmpty()) {
                throw new RuntimeException("Feeder returned no rows and failIfEmpty=true");
            }

            return rows;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load feeder data from SQL: " + e.getMessage(), e);
        }
    }

    private String resolveSql(FeederConfig feeder) {
        String sqlFile = feeder.getSqlFile();
        if (sqlFile != null && !sqlFile.isBlank()) {
            File file = new File(sqlFile);
            if (file.exists()) {
                try {
                    return Files.readString(Path.of(sqlFile), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read SQL file: " + sqlFile, e);
                }
            }
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(sqlFile)) {
                if (is != null) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read SQL file from classpath: " + sqlFile, e);
            }
            log.warn("SQL file not found: {}, falling back to default query", sqlFile);
        }
        return "SELECT 1 AS userId";
    }

    public Iterator<Map<String, Object>> createFeeder(List<Map<String, Object>> data) {
        return new CircularIterator<>(data);
    }

    public Iterator<Map<String, Object>> createFeeder(List<Map<String, Object>> data, String strategy) {
        if ("random".equalsIgnoreCase(strategy) || "shuffle".equalsIgnoreCase(strategy)) {
            List<Map<String, Object>> shuffled = new ArrayList<>(data);
            Collections.shuffle(shuffled);
            return new CircularIterator<>(shuffled);
        }
        return new CircularIterator<>(data);
    }

    private static class CircularIterator<T> implements Iterator<T> {
        private final List<T> data;
        private int index = 0;

        CircularIterator(List<T> data) {
            this.data = new ArrayList<>(data);
        }

        @Override
        public boolean hasNext() {
            return !data.isEmpty();
        }

        @Override
        public T next() {
            if (data.isEmpty()) throw new NoSuchElementException();
            T item = data.get(index % data.size());
            index++;
            return item;
        }
    }
}
