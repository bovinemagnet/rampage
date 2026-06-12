package io.rampage.console.verification;

import io.quarkus.runtime.StartupEvent;
import io.rampage.console.config.PathResolver;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Stages the bundled verification config triple (env + run + scenario +
 * GraphQL query) from classpath onto disk at console startup, rewriting the
 * {@code __VERIFICATION_DIR__} placeholder to the actual extraction directory
 * so Rampage's {@code ConfigLoader} can find the cross-referenced files.
 *
 * The Verify button on the dashboard enqueues a run pointing at the staged
 * env/run paths, giving the user a zero-config self-test of the full pipeline.
 */
@ApplicationScoped
public class VerificationConfig {

    /**
     * Creates a new {@code VerificationConfig} instance.
     * The CDI container calls this constructor; the staging directory is
     * resolved in the {@link jakarta.annotation.PostConstruct} {@code init} method.
     */
    public VerificationConfig() {}

    private static final Logger log = LoggerFactory.getLogger(VerificationConfig.class);
    private static final String PLACEHOLDER = "__VERIFICATION_DIR__";

    @ConfigProperty(name = "rampage.console.repo-root")
    java.util.Optional<String> repoRootRaw;

    @ConfigProperty(name = "rampage.console.verification.staging-dir")
    java.util.Optional<String> stagingDirOverride;

    private volatile String stagingDir;
    private volatile Path envPath;
    private volatile Path runPath;

    @PostConstruct
    void init() {
        if (stagingDir != null) return;
        stagingDir = stagingDirOverride
                .filter(s -> !s.isBlank())
                .orElseGet(() -> {
                    String start = repoRootRaw.filter(s -> !s.isBlank())
                            .orElseGet(() -> System.getProperty("user.dir"));
                    return PathResolver.resolveRepoRoot(start)
                            .resolve("build/console-verification").toString();
                });
    }

    void onStart(@Observes StartupEvent ev) {
        try {
            stage();
        } catch (IOException e) {
            log.error("Verification staging failed; Verify button will be unavailable: {}", e.getMessage(), e);
        }
    }

    /**
     * Test seam — sets the staging directory before triggering a manual {@link #stage()} call.
     *
     * @param dir the absolute path to use as the staging directory.
     */
    public void setStagingDir(String dir) {
        this.stagingDir = dir;
    }

    /**
     * Copies the bundled verification config files from the classpath to the staging directory,
     * rewriting the {@code __VERIFICATION_DIR__} placeholder with the actual staging path.
     *
     * @throws IOException if any classpath resource is missing or a file cannot be written.
     */
    public void stage() throws IOException {
        Path stage = Paths.get(stagingDir).toAbsolutePath().normalize();
        Files.createDirectories(stage.resolve("scenarios"));
        Files.createDirectories(stage.resolve("graphql"));

        copyTemplated("verification/environment.yaml", stage.resolve("environment.yaml"), stage);
        copyTemplated("verification/run.yaml",         stage.resolve("run.yaml"),         stage);
        copyTemplated("verification/scenarios/echo.yaml", stage.resolve("scenarios/echo.yaml"), stage);
        copyVerbatim("verification/graphql/echo.graphql", stage.resolve("graphql/echo.graphql"));

        this.envPath = stage.resolve("environment.yaml");
        this.runPath = stage.resolve("run.yaml");
        log.info("Verification config staged at {}", stage);
    }

    /**
     * Returns {@code true} if the verification config has been successfully staged and
     * both the environment and run files exist on disk.
     *
     * @return {@code true} when the verification pipeline is ready to be enqueued.
     */
    public boolean isReady() {
        return envPath != null && runPath != null
                && Files.isRegularFile(envPath) && Files.isRegularFile(runPath);
    }

    /**
     * Returns the absolute path to the staged verification environment YAML file.
     *
     * @return the environment file path, or {@code null} if staging has not yet completed.
     */
    public Path envPath() {
        return envPath;
    }

    /**
     * Returns the absolute path to the staged verification run YAML file.
     *
     * @return the run file path, or {@code null} if staging has not yet completed.
     */
    public Path runPath() {
        return runPath;
    }

    private void copyTemplated(String classpath, Path dest, Path stage) throws IOException {
        String body = readClasspath(classpath);
        body = body.replace(PLACEHOLDER, stage.toString());
        Files.writeString(dest, body, StandardCharsets.UTF_8);
    }

    private void copyVerbatim(String classpath, Path dest) throws IOException {
        try (InputStream is = open(classpath)) {
            Files.copy(is, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String readClasspath(String classpath) throws IOException {
        try (InputStream is = open(classpath)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private InputStream open(String classpath) throws IOException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpath);
        if (is == null) {
            throw new IOException("Bundled verification resource missing: " + classpath);
        }
        return is;
    }
}
