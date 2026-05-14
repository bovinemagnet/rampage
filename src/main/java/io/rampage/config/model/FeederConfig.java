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
    public int getMaxRows() { return maxRows; }
    public void setMaxRows(int maxRows) { this.maxRows = maxRows; }
    public boolean isFailIfOverLimit() { return failIfOverLimit; }
    public void setFailIfOverLimit(boolean failIfOverLimit) { this.failIfOverLimit = failIfOverLimit; }
    public String getOnExhaustion() { return onExhaustion; }
    public void setOnExhaustion(String onExhaustion) { this.onExhaustion = onExhaustion; }
    public String getOnMissingRequired() { return onMissingRequired; }
    public void setOnMissingRequired(String onMissingRequired) { this.onMissingRequired = onMissingRequired; }
    public Map<String, ColumnConfig> getColumns() { return columns; }
    public void setColumns(Map<String, ColumnConfig> columns) { this.columns = columns; }
}
