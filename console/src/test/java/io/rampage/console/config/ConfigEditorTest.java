package io.rampage.console.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigEditorTest {

    @TempDir
    Path root;

    private ConfigBrowser browser;
    private ConfigEditor editor;

    private static final String VALID_ENV = """
            id: local
            name: Local
            baseUrls:
              rest: http://localhost:9090
            http:
              connectTimeoutMillis: 1000
              requestTimeoutMillis: 5000
            security:
              mode: none
            safety:
              allowProduction: false
            """;

    private static final String VALID_RUN = """
            id: smoke
            name: Smoke
            version: 1
            environment: local
            scenarios:
              - id: hello
                file: scenarios/hello.yaml
                enabled: true
                weight: 100
            execution:
              mode: open
              workload:
                type: smoke
            """;

    private static final String VALID_SCENARIO = """
            id: hello
            name: Hello
            protocol: graphql
            endpointRef: rest
            request:
              variables: {}
            checks:
              httpStatus: 200
            """;

    @BeforeEach
    void setUp() throws Exception {
        Files.createDirectories(root.resolve("environments"));
        Files.createDirectories(root.resolve("runs"));
        Files.createDirectories(root.resolve("scenarios"));

        Files.writeString(root.resolve("environments/local.yaml"), VALID_ENV);
        Files.writeString(root.resolve("runs/smoke.yaml"), VALID_RUN);
        Files.writeString(root.resolve("scenarios/hello.yaml"), VALID_SCENARIO);

        browser = new ConfigBrowser();
        browser.setConfigDir(root.toString());

        editor = new ConfigEditor();
        editor.browser = browser;
    }

    @Test
    void readReturnsCurrentFileContent() throws Exception {
        String body = editor.read("environments/local.yaml");
        assertThat(body).isEqualTo(VALID_ENV);
    }

    @Test
    void saveWritesValidYamlAndReturnsOk() throws Exception {
        String updated = VALID_ENV.replace("Local", "Local (renamed)");
        ValidationResult result = editor.validateAndSave("environments/local.yaml", updated);
        assertThat(result.ok()).as("save should succeed").isTrue();
        assertThat(Files.readString(root.resolve("environments/local.yaml")))
                .isEqualTo(updated);
    }

    @Test
    void saveRejectsMalformedYamlAndLeavesFileUnchanged() throws Exception {
        String original = Files.readString(root.resolve("environments/local.yaml"));
        ValidationResult result = editor.validateAndSave(
                "environments/local.yaml",
                "name: ok\n  bad-indent: [unclosed\n");
        assertThat(result.ok()).isFalse();
        assertThat(result.errors()).isNotEmpty();
        assertThat(Files.readString(root.resolve("environments/local.yaml")))
                .as("original preserved on failure")
                .isEqualTo(original);
    }

    @Test
    void saveRejectsRunWithMissingScenarioId() {
        String badRun = VALID_RUN.replace("id: hello", "id: nonexistent");
        ValidationResult result = editor.validateAndSave("runs/smoke.yaml", badRun);
        assertThat(result.ok()).isFalse();
        assertThat(result.errors())
                .anyMatch(s -> s.contains("nonexistent")
                        && s.contains("no corresponding ScenarioConfig"));
    }

    @Test
    void savePathTraversalRejected() {
        ValidationResult result = editor.validateAndSave("../escape.yaml", "id: x\n");
        assertThat(result.ok()).isFalse();
        assertThat(result.errors().get(0)).contains("escapes config root");
    }

    @Test
    void saveUnknownDirectoryRejected() {
        ValidationResult result = editor.validateAndSave("queries/foo.yaml", "id: x\n");
        assertThat(result.ok()).isFalse();
        assertThat(result.errors().get(0)).contains("does not match a known config kind");
    }
}
