package io.rampage.factory;

import io.gatling.javaapi.core.CheckBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.rampage.config.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class ScenarioFactory {
    private static final Logger log = LoggerFactory.getLogger(ScenarioFactory.class);

    public ScenarioBuilder build(ScenarioConfig scenarioCfg, String graphqlQuery) {
        log.info("Building scenario: {}", scenarioCfg.getName());

        String endpointRef = scenarioCfg.getEndpointRef() != null ? scenarioCfg.getEndpointRef() : "graphql";
        String endpoint = "/" + endpointRef;

        String escapedQuery = graphqlQuery
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");

        Map<String, String> variables = scenarioCfg.getRequest() != null
            ? scenarioCfg.getRequest().getVariables() : null;
        String variablesJson = buildVariablesJson(variables);

        String bodyExpression = "{\"query\": \"" + escapedQuery + "\", \"variables\": " + variablesJson + "}";

        List<CheckBuilder> checks = buildChecks(scenarioCfg.getChecks());

        var request = http(scenarioCfg.getName())
            .post(endpoint)
            .header("Content-Type", "application/json");

        if (scenarioCfg.getHeaders() != null) {
            for (Map.Entry<String, String> entry : scenarioCfg.getHeaders().entrySet()) {
                request = request.header(entry.getKey(), entry.getValue());
            }
        }

        request = request.body(StringBody(bodyExpression));

        if (!checks.isEmpty()) {
            request = request.check(checks.toArray(new CheckBuilder[0]));
        }

        return scenario(scenarioCfg.getName()).exec(request);
    }

    private String buildVariablesJson(Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            String value = entry.getValue();
            if (value != null && value.startsWith("${feeder:") && value.endsWith("}")) {
                value = "#{" + value.substring(9, value.length() - 1) + "}";
            }
            sb.append("\"").append(entry.getKey()).append("\": \"").append(value).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private List<CheckBuilder> buildChecks(ChecksConfig checksConfig) {
        List<CheckBuilder> checks = new ArrayList<>();
        if (checksConfig == null) return checks;

        if (checksConfig.getHttpStatus() != null) {
            checks.add(status().is(checksConfig.getHttpStatus()));
        }

        if (checksConfig.getJsonPath() != null) {
            for (JsonPathCheck check : checksConfig.getJsonPath()) {
                if (check.getPath() == null) continue;
                String expectation = check.getExpectation();
                if ("exists".equalsIgnoreCase(expectation)) {
                    checks.add(jsonPath(check.getPath()).exists());
                } else if ("absentOrEmpty".equalsIgnoreCase(expectation)) {
                    checks.add(jsonPath(check.getPath()).notExists());
                } else if ("equalsSession".equalsIgnoreCase(expectation) && check.getSessionKey() != null) {
                    checks.add(jsonPath(check.getPath()).isEL("#{" + check.getSessionKey() + "}"));
                }
            }
        }

        return checks;
    }
}
