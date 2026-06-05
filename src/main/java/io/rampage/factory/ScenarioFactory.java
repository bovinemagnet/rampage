package io.rampage.factory;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.CheckBuilder;
import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.rampage.config.model.ChecksConfig;
import io.rampage.config.model.EnvironmentConfig;
import io.rampage.config.model.HttpConfig;
import io.rampage.config.model.RequestConfig;
import io.rampage.config.model.ScenarioConfig;
import io.rampage.config.model.StepConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public class ScenarioFactory {
    private static final Logger log = LoggerFactory.getLogger(ScenarioFactory.class);

    private final Supplier<String> correlationIdSupplier;
    private final Supplier<String> authTokenSupplier;
    private final Function<String, String> resourceLoader;

    public ScenarioFactory() {
        this(() -> UUID.randomUUID().toString(), () -> null, path -> "");
    }

    public ScenarioFactory(Supplier<String> correlationIdSupplier) {
        this(correlationIdSupplier, () -> null, path -> "");
    }

    public ScenarioFactory(Supplier<String> correlationIdSupplier, Supplier<String> authTokenSupplier) {
        this(correlationIdSupplier, authTokenSupplier, path -> "");
    }

    public ScenarioFactory(Supplier<String> correlationIdSupplier,
                           Supplier<String> authTokenSupplier,
                           Function<String, String> resourceLoader) {
        this.correlationIdSupplier = correlationIdSupplier;
        this.authTokenSupplier = authTokenSupplier != null ? authTokenSupplier : () -> null;
        this.resourceLoader = resourceLoader != null ? resourceLoader : path -> "";
    }

    public ScenarioBuilder build(ScenarioConfig scenarioCfg, String graphqlQuery) {
        return build(scenarioCfg, graphqlQuery, null, null, null);
    }

    public ScenarioBuilder build(ScenarioConfig scenarioCfg, String graphqlQuery, HttpConfig httpConfig) {
        return build(scenarioCfg, graphqlQuery, httpConfig, null, null);
    }

    public ScenarioBuilder build(ScenarioConfig scenarioCfg, String graphqlQuery,
                                 HttpConfig httpConfig, Map<String, String> effectiveHeaders) {
        return build(scenarioCfg, graphqlQuery, httpConfig, effectiveHeaders, null, null);
    }

    public ScenarioBuilder build(ScenarioConfig scenarioCfg, String graphqlQuery,
                                 HttpConfig httpConfig, Map<String, String> effectiveHeaders,
                                 EnvironmentConfig env) {
        return build(scenarioCfg, graphqlQuery, httpConfig, effectiveHeaders, env, null);
    }

    /**
     * Builds the scenario, attaching {@code feeder} (a Gatling {@code FeederBuilder} or a
     * record {@code Iterator}) <em>before</em> the request steps. The feed must precede the
     * request: otherwise the request body's {@code #{...}} Gatling EL placeholders are
     * resolved before any feeder row is polled, and Gatling fails the request build with
     * "No attribute named 'X' is defined". A {@code null} feeder leaves the chain unfed.
     */
    public ScenarioBuilder build(ScenarioConfig scenarioCfg, String graphqlQuery,
                                 HttpConfig httpConfig, Map<String, String> effectiveHeaders,
                                 EnvironmentConfig env, Object feeder) {
        log.info("Building scenario: {}", scenarioCfg.getName());

        List<StepConfig> steps = resolveSteps(scenarioCfg);

        Supplier<String> idSupplier = correlationIdSupplier;
        Supplier<String> tokenSupplier = authTokenSupplier;
        ChainBuilder sessionPrep = CoreDsl.exec(session -> {
            var s = session.set("correlationId", idSupplier.get());
            String token = tokenSupplier.get();
            return token != null ? s.set("authToken", token) : s.set("authToken", "");
        });

        ChainBuilder body = applyFeeder(sessionPrep, feeder);
        java.util.Map<String, String> queryCache = new java.util.HashMap<>();
        for (StepConfig step : steps) {
            String inlineBody = loadStepBody(step);
            String stepQuery = resolveStepGraphqlQuery(step, graphqlQuery, queryCache);
            body = body.exec(StepBuilder.build(scenarioCfg, step, stepQuery, inlineBody,
                httpConfig, effectiveHeaders, env));
        }
        return CoreDsl.scenario(scenarioCfg.getName()).exec(body);
    }

    @SuppressWarnings("unchecked")
    private ChainBuilder applyFeeder(ChainBuilder body, Object feeder) {
        if (feeder == null) {
            return body;
        }
        if (feeder instanceof io.gatling.javaapi.core.FeederBuilder<?> feederBuilder) {
            return body.feed(feederBuilder);
        }
        if (feeder instanceof java.util.Iterator) {
            return body.feed((java.util.Iterator<Map<String, Object>>) feeder);
        }
        log.warn("Ignoring unsupported feeder type: {}", feeder.getClass().getName());
        return body;
    }

    private String resolveStepGraphqlQuery(StepConfig step, String scenarioQuery,
                                           java.util.Map<String, String> cache) {
        RequestConfig req = step.getRequest();
        if (req == null || req.getGraphqlQueryFile() == null || req.getGraphqlQueryFile().isBlank()) {
            return scenarioQuery;
        }
        String file = req.getGraphqlQueryFile();
        return cache.computeIfAbsent(file, path -> {
            try {
                return resourceLoader.apply(path);
            } catch (Exception e) {
                log.warn("Failed to load graphqlQueryFile '{}' for step '{}': {}",
                    path, step.getName(), e.getMessage());
                return scenarioQuery;
            }
        });
    }

    private List<StepConfig> resolveSteps(ScenarioConfig scenarioCfg) {
        if (scenarioCfg.getSteps() != null && !scenarioCfg.getSteps().isEmpty()) {
            return scenarioCfg.getSteps();
        }
        StepConfig synthesised = new StepConfig();
        synthesised.setName(scenarioCfg.getName());
        synthesised.setEndpointRef(scenarioCfg.getEndpointRef());
        synthesised.setRequest(scenarioCfg.getRequest());
        synthesised.setChecks(scenarioCfg.getChecks());
        List<StepConfig> single = new ArrayList<>();
        single.add(synthesised);
        return single;
    }

    private String loadStepBody(StepConfig step) {
        RequestConfig req = step.getRequest();
        if (req == null || req.getBodyFile() == null || req.getBodyFile().isBlank()) return null;
        try {
            return resourceLoader.apply(req.getBodyFile());
        } catch (Exception e) {
            log.warn("Failed to load bodyFile '{}' for step '{}': {}",
                req.getBodyFile(), step.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * Builds a GraphQL JSON envelope body. Retained as a static helper for tests and
     * backward compatibility — production code calls into {@link RequestBuilder}.
     */
    public static String buildRequestBody(ScenarioConfig scenarioCfg, String graphqlQuery) {
        return RequestBuilder.buildGraphqlBody(scenarioCfg, graphqlQuery);
    }

    /**
     * Backward-compatible alias for {@link PlaceholderRewriter#rewriteVariableMap(Map)}.
     */
    public static Map<String, Object> rewriteFeederPlaceholders(Map<String, Object> variables) {
        return PlaceholderRewriter.rewriteVariableMap(variables);
    }

    /**
     * Backward-compatible helper that builds a list of {@link CheckBuilder}s from a
     * {@link ChecksConfig}. Production code calls {@link CheckFactory#build} directly.
     */
    public static List<CheckBuilder> buildChecks(ChecksConfig checksConfig) {
        return CheckFactory.build(checksConfig, null);
    }
}
