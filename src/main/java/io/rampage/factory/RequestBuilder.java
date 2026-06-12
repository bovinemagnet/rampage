package io.rampage.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.CheckBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import io.rampage.config.model.EnvironmentConfig;
import io.rampage.config.model.HttpConfig;
import io.rampage.config.model.RequestConfig;
import io.rampage.config.model.ScenarioConfig;
import io.rampage.config.model.StepConfig;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.http.HttpDsl.http;

/**
 * Builds a single Gatling {@link HttpRequestActionBuilder} from a {@link StepConfig}.
 *
 * <p>Owns:
 * <ul>
 *   <li>HTTP verb dispatch (GET/POST/PUT/PATCH/DELETE/HEAD)</li>
 *   <li>Path templating via {@link PlaceholderRewriter}</li>
 *   <li>Body type strategies: {@code graphql} (legacy default), {@code json}, {@code form},
 *       {@code text}, {@code none}</li>
 *   <li>Query parameter and form parameter expansion</li>
 *   <li>Header layering</li>
 *   <li>Per-scenario request timeout from {@link HttpConfig}</li>
 *   <li>Attaching {@link CheckBuilder}s</li>
 * </ul>
 *
 * <p>Backward compatibility: when {@link RequestConfig#getMethod()} is null and a
 * {@code graphqlQueryFile} is configured, the request defaults to a POST to the
 * scenario's endpoint with a GraphQL envelope body — the original behaviour.
 */
public final class RequestBuilder {

    private static final ObjectMapper BODY_MAPPER = new ObjectMapper();

    private RequestBuilder() {}

    /**
     * Builds a Gatling {@code HttpRequestActionBuilder} from the supplied step and
     * context objects.
     *
     * <p>The HTTP verb, path, body type, headers, query parameters, request timeout,
     * and checks are all derived from the step and scenario configuration. The
     * {@code graphqlQuery} and {@code inlineBodyFromFile} values are pre-loaded by
     * the caller to avoid repeated I/O.
     *
     * @param scenarioCfg        the scenario configuration providing defaults such
     *                           as endpoint reference and operation name
     * @param step               the step configuration describing this individual
     *                           request
     * @param graphqlQuery       the pre-loaded GraphQL query string; may be
     *                           {@code null} for non-GraphQL requests
     * @param inlineBodyFromFile the pre-loaded body file content referenced by
     *                           {@code request.bodyFile}; may be {@code null}
     * @param httpConfig         HTTP-level settings such as per-request timeout;
     *                           may be {@code null}
     * @param effectiveHeaders   the merged set of headers to attach to the request;
     *                           may be {@code null}
     * @param checks             the list of Gatling checks to attach; may be
     *                           {@code null} or empty
     * @param env                the environment configuration used to resolve
     *                           absolute URLs for step-level endpoint overrides;
     *                           may be {@code null}
     * @return the configured {@code HttpRequestActionBuilder}
     */
    public static HttpRequestActionBuilder build(ScenarioConfig scenarioCfg,
                                                  StepConfig step,
                                                  String graphqlQuery,
                                                  String inlineBodyFromFile,
                                                  HttpConfig httpConfig,
                                                  Map<String, String> effectiveHeaders,
                                                  List<CheckBuilder> checks,
                                                  EnvironmentConfig env) {
        RequestConfig request = step.getRequest();
        String method = resolveMethod(request);
        String path = resolvePath(scenarioCfg, step, request, env);

        HttpRequestActionBuilder rb = createRequest(method, path, requestName(scenarioCfg, step));

        if (httpConfig != null && httpConfig.getRequestTimeoutMillis() > 0) {
            rb = rb.requestTimeout(Duration.ofMillis(httpConfig.getRequestTimeoutMillis()));
        }

        if (effectiveHeaders != null) {
            for (Map.Entry<String, String> e : effectiveHeaders.entrySet()) {
                rb = rb.header(e.getKey(), PlaceholderRewriter.rewriteString(e.getValue()));
            }
        }

        Map<String, String> queryParams = request != null ? request.getQueryParams() : null;
        if (queryParams != null) {
            for (Map.Entry<String, String> q : queryParams.entrySet()) {
                rb = rb.queryParam(q.getKey(), PlaceholderRewriter.rewriteString(q.getValue()));
            }
        }

        BodyDecision body = decideBody(request, graphqlQuery, inlineBodyFromFile, scenarioCfg);
        if (body.contentType != null) {
            rb = rb.header("Content-Type", body.contentType);
        }
        if (body.formParams != null) {
            for (Map.Entry<String, String> f : body.formParams.entrySet()) {
                rb = rb.formParam(f.getKey(), PlaceholderRewriter.rewriteString(f.getValue()));
            }
        } else if (body.bodyExpression != null) {
            rb = rb.body(StringBody(body.bodyExpression));
        }

        if (checks != null && !checks.isEmpty()) {
            rb = rb.check(checks.toArray(new CheckBuilder[0]));
        }
        return rb;
    }

    private static String requestName(ScenarioConfig scenarioCfg, StepConfig step) {
        if (step.getName() != null && !step.getName().isBlank()) return step.getName();
        return scenarioCfg.getName() != null ? scenarioCfg.getName() : scenarioCfg.getId();
    }

    private static String resolveMethod(RequestConfig request) {
        if (request != null && request.getMethod() != null && !request.getMethod().isBlank()) {
            return request.getMethod().toUpperCase(Locale.ROOT);
        }
        // Legacy default: GraphQL is always POST.
        return "POST";
    }

    /**
     * Resolves the final request path for a step.
     *
     * <p>When the step declares an {@code endpointRef} that differs from the scenario's,
     * the returned path is an absolute URL prefixed with that endpoint's base URL — Gatling
     * treats absolute URLs as targets independent of the scenario's HttpProtocol baseUrl.
     * In all other cases the path is relative and resolves against the scenario protocol.
     * Paths that already start with {@code http://} or {@code https://} are returned untouched.
     */
    static String resolvePath(ScenarioConfig scenarioCfg, StepConfig step, RequestConfig request,
                              EnvironmentConfig env) {
        String raw;
        if (request != null && request.getPath() != null && !request.getPath().isBlank()) {
            raw = request.getPath();
        } else {
            String endpointRef = step.getEndpointRef() != null ? step.getEndpointRef()
                : (scenarioCfg.getEndpointRef() != null ? scenarioCfg.getEndpointRef() : "graphql");
            raw = "/" + endpointRef;
        }

        String resolved = PlaceholderRewriter.rewriteString(raw);
        if (resolved.startsWith("http://") || resolved.startsWith("https://")) {
            return resolved;
        }

        String stepRef = step.getEndpointRef();
        String scenarioRef = scenarioCfg.getEndpointRef();
        boolean stepOverridesEndpoint = stepRef != null && !stepRef.isBlank()
            && !stepRef.equals(scenarioRef);
        if (!stepOverridesEndpoint) return resolved;

        Map<String, String> baseUrls = env != null ? env.getBaseUrls() : null;
        if (baseUrls == null || !baseUrls.containsKey(stepRef)) return resolved;

        String baseUrl = baseUrls.get(stepRef);
        if (baseUrl == null || baseUrl.isBlank()) return resolved;
        if (baseUrl.endsWith("/") && resolved.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + resolved;
        }
        if (!baseUrl.endsWith("/") && !resolved.startsWith("/")) {
            return baseUrl + "/" + resolved;
        }
        return baseUrl + resolved;
    }

    private static HttpRequestActionBuilder createRequest(String method, String path, String name) {
        return switch (method) {
            case "GET" -> http(name).get(path);
            case "POST" -> http(name).post(path);
            case "PUT" -> http(name).put(path);
            case "PATCH" -> http(name).patch(path);
            case "DELETE" -> http(name).delete(path);
            case "HEAD" -> http(name).head(path);
            case "OPTIONS" -> http(name).options(path);
            default -> http(name).httpRequest(method, path);
        };
    }

    private static BodyDecision decideBody(RequestConfig request, String graphqlQuery,
                                            String inlineBodyFromFile, ScenarioConfig scenarioCfg) {
        String bodyType = inferBodyType(request);
        return switch (bodyType) {
            case "none" -> BodyDecision.none();
            case "form" -> BodyDecision.form(request != null ? request.getFormParams() : null);
            case "json" -> {
                String raw = pickRawBody(request, inlineBodyFromFile);
                yield BodyDecision.json(PlaceholderRewriter.rewriteString(raw));
            }
            case "text" -> {
                String raw = pickRawBody(request, inlineBodyFromFile);
                yield BodyDecision.text(PlaceholderRewriter.rewriteString(raw));
            }
            default -> BodyDecision.json(buildGraphqlBody(scenarioCfg, request, graphqlQuery));
        };
    }

    private static String inferBodyType(RequestConfig request) {
        if (request == null) return "graphql";
        if (request.getBodyType() != null && !request.getBodyType().isBlank()) {
            return request.getBodyType().toLowerCase(Locale.ROOT);
        }
        if (request.getGraphqlQueryFile() != null) return "graphql";
        if (request.getFormParams() != null) return "form";
        if (request.getBody() != null || request.getBodyFile() != null) return "json";
        return "none";
    }

    private static String pickRawBody(RequestConfig request, String inlineBodyFromFile) {
        if (request == null) return "";
        if (request.getBody() != null) return request.getBody();
        if (inlineBodyFromFile != null) return inlineBodyFromFile;
        return "";
    }

    static String buildGraphqlBody(ScenarioConfig scenarioCfg, String graphqlQuery) {
        return buildGraphqlBody(scenarioCfg, null, graphqlQuery);
    }

    /**
     * Builds the JSON envelope for a GraphQL request. When {@code stepRequest} is non-null and
     * declares its own {@code variables}, those take precedence over the scenario-level request
     * variables; otherwise the scenario-level variables are used as a fallback.
     */
    static String buildGraphqlBody(ScenarioConfig scenarioCfg, RequestConfig stepRequest, String graphqlQuery) {
        Map<String, Object> variables = null;
        if (stepRequest != null && stepRequest.getVariables() != null) {
            variables = stepRequest.getVariables();
        } else if (scenarioCfg.getRequest() != null) {
            variables = scenarioCfg.getRequest().getVariables();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", graphqlQuery != null ? graphqlQuery : "");
        body.put("variables", PlaceholderRewriter.rewriteVariableMap(variables));
        if (scenarioCfg.getOperationName() != null && !scenarioCfg.getOperationName().isBlank()) {
            body.put("operationName", scenarioCfg.getOperationName());
        }
        try {
            return BODY_MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to build GraphQL body for scenario '"
                + scenarioCfg.getName() + "'", e);
        }
    }

    private static final class BodyDecision {
        final String bodyExpression;
        final String contentType;
        final Map<String, String> formParams;

        private BodyDecision(String bodyExpression, String contentType, Map<String, String> formParams) {
            this.bodyExpression = bodyExpression;
            this.contentType = contentType;
            this.formParams = formParams;
        }

        static BodyDecision none() { return new BodyDecision(null, null, null); }
        static BodyDecision json(String body) { return new BodyDecision(body, "application/json", null); }
        static BodyDecision text(String body) { return new BodyDecision(body, "text/plain", null); }
        static BodyDecision form(Map<String, String> params) {
            return new BodyDecision(null, "application/x-www-form-urlencoded", params);
        }
    }
}
