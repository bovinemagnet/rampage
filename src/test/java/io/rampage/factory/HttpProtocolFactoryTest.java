package io.rampage.factory;

import io.rampage.config.model.EnvironmentConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HttpProtocolFactoryTest {
    private HttpProtocolFactory factory;

    @BeforeEach
    void setUp() {
        factory = new HttpProtocolFactory();
    }

    private EnvironmentConfig envWithBaseUrls(Map<String, String> baseUrls) {
        EnvironmentConfig env = new EnvironmentConfig();
        env.setId("test");
        env.setName("Test");
        env.setBaseUrls(baseUrls);
        return env;
    }

    @Test
    void resolveBaseUrl_throwsWhenEndpointRefNotInBaseUrls() {
        EnvironmentConfig env = envWithBaseUrls(Map.of("rest", "http://localhost:8080"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> factory.resolveBaseUrl(env, "graphql"));
        assertTrue(ex.getMessage().contains("graphql"),
            "Expected error to mention the unknown endpointRef: " + ex.getMessage());
    }

    @Test
    void resolveBaseUrl_returnsMatchingBaseUrl() {
        Map<String, String> urls = new LinkedHashMap<>();
        urls.put("rest", "http://localhost:8080");
        urls.put("graphql", "http://localhost:9090/graphql");
        EnvironmentConfig env = envWithBaseUrls(urls);

        assertEquals("http://localhost:9090/graphql",
            factory.resolveBaseUrl(env, "graphql"));
    }

    @Test
    void resolveBaseUrl_fallsBackToRestKeyWhenEndpointRefNull() {
        Map<String, String> urls = new LinkedHashMap<>();
        urls.put("rest", "http://localhost:8080");
        urls.put("graphql", "http://localhost:9090/graphql");
        EnvironmentConfig env = envWithBaseUrls(urls);

        assertEquals("http://localhost:8080", factory.resolveBaseUrl(env, null));
    }

    @Test
    void resolveBaseUrl_fallsBackToFirstBaseUrlWhenNoRestKey() {
        Map<String, String> urls = new LinkedHashMap<>();
        urls.put("only", "http://localhost:8081");
        EnvironmentConfig env = envWithBaseUrls(urls);

        assertEquals("http://localhost:8081", factory.resolveBaseUrl(env, null));
    }

    @Test
    void resolveBaseUrl_fallsBackToLocalhostWhenBaseUrlsEmpty() {
        EnvironmentConfig env = envWithBaseUrls(Map.of());
        assertEquals("http://localhost:8080", factory.resolveBaseUrl(env, null));
    }

    @Test
    void resolveBaseUrl_treatsBlankRefAsNull() {
        Map<String, String> urls = new LinkedHashMap<>();
        urls.put("rest", "http://localhost:8080");
        EnvironmentConfig env = envWithBaseUrls(urls);

        assertEquals("http://localhost:8080", factory.resolveBaseUrl(env, "   "));
    }
}
