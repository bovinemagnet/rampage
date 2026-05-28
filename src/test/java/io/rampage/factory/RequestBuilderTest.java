package io.rampage.factory;

import io.rampage.config.model.EnvironmentConfig;
import io.rampage.config.model.RequestConfig;
import io.rampage.config.model.ScenarioConfig;
import io.rampage.config.model.StepConfig;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RequestBuilderTest {

    private EnvironmentConfig envWithBaseUrls(Map<String, String> baseUrls) {
        EnvironmentConfig env = new EnvironmentConfig();
        env.setId("test");
        env.setName("Test");
        env.setBaseUrls(baseUrls);
        return env;
    }

    @Test
    void resolvePath_returnsRelativePathWhenStepEndpointRefMatchesScenario() {
        ScenarioConfig scenarioCfg = new ScenarioConfig();
        scenarioCfg.setId("s1");
        scenarioCfg.setEndpointRef("graphql");

        StepConfig step = new StepConfig();
        step.setName("call");
        step.setEndpointRef("graphql");

        RequestConfig req = new RequestConfig();
        req.setPath("/users");
        step.setRequest(req);

        EnvironmentConfig env = envWithBaseUrls(Map.of(
            "graphql", "http://localhost:9090",
            "rest", "http://localhost:8080"));

        assertEquals("/users", RequestBuilder.resolvePath(scenarioCfg, step, req, env));
    }

    @Test
    void resolvePath_returnsRelativePathWhenStepEndpointRefIsNull() {
        ScenarioConfig scenarioCfg = new ScenarioConfig();
        scenarioCfg.setId("s1");
        scenarioCfg.setEndpointRef("graphql");

        StepConfig step = new StepConfig();
        step.setName("call");

        RequestConfig req = new RequestConfig();
        req.setPath("/users");
        step.setRequest(req);

        EnvironmentConfig env = envWithBaseUrls(Map.of(
            "graphql", "http://localhost:9090"));

        assertEquals("/users", RequestBuilder.resolvePath(scenarioCfg, step, req, env));
    }

    @Test
    void resolvePath_returnsAbsoluteUrlWhenStepEndpointRefDiffers() {
        ScenarioConfig scenarioCfg = new ScenarioConfig();
        scenarioCfg.setId("s1");
        scenarioCfg.setEndpointRef("graphql");

        StepConfig step = new StepConfig();
        step.setName("call-rest-side");
        step.setEndpointRef("rest");

        RequestConfig req = new RequestConfig();
        req.setPath("/health");
        step.setRequest(req);

        Map<String, String> urls = new LinkedHashMap<>();
        urls.put("graphql", "http://gql.example/graphql");
        urls.put("rest", "http://rest.example");
        EnvironmentConfig env = envWithBaseUrls(urls);

        assertEquals("http://rest.example/health",
            RequestBuilder.resolvePath(scenarioCfg, step, req, env));
    }

    @Test
    void resolvePath_handlesTrailingSlashOnBaseUrl() {
        ScenarioConfig scenarioCfg = new ScenarioConfig();
        scenarioCfg.setEndpointRef("graphql");

        StepConfig step = new StepConfig();
        step.setEndpointRef("rest");

        RequestConfig req = new RequestConfig();
        req.setPath("/health");
        step.setRequest(req);

        EnvironmentConfig env = envWithBaseUrls(Map.of(
            "graphql", "http://gql.example/graphql",
            "rest", "http://rest.example/"));

        assertEquals("http://rest.example/health",
            RequestBuilder.resolvePath(scenarioCfg, step, req, env));
    }

    @Test
    void resolvePath_leavesPathLiteralWhenPathAlreadyAbsolute() {
        ScenarioConfig scenarioCfg = new ScenarioConfig();
        scenarioCfg.setEndpointRef("graphql");

        StepConfig step = new StepConfig();
        step.setEndpointRef("rest");

        RequestConfig req = new RequestConfig();
        req.setPath("https://other.example/some/full/url");
        step.setRequest(req);

        EnvironmentConfig env = envWithBaseUrls(Map.of(
            "graphql", "http://gql.example",
            "rest", "http://rest.example"));

        assertEquals("https://other.example/some/full/url",
            RequestBuilder.resolvePath(scenarioCfg, step, req, env));
    }

    @Test
    void buildGraphqlBody_usesStepVariablesWhenStepRequestSet() throws Exception {
        ScenarioConfig sc = new ScenarioConfig();
        sc.setId("test");
        sc.setEndpointRef("graphql");
        RequestConfig scenarioReq = new RequestConfig();
        Map<String, Object> scenarioVars = new LinkedHashMap<>();
        scenarioVars.put("scenarioVar", "scenarioValue");
        scenarioReq.setVariables(scenarioVars);
        sc.setRequest(scenarioReq);

        RequestConfig stepReq = new RequestConfig();
        Map<String, Object> stepVars = new LinkedHashMap<>();
        stepVars.put("stepVar", "stepValue");
        stepReq.setVariables(stepVars);

        String body = RequestBuilder.buildGraphqlBody(sc, stepReq, "query { foo }");

        Map<String, Object> parsed = new com.fasterxml.jackson.databind.ObjectMapper()
            .readValue(body, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        Map<?, ?> variables = (Map<?, ?>) parsed.get("variables");
        assertEquals("stepValue", variables.get("stepVar"));
        assertFalse(variables.containsKey("scenarioVar"),
            "Step-level variables should override, not merge");
    }

    @Test
    void buildGraphqlBody_fallsBackToScenarioVariablesWhenStepHasNoVariables() throws Exception {
        ScenarioConfig sc = new ScenarioConfig();
        sc.setId("test");
        RequestConfig scenarioReq = new RequestConfig();
        Map<String, Object> scenarioVars = new LinkedHashMap<>();
        scenarioVars.put("scenarioVar", "scenarioValue");
        scenarioReq.setVariables(scenarioVars);
        sc.setRequest(scenarioReq);

        RequestConfig stepReq = new RequestConfig();
        // No variables set on stepReq

        String body = RequestBuilder.buildGraphqlBody(sc, stepReq, "query { foo }");

        Map<String, Object> parsed = new com.fasterxml.jackson.databind.ObjectMapper()
            .readValue(body, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        Map<?, ?> variables = (Map<?, ?>) parsed.get("variables");
        assertEquals("scenarioValue", variables.get("scenarioVar"));
    }

    @Test
    void buildGraphqlBody_emptyVariablesWhenNeitherSet() throws Exception {
        ScenarioConfig sc = new ScenarioConfig();
        sc.setId("test");

        String body = RequestBuilder.buildGraphqlBody(sc, null, "query { foo }");

        Map<String, Object> parsed = new com.fasterxml.jackson.databind.ObjectMapper()
            .readValue(body, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        Map<?, ?> variables = (Map<?, ?>) parsed.get("variables");
        assertTrue(variables.isEmpty());
    }

    @Test
    void resolvePath_fallsBackToEndpointRefAsPathWhenNoRequestPathConfigured() {
        ScenarioConfig scenarioCfg = new ScenarioConfig();
        scenarioCfg.setEndpointRef("graphql");

        StepConfig step = new StepConfig();
        step.setEndpointRef("graphql");

        EnvironmentConfig env = envWithBaseUrls(Map.of("graphql", "http://gql.example/graphql"));

        // No request set → legacy default: "/" + endpointRef
        assertEquals("/graphql", RequestBuilder.resolvePath(scenarioCfg, step, null, env));
    }
}
