package io.rampage.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ChecksConfig {
    @JsonProperty("httpStatus")
    private Integer httpStatus;

    @JsonProperty("jsonPath")
    private List<JsonPathCheck> jsonPath;

    public ChecksConfig() {}

    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
    public List<JsonPathCheck> getJsonPath() { return jsonPath; }
    public void setJsonPath(List<JsonPathCheck> jsonPath) { this.jsonPath = jsonPath; }
}
