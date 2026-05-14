package io.rampage.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.CheckBuilder;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.rampage.config.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class ScenarioFactory {
    private static final Logger log = LoggerFactory.getLogger(ScenarioFactory.class);
    private static final ObjectMapper BODY_MAPPER = new ObjectMapper();
    private static final Pattern FEEDER_PLACEHOLDER = Pattern.compile("^\\$\\{feeder:([^}]+)\\}$");

    private final Supplier<String> correlationIdSupplier;

    public ScenarioFactory() {
        this(() -> UUID.randomUUID().toString());
    }

    public ScenarioFactory(Supplier<String> correlationIdSupplier) {
        this.correlationIdSupplier = correlationIdSupplier;
    }

    public ScenarioBuilder build(ScenarioConfig scenarioCfg, String graphqlQuery) {
        log.info("Building scenario: {}", scenarioCfg.getName());

        String endpointRef = scenarioCfg.getEndpointRef() != null ? scenarioCfg.getEndpointRef() : "graphql";
        String endpoint = "/" + endpointRef;

        String bodyExpression = buildRequestBody(scenarioCfg, graphqlQuery);

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

        Supplier<String> idSupplier = correlationIdSupplier;
        ChainBuilder withCorrelationId = CoreDsl.exec(session -> session.set("correlationId", idSupplier.get()));

        return scenario(scenarioCfg.getName()).exec(withCorrelationId, request);
    }

    static String buildRequestBody(ScenarioConfig scenarioCfg, String graphqlQuery) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", graphqlQuery != null ? graphqlQuery : "");
        body.put("variables", rewriteFeederPlaceholders(scenarioCfg.getRequest() != null
            ? scenarioCfg.getRequest().getVariables() : null));
        if (scenarioCfg.getOperationName() != null && !scenarioCfg.getOperationName().isBlank()) {
            body.put("operationName", scenarioCfg.getOperationName());
        }
        try {
            return BODY_MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to build request body for scenario '"
                + scenarioCfg.getName() + "'", e);
        }
    }

    static Map<String, Object> rewriteFeederPlaceholders(Map<String, Object> variables) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (variables == null) return result;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String s) {
                Matcher m = FEEDER_PLACEHOLDER.matcher(s);
                if (m.matches()) {
                    result.put(entry.getKey(), "#{" + m.group(1) + "}");
                    continue;
                }
            }
            result.put(entry.getKey(), value);
        }
        return result;
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
