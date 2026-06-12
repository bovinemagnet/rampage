package io.rampage.factory;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.CheckBuilder;
import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.core.PauseType;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import io.rampage.config.model.AfterRequestPause;
import io.rampage.config.model.EnvironmentConfig;
import io.rampage.config.model.HttpConfig;
import io.rampage.config.model.PausesConfig;
import io.rampage.config.model.ScenarioConfig;
import io.rampage.config.model.StepConfig;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Composes a single step into a Gatling {@link ChainBuilder}: optional before-pause,
 * the HTTP request (with checks and extractions), and optional after-pause.
 *
 * <p>Pause precedence at each scope:
 * <ol>
 *   <li>Step-level {@link StepConfig#getPauseAfter()}</li>
 *   <li>Scenario-level {@link ScenarioConfig#getPauses()} {@code afterRequest} (fallback)</li>
 * </ol>
 */
public final class StepBuilder {

    private StepBuilder() {}

    /**
     * Builds a Gatling {@code ChainBuilder} for a single scenario step,
     * consisting of an optional before-pause, the HTTP request with its checks
     * and extractions, and an optional after-pause.
     *
     * @param scenarioCfg        the scenario configuration providing pause
     *                           defaults and the scenario-level endpoint reference
     * @param step               the step configuration to build
     * @param graphqlQuery       the pre-loaded GraphQL query string; may be
     *                           {@code null} for non-GraphQL steps
     * @param inlineBodyFromFile the pre-loaded body file content referenced by
     *                           {@code request.bodyFile}; may be {@code null}
     * @param httpConfig         HTTP-level settings such as per-request timeout;
     *                           may be {@code null}
     * @param effectiveHeaders   the merged set of headers to attach to the
     *                           request; may be {@code null}
     * @param env                the environment configuration used for
     *                           cross-endpoint URL resolution; may be {@code null}
     * @return a {@code ChainBuilder} representing the complete step
     */
    public static ChainBuilder build(ScenarioConfig scenarioCfg,
                                      StepConfig step,
                                      String graphqlQuery,
                                      String inlineBodyFromFile,
                                      HttpConfig httpConfig,
                                      Map<String, String> effectiveHeaders,
                                      EnvironmentConfig env) {
        List<CheckBuilder> checks = CheckFactory.build(step.getChecks(), step.getExtract());
        HttpRequestActionBuilder request = RequestBuilder.build(
            scenarioCfg, step, graphqlQuery, inlineBodyFromFile, httpConfig, effectiveHeaders, checks, env);

        ChainBuilder before = beforePause(scenarioCfg);
        ChainBuilder chain = before != null ? before.exec(request) : CoreDsl.exec(request);

        ChainBuilder afterPause = afterPause(scenarioCfg, step);
        return afterPause != null ? chain.exec(afterPause) : chain;
    }

    private static ChainBuilder beforePause(ScenarioConfig scenarioCfg) {
        PausesConfig pauses = scenarioCfg.getPauses();
        if (pauses == null || pauses.getBeforeRequestMillis() <= 0) return null;
        return CoreDsl.exec(CoreDsl.pause(Duration.ofMillis(pauses.getBeforeRequestMillis())));
    }

    private static ChainBuilder afterPause(ScenarioConfig scenarioCfg, StepConfig step) {
        AfterRequestPause configured = step.getPauseAfter();
        if (configured == null && scenarioCfg.getPauses() != null) {
            configured = scenarioCfg.getPauses().getAfterRequest();
        }
        if (configured == null) return null;

        long min = configured.getMinMillis();
        long max = configured.getMaxMillis();
        String strategy = configured.getStrategy() != null
            ? configured.getStrategy().toLowerCase(Locale.ROOT) : "constant";

        if (max <= 0 && min <= 0) return null;
        if (max <= 0) max = min;
        if (min <= 0) min = 0;

        Duration minD = Duration.ofMillis(min);
        Duration maxD = Duration.ofMillis(max);

        return switch (strategy) {
            case "uniform" -> CoreDsl.exec(CoreDsl.pause(minD, maxD));
            case "exponential" -> CoreDsl.exec(CoreDsl.pause(minD, PauseType.Exponential));
            default -> {
                if (min == max) yield CoreDsl.exec(CoreDsl.pause(minD));
                yield CoreDsl.exec(CoreDsl.pause(minD, maxD));
            }
        };
    }
}
