package io.rampage.factory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Best-effort redaction for log/snapshot output. Supports two redaction sources:
 * <ul>
 *   <li><b>JSON path</b> expressions such as {@code $.data.user.email} that null out
 *       a JSON node's value, replacing it with {@code "***REDACTED***"}.</li>
 *   <li><b>Substring</b> redaction — any occurrence of a value in {@code sensitiveValues}
 *       is replaced with {@code "***REDACTED***"} in the final string.</li>
 * </ul>
 *
 * <p>The JSON path syntax is intentionally minimal: a leading {@code $} then dot-separated
 * keys. Arrays are not supported. Use a JSONPath library if richer support is needed.
 */
public final class BodyRedactor {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String REDACTED = "***REDACTED***";

    private BodyRedactor() {}

    public static String redact(String body, List<String> jsonPaths, Set<String> sensitiveValues) {
        if (body == null || body.isEmpty()) return body;
        String result = body;
        if (jsonPaths != null && !jsonPaths.isEmpty()) {
            result = redactJsonFields(result, jsonPaths);
        }
        if (sensitiveValues != null && !sensitiveValues.isEmpty()) {
            for (String value : ordered(sensitiveValues)) {
                if (value == null || value.isEmpty()) continue;
                result = result.replace(value, REDACTED);
            }
        }
        return result;
    }

    private static String redactJsonFields(String body, List<String> jsonPaths) {
        try {
            JsonNode root = MAPPER.readTree(body);
            for (String path : jsonPaths) {
                if (path == null || path.isBlank()) continue;
                redactNode(root, path);
            }
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            // Body is not JSON — fall back to leaving it as-is (substring redaction will still apply).
            return body;
        }
    }

    private static void redactNode(JsonNode root, String path) {
        if (!path.startsWith("$")) return;
        String[] parts = path.substring(1).split("\\.");
        JsonNode current = root;
        for (int i = 1; i < parts.length - 1; i++) {
            if (current == null) return;
            current = current.get(parts[i]);
        }
        if (current instanceof ObjectNode obj && parts.length > 1) {
            String leaf = parts[parts.length - 1];
            if (obj.has(leaf)) {
                obj.put(leaf, REDACTED);
            }
        }
    }

    private static List<String> ordered(Set<String> values) {
        List<String> list = new java.util.ArrayList<>(values);
        list.sort((a, b) -> Integer.compare(b.length(), a.length()));
        return Collections.unmodifiableList(list);
    }
}
