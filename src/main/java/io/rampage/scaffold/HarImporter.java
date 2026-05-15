package io.rampage.scaffold;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Generates a Rampage scenario YAML from a HAR (HTTP Archive) capture.
 *
 * <p>HAR is the JSON format every browser dev-tools "save all as HAR" produces. This
 * importer walks {@code log.entries[]}, filters out static assets and CORS preflights,
 * and emits a single scenario with one step per remaining request. Path, query params,
 * headers, and request body are preserved verbatim — the user can then template values
 * with {@code ${feeder:foo}} placeholders by hand.
 *
 * <p>If multiple hosts appear in the capture, the scenario is restricted to the
 * busiest host unless an explicit {@code host} filter is provided.
 */
public final class HarImporter {

    private static final Logger log = LoggerFactory.getLogger(HarImporter.class);

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ObjectMapper YAML = new ObjectMapper(
        new YAMLFactory()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES))
        .enable(SerializationFeature.INDENT_OUTPUT);

    /** Mime-type prefixes that we treat as static assets and exclude. */
    private static final Set<String> STATIC_MIMES = Set.of(
        "text/css", "text/javascript", "application/javascript",
        "image/", "font/", "video/", "audio/");

    /** URL extensions we treat as static assets and exclude. */
    private static final Set<String> STATIC_EXTENSIONS = Set.of(
        ".css", ".js", ".png", ".jpg", ".jpeg", ".gif", ".svg", ".ico",
        ".woff", ".woff2", ".ttf", ".eot", ".map");

    private HarImporter() {}

    public static final class Options {
        public String scenarioId;
        public String hostFilter;          // optional; e.g. "api.example.com"
        public Set<String> methodFilter;   // optional; uppercase HTTP verbs
        public boolean includeStatic;      // default false

        public Options(String scenarioId) {
            this.scenarioId = scenarioId;
        }
    }

    /** Read {@code harFile}, generate scenario YAML, write it to {@code outFile}. */
    public static int importHar(Path harFile, Path outFile, Options options) throws IOException {
        Map<String, Object> har = JSON.readValue(harFile.toFile(),
            new TypeReference<Map<String, Object>>() {});
        List<Map<String, Object>> entries = extractEntries(har);
        log.info("HAR contains {} entries", entries.size());

        String chosenHost = options.hostFilter != null && !options.hostFilter.isBlank()
            ? options.hostFilter
            : pickBusiestHost(entries);
        log.info("Host filter: {}", chosenHost);

        List<Map<String, Object>> steps = new ArrayList<>();
        int stepNum = 1;
        for (Map<String, Object> entry : entries) {
            Map<String, Object> request = asMap(entry.get("request"));
            if (request == null) continue;
            if (!matchesHost(request, chosenHost)) continue;
            if (!matchesMethod(request, options)) continue;
            if (!options.includeStatic && isStaticAsset(request, asMap(entry.get("response")))) continue;
            Map<String, Object> step = toStep(request, "step-" + stepNum++);
            steps.add(step);
        }
        log.info("Filtered down to {} relevant steps", steps.size());

        Map<String, Object> scenario = buildScenario(options.scenarioId, chosenHost, steps);
        Files.createDirectories(outFile.getParent());
        Files.writeString(outFile, YAML.writeValueAsString(scenario), StandardCharsets.UTF_8);
        log.info("Wrote scenario to {}", outFile);
        return steps.size();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> extractEntries(Map<String, Object> har) {
        Map<String, Object> logBlock = asMap(har.get("log"));
        if (logBlock == null) throw new IllegalArgumentException("HAR file missing top-level 'log' block");
        Object entries = logBlock.get("entries");
        if (!(entries instanceof List)) throw new IllegalArgumentException("HAR log.entries must be an array");
        return (List<Map<String, Object>>) entries;
    }

    private static String pickBusiestHost(List<Map<String, Object>> entries) {
        Map<String, Integer> counts = new HashMap<>();
        for (Map<String, Object> e : entries) {
            Map<String, Object> req = asMap(e.get("request"));
            String host = hostOf(req);
            if (host != null) counts.merge(host, 1, Integer::sum);
        }
        return counts.entrySet().stream()
            .max(Comparator.comparingInt(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    private static boolean matchesHost(Map<String, Object> request, String wantedHost) {
        if (wantedHost == null) return true;
        String h = hostOf(request);
        return h != null && h.equalsIgnoreCase(wantedHost);
    }

    private static boolean matchesMethod(Map<String, Object> request, Options options) {
        String method = String.valueOf(request.getOrDefault("method", "GET")).toUpperCase(Locale.ROOT);
        if ("OPTIONS".equals(method)) return false;
        if (options.methodFilter == null || options.methodFilter.isEmpty()) return true;
        return options.methodFilter.contains(method);
    }

    private static boolean isStaticAsset(Map<String, Object> request, Map<String, Object> response) {
        String url = String.valueOf(request.getOrDefault("url", ""));
        String pathLower = pathOnly(url).toLowerCase(Locale.ROOT);
        for (String ext : STATIC_EXTENSIONS) {
            if (pathLower.endsWith(ext)) return true;
        }
        if (response != null) {
            Map<String, Object> content = asMap(response.get("content"));
            String mime = content == null ? null : String.valueOf(content.getOrDefault("mimeType", ""));
            if (mime != null) {
                String mimeLower = mime.toLowerCase(Locale.ROOT);
                for (String prefix : STATIC_MIMES) {
                    if (mimeLower.startsWith(prefix)) return true;
                }
            }
        }
        return false;
    }

    private static Map<String, Object> toStep(Map<String, Object> request, String stepName) {
        String method = String.valueOf(request.getOrDefault("method", "GET")).toUpperCase(Locale.ROOT);
        String url = String.valueOf(request.getOrDefault("url", ""));
        String path = pathOnly(url);

        Map<String, Object> requestBlock = new LinkedHashMap<>();
        requestBlock.put("method", method);
        requestBlock.put("path", path.isEmpty() ? "/" : path);

        Map<String, String> queryParams = collectKvList(request.get("queryString"));
        if (!queryParams.isEmpty()) requestBlock.put("queryParams", queryParams);

        Map<String, String> headers = collectHeaders(request.get("headers"));
        if (!headers.isEmpty()) requestBlock.put("headers", headers);

        Map<String, Object> postData = asMap(request.get("postData"));
        if (postData != null) {
            String mime = String.valueOf(postData.getOrDefault("mimeType", "")).toLowerCase(Locale.ROOT);
            String text = (String) postData.get("text");
            if (text != null && !text.isBlank()) {
                if (mime.contains("json") || text.trim().startsWith("{") || text.trim().startsWith("[")) {
                    requestBlock.put("bodyType", "json");
                } else if (mime.contains("form")) {
                    requestBlock.put("bodyType", "form");
                } else {
                    requestBlock.put("bodyType", "text");
                }
                requestBlock.put("body", text);
            } else {
                requestBlock.put("bodyType", "none");
            }
        } else {
            requestBlock.put("bodyType", "none");
        }

        Map<String, Object> step = new LinkedHashMap<>();
        step.put("name", stepName);
        step.put("request", requestBlock);
        Map<String, Object> checks = new LinkedHashMap<>();
        checks.put("httpStatus", 200);
        step.put("checks", checks);
        return step;
    }

    private static Map<String, Object> buildScenario(String id, String host, List<Map<String, Object>> steps) {
        Map<String, Object> scenario = new LinkedHashMap<>();
        scenario.put("id", id);
        scenario.put("name", "Imported from HAR — " + (host != null ? host : "all hosts"));
        scenario.put("protocol", "rest");
        scenario.put("endpointRef", "rest");
        scenario.put("description",
            "Generated by HarImporter. Review URLs, headers, and bodies before running. "
            + "Replace dynamic values with ${feeder:colName} placeholders as needed.");
        Map<String, Object> workload = new LinkedHashMap<>();
        workload.put("inheritFromRun", true);
        scenario.put("workload", workload);
        scenario.put("steps", steps);
        Map<String, Object> safety = new LinkedHashMap<>();
        safety.put("mutating", containsMutatingMethod(steps));
        safety.put("idempotent", false);
        scenario.put("safety", safety);
        return scenario;
    }

    private static boolean containsMutatingMethod(List<Map<String, Object>> steps) {
        Set<String> mutating = Set.of("POST", "PUT", "PATCH", "DELETE");
        for (Map<String, Object> step : steps) {
            Map<String, Object> req = asMap(step.get("request"));
            if (req == null) continue;
            String method = String.valueOf(req.getOrDefault("method", "")).toUpperCase(Locale.ROOT);
            if (mutating.contains(method)) return true;
        }
        return false;
    }

    private static Map<String, String> collectKvList(Object listObj) {
        Map<String, String> out = new LinkedHashMap<>();
        if (!(listObj instanceof List<?> list)) return out;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> m)) continue;
            Object name = m.get("name");
            Object value = m.get("value");
            if (name != null) out.put(name.toString(), value == null ? "" : value.toString());
        }
        return out;
    }

    private static Map<String, String> collectHeaders(Object listObj) {
        Map<String, String> headers = collectKvList(listObj);
        Map<String, String> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : headers.entrySet()) {
            String name = e.getKey();
            String lower = name.toLowerCase(Locale.ROOT);
            // Strip browser-internal pseudo headers and ones that the framework manages.
            if (lower.startsWith(":")) continue;
            if (lower.equals("host") || lower.equals("content-length") || lower.equals("connection")
                || lower.equals("accept-encoding") || lower.equals("authorization")
                || lower.equals("cookie") || lower.equals("origin") || lower.equals("referer")) continue;
            filtered.put(name, e.getValue());
        }
        return filtered;
    }

    private static String hostOf(Map<String, Object> request) {
        if (request == null) return null;
        try {
            URI u = URI.create(String.valueOf(request.getOrDefault("url", "")));
            return u.getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private static String pathOnly(String url) {
        try {
            URI u = URI.create(url);
            String p = u.getRawPath();
            return p == null ? "" : p;
        } catch (Exception e) {
            return url;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    /** CLI for the {@code importHar} Gradle task. */
    public static void main(String[] args) throws Exception {
        String harFile = required("rampage.har.file", System.getProperty("rampage.har.file"));
        String scenarioId = required("rampage.scenario.id", System.getProperty("rampage.scenario.id"));
        String outDir = System.getProperty("rampage.scenario.dir", "config/scenarios");
        Options opts = new Options(scenarioId);
        opts.hostFilter = System.getProperty("rampage.har.host");
        String methods = System.getProperty("rampage.har.methods");
        if (methods != null && !methods.isBlank()) {
            Set<String> set = new java.util.LinkedHashSet<>();
            for (String m : methods.split(",")) {
                if (!m.isBlank()) set.add(m.trim().toUpperCase(Locale.ROOT));
            }
            opts.methodFilter = set;
        }
        opts.includeStatic = Boolean.parseBoolean(System.getProperty("rampage.har.includeStatic", "false"));
        Path output = Path.of(outDir, scenarioId + ".yaml");
        int steps = importHar(Path.of(harFile), output, opts);
        log.info("Imported {} step(s) into {}", steps, output);
    }

    private static String required(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("System property '-D" + key + "' is required");
        }
        return value;
    }
}
