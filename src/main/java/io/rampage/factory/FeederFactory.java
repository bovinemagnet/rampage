package io.rampage.factory;

import io.rampage.config.model.ColumnConfig;
import io.rampage.config.model.DatabaseConfig;
import io.rampage.config.model.FeederConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.Locale;

public class FeederFactory {
    private static final Logger log = LoggerFactory.getLogger(FeederFactory.class);

    /**
     * Backwards-compatible entry point that creates an ad-hoc pool per call. Prefer
     * {@link #loadFromSql(DataSource, FeederConfig)} with a shared {@link DataSourceRegistry}.
     */
    public List<Map<String, Object>> loadFromSql(DatabaseConfig db, FeederConfig feeder, SecretResolver secretResolver) {
        try (DataSourceRegistry registry = new DataSourceRegistry(secretResolver)) {
            DataSource ds = registry.getOrCreate("adhoc", db);
            return loadFromSql(ds, feeder);
        }
    }

    public List<Map<String, Object>> loadFromSql(DataSource dataSource, FeederConfig feeder) {
        String sql = resolveSql(feeder);
        log.info("Loading feeder data from SQL, preload={}", feeder.isPreload());
        int maxRows = feeder.getMaxRows() > 0 ? feeder.getMaxRows() : Integer.MAX_VALUE;
        boolean failOver = feeder.isFailIfOverLimit();
        boolean failOnMissing = !"skip".equalsIgnoreCase(feeder.getOnMissingRequired());

        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            Set<String> columnLabelsLower = new HashSet<>();
            for (int i = 1; i <= columnCount; i++) {
                columnLabelsLower.add(meta.getColumnLabel(i).toLowerCase(Locale.ROOT));
            }
            validateDeclaredColumnsPresent(feeder, columnLabelsLower);

            while (rs.next()) {
                if (rows.size() >= maxRows) {
                    if (failOver) {
                        throw new RuntimeException("Feeder produced more than " + maxRows
                            + " rows and failIfOverLimit=true");
                    }
                    log.warn("Feeder row cap reached ({}); remaining rows ignored", maxRows);
                    break;
                }
                Map<String, Object> raw = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    raw.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                Map<String, Object> processed = applyColumnRules(feeder, raw, failOnMissing);
                if (processed != null) {
                    rows.add(processed);
                }
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

    private void validateDeclaredColumnsPresent(FeederConfig feeder, Set<String> labelsLower) {
        if (feeder.getColumns() == null) return;
        List<String> missing = new ArrayList<>();
        for (String declared : feeder.getColumns().keySet()) {
            if (!labelsLower.contains(declared.toLowerCase(Locale.ROOT))) {
                missing.add(declared);
            }
        }
        if (!missing.isEmpty()) {
            throw new RuntimeException("Feeder SQL result is missing declared columns: " + missing);
        }
    }

    private Map<String, Object> applyColumnRules(FeederConfig feeder, Map<String, Object> raw, boolean failOnMissing) {
        Map<String, ColumnConfig> declared = feeder.getColumns();
        if (declared == null || declared.isEmpty()) return raw;

        Map<String, Object> result = new LinkedHashMap<>(raw);
        for (Map.Entry<String, ColumnConfig> entry : declared.entrySet()) {
            String declaredName = entry.getKey();
            ColumnConfig col = entry.getValue();
            Object value = findValueCaseInsensitive(raw, declaredName);

            if (col.isRequired() && value == null) {
                if (failOnMissing) {
                    throw new RuntimeException("Feeder row missing required column '" + declaredName + "'");
                }
                return null;
            }

            String key = col.getSessionKey() != null && !col.getSessionKey().isBlank()
                ? col.getSessionKey() : declaredName;
            if (!key.equals(declaredName)) {
                // Remove the original label and write under the session key
                String originalLabel = findKeyCaseInsensitive(raw, declaredName);
                if (originalLabel != null) {
                    result.remove(originalLabel);
                }
                result.put(key, value);
            }
        }
        return result;
    }

    private Object findValueCaseInsensitive(Map<String, Object> row, String key) {
        String found = findKeyCaseInsensitive(row, key);
        return found != null ? row.get(found) : null;
    }

    private String findKeyCaseInsensitive(Map<String, Object> row, String key) {
        if (row.containsKey(key)) return key;
        for (String k : row.keySet()) {
            if (k.equalsIgnoreCase(key)) return k;
        }
        return null;
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

    /**
     * Streaming feeder: opens a JDBC connection + ResultSet and returns an iterator
     * that lazily reads rows. The iterator implements {@link AutoCloseable}; callers
     * should close it (e.g. in the simulation's {@code after()}). The iterator's
     * {@code next()} method is synchronised for safe concurrent use by Gatling.
     */
    public StreamingFeeder streamFromSql(DataSource dataSource, FeederConfig feeder) {
        String sql = resolveSql(feeder);
        log.info("Opening streaming feeder, maxRows={}", feeder.getMaxRows());
        try {
            Connection conn = dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            Set<String> labelsLower = new HashSet<>();
            String[] labels = new String[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                labels[i - 1] = meta.getColumnLabel(i);
                labelsLower.add(labels[i - 1].toLowerCase(Locale.ROOT));
            }
            validateDeclaredColumnsPresent(feeder, labelsLower);
            return new StreamingFeeder(conn, stmt, rs, labels, feeder);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open streaming feeder: " + e.getMessage(), e);
        }
    }

    public class StreamingFeeder implements Iterator<Map<String, Object>>, AutoCloseable {
        private final Connection conn;
        private final PreparedStatement stmt;
        private final ResultSet rs;
        private final String[] labels;
        private final FeederConfig feeder;
        private final int maxRows;
        private final boolean failOnMissing;
        private int delivered = 0;
        private Map<String, Object> nextRow;
        private boolean closed = false;
        private boolean prefetched = false;

        StreamingFeeder(Connection conn, PreparedStatement stmt, ResultSet rs,
                        String[] labels, FeederConfig feeder) {
            this.conn = conn;
            this.stmt = stmt;
            this.rs = rs;
            this.labels = labels;
            this.feeder = feeder;
            this.maxRows = feeder.getMaxRows() > 0 ? feeder.getMaxRows() : Integer.MAX_VALUE;
            this.failOnMissing = !"skip".equalsIgnoreCase(feeder.getOnMissingRequired());
        }

        @Override
        public synchronized boolean hasNext() {
            if (closed) return false;
            if (prefetched) return nextRow != null;
            prefetch();
            return nextRow != null;
        }

        @Override
        public synchronized Map<String, Object> next() {
            if (!prefetched) prefetch();
            if (nextRow == null) throw new NoSuchElementException();
            Map<String, Object> row = nextRow;
            nextRow = null;
            prefetched = false;
            delivered++;
            return row;
        }

        private void prefetch() {
            prefetched = true;
            try {
                while (delivered < maxRows && rs.next()) {
                    Map<String, Object> raw = new LinkedHashMap<>();
                    for (int i = 0; i < labels.length; i++) {
                        raw.put(labels[i], rs.getObject(i + 1));
                    }
                    Map<String, Object> processed = applyColumnRules(feeder, raw, failOnMissing);
                    if (processed != null) {
                        nextRow = processed;
                        return;
                    }
                }
                nextRow = null;
                closeQuietly();
            } catch (SQLException e) {
                closeQuietly();
                throw new RuntimeException("Streaming feeder read failed: " + e.getMessage(), e);
            }
        }

        private void closeQuietly() {
            close();
        }

        @Override
        public synchronized void close() {
            if (closed) return;
            closed = true;
            try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException ignored) {}
            try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
        }
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
