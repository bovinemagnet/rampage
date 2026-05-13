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

    private final EnvironmentConfig envConfig = configLoader.loadEnvironment();
    private final RunConfig runConfig = configLoader.loadRun();

    {
        List<ScenarioConfig> scenarioConfigs = new ArrayList<>();
        if (runConfig.getScenarios() != null) {
            for (ScenarioRef ref : runConfig.getScenarios()) {
                if (ref.isEnabled()) {
                    scenarioConfigs.add(configLoader.loadScenario("scenarios/" + ref.getId() + ".yaml"));
                }
            }
        }

        validator.validate(envConfig, runConfig, scenarioConfigs);

        String endpointRef = scenarioConfigs.isEmpty() ? null
            : scenarioConfigs.get(0).getEndpointRef();
        HttpProtocolBuilder httpProtocol = httpProtocolFactory.build(envConfig, secretResolver, endpointRef);

        List<PopulationBuilder> populations = new ArrayList<>();
        for (ScenarioConfig scenarioCfg : scenarioConfigs) {
            String queryFile = scenarioCfg.getRequest() != null
                ? scenarioCfg.getRequest().getGraphqlQueryFile() : null;
            String graphqlQuery = queryFile != null ? configLoader.loadResource(queryFile) : "";

            List<Map<String, Object>> feederData = Collections.emptyList();
            if (scenarioCfg.getFeeder() != null && envConfig.getDatabases() != null) {
                String dbRef = scenarioCfg.getFeeder().getDatabaseRef();
                DatabaseConfig db = dbRef != null ? envConfig.getDatabases().get(dbRef) : null;
                if (db != null) {
                    try {
                        feederData = feederFactory.loadFromSql(db, scenarioCfg.getFeeder(), secretResolver);
                    } catch (Exception e) {
                        log.warn("Failed to load feeder data: {}", e.getMessage());
                    }
                }
            }

            ScenarioBuilder scenarioBuilder = scenarioFactory.build(scenarioCfg, graphqlQuery);

            if (!feederData.isEmpty()) {
                scenarioBuilder = scenarioBuilder.feed(listFeeder(feederData).circular());
            }

            WorkloadConfig workload = null;
            if (scenarioCfg.getWorkload() != null && !scenarioCfg.getWorkload().isInheritFromRun()) {
                // Use scenario-level workload (not yet implemented)
            } else if (runConfig.getExecution() != null) {
                workload = runConfig.getExecution().getWorkload();
            }

            if (workload == null) {
                workload = new WorkloadConfig();
                workload.setType("smoke");
                workload.setUsers(1);
            }

            OpenInjectionStep[] injectionSteps = workloadFactory.buildInjection(workload);
            populations.add(scenarioBuilder.injectOpen(injectionSteps));
        }

        List<Assertion> assertions = buildAssertions(runConfig.getAssertions());

        setUp(populations.toArray(new PopulationBuilder[0]))
            .assertions(assertions.toArray(new Assertion[0]))
            .protocols(httpProtocol);
    }

    private List<Assertion> buildAssertions(AssertionsConfig assertionsConfig) {
        List<Assertion> assertions = new ArrayList<>();
        if (assertionsConfig != null && assertionsConfig.getGlobal() != null) {
            GlobalAssertionConfig global = assertionsConfig.getGlobal();
            if (global.getMaxResponseTimeP95Millis() > 0) {
                assertions.add(global().responseTime().percentile(95)
                    .lt((int) global.getMaxResponseTimeP95Millis()));
            }
            if (global.getMaxResponseTimeP99Millis() > 0) {
                assertions.add(global().responseTime().percentile(99)
                    .lt((int) global.getMaxResponseTimeP99Millis()));
            }
            if (global.getMaxErrorPercentage() > 0) {
                assertions.add(global().failedRequests().percent()
                    .lt(global.getMaxErrorPercentage()));
            }
        }
        return assertions;
    }
}
