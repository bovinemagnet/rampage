package io.rampage.console.verification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationConfigTest {

    @TempDir
    Path stagingRoot;

    private VerificationConfig config;

    @BeforeEach
    void setUp() {
        config = new VerificationConfig();
        config.setStagingDir(stagingRoot.toString());
    }

    @Test
    void stageExtractsBundledResources() throws IOException {
        config.stage();

        assertThat(Files.isRegularFile(stagingRoot.resolve("environment.yaml"))).isTrue();
        assertThat(Files.isRegularFile(stagingRoot.resolve("run.yaml"))).isTrue();
        assertThat(Files.isRegularFile(stagingRoot.resolve("scenarios/echo.yaml"))).isTrue();
        assertThat(Files.isRegularFile(stagingRoot.resolve("graphql/echo.graphql"))).isTrue();
    }

    @Test
    void placeholderIsRewrittenToAbsoluteStagingPath() throws IOException {
        config.stage();

        String run = Files.readString(stagingRoot.resolve("run.yaml"));
        assertThat(run)
                .doesNotContain("__VERIFICATION_DIR__")
                .contains(stagingRoot.toAbsolutePath().normalize().toString());

        String scenario = Files.readString(stagingRoot.resolve("scenarios/echo.yaml"));
        assertThat(scenario)
                .doesNotContain("__VERIFICATION_DIR__")
                .contains(stagingRoot.toAbsolutePath().normalize().toString() + "/graphql/echo.graphql");
    }

    @Test
    void graphqlFileCopiedVerbatim() throws IOException {
        config.stage();
        String query = Files.readString(stagingRoot.resolve("graphql/echo.graphql"));
        assertThat(query)
                .contains("query Echo($msg: String!)")
                .contains("echo(msg: $msg)");
    }

    @Test
    void readyAfterStage() throws IOException {
        assertThat(config.isReady()).isFalse();
        config.stage();
        assertThat(config.isReady()).isTrue();
        assertThat(config.envPath()).exists();
        assertThat(config.runPath()).exists();
    }
}
