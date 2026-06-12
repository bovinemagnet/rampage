package io.rampage.factory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites Rampage YAML placeholders into Gatling Expression Language so values flow
 * from feeders and prior-step extractions into request paths, bodies, headers, and
 * query/form parameters at runtime.
 *
 * <p>Supported forms (case-insensitive {@code feeder} / {@code session} prefix):
 * <ul>
 *   <li>{@code ${feeder:userId}} → {@code #{userId}} — populated by the row feeder</li>
 *   <li>{@code ${session:orderId}} → {@code #{orderId}} — populated by an earlier
 *       step's {@code extract} or check {@code saveAs}</li>
 * </ul>
 *
 * <p>{@link #rewriteString(String)} replaces every occurrence inside a string (so
 * paths like {@code /users/${feeder:userId}/orders/${session:orderId}} resolve fully),
 * whereas {@link #rewriteVariableMap(Map)} preserves the original semantics used by
 * GraphQL variable rewriting: only exact-match values are rewritten, partial matches
 * pass through untouched (because GraphQL variables are typed and rarely string-spliced).
 */
public final class PlaceholderRewriter {

    private static final Pattern EXACT = Pattern.compile("^\\$\\{(feeder|session):([^}]+)\\}$",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern ANY = Pattern.compile("\\$\\{(feeder|session):([^}]+)\\}",
        Pattern.CASE_INSENSITIVE);

    private PlaceholderRewriter() {}

    /**
     * Replaces all {@code ${feeder:key}} and {@code ${session:key}} placeholders
     * in {@code input} with the Gatling EL form {@code #{key}}.
     *
     * <p>Returns the original reference unchanged if {@code input} is {@code null}
     * or empty.
     *
     * @param input the string to rewrite; may be {@code null}
     * @return the rewritten string, or the original value if no placeholders are found
     */
    public static String rewriteString(String input) {
        if (input == null || input.isEmpty()) return input;
        Matcher m = ANY.matcher(input);
        StringBuilder out = new StringBuilder(input.length());
        while (m.find()) {
            m.appendReplacement(out, Matcher.quoteReplacement("#{" + m.group(2) + "}"));
        }
        m.appendTail(out);
        return out.toString();
    }

    /**
     * Rewrites every value in the given {@code String}-valued map using
     * {@link #rewriteString(String)}, preserving key order.
     *
     * <p>Returns an empty map when {@code input} is {@code null}.
     *
     * @param input the map whose values should be rewritten; may be {@code null}
     * @return a new map with the same keys and rewritten values
     */
    public static Map<String, String> rewriteStringMap(Map<String, String> input) {
        Map<String, String> out = new LinkedHashMap<>();
        if (input == null) return out;
        for (Map.Entry<String, String> e : input.entrySet()) {
            out.put(e.getKey(), rewriteString(e.getValue()));
        }
        return out;
    }

    /**
     * Rewrites exact-match placeholder values in a GraphQL variables map.
     *
     * <p>Only values that are a {@code String} whose entire content matches a single
     * {@code ${feeder:key}} or {@code ${session:key}} placeholder are rewritten to
     * {@code #{key}}. Values that contain embedded placeholders alongside other text,
     * or values of non-{@code String} types, are left unchanged. This preserves the
     * typed semantics of GraphQL variables.
     *
     * <p>Returns an empty map when {@code input} is {@code null}.
     *
     * @param input the GraphQL variables map; may be {@code null}
     * @return a new map with eligible placeholder values rewritten
     */
    public static Map<String, Object> rewriteVariableMap(Map<String, Object> input) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (input == null) return out;
        for (Map.Entry<String, Object> e : input.entrySet()) {
            Object v = e.getValue();
            if (v instanceof String s) {
                Matcher m = EXACT.matcher(s);
                if (m.matches()) {
                    out.put(e.getKey(), "#{" + m.group(2) + "}");
                    continue;
                }
            }
            out.put(e.getKey(), v);
        }
        return out;
    }

    /**
     * Returns the names of session keys referenced by a string ({@code ${session:x}}).
     * Used by the validator to flag references to keys that no earlier step extracts.
     *
     * @param input the string to scan for {@code ${session:key}} placeholders;
     *              may be {@code null}
     * @return a list of referenced session key names, in the order they appear;
     *         never {@code null}
     */
    public static java.util.List<String> referencedSessionKeys(String input) {
        java.util.List<String> keys = new java.util.ArrayList<>();
        if (input == null) return keys;
        Matcher m = ANY.matcher(input);
        while (m.find()) {
            if ("session".equalsIgnoreCase(m.group(1))) {
                keys.add(m.group(2));
            }
        }
        return keys;
    }
}
