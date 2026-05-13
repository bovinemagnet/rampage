package io.rampage.factory;

import io.rampage.config.model.DatabaseConfig;
import io.rampage.config.model.FeederConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

public class FeederFactory {
    private static final Logger log = LoggerFactory.getLogger(FeederFactory.class);

    public List<Map<String, Object>> loadFromSql(DatabaseConfig db, FeederConfig feeder, SecretResolver secretResolver) {
        String url = db.getUrl();
        String username = secretResolver.resolve(db.getUsernameRef());
        String password = secretResolver.resolve(db.getPasswordRef());

        log.info("Loading feeder data from SQL, preload={}", feeder.getPreload());

        try {
            Class.forName(db.getDriver());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("JDBC driver not found: " + db.getDriver(), e);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, username, password);
             PreparedStatement stmt = conn.prepareStatement(feeder.getQuery());
             ResultSet rs = stmt.executeQuery()) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            int count = 0;

            while (rs.next() && count < feeder.getPreload()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
                count++;
            }

            log.info("Loaded {} feeder rows from SQL", rows.size());
            return rows;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to load feeder data from SQL: " + e.getMessage(), e);
        }
    }

    public Iterator<Map<String, Object>> createFeeder(List<Map<String, Object>> data) {
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
