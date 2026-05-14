package io.rampage.factory;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.CheckBuilder;
import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.rampage.config.model.ChecksConfig;
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
        return build(scenarioCfg, graphqlQuery, null, null);
    }

    public ScenarioBuilder build(ScenarioConfig scenarioCfg, String graphqlQuery, HttpConfig httpConfig) {
        return build(scenarioCfg, graphqlQuery, httpConfig, null);
    }

    public ScenarioBuilder build(ScenarioConfig scenarioCfg, String graphqlQuery,
                                 HttpConfig httpConfig, Map<String, String> effectiveHeaders) {
        log.info("Building scenario: {}", scenarioCfg.getName());

        List<StepConfig> steps = resolveSteps(scenarioCfg);

        Supplier<String> idSupplier = correlationIdSupplier;
        Supplier<String> tokenSupplier = authTokenSupplier;
        ChainBuilder sessionPrep = CoreDsl.exec(session -> {
            var s = session.set("correlationId", idSupplier.get());
            String token = tokenSupplier.get();
            return token != null ? s.set("authToken", token) : s.set("authToken", "");
        });

        ChainBuilder body = sessionPrep;
        for (StepConfig step : steps) {
            String inlineBody = loadStepBody(step);
            body = body.exec(StepBuilder.build(scenarioCfg, step, graphqlQuery, inlineBody,
                httpConfig, effectiveHeaders));
        }
        return CoreDsl.scenario(scenarioCfg.getName()).exec(body);
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
