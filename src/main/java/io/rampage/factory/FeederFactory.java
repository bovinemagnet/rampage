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

/**
 * Loads and manages feeder data for Gatling scenarios, sourcing rows from a JDBC data source.
 *
 * <p>SQL resolution follows a two-step fallback: the configured SQL file is looked up on the
 * filesystem first, then on the classpath. If neither is found, the stub query
 * {@code SELECT 1 AS userId} is used so that the simulation can still start.
 *
 * <p>Two loading modes are available:
 * <ul>
 *   <li><b>Eager (pre-load)</b> — {@link #loadFromSql(DataSource, FeederConfig)} reads the
 *       entire result set into memory before the simulation begins.</li>
 *   <li><b>Streaming</b> — {@link #streamFromSql(DataSource, FeederConfig)} keeps the JDBC
 *       connection open and reads rows lazily via a {@link StreamingFeeder} iterator.</li>
 * </ul>
 *
 * <p>Column-level rules (renaming via {@code sessionKey}, {@code required}, and
 * {@code sensitive} flags) declared in {@code FeederConfig.columns} are applied to every
 * row after it is read.
 */
public class FeederFactory {
    private static final Logger log = LoggerFactory.getLogger(FeederFactory.class);

    private SecretResolver secretResolverForTracking;

    /**
     * Creates a {@code FeederFactory} with no secret resolver for sensitive-value tracking.
     */
    public FeederFactory() {}

    /**
     * Creates a {@code FeederFactory} that registers sensitive column values with the
     * supplied resolver so they can be redacted in logs and snapshots.
     *
     * @param secretResolverForTracking the resolver that receives sensitive column values;
     *                                  may be updated at call time via the ad-hoc overload
     */
    public FeederFactory(SecretResolver secretResolverForTracking) {
        this.secretResolverForTracking = secretResolverForTracking;
    }

    /**
     * Backwards-compatible entry point that creates an ad-hoc pool per call. Prefer
     * {@link #loadFromSql(DataSource, FeederConfig)} with a shared {@link DataSourceRegistry}.
     *
     * @param db             the database configuration used to build the temporary pool
     * @param feeder         the feeder configuration controlling SQL resolution and column rules
     * @param secretResolver the resolver used for credential expansion and sensitive tracking
     * @return the list of feeder rows as ordered maps of column name to value
     */
    public List<Map<String, Object>> loadFromSql(DatabaseConfig db, FeederConfig feeder, SecretResolver secretResolver) {
        SecretResolver previous = this.secretResolverForTracking;
        this.secretResolverForTracking = secretResolver;
        try (DataSourceRegistry registry = new DataSourceRegistry(secretResolver)) {
            DataSource ds = registry.getOrCreate("adhoc", db);
            return loadFromSql(ds, feeder);
        } finally {
            this.secretResolverForTracking = previous;
        }
    }

    /**
     * Eagerly loads all feeder rows from the given data source into memory.
     *
     * <p>The SQL to execute is resolved via the feeder's {@code sqlFile} field (filesystem
     * then classpath), falling back to {@code SELECT 1 AS userId}. Column rules declared in
     * the feeder configuration are applied to each row. Rows that fail a {@code required}
     * column check are dropped (when {@code onMissingRequired=skip}) or cause a
     * {@code RuntimeException} (the default). A {@code RuntimeException} is also thrown
     * when {@code failIfEmpty=true} and no rows are returned.
     *
     * @param dataSource the JDBC data source to query
     * @param feeder     the feeder configuration controlling SQL resolution, row limits, and
     *                   column rules
     * @return the list of feeder rows, each an ordered map of session-key to value
     * @throws RuntimeException if the SQL query fails or a feeder constraint is violated
     */
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

            if (col.isSensitive() && value != null && secretResolverForTracking != null) {
                secretResolverForTracking.trackSensitive(value.toString());
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
     * Opens a streaming feeder backed by a live JDBC {@code ResultSet}.
     *
     * <p>The returned {@link StreamingFeeder} lazily reads rows on demand rather than
     * loading the entire result set at once. It implements {@code AutoCloseable}; callers
     * should close it (e.g. in the simulation's {@code after()} hook). The iterator's
     * {@code next()} method is synchronised for safe concurrent use by Gatling.
     *
     * @param dataSource the JDBC data source to query
     * @param feeder     the feeder configuration controlling SQL resolution and column rules
     * @return an open {@link StreamingFeeder} positioned before the first row
     * @throws RuntimeException if the JDBC connection or query cannot be established
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

    /**
     * A lazy, synchronised iterator over a live JDBC {@code ResultSet} that applies
     * {@code FeederConfig} column rules to each row as it is consumed.
     *
     * <p>The underlying JDBC resources are closed automatically when the result set is
     * exhausted or when {@link #close()} is called explicitly. Thread safety is provided
     * by synchronising {@link #hasNext()} and {@link #next()}.
     */
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

    /**
     * Creates a circular iterator over the supplied feeder rows using the default
     * {@code circular} strategy.
     *
     * @param data the feeder rows to iterate; must not be {@code null}
     * @return a circular iterator that wraps around to the beginning when exhausted
     */
    public Iterator<Map<String, Object>> createFeeder(List<Map<String, Object>> data) {
        return new CircularIterator<>(data);
    }

    /**
     * Creates an iterator over the supplied feeder rows using the named strategy.
     *
     * <p>When {@code strategy} is {@code "random"} or {@code "shuffle"}, the list is
     * shuffled before being wrapped in a circular iterator. All other values (including
     * {@code "circular"}) produce an unshuffled circular iterator.
     *
     * @param data     the feeder rows to iterate; must not be {@code null}
     * @param strategy the iteration strategy ({@code "circular"}, {@code "random"}, or
     *                 {@code "shuffle"}); unrecognised values fall back to circular
     * @return an iterator over the rows
     */
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
