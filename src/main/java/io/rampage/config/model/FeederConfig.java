package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Configuration for a scenario's data feeder.
 *
 * <p>Bound to the {@code feeder} key in a scenario YAML file. Describes how
 * {@code FeederFactory} loads test data rows from a JDBC database: which
 * database to connect to, which SQL file to execute, how rows are cycled
 * through during the simulation, and what to do when the data is exhausted
 * or a required column value is missing.</p>
 *
 * <p>Rows are preloaded into memory in their entirety before the simulation
 * starts. The {@code columns} map provides per-column metadata that controls
 * type coercion, session storage, and sensitive-value masking.</p>
 */
public class FeederConfig {
    @JsonProperty("type")
    private String type;

    @JsonProperty("databaseRef")
    private String databaseRef;

    @JsonProperty("sqlFile")
    private String sqlFile;

    @JsonProperty("strategy")
    private String strategy = "circular";

    @JsonProperty("preload")
    private boolean preload = true;

    @JsonProperty("failIfEmpty")
    private boolean failIfEmpty = false;

    @JsonProperty("maxRows")
    private int maxRows = 10000;

    @JsonProperty("failIfOverLimit")
    private boolean failIfOverLimit = false;

    @JsonProperty("onExhaustion")
    private String onExhaustion = "stop";

    @JsonProperty("onMissingRequired")
    private String onMissingRequired = "fail";

    @JsonProperty("columns")
    private Map<String, ColumnConfig> columns;

    /**
     * Constructs a {@code FeederConfig} with default values: {@code strategy} is
     * {@code "circular"}, {@code preload} is {@code true}, {@code failIfEmpty} is
     * {@code false}, {@code maxRows} is {@code 10000}, {@code failIfOverLimit} is
     * {@code false}, {@code onExhaustion} is {@code "stop"}, and
     * {@code onMissingRequired} is {@code "fail"}.
     */
    public FeederConfig() {}

    /**
     * Returns the feeder type (for example, {@code "sql"}).
     *
     * @return the feeder type, or {@code null} if not configured
     */
    public String getType() { return type; }

    /**
     * Sets the feeder type.
     *
     * @param type the feeder type to use
     */
    public void setType(String type) { this.type = type; }

    /**
     * Returns the key that identifies the database in the environment's {@code databases} map.
     *
     * @return the database reference key, or {@code null} if not configured
     */
    public String getDatabaseRef() { return databaseRef; }

    /**
     * Sets the database reference key.
     *
     * @param databaseRef the key identifying the target database
     */
    public void setDatabaseRef(String databaseRef) { this.databaseRef = databaseRef; }

    /**
     * Returns the path to the SQL file whose query populates the feeder.
     * The file is resolved from the filesystem first, then from the classpath.
     *
     * @return the SQL file path, or {@code null} if not configured
     */
    public String getSqlFile() { return sqlFile; }

    /**
     * Sets the path to the SQL file.
     *
     * @param sqlFile the SQL file path to use
     */
    public void setSqlFile(String sqlFile) { this.sqlFile = sqlFile; }

    /**
     * Returns the row-cycling strategy (for example, {@code "circular"} or {@code "random"}).
     * Defaults to {@code "circular"}.
     *
     * @return the cycling strategy
     */
    public String getStrategy() { return strategy; }

    /**
     * Sets the row-cycling strategy.
     *
     * @param strategy the cycling strategy to use
     */
    public void setStrategy(String strategy) { this.strategy = strategy; }

    /**
     * Returns whether all rows should be loaded into memory before the simulation starts.
     * Defaults to {@code true}.
     *
     * @return {@code true} if rows are preloaded; {@code false} otherwise
     */
    public boolean isPreload() { return preload; }

    /**
     * Sets whether rows should be preloaded into memory before the simulation starts.
     *
     * @param preload {@code true} to preload all rows
     */
    public void setPreload(boolean preload) { this.preload = preload; }

    /**
     * Returns whether the simulation should fail at startup if the feeder query returns no rows.
     * Defaults to {@code false}.
     *
     * @return {@code true} if an empty result set is fatal; {@code false} otherwise
     */
    public boolean isFailIfEmpty() { return failIfEmpty; }

    /**
     * Sets whether the simulation should fail at startup when the feeder returns no rows.
     *
     * @param failIfEmpty {@code true} to treat an empty result set as fatal
     */
    public void setFailIfEmpty(boolean failIfEmpty) { this.failIfEmpty = failIfEmpty; }

    /**
     * Returns the maximum number of rows to load from the SQL query.
     * Defaults to {@code 10000}.
     *
     * @return the row limit
     */
    public int getMaxRows() { return maxRows; }

    /**
     * Sets the maximum number of rows to load from the SQL query.
     *
     * @param maxRows the maximum number of rows
     */
    public void setMaxRows(int maxRows) { this.maxRows = maxRows; }

    /**
     * Returns whether the simulation should fail if the SQL query returns more rows than {@code maxRows}.
     * Defaults to {@code false}.
     *
     * @return {@code true} if exceeding {@code maxRows} is fatal; {@code false} otherwise
     */
    public boolean isFailIfOverLimit() { return failIfOverLimit; }

    /**
     * Sets whether the simulation should fail when the SQL result exceeds {@code maxRows}.
     *
     * @param failIfOverLimit {@code true} to treat a result over the limit as fatal
     */
    public void setFailIfOverLimit(boolean failIfOverLimit) { this.failIfOverLimit = failIfOverLimit; }

    /**
     * Returns the behaviour when the feeder data is exhausted
     * (for example, {@code "stop"} or {@code "recycle"}).
     * Defaults to {@code "stop"}.
     *
     * @return the exhaustion behaviour
     */
    public String getOnExhaustion() { return onExhaustion; }

    /**
     * Sets the behaviour when the feeder data is exhausted.
     *
     * @param onExhaustion the exhaustion behaviour to use
     */
    public void setOnExhaustion(String onExhaustion) { this.onExhaustion = onExhaustion; }

    /**
     * Returns the behaviour when a required column value is missing from a row
     * (for example, {@code "fail"} or {@code "skip"}).
     * Defaults to {@code "fail"}.
     *
     * @return the missing-required-column behaviour
     */
    public String getOnMissingRequired() { return onMissingRequired; }

    /**
     * Sets the behaviour when a required column value is missing from a row.
     *
     * @param onMissingRequired the behaviour to apply
     */
    public void setOnMissingRequired(String onMissingRequired) { this.onMissingRequired = onMissingRequired; }

    /**
     * Returns the per-column metadata map, keyed by SQL column name.
     *
     * @return the columns map, or {@code null} if not configured
     */
    public Map<String, ColumnConfig> getColumns() { return columns; }

    /**
     * Sets the per-column metadata map.
     *
     * @param columns a map of SQL column names to their configurations
     */
    public void setColumns(Map<String, ColumnConfig> columns) { this.columns = columns; }
}
