package io.rampage.factory;

import io.rampage.config.model.DatabaseConfig;
import io.rampage.config.model.EnvironmentConfig;
import io.rampage.config.model.FeederConfig;
import io.rampage.config.model.MetadataConfig;
import io.rampage.config.model.RequestConfig;
import io.rampage.config.model.RunConfig;
import io.rampage.config.model.ScenarioConfig;
import io.rampage.config.model.StepConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlaceholderSubstitutorTest {

    private RunConfig run() {
        RunConfig run = new RunConfig();
        run.setId("test-run-id");
        run.setName("Test Run");
        run.setVersion(7);
        run.setEnvironment("perf");
        return run;
    }

    @Test
    void expand_runFieldId() {
        String result = PlaceholderSubstitutor.expand("X-Run-Id: ${run:id}", null, run(), new SecretResolver());
        assertEquals("X-Run-Id: test-run-id", result);
    }

    @Test
    void expand_runFieldVersion() {
        String result = PlaceholderSubstitutor.expand("v=${run:version}", null, run(), new SecretResolver());
        assertEquals("v=7", result);
    }

    @Test
    void expand_systemProperty() {
        System.setProperty("rampage.test.placeholder", "from-sys");
        try {
            String result = PlaceholderSubstitutor.expand("sys=${sys:rampage.test.placeholder}",
                null, run(), new SecretResolver());
            assertEquals("sys=from-sys", result);
        } finally {
            System.clearProperty("rampage.test.placeholder");
        }
    }

    @Test
    void expand_throwsOnUnknownRunField() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> PlaceholderSubstitutor.expand("${run:owner}", null, run(), new SecretResolver()));
        assertTrue(ex.getMessage().contains("owner"));
    }

    @Test
    void expand_collectsErrorsWhenMissing() {
        List<String> errors = new ArrayList<>();
        String result = PlaceholderSubstitutor.expand(
            "missing=${env:RAMPAGE_TEST_DEFINITELY_NOT_SET}",
            null, run(), new SecretResolver(), errors);
        assertEquals(1, errors.size());
        assertEquals("missing=", result);
    }

    @Test
    void expand_passesFeederPlaceholderThroughUntouched() {
        List<String> errors = new ArrayList<>();
        String result = PlaceholderSubstitutor.expand(
            "/users/${feeder:userId}", null, run(), new SecretResolver(), errors);
        assertTrue(errors.isEmpty(), "feeder placeholders must not be treated as errors: " + errors);
        assertEquals("/users/${feeder:userId}", result);
    }

    @Test
    void expand_passesSessionPlaceholderThroughUntouched() {
        List<String> errors = new ArrayList<>();
        String result = PlaceholderSubstitutor.expand(
            "/orders/${session:orderId}", null, run(), new SecretResolver(), errors);
        assertTrue(errors.isEmpty(), "session placeholders must not be treated as errors: " + errors);
        assertEquals("/orders/${session:orderId}", result);
    }

    @Test
    void expand_mixesRunAndFeederPlaceholders() {
        String result = PlaceholderSubstitutor.expand(
            "/runs/${run:id}/users/${feeder:userId}", null, run(), new SecretResolver());
        assertEquals("/runs/test-run-id/users/${feeder:userId}", result);
    }

    @Test
    void expand_throwsOnGenuinelyUnknownKind() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> PlaceholderSubstitutor.expand("${bogus:x}", null, run(), new SecretResolver()));
        assertTrue(ex.getMessage().contains("bogus"));
    }

    @Test
    void expandInPlace_leavesFeederPlaceholderForRewriter() {
        ScenarioConfig sc = new ScenarioConfig();
        sc.setId("test");
        RequestConfig req = new RequestConfig();
        req.setPath("/users/${feeder:userId}");
        sc.setRequest(req);

        List<String> errors = PlaceholderSubstitutor.expandInPlace(null, run(), List.of(sc), new SecretResolver());

        assertTrue(errors.isEmpty(), "expected no errors: " + errors);
        assertEquals("/users/${feeder:userId}", sc.getRequest().getPath());
    }

    @Test
    void expand_escapedPlaceholderPassesThrough() {
        String result = PlaceholderSubstitutor.expand("literal=\\${run:id}", null, run(), new SecretResolver());
        assertEquals("literal=${run:id}", result);
    }

    @Test
    void expand_handlesMultiplePlaceholders() {
        String result = PlaceholderSubstitutor.expand("id=${run:id} name=${run:name}",
            null, run(), new SecretResolver());
        assertEquals("id=test-run-id name=Test Run", result);
    }

    @Test
    void expand_passesThroughTextWithoutPlaceholders() {
        assertEquals("plain text", PlaceholderSubstitutor.expand("plain text", null, run(), new SecretResolver()));
    }

    @Test
    void expand_returnsNullForNullInput() {
        assertNull(PlaceholderSubstitutor.expand(null, null, run(), new SecretResolver()));
    }

    @Test
    void expandInPlace_expandsBaseUrlsValues() {
        System.setProperty("rampage.test.host", "api.example.com");
        try {
            EnvironmentConfig env = new EnvironmentConfig();
            Map<String, String> urls = new LinkedHashMap<>();
            urls.put("rest", "https://${sys:rampage.test.host}/v1");
            urls.put("graphql", "https://${sys:rampage.test.host}/graphql");
            env.setBaseUrls(urls);

            List<String> errors = PlaceholderSubstitutor.expandInPlace(env, run(), null, new SecretResolver());

            assertTrue(errors.isEmpty(), "expected no errors: " + errors);
            assertEquals("https://api.example.com/v1", env.getBaseUrls().get("rest"));
            assertEquals("https://api.example.com/graphql", env.getBaseUrls().get("graphql"));
        } finally {
            System.clearProperty("rampage.test.host");
        }
    }

    @Test
    void expandInPlace_expandsDatabaseJdbcUrl() {
        System.setProperty("rampage.test.dbhost", "db.internal");
        try {
            EnvironmentConfig env = new EnvironmentConfig();
            DatabaseConfig db = new DatabaseConfig();
            db.setJdbcUrl("jdbc:postgresql://${sys:rampage.test.dbhost}:5432/perf");
            db.setDriverClassName("org.postgresql.Driver");
            env.setDatabases(Map.of("sourceData", db));

            List<String> errors = PlaceholderSubstitutor.expandInPlace(env, run(), null, new SecretResolver());

            assertTrue(errors.isEmpty(), "expected no errors: " + errors);
            assertEquals("jdbc:postgresql://db.internal:5432/perf",
                env.getDatabases().get("sourceData").getJdbcUrl());
        } finally {
            System.clearProperty("rampage.test.dbhost");
        }
    }

    @Test
    void expandInPlace_expandsScenarioRequestFields() {
        ScenarioConfig sc = new ScenarioConfig();
        sc.setId("test");
        RequestConfig req = new RequestConfig();
        req.setPath("/users/${run:id}");
        req.setBody("{\"name\":\"${run:name}\"}");
        sc.setRequest(req);

        List<String> errors = PlaceholderSubstitutor.expandInPlace(null, run(), List.of(sc), new SecretResolver());

        assertTrue(errors.isEmpty(), "expected no errors: " + errors);
        assertEquals("/users/test-run-id", sc.getRequest().getPath());
        assertEquals("{\"name\":\"Test Run\"}", sc.getRequest().getBody());
    }

    @Test
    void expandInPlace_expandsScenarioFeederSqlFile() {
        ScenarioConfig sc = new ScenarioConfig();
        sc.setId("test");
        FeederConfig feeder = new FeederConfig();
        feeder.setSqlFile("config/queries/${run:environment}.sql");
        sc.setFeeder(feeder);

        List<String> errors = PlaceholderSubstitutor.expandInPlace(null, run(), List.of(sc), new SecretResolver());

        assertTrue(errors.isEmpty(), "expected no errors: " + errors);
        assertEquals("config/queries/perf.sql", sc.getFeeder().getSqlFile());
    }

    @Test
    void expandInPlace_expandsStepRequestPath() {
        ScenarioConfig sc = new ScenarioConfig();
        sc.setId("test");
        StepConfig step = new StepConfig();
        step.setName("call");
        RequestConfig req = new RequestConfig();
        req.setPath("/runs/${run:id}/results");
        step.setRequest(req);
        sc.setSteps(List.of(step));

        List<String> errors = PlaceholderSubstitutor.expandInPlace(null, run(), List.of(sc), new SecretResolver());

        assertTrue(errors.isEmpty(), "expected no errors: " + errors);
        assertEquals("/runs/test-run-id/results", sc.getSteps().get(0).getRequest().getPath());
    }

    @Test
    void expandInPlace_expandsRunMetadataDescription() {
        RunConfig r = run();
        MetadataConfig md = new MetadataConfig();
        md.setOwner("perf-team");
        md.setDescription("Run for ${run:environment}");
        r.setMetadata(md);

        List<String> errors = PlaceholderSubstitutor.expandInPlace(null, r, null, new SecretResolver());

        assertTrue(errors.isEmpty(), "expected no errors: " + errors);
        assertEquals("Run for perf", r.getMetadata().getDescription());
    }

    @Test
    void expandInPlace_reportsErrorsForUnresolvedPlaceholders() {
        EnvironmentConfig env = new EnvironmentConfig();
        env.setBaseUrls(Map.of("rest", "https://${env:RAMPAGE_TEST_TOTALLY_UNSET_VAR_QQ}/api"));

        List<String> errors = PlaceholderSubstitutor.expandInPlace(env, run(), null, new SecretResolver());

        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("RAMPAGE_TEST_TOTALLY_UNSET_VAR_QQ")));
    }
}
