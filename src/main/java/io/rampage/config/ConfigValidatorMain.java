package io.rampage.config;

import io.rampage.config.model.*;
import io.rampage.factory.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Command-line entry point for validating the Rampage load-test configuration without
 * executing a simulation.
 *
 * <p>Loads the environment, run, and all enabled scenario YAML files using
 * {@code ConfigLoader}, then passes them to {@code ConfigValidator}. Exits with code
 * {@code 0} on success, {@code 1} on validation failure or any other error.</p>
 *
 * <p>Configuration files are resolved using the same classpath and system-property
 * overrides as the simulation itself ({@code loadtest.env} and {@code loadtest.run}
 * system properties).</p>
 */
public class ConfigValidatorMain {
    private static final Logger log = LoggerFactory.getLogger(ConfigValidatorMain.class);

    /** Creates the validator entry point. */
    public ConfigValidatorMain() {}

    /**
     * Validates the load-test configuration and exits the JVM.
     *
     * <p>Exits with code {@code 0} if validation passes, or {@code 1} if validation
     * errors are found or an unexpected exception occurs.</p>
     *
     * @param args command-line arguments (not used; configuration is resolved via system
     *             properties and classpath)
     */
    public static void main(String[] args) {
        log.info("Starting load test configuration validation");
        try {
            ConfigLoader loader = new ConfigLoader();
            ConfigValidator validator = new ConfigValidator();

            EnvironmentConfig env = loader.loadEnvironment();
            RunConfig run = loader.loadRun();

            List<ScenarioConfig> scenarios = new ArrayList<>();
            if (run.getScenarios() != null) {
                for (ScenarioRef ref : run.getScenarios()) {
                    if (ref.isEnabled()) {
                        try {
                            scenarios.add(loader.loadScenario(ref));
                        } catch (Exception e) {
                            log.warn("Failed to load scenario '{}': {}", ref.getId(), e.getMessage());
                        }
                    }
                }
            }

            validator.validate(env, run, scenarios);
            log.info("Configuration validation PASSED");
            System.exit(0);
        } catch (ConfigValidator.ConfigValidationException e) {
            log.error("Configuration validation FAILED:");
            e.getErrors().forEach(err -> log.error("  - {}", err));
            System.exit(1);
        } catch (Exception e) {
            log.error("Configuration validation ERROR: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
