package io.rampage.config;

import io.rampage.config.model.*;
import io.rampage.factory.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ConfigValidatorMain {
    private static final Logger log = LoggerFactory.getLogger(ConfigValidatorMain.class);

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
