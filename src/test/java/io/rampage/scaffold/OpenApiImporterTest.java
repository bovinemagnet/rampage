package io.rampage.scaffold;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiImporterTest {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private Path copyFixture(Path tmp) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("sample-openapi.yaml")) {
            assertNotNull(is, "sample-openapi.yaml missing from test resources");
            Path spec = tmp.resolve("openapi.yaml");
            Files.write(spec, is.readAllBytes());
            return spec;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readScenario(Path file) throws IOException {
        return YAML_MAPPER.readValue(file.toFile(), new TypeReference<Map<String, Object>>() {});
    }

    @Test
    void importSpec_writesOneScenarioPerOperation(@TempDir Path tmp) throws Exception {
        Path spec = copyFixture(tmp);
        Path outDir = tmp.resolve("scenarios");
        int written = OpenApiImporter.importSpec(spec, outDir, new OpenApiImporter.Options());
        assertEquals(3, written, "expected one scenario per operation in the fixture");
        try (var stream = Files.list(outDir)) {
            assertEquals(3, stream.count());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void importSpec_pathParametersBecomeFeederPlaceholders(@TempDir Path tmp) throws Exception {
        Path spec = copyFixture(tmp);
        Path outDir = tmp.resolve("scenarios");
        OpenApiImporter.importSpec(spec, outDir, new OpenApiImporter.Options());
        Map<String, Object> getUser = readScenario(outDir.resolve("getuser.yaml"));
        Map<String, Object> request = (Map<String, Object>) getUser.get("request");
        assertEquals("GET", request.get("method"));
        assertEquals("/users/${feeder:userId}", request.get("path"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void importSpec_queryParametersGetFeederPlaceholders(@TempDir Path tmp) throws Exception {
        Path spec = copyFixture(tmp);
        Path outDir = tmp.resolve("scenarios");
        OpenApiImporter.importSpec(spec, outDir, new OpenApiImporter.Options());
        Map<String, Object> getUser = readScenario(outDir.resolve("getuser.yaml"));
        Map<String, Object> request = (Map<String, Object>) getUser.get("request");
        Map<String, String> qp = (Map<String, String>) request.get("queryParams");
        assertEquals("${feeder:expand}", qp.get("expand"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void importSpec_inlinesExampleFromRequestBody(@TempDir Path tmp) throws Exception {
        Path spec = copyFixture(tmp);
        Path outDir = tmp.resolve("scenarios");
        OpenApiImporter.importSpec(spec, outDir, new OpenApiImporter.Options());
        Map<String, Object> createOrder = readScenario(outDir.resolve("createorder.yaml"));
        Map<String, Object> request = (Map<String, Object>) createOrder.get("request");
        assertEquals("POST", request.get("method"));
        assertEquals("json", request.get("bodyType"));
        String body = String.valueOf(request.get("body"));
        assertTrue(body.contains("\"userId\":\"u-1\""), "expected the spec example body to be inlined; got: " + body);
    }

    @Test
    @SuppressWarnings("unchecked")
    void importSpec_usesFirstSuccessfulStatusForCheck(@TempDir Path tmp) throws Exception {
        Path spec = copyFixture(tmp);
        Path outDir = tmp.resolve("scenarios");
        OpenApiImporter.importSpec(spec, outDir, new OpenApiImporter.Options());
        Map<String, Object> createOrder = readScenario(outDir.resolve("createorder.yaml"));
        Map<String, Object> checks = (Map<String, Object>) createOrder.get("checks");
        assertEquals(201, checks.get("httpStatus"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void importSpec_marksMutatingPostAsMutating(@TempDir Path tmp) throws Exception {
        Path spec = copyFixture(tmp);
        Path outDir = tmp.resolve("scenarios");
        OpenApiImporter.importSpec(spec, outDir, new OpenApiImporter.Options());
        Map<String, Object> createOrder = readScenario(outDir.resolve("createorder.yaml"));
        Map<String, Object> safety = (Map<String, Object>) createOrder.get("safety");
        assertEquals(true, safety.get("mutating"));
        assertEquals(false, safety.get("idempotent"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void importSpec_marksGetAsNonMutatingAndIdempotent(@TempDir Path tmp) throws Exception {
        Path spec = copyFixture(tmp);
        Path outDir = tmp.resolve("scenarios");
        OpenApiImporter.importSpec(spec, outDir, new OpenApiImporter.Options());
        Map<String, Object> getUser = readScenario(outDir.resolve("getuser.yaml"));
        Map<String, Object> safety = (Map<String, Object>) getUser.get("safety");
        assertEquals(false, safety.get("mutating"));
        assertEquals(true, safety.get("idempotent"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void importSpec_synthesisesIdWhenOperationIdMissing(@TempDir Path tmp) throws Exception {
        Path spec = copyFixture(tmp);
        Path outDir = tmp.resolve("scenarios");
        OpenApiImporter.importSpec(spec, outDir, new OpenApiImporter.Options());
        // The cancel operation has no operationId — id should be synthesised from method+path.
        try (var stream = Files.list(outDir)) {
            List<String> names = stream.map(p -> p.getFileName().toString()).toList();
            assertTrue(names.stream().anyMatch(n -> n.contains("cancel")),
                "expected a synthesised id containing 'cancel'; got: " + names);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void importSpec_carriesOperationTagsThrough(@TempDir Path tmp) throws Exception {
        Path spec = copyFixture(tmp);
        Path outDir = tmp.resolve("scenarios");
        OpenApiImporter.importSpec(spec, outDir, new OpenApiImporter.Options());
        Map<String, Object> getUser = readScenario(outDir.resolve("getuser.yaml"));
        List<String> tags = (List<String>) getUser.get("tags");
        assertNotNull(tags);
        assertTrue(tags.contains("users"));
        assertTrue(tags.contains("read-only"));
    }

    @Test
    void importSpec_throwsOnMalformedSpec(@TempDir Path tmp) throws Exception {
        Path spec = tmp.resolve("bad.yaml");
        Files.writeString(spec, "this is not openapi");
        assertThrows(IOException.class,
            () -> OpenApiImporter.importSpec(spec, tmp.resolve("out"), new OpenApiImporter.Options()));
    }

    @Test
    void scenarioId_collapsesAndLowercases() {
        var op = new io.swagger.v3.oas.models.Operation();
        op.setOperationId(null);
        // Synthesised from method + path with placeholders → path params become "by-",
        // then any non-alphanumeric run collapses to a single "-".
        String id = OpenApiImporter.scenarioId("perf-", "GET", "/users/{userId}/orders", op);
        assertEquals("perf-get-users-by-orders", id);
    }
}
