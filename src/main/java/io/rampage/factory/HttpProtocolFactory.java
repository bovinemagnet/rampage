package io.rampage.factory;

import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.rampage.config.model.EnvironmentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static io.gatling.javaapi.http.HttpDsl.http;

public class HttpProtocolFactory {
    private static final Logger log = LoggerFactory.getLogger(HttpProtocolFactory.class);

    public HttpProtocolBuilder build(EnvironmentConfig env, SecretResolver secretResolver) {
        return build(env, secretResolver, null);
    }

    /**
     * Resolves the base URL for the given endpointRef.
     * Throws IllegalArgumentException when endpointRef is non-blank but missing from env.baseUrls.
     * Null/blank endpointRef triggers the fallback chain: "rest" → first configured URL → localhost:8080.
     */
    public String resolveBaseUrl(EnvironmentConfig env, String endpointRef) {
        Map<String, String> baseUrls = env != null ? env.getBaseUrls() : null;
        boolean refIsExplicit = endpointRef != null && !endpointRef.isBlank();

        if (refIsExplicit) {
            if (baseUrls == null || !baseUrls.containsKey(endpointRef)) {
                String available = baseUrls == null ? "(none)" : baseUrls.keySet().toString();
                throw new IllegalArgumentException(
                    "Unknown endpointRef '" + endpointRef + "' — must be one of " + available);
            }
            return baseUrls.get(endpointRef);
        }

        if (baseUrls != null && !baseUrls.isEmpty()) {
            if (baseUrls.containsKey("rest")) {
                return baseUrls.get("rest");
            }
            return baseUrls.values().iterator().next();
        }
        return "http://localhost:8080";
    }

    public HttpProtocolBuilder build(EnvironmentConfig env, SecretResolver secretResolver, String endpointRef) {
        String baseUrl = resolveBaseUrl(env, endpointRef);
        log.info("Building HTTP protocol for base URL: {}", baseUrl);
        HttpProtocolBuilder builder = http.baseUrl(baseUrl);

        if (env.getHttp() != null) {
            if (env.getHttp().getAcceptHeader() != null) {
                builder = builder.acceptHeader(env.getHttp().getAcceptHeader());
            }
            if (env.getHttp().getContentTypeHeader() != null) {
                builder = builder.contentTypeHeader(env.getHttp().getContentTypeHeader());
            }
            if (!env.getHttp().isFollowRedirects()) {
                builder = builder.disableFollowRedirect();
            }
        }

        if (env.getSecurity() != null) {
            String mode = env.getSecurity().getMode();
            if ("bearer-token".equalsIgnoreCase(mode) || "oauth-client-credentials".equalsIgnoreCase(mode)) {
                // The Authorization header value is sourced from session attribute 'authToken',
                // populated by ScenarioFactory before each request. Static tokens read the
                // same value every time; OAuth providers may refresh in the background.
                builder = builder.header("Authorization", "Bearer #{authToken}");
                log.debug("Configured session-based Authorization header for mode={}", mode);
            }
            if (env.getSecurity().getHeaders() != null) {
                for (Map.Entry<String, String> entry : env.getSecurity().getHeaders().entrySet()) {
                    builder = builder.header(entry.getKey(), entry.getValue());
                }
            }
        }

        if (env.getObservability() != null && env.getObservability().getCorrelationIdHeader() != null) {
            builder = builder.header(env.getObservability().getCorrelationIdHeader(), "#{correlationId}");
        }

        builder = builder.connectionHeader("keep-alive");

        return builder;
    }
}
