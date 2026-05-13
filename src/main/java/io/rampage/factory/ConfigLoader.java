package io.rampage.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.rampage.config.model.EnvironmentConfig;
import io.rampage.config.model.RunConfig;
import io.rampage.config.model.ScenarioConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ConfigLoader {
    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);
    private final ObjectMapper mapper;

    public ConfigLoader() {
        this.mapper = new ObjectMapper(new YAMLFactory());
        this.mapper.registerModule(new Jdk8Module());
    }

    public EnvironmentConfig loadEnvironment(String resourcePath) {
        log.info("Loading environment config from: {}", resourcePath);
        try (InputStream is = getResourceStream(resourcePath)) {
            EnvironmentConfig config = mapper.readValue(is, EnvironmentConfig.class);
            if (config.getEnvironment() != null) {
                log.info("Loaded environment: {}", config.getEnvironment().getName());
            }
            return config;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load environment config from: " + resourcePath, e);
        }
    }

    public RunConfig loadRun(String resourcePath) {
        log.info("Loading run config from: {}", resourcePath);
        try (InputStream is = getResourceStream(resourcePath)) {
            RunConfig config = mapper.readValue(is, RunConfig.class);
            if (config.getRun() != null) {
                log.info("Loaded run: {}", config.getRun().getName());
            }
            return config;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load run config from: " + resourcePath, e);
        }
    }

    public ScenarioConfig loadScenario(String resourcePath) {
        log.info("Loading scenario config from: {}", resourcePath);
        try (InputStream is = getResourceStream(resourcePath)) {
            ScenarioConfig config = mapper.readValue(is, ScenarioConfig.class);
            if (config.getScenario() != null) {
                log.info("Loaded scenario: {}", config.getScenario().getName());
            }
            return config;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load scenario config from: " + resourcePath, e);
        }
    }

    public String loadResource(String resourcePath) {
        log.info("Loading resource: {}", resourcePath);
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
