package io.rampage.factory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.rampage.config.model.RequestConfig;
import io.rampage.config.model.ScenarioConfig;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioFactoryTest {
    private static final ObjectMapper PARSER = new ObjectMapper();

    private ScenarioConfig newScenario(String id) {
        ScenarioConfig sc = new ScenarioConfig();
        sc.setId(id);
        sc.setName(id);
        sc.setEndpointRef("graphql");
        return sc;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseBody(String json) throws Exception {
        return PARSER.readValue(json, new TypeReference<Map<String, Object>>() {});
    }

    @Test
    void buildRequestBody_includesQueryAndEmptyVariables() throws Exception {
        ScenarioConfig sc = newScenario("test");
        String body = ScenarioFactory.buildRequestBody(sc, "query Foo { foo }");

        Map<String, Object> parsed = parseBody(body);
        assertEquals("query Foo { foo }", parsed.get("query"));
        assertTrue(parsed.containsKey("variables"));
        assertTrue(((Map<?, ?>) parsed.get("variables")).isEmpty());
        assertFalse(parsed.containsKey("operationName"));
    }

    @Test
    void buildRequestBody_includesOperationNameWhenSet() throws Exception {
        ScenarioConfig sc = newScenario("test");
        sc.setOperationName("GetUser");
        String body = ScenarioFactory.buildRequestBody(sc, "query GetUser { user { id } }");

        Map<String, Object> parsed = parseBody(body);
        assertEquals("GetUser", parsed.get("operationName"));
    }

    @Test
    void buildRequestBody_rewritesFeederPlaceholdersToGatlingEl() throws Exception {
        ScenarioConfig sc = newScenario("test");
        RequestConfig req = new RequestConfig();
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("userId", "${feeder:userId}");
        req.setVariables(vars);
        sc.setRequest(req);

        String body = ScenarioFactory.buildRequestBody(sc, "query Q { user(id: $userId) { id } }");

        Map<String, Object> parsed = parseBody(body);
        Map<?, ?> resolvedVars = (Map<?, ?>) parsed.get("variables");
        assertEquals("#{userId}", resolvedVars.get("userId"));
    }

    @Test
    void buildRequestBody_preservesBooleanVariables() throws Exception {
        ScenarioConfig sc = newScenario("test");
        RequestConfig req = new RequestConfig();
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("includeInactive", false);
        vars.put("active", true);
        req.setVariables(vars);
        sc.setRequest(req);

        String body = ScenarioFactory.buildRequestBody(sc, "query Q { foo }");

        // Boolean should round-trip as boolean, not "false" string.
        Map<String, Object> parsed = parseBody(body);
        Map<?, ?> resolvedVars = (Map<?, ?>) parsed.get("variables");
        assertEquals(Boolean.FALSE, resolvedVars.get("includeInactive"));
        assertEquals(Boolean.TRUE, resolvedVars.get("active"));
    }

    @Test
    void buildRequestBody_preservesNumericVariables() throws Exception {
        ScenarioConfig sc = newScenario("test");
        RequestConfig req = new RequestConfig();
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("limit", 50);
        vars.put("ratio", 1.5);
        req.setVariables(vars);
        sc.setRequest(req);

        String body = ScenarioFactory.buildRequestBody(sc, "query Q { foo }");

        Map<String, Object> parsed = parseBody(body);
        Map<?, ?> resolvedVars = (Map<?, ?>) parsed.get("variables");
        assertEquals(50, ((Number) resolvedVars.get("limit")).intValue());
        assertEquals(1.5, ((Number) resolvedVars.get("ratio")).doubleValue());
    }

    @Test
    void buildRequestBody_preservesNullVariables() throws Exception {
        ScenarioConfig sc = newScenario("test");
        RequestConfig req = new RequestConfig();
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("optional", null);
        req.setVariables(vars);
        sc.setRequest(req);

        String body = ScenarioFactory.buildRequestBody(sc, "query Q { foo }");

        Map<String, Object> parsed = parseBody(body);
        Map<?, ?> resolvedVars = (Map<?, ?>) parsed.get("variables");
        assertTrue(resolvedVars.containsKey("optional"));
        assertNull(resolvedVars.get("optional"));
    }

    @Test
    void buildRequestBody_escapesQuotesAndUnicodeInQuery() throws Exception {
        ScenarioConfig sc = newScenario("test");
        String tricky = "query Q { foo(s: \"naïve \\\\ \"bar\" \") { id } }";
        String body = ScenarioFactory.buildRequestBody(sc, tricky);

        Map<String, Object> parsed = parseBody(body);
        // Round-trip preserves the exact query string.
        assertEquals(tricky, parsed.get("query"));
    }

    @Test
    void buildRequestBody_handlesMissingRequestBlock() throws Exception {
        ScenarioConfig sc = newScenario("test");
        // No RequestConfig set
        String body = ScenarioFactory.buildRequestBody(sc, "query Q { foo }");

        Map<String, Object> parsed = parseBody(body);
        assertTrue(((Map<?, ?>) parsed.get("variables")).isEmpty());
    }

    @Test
    void buildRequestBody_handlesNullQuery() throws Exception {
        ScenarioConfig sc = newScenario("test");
        String body = ScenarioFactory.buildRequestBody(sc, null);

        Map<String, Object> parsed = parseBody(body);
        assertEquals("", parsed.get("query"));
    }

    @Test
    void rewriteFeederPlaceholders_passesThroughLiteralStrings() {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("plain", "hello");
        vars.put("partial", "prefix-${feeder:x}-suffix");
        Map<String, Object> result = ScenarioFactory.rewriteFeederPlaceholders(vars);

        // Only exact-match placeholders are rewritten; partial matches remain literal.
        assertEquals("hello", result.get("plain"));
        assertEquals("prefix-${feeder:x}-suffix", result.get("partial"));
    }

    @Test
    void rewriteFeederPlaceholders_handlesNullInput() {
        Map<String, Object> result = ScenarioFactory.rewriteFeederPlaceholders(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // Full build() wiring is covered by the simulation integration test (see F-038);
    // Gatling's HttpDsl static initialiser requires the Gatling runtime which is not
    // present in the standard test source set.
}
