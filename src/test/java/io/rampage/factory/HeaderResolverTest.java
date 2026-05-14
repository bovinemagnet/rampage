package io.rampage.factory;

import io.rampage.config.model.EnvironmentConfig;
import io.rampage.config.model.ObservabilityConfig;
import io.rampage.config.model.RunConfig;
import io.rampage.config.model.ScenarioConfig;
import io.rampage.config.model.ScenarioSecurityConfig;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HeaderResolverTest {

    private EnvironmentConfig envWithCorrelationHeader() {
        EnvironmentConfig env = new EnvironmentConfig();
        ObservabilityConfig obs = new ObservabilityConfig();
        obs.setCorrelationIdHeader("X-Correlation-Id");
        env.setObservability(obs);
        return env;
    }

    @Test
    void protectedHeaders_alwaysIncludesAuthorization() {
        assertTrue(HeaderResolver.protectedHeaders(null).contains("Authorization"));
    }

    @Test
    void protectedHeaders_includesCorrelationIdHeaderFromEnv() {
        EnvironmentConfig env = envWithCorrelationHeader();
        assertTrue(HeaderResolver.protectedHeaders(env).contains("X-Correlation-Id"));
    }

    @Test
    void validateOverrides_rejectsScenarioOverridingAuthorization() {
        EnvironmentConfig env = envWithCorrelationHeader();
        ScenarioConfig sc = new ScenarioConfig();
        sc.setId("rogue");
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer evil");
        sc.setHeaders(headers);

        List<String> errors = HeaderResolver.validateOverrides(env, sc);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Authorization"));
        assertTrue(errors.get(0).contains("rogue"));
    }

    @Test
    void validateOverrides_caseInsensitive() {
        EnvironmentConfig env = envWithCorrelationHeader();
        ScenarioConfig sc = new ScenarioConfig();
        sc.setId("rogue");
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("authorization", "Bearer evil");
        sc.setHeaders(headers);

        List<String> errors = HeaderResolver.validateOverrides(env, sc);
        assertEquals(1, errors.size());
    }

    @Test
    void validateOverrides_allowsWhenScenarioOptsIn() {
        EnvironmentConfig env = envWithCorrelationHeader();
        ScenarioConfig sc = new ScenarioConfig();
        sc.setId("authorised-override");
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer custom");
        sc.setHeaders(headers);
        ScenarioSecurityConfig sec = new ScenarioSecurityConfig();
        sec.setAllowAuthOverride(true);
        sc.setSecurity(sec);

        List<String> errors = HeaderResolver.validateOverrides(env, sc);
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateOverrides_rejectsCorrelationHeaderOverride() {
        EnvironmentConfig env = envWithCorrelationHeader();
        ScenarioConfig sc = new ScenarioConfig();
        sc.setId("rogue");
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Correlation-Id", "fixed-1");
        sc.setHeaders(headers);

        List<String> errors = HeaderResolver.validateOverrides(env, sc);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("X-Correlation-Id"));
    }

    @Test
    void validateOverrides_passesForBenignHeaders() {
        EnvironmentConfig env = envWithCorrelationHeader();
        ScenarioConfig sc = new ScenarioConfig();
        sc.setId("ok");
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Business-Process", "checkout");
        sc.setHeaders(headers);

        assertTrue(HeaderResolver.validateOverrides(env, sc).isEmpty());
    }

    @Test
    void resolveScenarioHeaders_layersRunOverScenarioInRunFirstOrder() {
        EnvironmentConfig env = envWithCorrelationHeader();
        RunConfig run = new RunConfig();
        run.setHeaders(Map.of("X-Run", "from-run", "X-Override", "from-run"));

        ScenarioConfig sc = new ScenarioConfig();
        sc.setHeaders(Map.of("X-Scenario", "from-scenario", "X-Override", "from-scenario"));

        Map<String, String> resolved = HeaderResolver.resolveScenarioHeaders(env, run, sc);

        assertEquals("from-run", resolved.get("X-Run"));
        assertEquals("from-scenario", resolved.get("X-Scenario"));
        // Scenario should override run for non-protected headers (run → scenario layering).
        assertEquals("from-scenario", resolved.get("X-Override"));
    }

    @Test
    void resolveScenarioHeaders_handlesNullInputs() {
        Map<String, String> resolved = HeaderResolver.resolveScenarioHeaders(null, null, null);
        assertNotNull(resolved);
        assertTrue(resolved.isEmpty());
    }
}
