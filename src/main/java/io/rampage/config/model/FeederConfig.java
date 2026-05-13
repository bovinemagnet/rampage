package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

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

    @JsonProperty("columns")
    private Map<String, ColumnConfig> columns;

    public FeederConfig() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDatabaseRef() { return databaseRef; }
    public void setDatabaseRef(String databaseRef) { this.databaseRef = databaseRef; }
    public String getSqlFile() { return sqlFile; }
    public void setSqlFile(String sqlFile) { this.sqlFile = sqlFile; }
    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
    public boolean isPreload() { return preload; }
    public void setPreload(boolean preload) { this.preload = preload; }
    public boolean isFailIfEmpty() { return failIfEmpty; }
    public void setFailIfEmpty(boolean failIfEmpty) { this.failIfEmpty = failIfEmpty; }
    public Map<String, ColumnConfig> getColumns() { return columns; }
    public void setColumns(Map<String, ColumnConfig> columns) { this.columns = columns; }
}
