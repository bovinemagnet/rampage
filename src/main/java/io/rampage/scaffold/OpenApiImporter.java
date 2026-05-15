package io.rampage.scaffold;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Generates Rampage scenario YAMLs from an OpenAPI 3.x spec.
 *
 * <p>One scenario is emitted per operation (path × method). Path parameters declared
 * in the spec ({@code /users/{userId}}) become {@code ${feeder:userId}} placeholders
 * so the user can wire a feeder column straight into them. Request bodies declared
 * with an {@code example} or schema {@code default}/{@code example} are inlined as
 * the step's JSON body; otherwise an empty {@code {}} is used.
 *
 * <p>The first declared 2xx response is wired as the {@code httpStatus} check.
 */
public final class OpenApiImporter {

    private static final Logger log = LoggerFactory.getLogger(OpenApiImporter.class);

    private static final ObjectMapper YAML = new ObjectMapper(
        new YAMLFactory()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES))
        .enable(SerializationFeature.INDENT_OUTPUT);

    private static final ObjectMapper JSON = new ObjectMapper();

    private OpenApiImporter() {}

    public static final class Options {
        public String prefix = "";
        public String endpointRef = "rest";
    }

    /** Parse the OpenAPI spec at {@code specFile} and emit one scenario per operation. */
    public static int importSpec(Path specFile, Path outDir, Options options) throws IOException {
        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(
            specFile.toAbsolutePath().toString(), null, null);
        OpenAPI api = result.getOpenAPI();
        if (api == null) {
            throw new IOException("Failed to parse OpenAPI spec: "
                + (result.getMessages() == null ? "unknown error" : String.join("; ", result.getMessages())));
        }
        Map<String, PathItem> paths = api.getPaths();
        if (paths == null || paths.isEmpty()) {
            throw new IOException("OpenAPI spec contains no paths");
        }
        Files.createDirectories(outDir);
        int written = 0;
        for (Map.Entry<String, PathItem> entry : paths.entrySet()) {
            String path = entry.getKey();
            PathItem item = entry.getValue();
            for (Map.Entry<PathItem.HttpMethod, Operation> op : item.readOperationsMap().entrySet()) {
                String scenarioId = scenarioId(options.prefix, op.getKey().name(), path, op.getValue());
                Path file = outDir.resolve(scenarioId + ".yaml");
                Map<String, Object> scenario = buildScenario(scenarioId, op.getKey().name(),
                    path, op.getValue(), options);
                Files.writeString(file, YAML.writeValueAsString(scenario), StandardCharsets.UTF_8);
                written++;
                log.info("Wrote {}", file);
            }
        }
        return written;
    }

    static String scenarioId(String prefix, String method, String path, Operation op) {
        String base;
        if (op.getOperationId() != null && !op.getOperationId().isBlank()) {
            base = op.getOperationId();
        } else {
            base = method.toLowerCase(Locale.ROOT) + path
                .replaceAll("\\{[^}]+\\}", "by-")
                .replaceAll("[^a-zA-Z0-9]+", "-");
        }
        String id = prefix + base;
        return id.replaceAll("-+", "-").replaceAll("(^-|-$)", "").toLowerCase(Locale.ROOT);
    }

    static Map<String, Object> buildScenario(String id, String httpMethod, String path,
                                              Operation op, Options options) {
        String templatedPath = templatePath(path);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("method", httpMethod.toUpperCase(Locale.ROOT));
        request.put("path", templatedPath);

        Map<String, String> queryParams = collectQueryParams(op);
        if (!queryParams.isEmpty()) request.put("queryParams", queryParams);

        BodyDecision body = decideBody(op.getRequestBody());
        request.put("bodyType", body.bodyType);
        if (body.body != null) request.put("body", body.body);

        Map<String, Object> checks = new LinkedHashMap<>();
        Integer status = firstSuccessfulStatus(op);
        checks.put("httpStatus", status != null ? status : 200);

        Map<String, Object> scenario = new LinkedHashMap<>();
        scenario.put("id", id);
        String summary = op.getSummary();
        scenario.put("name", summary != null && !summary.isBlank() ? summary : (httpMethod + " " + path));
        scenario.put("protocol", "rest");
        scenario.put("endpointRef", options.endpointRef);
        if (op.getDescription() != null && !op.getDescription().isBlank()) {
            scenario.put("description", op.getDescription());
        } else {
            scenario.put("description", "Generated by OpenApiImporter from " + httpMethod + " " + path
                + ". Wire feeder columns into the ${feeder:...} placeholders before running.");
        }
        scenario.put("request", request);
        scenario.put("checks", checks);
        Map<String, Object> workload = new LinkedHashMap<>();
        workload.put("inheritFromRun", true);
        scenario.put("workload", workload);
        if (op.getTags() != null && !op.getTags().isEmpty()) {
            scenario.put("tags", new ArrayList<>(op.getTags()));
        }
        Map<String, Object> safety = new LinkedHashMap<>();
        safety.put("mutating", isMutating(httpMethod));
        safety.put("idempotent", isIdempotent(httpMethod));
        scenario.put("safety", safety);
        return scenario;
    }

    private static String templatePath(String openapiPath) {
        // /users/{userId}/orders/{orderId} → /users/${feeder:userId}/orders/${feeder:orderId}
        return openapiPath.replaceAll("\\{([^}]+)\\}", "\\$\\{feeder:$1\\}");
    }

    private static Map<String, String> collectQueryParams(Operation op) {
        Map<String, String> params = new LinkedHashMap<>();
        if (op.getParameters() == null) return params;
        for (Parameter p : op.getParameters()) {
            if (!"query".equals(p.getIn())) continue;
            String name = p.getName();
            if (name == null) continue;
            params.put(name, "${feeder:" + name + "}");
        }
        return params;
    }

    private static BodyDecision decideBody(RequestBody requestBody) {
        if (requestBody == null || requestBody.getContent() == null) return BodyDecision.none();
        for (Map.Entry<String, MediaType> entry : requestBody.getContent().entrySet()) {
            String mime = entry.getKey().toLowerCase(Locale.ROOT);
            MediaType media = entry.getValue();
            String example = exampleFor(media);
            if (mime.contains("json")) {
                return BodyDecision.json(example != null ? example : "{}");
            } else if (mime.contains("form")) {
                return BodyDecision.form();
            } else {
                return BodyDecision.text(example != null ? example : "");
            }
        }
        return BodyDecision.none();
    }

    private static String exampleFor(MediaType media) {
        if (media == null) return null;
        if (media.getExample() != null) return jsonString(media.getExample());
        if (media.getExamples() != null && !media.getExamples().isEmpty()) {
            var first = media.getExamples().values().iterator().next();
            if (first != null && first.getValue() != null) return jsonString(first.getValue());
        }
        if (media.getSchema() != null && media.getSchema().getExample() != null) {
            return jsonString(media.getSchema().getExample());
        }
        return null;
    }

    private static String jsonString(Object value) {
        if (value instanceof String s) return s;
        try {
            return JSON.writeValueAsString(value);
        } catch (Exception e) {
            return value.toString();
        }
    }

    private static Integer firstSuccessfulStatus(Operation op) {
        if (op.getResponses() == null) return null;
        for (Map.Entry<String, ApiResponse> e : op.getResponses().entrySet()) {
            String code = e.getKey();
            if (code == null) continue;
            try {
                int n = Integer.parseInt(code);
                if (n >= 200 && n < 300) return n;
            } catch (NumberFormatException ignored) {
                // "default" or "2XX" — skip; httpStatus default falls back to 200.
            }
        }
        return null;
    }

    private static boolean isMutating(String method) {
        String m = method.toUpperCase(Locale.ROOT);
        return "POST".equals(m) || "PUT".equals(m) || "PATCH".equals(m) || "DELETE".equals(m);
    }

    private static boolean isIdempotent(String method) {
        String m = method.toUpperCase(Locale.ROOT);
        return "GET".equals(m) || "HEAD".equals(m) || "PUT".equals(m) || "DELETE".equals(m) || "OPTIONS".equals(m);
    }

    private static final class BodyDecision {
        final String bodyType;
        final String body;

        private BodyDecision(String bodyType, String body) {
            this.bodyType = bodyType;
            this.body = body;
        }

        static BodyDecision none() { return new BodyDecision("none", null); }
        static BodyDecision json(String body) { return new BodyDecision("json", body); }
        static BodyDecision text(String body) { return new BodyDecision("text", body); }
        static BodyDecision form() { return new BodyDecision("form", null); }
    }

    /** CLI for the {@code importOpenApi} Gradle task. */
    public static void main(String[] args) throws Exception {
        String specFile = required("rampage.openapi.file", System.getProperty("rampage.openapi.file"));
        String outDir = System.getProperty("rampage.scenario.dir", "config/scenarios");
        Options opts = new Options();
        opts.prefix = System.getProperty("rampage.scenario.prefix", "");
        opts.endpointRef = System.getProperty("rampage.openapi.endpointRef", "rest");
        int written = importSpec(Path.of(specFile), Path.of(outDir), opts);
        log.info("Imported {} scenario(s) into {}", written, outDir);
    }

    private static String required(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("System property '-D" + key + "' is required");
        }
        return value;
    }
}
