package io.rampage.simulation;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import io.rampage.config.model.*;
import io.rampage.factory.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class RampageSimulation extends Simulation {
    private static final Logger log = LoggerFactory.getLogger(RampageSimulation.class);

    private final ConfigLoader configLoader = new ConfigLoader();
    private final SecretResolver secretResolver = new SecretResolver();
    private final ConfigValidator validator = new ConfigValidator();
    private final HttpProtocolFactory httpProtocolFactory = new HttpProtocolFactory();
    private final ScenarioFactory scenarioFactory = new ScenarioFactory();
    private final WorkloadFactory workloadFactory = new WorkloadFactory();
    private final FeederFactory feederFactory = new FeederFactory();

    private final EnvironmentConfig envConfig = configLoader.loadEnvironment("environment.yaml");
    private final RunConfig runConfig = configLoader.loadRun("run.yaml");

    {
        List<ScenarioConfig> scenarioConfigs = new ArrayList<>();
        for (String scenarioName : runConfig.getRun().getScenarios()) {
            scenarioConfigs.add(configLoader.loadScenario("scenarios/" + scenarioName + ".yaml"));
        }

        validator.validate(envConfig, runConfig, scenarioConfigs);

        HttpProtocolBuilder httpProtocol = httpProtocolFactory.build(
            envConfig.getEnvironment(), secretResolver);

        List<PopulationBuilder> populations = new ArrayList<>();
        for (ScenarioConfig scenarioConfig : scenarioConfigs) {
            ScenarioConfig.Scenario scenarioCfg = scenarioConfig.getScenario();

            String graphqlQuery = configLoader.loadResource(scenarioCfg.getGraphql().getQueryFile());

            List<Map<String, Object>> feederData = Collections.emptyList();
            if (scenarioCfg.getFeeder() != null) {
                try {
                    feederData = feederFactory.loadFromSql(
                        envConfig.getEnvironment().getDatabase(),
                        scenarioCfg.getFeeder(),
                        secretResolver
                    );
                } catch (Exception e) {
                    log.warn("Failed to load feeder data, continuing with empty feeder: {}", e.getMessage());
                }
            }

            ScenarioBuilder scenarioBuilder = scenarioFactory.build(scenarioCfg, graphqlQuery);

            if (!feederData.isEmpty()) {
                scenarioBuilder = scenarioBuilder.feed(listFeeder(feederData).circular());
            }

            WorkloadConfig workload = scenarioCfg.getWorkload() != null
                ? scenarioCfg.getWorkload()
                : runConfig.getRun().getWorkload();

            OpenInjectionStep[] injectionSteps = workloadFactory.buildInjection(workload);
            populations.add(scenarioBuilder.injectOpen(injectionSteps));
        }

        List<Assertion> assertions = buildAssertions(runConfig.getRun().getAssertions());

        setUp(populations.toArray(new PopulationBuilder[0]))
            .assertions(assertions.toArray(new Assertion[0]))
            .protocols(httpProtocol);
    }

    private List<Assertion> buildAssertions(AssertionConfig assertionConfig) {
        List<Assertion> assertions = new ArrayList<>();
        if (assertionConfig != null) {
            if (assertionConfig.getGlobalResponseTimeMaxMs() > 0) {
                assertions.add(global().responseTime().max()
                    .lt((int) assertionConfig.getGlobalResponseTimeMaxMs()));
            }
            if (assertionConfig.getGlobalResponseTimeMeanMs() > 0) {
                assertions.add(global().responseTime().mean()
                    .lt((int) assertionConfig.getGlobalResponseTimeMeanMs()));
            }
            if (assertionConfig.getGlobalErrorRatePercent() > 0) {
                assertions.add(global().failedRequests().percent()
                    .lt(assertionConfig.getGlobalErrorRatePercent()));
            }
        }
        return assertions;
    }
}
