package io.rampage.factory;

import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.rampage.config.model.EnvironmentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static io.gatling.javaapi.http.HttpDsl.http;

/**
 * Builds a Gatling {@code HttpProtocolBuilder} from an {@code EnvironmentConfig}.
 *
 * <p>Base-URL resolution follows a fallback chain when no explicit {@code endpointRef} is
 * supplied: the key {@code "rest"} is tried first, then the first configured URL, and
 * finally {@code http://localhost:8080} if no URLs are defined at all.
 *
 * <p>When the environment security mode is {@code bearer-token} or
 * {@code oauth-client-credentials}, an {@code Authorization: Bearer} header is added using
 * the Gatling session attribute {@code authToken}. Observability correlation-ID headers and
 * any additional security headers declared in the environment are also applied.
 */
public class HttpProtocolFactory {
    private static final Logger log = LoggerFactory.getLogger(HttpProtocolFactory.class);

    /**
     * Creates a new {@code HttpProtocolFactory}.
     */
    public HttpProtocolFactory() {}

    /**
     * Builds an {@code HttpProtocolBuilder} using the default endpoint (no explicit
     * {@code endpointRef}).
     *
     * @param env            the environment configuration; must not be {@code null}
     * @param secretResolver the resolver used to expand token references
     * @return a configured {@code HttpProtocolBuilder}
     */
    public HttpProtocolBuilder build(EnvironmentConfig env, SecretResolver secretResolver) {
        return build(env, secretResolver, null);
    }

    /**
     * Resolves the base URL for the given endpoint reference.
     *
     * <p>When {@code endpointRef} is non-blank it must be a key in {@code env.baseUrls};
     * an {@code IllegalArgumentException} is thrown if it is not. When {@code endpointRef}
     * is {@code null} or blank the fallback chain is applied: {@code "rest"} key → first
     * configured URL → {@code http://localhost:8080}.
     *
     * @param env         the environment configuration; may be {@code null}
     * @param endpointRef the named endpoint to look up; {@code null} or blank triggers
     *                    the fallback chain
     * @return the resolved base URL string; never {@code null}
     * @throws IllegalArgumentException if {@code endpointRef} is non-blank but not present
     *                                  in {@code env.baseUrls}
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

    /**
     * Builds an {@code HttpProtocolBuilder} targeting the named endpoint.
     *
     * <p>HTTP defaults (accept header, content-type, redirect behaviour), security headers,
     * and observability headers from the environment configuration are all applied to the
     * builder. A {@code keep-alive} connection header is always added.
     *
     * @param env            the environment configuration; must not be {@code null}
     * @param secretResolver the resolver used to expand token references
     * @param endpointRef    the named endpoint reference to use as the base URL; {@code null}
     *                       or blank triggers the fallback chain in
     *                       {@link #resolveBaseUrl(EnvironmentConfig, String)}
     * @return a fully configured {@code HttpProtocolBuilder}
     */
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
