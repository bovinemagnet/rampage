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

    public HttpProtocolBuilder build(EnvironmentConfig env, SecretResolver secretResolver, String endpointRef) {
        String baseUrl = null;
        if (env.getBaseUrls() != null && !env.getBaseUrls().isEmpty()) {
            if (endpointRef != null && env.getBaseUrls().containsKey(endpointRef)) {
                baseUrl = env.getBaseUrls().get(endpointRef);
            } else if (env.getBaseUrls().containsKey("rest")) {
                baseUrl = env.getBaseUrls().get("rest");
            } else {
                baseUrl = env.getBaseUrls().values().iterator().next();
            }
        }
        if (baseUrl == null) {
            baseUrl = "http://localhost:8080";
        }

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
