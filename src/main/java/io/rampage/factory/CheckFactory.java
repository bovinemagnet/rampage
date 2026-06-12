package io.rampage.factory;

import io.gatling.javaapi.core.CheckBuilder;
import io.rampage.config.model.ChecksConfig;
import io.rampage.config.model.ExtractConfig;
import io.rampage.config.model.HeaderCheck;
import io.rampage.config.model.JsonPathCheck;
import io.rampage.config.model.RegexCheck;

import java.util.ArrayList;
import java.util.List;

import static io.gatling.javaapi.core.CoreDsl.bodyString;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.regex;
import static io.gatling.javaapi.core.CoreDsl.responseTimeInMillis;
import static io.gatling.javaapi.core.CoreDsl.substring;
import static io.gatling.javaapi.http.HttpDsl.header;
import static io.gatling.javaapi.http.HttpDsl.status;

/**
 * Compiles {@link ChecksConfig} and {@link ExtractConfig} into Gatling {@link CheckBuilder}s.
 *
 * <p>Supported check kinds:
 * <ul>
 *   <li>HTTP status equality</li>
 *   <li>JSONPath: {@code exists}, {@code absentOrEmpty}, {@code equalsSession},
 *       {@code equalsValue}, with optional {@code saveAs}</li>
 *   <li>Regex: {@code exists}, {@code matches}, with optional {@code sessionKey} (saveAs)</li>
 *   <li>Header: {@code exists}, {@code equals}, {@code matches}, with optional {@code sessionKey}</li>
 *   <li>Response-time upper bound (milliseconds)</li>
 *   <li>Body substring presence</li>
 *   <li>Extract: explicit response capture into a named session key</li>
 * </ul>
 */
public final class CheckFactory {

    /** Prevents instantiation of this utility class. */
    private CheckFactory() {}

    /**
     * Compiles check and extract configurations into a list of Gatling {@code CheckBuilder}s.
     *
     * <p>All check kinds declared in {@code checks} are processed first, followed by
     * the explicit session-capture entries in {@code extracts}. Either argument may be
     * {@code null}, in which case that group is skipped.
     *
     * @param checks   the checks configuration for a scenario or step; may be {@code null}
     * @param extracts the list of explicit response-capture configurations; may be {@code null}
     * @return an ordered list of {@code CheckBuilder}s ready to attach to a Gatling request;
     *         never {@code null}
     */
    public static List<CheckBuilder> build(ChecksConfig checks, List<ExtractConfig> extracts) {
        List<CheckBuilder> result = new ArrayList<>();
        if (checks != null) {
            if (checks.getHttpStatus() != null) {
                result.add(status().is(checks.getHttpStatus()));
            }
            addJsonPathChecks(checks.getJsonPath(), result);
            addRegexChecks(checks.getRegex(), result);
            addHeaderChecks(checks.getHeader(), result);
            if (checks.getResponseTimeMillis() != null && checks.getResponseTimeMillis() > 0) {
                result.add(responseTimeInMillis().lte(checks.getResponseTimeMillis().intValue()));
            }
            if (checks.getBodyContains() != null) {
                for (String needle : checks.getBodyContains()) {
                    if (needle != null && !needle.isEmpty()) {
                        result.add(substring(needle).exists());
                    }
                }
            }
        }
        addExtracts(extracts, result);
        return result;
    }

    private static void addJsonPathChecks(List<JsonPathCheck> checks, List<CheckBuilder> out) {
        if (checks == null) return;
        for (JsonPathCheck c : checks) {
            if (c == null || c.getPath() == null) continue;
            String expectation = c.getExpectation();
            CheckBuilder builder;
            if ("absentOrEmpty".equalsIgnoreCase(expectation)) {
                builder = jsonPath(c.getPath()).notExists();
            } else if ("equalsSession".equalsIgnoreCase(expectation) && c.getSessionKey() != null) {
                builder = jsonPath(c.getPath()).isEL("#{" + c.getSessionKey() + "}");
            } else if ("equalsValue".equalsIgnoreCase(expectation) && c.getEqualsValue() != null) {
                builder = jsonPath(c.getPath()).is(c.getEqualsValue());
            } else if (c.getEqualsValue() != null) {
                // expectation omitted but a literal was provided — treat as equals
                builder = jsonPath(c.getPath()).is(c.getEqualsValue());
            } else {
                // Default and "exists" fall through to existence check.
                builder = jsonPath(c.getPath()).exists();
            }
            if (c.getSaveAs() != null && !c.getSaveAs().isBlank()) {
                builder = ((CheckBuilder.Final) builder).saveAs(c.getSaveAs());
            }
            out.add(builder);
        }
    }

    private static void addRegexChecks(List<RegexCheck> checks, List<CheckBuilder> out) {
        if (checks == null) return;
        for (RegexCheck c : checks) {
            if (c == null || c.getPattern() == null) continue;
            String expectation = c.getExpectation();
            CheckBuilder builder = regex(c.getPattern()).exists();
            if ("matches".equalsIgnoreCase(expectation)) {
                // Gatling regex().exists() asserts the pattern matches at least once.
                // No additional configuration needed for "matches" — treat as alias.
                builder = regex(c.getPattern()).exists();
            }
            if (c.getSessionKey() != null && !c.getSessionKey().isBlank()) {
                builder = ((CheckBuilder.Final) builder).saveAs(c.getSessionKey());
            }
            out.add(builder);
        }
    }

    private static void addHeaderChecks(List<HeaderCheck> checks, List<CheckBuilder> out) {
        if (checks == null) return;
        for (HeaderCheck c : checks) {
            if (c == null || c.getName() == null || c.getName().isBlank()) continue;
            String expectation = c.getExpectation();
            CheckBuilder builder;
            if ("equals".equalsIgnoreCase(expectation) && c.getValue() != null) {
                builder = header(c.getName()).is(c.getValue());
            } else if ("matches".equalsIgnoreCase(expectation) && c.getValue() != null) {
                builder = header(c.getName()).transform(s -> s != null && s.matches(c.getValue())).is(true);
            } else {
                builder = header(c.getName()).exists();
            }
            if (c.getSessionKey() != null && !c.getSessionKey().isBlank()) {
                builder = ((CheckBuilder.Final) builder).saveAs(c.getSessionKey());
            }
            out.add(builder);
        }
    }

    private static void addExtracts(List<ExtractConfig> extracts, List<CheckBuilder> out) {
        if (extracts == null) return;
        for (ExtractConfig e : extracts) {
            if (e == null || e.getSessionKey() == null || e.getSessionKey().isBlank()) continue;
            String type = e.getType() != null ? e.getType().toLowerCase(java.util.Locale.ROOT) : "jsonpath";
            String def = e.getDefaultValue();
            switch (type) {
                case "regex": {
                    if (e.getPath() == null) continue;
                    var find = regex(e.getPath()).find();
                    out.add(def != null ? find.withDefault(def).saveAs(e.getSessionKey())
                                        : find.saveAs(e.getSessionKey()));
                    break;
                }
                case "header": {
                    if (e.getPath() == null) continue;
                    var find = header(e.getPath());
                    out.add(def != null ? find.withDefault(def).saveAs(e.getSessionKey())
                                        : find.saveAs(e.getSessionKey()));
                    break;
                }
                case "body": {
                    var find = bodyString();
                    out.add(def != null ? find.withDefault(def).saveAs(e.getSessionKey())
                                        : find.saveAs(e.getSessionKey()));
                    break;
                }
                default: { // jsonPath / jsonpath / unspecified
                    if (e.getPath() == null) continue;
                    var find = jsonPath(e.getPath()).find();
                    out.add(def != null ? find.withDefault(def).saveAs(e.getSessionKey())
                                        : find.saveAs(e.getSessionKey()));
                }
            }
        }
    }
}
