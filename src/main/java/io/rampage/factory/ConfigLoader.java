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

public class ConfigLoader {
    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);
    private final ObjectMapper mapper;

    public ConfigLoader() {
        this.mapper = new ObjectMapper(new YAMLFactory());
        this.mapper.registerModule(new Jdk8Module());
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public EnvironmentConfig loadEnvironment() {
        String path = System.getProperty("loadtest.env");
        if (path != null) {
            return loadEnvironmentFromFilesystem(path);
        }
        return loadEnvironment("environment.yaml");
    }

    public RunConfig loadRun() {
        String path = System.getProperty("loadtest.run");
        if (path != null) {
            return loadRunFromFilesystem(path);
        }
        return loadRun("run.yaml");
    }

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
