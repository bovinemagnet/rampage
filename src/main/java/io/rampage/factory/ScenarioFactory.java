package io.rampage.factory;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.rampage.config.model.CheckConfig;
import io.rampage.config.model.ScenarioConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class ScenarioFactory {
    private static final Logger log = LoggerFactory.getLogger(ScenarioFactory.class);

    public ScenarioBuilder build(ScenarioConfig.Scenario scenarioCfg, String graphqlQuery) {
        log.info("Building scenario: {}", scenarioCfg.getName());

        // Escape quotes in graphql query for JSON embedding
        String escapedQuery = graphqlQuery
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");

        // Build variables JSON snippet
        String variablesJson = buildVariablesJson(scenarioCfg.getGraphql().getVariables());

        // Build request body as Gatling EL expression
        String bodyExpression = "{\"query\": \"" + escapedQuery + "\", \"variables\": " + variablesJson + "}";

        // Build checks
        List<io.gatling.javaapi.core.CheckBuilder> checks = buildChecks(scenarioCfg.getChecks());

        var request = http(scenarioCfg.getName())
            .post(scenarioCfg.getGraphql().getEndpoint())
            .header("Content-Type", "application/json");

        // Add scenario-specific headers
        if (scenarioCfg.getHeaders() != null) {
            for (Map.Entry<String, String> entry : scenarioCfg.getHeaders().entrySet()) {
                request = request.header(entry.getKey(), entry.getValue());
            }
        }

        request = request.body(StringBody(bodyExpression));

        // Add checks
        if (!checks.isEmpty()) {
            request = request.check(checks.toArray(new io.gatling.javaapi.core.CheckBuilder[0]));
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
            sb.append("\"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private List<io.gatling.javaapi.core.CheckBuilder> buildChecks(List<CheckConfig> checkConfigs) {
        List<io.gatling.javaapi.core.CheckBuilder> checks = new ArrayList<>();
        if (checkConfigs == null) return checks;

        for (CheckConfig checkConfig : checkConfigs) {
            if (checkConfig.getJsonPath() != null) {
                if (Boolean.TRUE.equals(checkConfig.getExists())) {
                    checks.add(jsonPath(checkConfig.getJsonPath()).exists());
                } else if (Boolean.TRUE.equals(checkConfig.getNotExists())) {
                    checks.add(jsonPath(checkConfig.getJsonPath()).notExists());
                }
            }
        }
        return checks;
    }
}
