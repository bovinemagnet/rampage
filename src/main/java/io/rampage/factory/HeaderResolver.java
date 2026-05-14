package io.rampage.factory;

import io.rampage.config.model.EnvironmentConfig;
import io.rampage.config.model.RunConfig;
import io.rampage.config.model.ScenarioConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Computes the effective header map per PRD §14 precedence and enforces protected-header
 * overrides. Layering order: framework → environment → run → scenario.
 */
public final class HeaderResolver {

    private HeaderResolver() {}

    private static final String AUTHORIZATION = "Authorization";

    public static Set<String> protectedHeaders(EnvironmentConfig env) {
        Set<String> protectedHeaders = new LinkedHashSet<>();
        protectedHeaders.add(AUTHORIZATION);
        if (env != null && env.getObservability() != null
            && env.getObservability().getCorrelationIdHeader() != null
            && !env.getObservability().getCorrelationIdHeader().isBlank()) {
            protectedHeaders.add(env.getObservability().getCorrelationIdHeader());
        }
        return protectedHeaders;
    }

    /**
     * Returns scenario-level errors for any scenario header that attempts to override a
     * protected header without the scenario opting in via {@code security.allowAuthOverride}.
     */
    public static List<String> validateOverrides(EnvironmentConfig env, ScenarioConfig sc) {
        List<String> errors = new ArrayList<>();
        if (sc == null || sc.getHeaders() == null || sc.getHeaders().isEmpty()) return errors;
        boolean allowed = sc.getSecurity() != null && sc.getSecurity().isAllowAuthOverride();
        if (allowed) return errors;

        Set<String> protectedLower = protectedLower(env);
        for (String key : sc.getHeaders().keySet()) {
            if (key == null) continue;
            if (protectedLower.contains(key.toLowerCase(Locale.ROOT))) {
                errors.add("scenario." + sc.getId() + ".headers must not override protected header '"
                    + key + "'. Set scenario.security.allowAuthOverride=true to permit this.");
            }
        }
        return errors;
    }

    /**
     * Compute the layered headers for a scenario in framework → env → run → scenario order.
     * The framework-required headers (e.g. Content-Type) are not included here — the
     * Gatling HTTP protocol layer attaches them.
     */
    public static Map<String, String> resolveScenarioHeaders(EnvironmentConfig env, RunConfig run, ScenarioConfig sc) {
        Map<String, String> resolved = new LinkedHashMap<>();
        // Environment headers (from security and observability) live on the HttpProtocolBuilder,
        // not here, so we don't duplicate them. Run + scenario headers are scenario-scoped.
        if (run != null && run.getHeaders() != null) {
            resolved.putAll(run.getHeaders());
        }
        if (sc != null && sc.getHeaders() != null) {
            resolved.putAll(sc.getHeaders());
        }
        return Collections.unmodifiableMap(resolved);
    }

    private static Set<String> protectedLower(EnvironmentConfig env) {
        Set<String> lower = new LinkedHashSet<>();
        for (String h : protectedHeaders(env)) {
            lower.add(h.toLowerCase(Locale.ROOT));
        }
        return lower;
    }
}
