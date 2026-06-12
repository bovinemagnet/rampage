package io.rampage.factory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.rampage.config.model.EnvironmentConfig;
import io.rampage.config.model.RunConfig;
import io.rampage.config.model.ScenarioConfig;
import io.rampage.config.model.ScenarioRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and deserialises Rampage YAML configuration files into their corresponding model objects.
 *
 * <p>Each load method follows a two-step resolution strategy: the system property
 * ({@code loadtest.env} / {@code loadtest.run}) is checked first; if absent, the classpath
 * resource at the given path is used. Scenario loading via
 * {@link #loadScenario(ScenarioRef)} additionally attempts the filesystem path declared in
 * the {@code ScenarioRef}, then the classpath, then the convention-based path
 * {@code scenarios/<id>.yaml}. A failure at every location throws a {@code RuntimeException}
 * that lists all attempted paths.
 *
 * <p>The underlying Jackson {@code ObjectMapper} is configured with
 * {@code FAIL_ON_UNKNOWN_PROPERTIES=false} so that unrecognised YAML keys are silently
 * ignored.
 */
public class ConfigLoader {
    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);
    private final ObjectMapper mapper;

    /**
     * Creates a {@code ConfigLoader} with a Jackson YAML mapper configured to ignore unknown
     * properties and to support {@code Optional} fields via the JDK 8 module.
     */
    public ConfigLoader() {
        this.mapper = new ObjectMapper(new YAMLFactory());
        this.mapper.registerModule(new Jdk8Module());
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Loads the environment configuration, preferring the filesystem path in the
     * {@code loadtest.env} system property; falls back to the classpath resource
     * {@code environment.yaml}.
     *
     * @return the deserialised {@code EnvironmentConfig}
     * @throws RuntimeException if the file cannot be found or parsed
     */
    public EnvironmentConfig loadEnvironment() {
        String path = System.getProperty("loadtest.env");
        if (path != null) {
            return loadEnvironmentFromFilesystem(path);
        }
        return loadEnvironment("environment.yaml");
    }

    /**
     * Loads the run configuration, preferring the filesystem path in the
     * {@code loadtest.run} system property; falls back to the classpath resource
     * {@code run.yaml}.
     *
     * @return the deserialised {@code RunConfig}
     * @throws RuntimeException if the file cannot be found or parsed
     */
    public RunConfig loadRun() {
        String path = System.getProperty("loadtest.run");
        if (path != null) {
            return loadRunFromFilesystem(path);
        }
        return loadRun("run.yaml");
    }

    /**
     * Loads the environment configuration from a classpath resource at the given path.
     *
     * @param resourcePath the classpath-relative path to the environment YAML file
     * @return the deserialised {@code EnvironmentConfig}
     * @throws RuntimeException if the resource is not found or cannot be parsed
     */
    public EnvironmentConfig loadEnvironment(String resourcePath) {
        log.info("Loading environment config from classpath: {}", resourcePath);
        try (InputStream is = getResourceStream(resourcePath)) {
            EnvironmentConfig config = mapper.readValue(is, EnvironmentConfig.class);
            log.info("Loaded environment: {}", config.getName());
            return config;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load environment config from: " + resourcePath, e);
        }
    }

    /**
     * Loads the environment configuration directly from the filesystem at the given path.
     *
     * @param filePath the absolute or relative filesystem path to the environment YAML file
     * @return the deserialised {@code EnvironmentConfig}
     * @throws RuntimeException if the file cannot be read or parsed
     */
    public EnvironmentConfig loadEnvironmentFromFilesystem(String filePath) {
        log.info("Loading environment config from filesystem: {}", filePath);
        try (InputStream is = new FileInputStream(filePath)) {
            EnvironmentConfig config = mapper.readValue(is, EnvironmentConfig.class);
            log.info("Loaded environment: {}", config.getName());
            return config;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load environment config from filesystem: " + filePath, e);
        }
    }

    /**
     * Loads the run configuration from a classpath resource at the given path.
     *
     * @param resourcePath the classpath-relative path to the run YAML file
     * @return the deserialised {@code RunConfig}
     * @throws RuntimeException if the resource is not found or cannot be parsed
     */
    public RunConfig loadRun(String resourcePath) {
        log.info("Loading run config from classpath: {}", resourcePath);
        try (InputStream is = getResourceStream(resourcePath)) {
            RunConfig config = mapper.readValue(is, RunConfig.class);
            log.info("Loaded run: {}", config.getName());
            return config;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load run config from: " + resourcePath, e);
        }
    }

    /**
     * Loads the run configuration directly from the filesystem at the given path.
     *
     * @param filePath the absolute or relative filesystem path to the run YAML file
     * @return the deserialised {@code RunConfig}
     * @throws RuntimeException if the file cannot be read or parsed
     */
    public RunConfig loadRunFromFilesystem(String filePath) {
        log.info("Loading run config from filesystem: {}", filePath);
        try (InputStream is = new FileInputStream(filePath)) {
            RunConfig config = mapper.readValue(is, RunConfig.class);
            log.info("Loaded run: {}", config.getName());
            return config;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load run config from filesystem: " + filePath, e);
        }
    }

    /**
     * Loads a scenario configuration from a classpath resource at the given path.
     *
     * @param resourcePath the classpath-relative path to the scenario YAML file
     * @return the deserialised {@code ScenarioConfig}
     * @throws RuntimeException if the resource is not found or cannot be parsed
     */
    public ScenarioConfig loadScenario(String resourcePath) {
        log.info("Loading scenario config from classpath: {}", resourcePath);
        try (InputStream is = getResourceStream(resourcePath)) {
            ScenarioConfig config = mapper.readValue(is, ScenarioConfig.class);
            log.info("Loaded scenario: {}", config.getName());
            return config;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load scenario config from: " + resourcePath, e);
        }
    }

    /**
     * Loads a scenario configuration directly from the filesystem at the given path.
     *
     * @param filePath the absolute or relative filesystem path to the scenario YAML file
     * @return the deserialised {@code ScenarioConfig}
     * @throws RuntimeException if the file cannot be read or parsed
     */
    public ScenarioConfig loadScenarioFromFilesystem(String filePath) {
        log.info("Loading scenario config from filesystem: {}", filePath);
        try (InputStream is = new FileInputStream(filePath)) {
            ScenarioConfig config = mapper.readValue(is, ScenarioConfig.class);
            log.info("Loaded scenario: {}", config.getName());
            return config;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load scenario config from filesystem: " + filePath, e);
        }
    }

    /**
     * Resolves and loads a scenario configuration using the unified lookup strategy.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Filesystem path declared in {@code ref.file} (if non-blank)</li>
     *   <li>Classpath resource at {@code ref.file} (if non-blank)</li>
     *   <li>Classpath resource at {@code scenarios/<ref.id>.yaml} (id-convention fallback)</li>
     * </ol>
     * A {@code RuntimeException} is thrown if all locations are exhausted; the message
     * lists every path that was attempted.
     *
     * @param ref the scenario reference from the run configuration; must not be {@code null}
     * @return the deserialised {@code ScenarioConfig}
     * @throws IllegalArgumentException if {@code ref} is {@code null}
     * @throws RuntimeException         if the scenario cannot be resolved from any location
     */
    public ScenarioConfig loadScenario(ScenarioRef ref) {
        if (ref == null) {
            throw new IllegalArgumentException("ScenarioRef must not be null");
        }
        List<String> attempts = new ArrayList<>();
        String file = ref.getFile();

        if (file != null && !file.isBlank()) {
            File fsFile = new File(file);
            attempts.add("filesystem: " + file);
            if (fsFile.exists() && fsFile.isFile()) {
                return loadScenarioFromFilesystem(file);
            }

            attempts.add("classpath: " + file);
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(file)) {
                if (is != null) {
                    log.info("Loading scenario config from classpath: {}", file);
                    ScenarioConfig config = mapper.readValue(is, ScenarioConfig.class);
                    log.info("Loaded scenario: {}", config.getName());
                    return config;
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to load scenario config from classpath: " + file, e);
            }
        }

        if (ref.getId() != null && !ref.getId().isBlank()) {
            String idPath = "scenarios/" + ref.getId() + ".yaml";
            attempts.add("classpath (id convention): " + idPath);
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(idPath)) {
                if (is != null) {
                    log.info("Loading scenario config from classpath: {}", idPath);
                    ScenarioConfig config = mapper.readValue(is, ScenarioConfig.class);
                    log.info("Loaded scenario: {}", config.getName());
                    return config;
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to load scenario config from classpath: " + idPath, e);
            }
        }

        throw new RuntimeException(
            "Failed to resolve scenario '" + ref.getId() + "'. Attempted: " + String.join("; ", attempts));
    }

    /**
     * Loads an arbitrary text resource, checking the filesystem first then the classpath.
     *
     * @param resourcePath the filesystem path or classpath-relative path of the resource
     * @return the full contents of the resource as a UTF-8 string
     * @throws RuntimeException if the resource cannot be found or read
     */
    public String loadResource(String resourcePath) {
        log.info("Loading resource: {}", resourcePath);
        File file = new File(resourcePath);
        if (file.exists()) {
            try {
                return Files.readString(Path.of(resourcePath), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load resource from filesystem: " + resourcePath, e);
            }
        }
        try (InputStream is = getResourceStream(resourcePath)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource: " + resourcePath, e);
        }
    }

    private InputStream getResourceStream(String resourcePath) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new RuntimeException("Resource not found on classpath: " + resourcePath);
        }
        return is;
    }
}
