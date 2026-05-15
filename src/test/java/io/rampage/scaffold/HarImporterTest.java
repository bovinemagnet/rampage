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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HarImporterTest {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private Path copyFixture(Path tmp) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("sample-capture.har")) {
            assertNotNull(is, "sample-capture.har missing from test resources");
            Path har = tmp.resolve("sample.har");
            Files.write(har, is.readAllBytes());
            return har;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readScenario(Path file) throws IOException {
        return YAML_MAPPER.readValue(file.toFile(), new TypeReference<Map<String, Object>>() {});
    }

    @Test
    void importHar_filtersOptionsAndStaticAssets(@TempDir Path tmp) throws Exception {
        Path har = copyFixture(tmp);
        Path out = tmp.resolve("scenarios/imported.yaml");
        int stepCount = HarImporter.importHar(har, out, new HarImporter.Options("imported"));
        assertEquals(3, stepCount,
            "expected 3 steps after dropping the OPTIONS preflight and the .js asset");
        assertTrue(Files.isRegularFile(out));
    }

    @Test
    void importHar_picksBusiestHostByDefault(@TempDir Path tmp) throws Exception {
        Path har = copyFixture(tmp);
        Path out = tmp.resolve("scenarios/imported.yaml");
        HarImporter.importHar(har, out, new HarImporter.Options("imported"));
        Map<String, Object> scenario = readScenario(out);
        assertTrue(scenario.get("name").toString().contains("api.example.com"),
            "expected name to mention the busiest host");
    }

    @Test
    @SuppressWarnings("unchecked")
    void importHar_capturesMethodPathAndQueryParams(@TempDir Path tmp) throws Exception {
        Path har = copyFixture(tmp);
        Path out = tmp.resolve("scenarios/imported.yaml");
        HarImporter.importHar(har, out, new HarImporter.Options("imported"));
        Map<String, Object> scenario = readScenario(out);
        List<Map<String, Object>> steps = (List<Map<String, Object>>) scenario.get("steps");

        Map<String, Object> getStep = steps.get(0);
        Map<String, Object> req = (Map<String, Object>) getStep.get("request");
        assertEquals("GET", req.get("method"));
        assertEquals("/users/42", req.get("path"));
        assertEquals("none", req.get("bodyType"));
        Map<String, String> qp = (Map<String, String>) req.get("queryParams");
        assertEquals("profile", qp.get("expand"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void importHar_inlinesPostBodyWithJsonBodyType(@TempDir Path tmp) throws Exception {
        Path har = copyFixture(tmp);
        Path out = tmp.resolve("scenarios/imported.yaml");
        HarImporter.importHar(har, out, new HarImporter.Options("imported"));
        Map<String, Object> scenario = readScenario(out);
        List<Map<String, Object>> steps = (List<Map<String, Object>>) scenario.get("steps");

        Map<String, Object> postStep = steps.stream()
            .filter(s -> "POST".equals(((Map<String, Object>) s.get("request")).get("method")))
            .findFirst().orElseThrow();
        Map<String, Object> req = (Map<String, Object>) postStep.get("request");
        assertEquals("/orders", req.get("path"));
        assertEquals("json", req.get("bodyType"));
        assertTrue(req.get("body").toString().contains("\"userId\":\"42\""));
    }

    @Test
    @SuppressWarnings("unchecked")
    void importHar_stripsSensitiveAndManagedHeaders(@TempDir Path tmp) throws Exception {
        Path har = copyFixture(tmp);
        Path out = tmp.resolve("scenarios/imported.yaml");
        HarImporter.importHar(har, out, new HarImporter.Options("imported"));
        Map<String, Object> scenario = readScenario(out);
        List<Map<String, Object>> steps = (List<Map<String, Object>>) scenario.get("steps");
        Map<String, Object> req = (Map<String, Object>) steps.get(0).get("request");
        Map<String, String> headers = (Map<String, String>) req.get("headers");
        assertNotNull(headers);
        // Authorization, Cookie should be stripped — they're framework- or secret-managed.
        assertFalse(headers.containsKey("Authorization"), "Authorization must not be inlined");
        assertFalse(headers.containsKey("Cookie"), "Cookie must not be inlined");
        assertEquals("application/json", headers.get("Accept"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void importHar_marksMutatingWhenAnyStepIsPostPutDelete(@TempDir Path tmp) throws Exception {
        Path har = copyFixture(tmp);
        Path out = tmp.resolve("scenarios/imported.yaml");
        HarImporter.importHar(har, out, new HarImporter.Options("imported"));
        Map<String, Object> scenario = readScenario(out);
        Map<String, Object> safety = (Map<String, Object>) scenario.get("safety");
        assertEquals(true, safety.get("mutating"));
    }

    @Test
    void importHar_methodFilterRestrictsToGetOnly(@TempDir Path tmp) throws Exception {
        Path har = copyFixture(tmp);
        Path out = tmp.resolve("scenarios/imported.yaml");
        HarImporter.Options opts = new HarImporter.Options("imported");
        opts.methodFilter = Set.of("GET");
        int stepCount = HarImporter.importHar(har, out, opts);
        assertEquals(2, stepCount, "expected only the two GET requests after method filter");
    }

    @Test
    void importHar_includeStaticKeepsAssetRequestsButHostFilterStillApplies(@TempDir Path tmp) throws Exception {
        Path har = copyFixture(tmp);
        Path out = tmp.resolve("scenarios/imported.yaml");
        HarImporter.Options opts = new HarImporter.Options("imported");
        opts.includeStatic = true;
        // Default host filter still keeps us on api.example.com — cdn.example.com asset
        // is excluded by host, not by static-asset filter.
        int stepCount = HarImporter.importHar(har, out, opts);
        assertEquals(3, stepCount);
    }

    @Test
    void importHar_throwsOnMalformedHar(@TempDir Path tmp) throws Exception {
        Path har = tmp.resolve("bad.har");
        Files.writeString(har, "{}");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> HarImporter.importHar(har, tmp.resolve("out.yaml"), new HarImporter.Options("x")));
        assertTrue(ex.getMessage().contains("log"));
    }
}
