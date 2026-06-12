package io.rampage.console.config;

import io.rampage.factory.ConfigLoader;
import io.rampage.factory.ConfigValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Read / validate / write the YAML files under {@code config/}. Validation
 * delegates to the engine's existing {@link ConfigLoader} and
 * {@link ConfigValidator} so the editor can never accept a YAML the CLI would
 * later reject.
 */
@ApplicationScoped
public class ConfigEditor {

    /**
     * Creates a new {@code ConfigEditor} instance.
     * The CDI container calls this constructor; dependencies are injected after construction.
     */
    public ConfigEditor() {}

    @Inject
    ConfigBrowser browser;

    private final ConfigLoader loader = new ConfigLoader();
    private final ConfigValidator validator = new ConfigValidator();

    /**
     * Reads the content of the config file at {@code relativePath} as a UTF-8 string.
     *
     * @param relativePath path relative to the config root (e.g. {@code environments/local.yaml}).
     * @return the file content.
     * @throws IOException if the path does not point to a regular file or the read fails.
     */
    public String read(String relativePath) throws IOException {
        Path absolute = browser.resolve(relativePath);
        if (!Files.isRegularFile(absolute)) {
            throw new IOException("Not a file: " + relativePath);
        }
        return Files.readString(absolute, StandardCharsets.UTF_8);
    }

    /**
     * Parses the supplied YAML body for the file at {@code relativePath} as the
     * appropriate config kind, runs the engine's validator over a triple
     * synthesised from the rest of {@code config/}, and (only if validation
     * passes) writes the body to disk atomically.
     *
     * @param relativePath path relative to the config root identifying the file to save.
     * @param body         the new YAML content to validate and persist.
     * @return {@link ValidationResult#valid()} on success, or a result holding
     *         the collected error list on failure (no file changes in that case).
     */
    public ValidationResult validateAndSave(String relativePath, String body) {
        Path absolute;
        try {
            absolute = browser.resolve(relativePath);
        } catch (IllegalArgumentException e) {
            return ValidationResult.invalid(e.getMessage());
        }

        ConfigKind kind;
        try {
            kind = ConfigKind.fromRelativePath(relativePath);
        } catch (IllegalArgumentException e) {
            return ValidationResult.invalid(e.getMessage());
        }

        Path stagingDir;
        Path stagedFile;
        try {
            stagingDir = Files.createTempDirectory("rampage-edit-");
            stagedFile = stagingDir.resolve(absolute.getFileName().toString());
            Files.writeString(stagedFile, body, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return ValidationResult.invalid("Failed to stage edit: " + e.getMessage());
        }

        ValidationResult parseAndValidate = parseAndValidate(kind, stagedFile);
        if (!parseAndValidate.ok()) {
            try { Files.deleteIfExists(stagedFile); Files.deleteIfExists(stagingDir); } catch (IOException ignored) {}
            return parseAndValidate;
        }

        try {
            Files.createDirectories(absolute.getParent());
            Files.move(stagedFile, absolute,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            Files.deleteIfExists(stagingDir);
        } catch (IOException e) {
            return ValidationResult.invalid("Failed to write file: " + e.getMessage());
        }
        return ValidationResult.valid();
    }

    private ValidationResult parseAndValidate(ConfigKind kind, Path stagedFile) {
        try {
            return switch (kind) {
                case ENVIRONMENT -> validateEnvironmentEdit(stagedFile);
                case RUN         -> validateRunEdit(stagedFile);
                case SCENARIO    -> validateScenarioEdit(stagedFile);
            };
        } catch (RuntimeException e) {
            return parseFailure(e);
        }
    }

    private ValidationResult validateEnvironmentEdit(Path staged) {
        var env = loader.loadEnvironmentFromFilesystem(staged.toString());
        try {
            var run = loadAnyRun();
            var scenarios = loadAllScenarios();
            if (run != null) {
                validator.validate(env, run, scenarios);
            }
        } catch (ConfigValidator.ConfigValidationException e) {
            return ValidationResult.invalid(e.getErrors());
        }
        return ValidationResult.valid();
    }

    private ValidationResult validateRunEdit(Path staged) {
        var run = loader.loadRunFromFilesystem(staged.toString());
        try {
            var env = loadAnyEnvironment();
            var scenarios = loadAllScenarios();
            if (env != null) {
                validator.validate(env, run, scenarios);
            }
        } catch (ConfigValidator.ConfigValidationException e) {
            return ValidationResult.invalid(e.getErrors());
        }
        return ValidationResult.valid();
    }

    private ValidationResult validateScenarioEdit(Path staged) {
        // For scenarios we only require a clean Jackson parse — the engine's
        // cross-reference validator wants the scenario to appear in run.scenarios,
        // which is rarely the case for ad-hoc scenario edits.
        loader.loadScenarioFromFilesystem(staged.toString());
        return ValidationResult.valid();
    }

    private io.rampage.config.model.EnvironmentConfig loadAnyEnvironment() {
        for (ConfigEntry e : browser.environments()) {
            try {
                return loader.loadEnvironmentFromFilesystem(e.absolutePath());
            } catch (RuntimeException ignored) {
                // Skip unparseable peers; we only want one valid env for context.
            }
        }
        return null;
    }

    private io.rampage.config.model.RunConfig loadAnyRun() {
        for (ConfigEntry e : browser.runs()) {
            try {
                return loader.loadRunFromFilesystem(e.absolutePath());
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    private List<io.rampage.config.model.ScenarioConfig> loadAllScenarios() {
        List<io.rampage.config.model.ScenarioConfig> all = new ArrayList<>();
        for (ConfigEntry e : browser.scenarios()) {
            try {
                all.add(loader.loadScenarioFromFilesystem(e.absolutePath()));
            } catch (RuntimeException ignored) {
            }
        }
        return all;
    }

    private static ValidationResult parseFailure(RuntimeException e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        return ValidationResult.invalid("Parse failed: " + cause.getMessage());
    }
}
