package io.rampage.simulation;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import io.rampage.config.model.*;
import io.rampage.factory.*;
import io.rampage.reporting.ConfigSnapshotWriter;
import io.rampage.reporting.DryRunSummaryWriter;
import io.rampage.reporting.RunMetadataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class RampageSimulation extends Simulation {
    private static final Logger log = LoggerFactory.getLogger(RampageSimulation.class);

    private final ConfigLoader configLoader = new ConfigLoader();
    private final SecretResolver secretResolver = new SecretResolver();
    private final ConfigValidator validator = new ConfigValidator(secretResolver);
    private final HttpProtocolFactory httpProtocolFactory = new HttpProtocolFactory();
    private final WorkloadFactory workloadFactory = new WorkloadFactory();
    private final FeederFactory feederFactory = new FeederFactory(secretResolver);
    private final RunMetadataWriter runMetadataWriter = new RunMetadataWriter();
    private final DryRunSummaryWriter dryRunSummaryWriter = new DryRunSummaryWriter();
    private final ConfigSnapshotWriter configSnapshotWriter = new ConfigSnapshotWriter();
    private final DataSourceRegistry dataSourceRegistry = new DataSourceRegistry(secretResolver);

    private final EnvironmentConfig envConfig = configLoader.loadEnvironment();
    private final RunConfig runConfig = configLoader.loadRun();
    private final List<ScenarioConfig> activeScenarios = new ArrayList<>();
    private final Instant startedAt = Instant.now();
    private TokenProvider tokenProvider;
    private TokenRefresher tokenRefresher;
    private ScenarioFactory scenarioFactory;
    private final List<FeederFactory.StreamingFeeder> streamingFeeders = new ArrayList<>();
    private final Map<String, Object> feederRowCounts = new LinkedHashMap<>();

    {
        if (runConfig.getScenarios() != null) {
            for (ScenarioRef ref : runConfig.getScenarios()) {
                if (ref.isEnabled()) {
                    activeScenarios.add(configLoader.loadScenario(ref));
                }
            }
        }
        List<ScenarioConfig> scenarioConfigs = activeScenarios;

        List<String> placeholderErrors = PlaceholderSubstitutor.expandInPlace(envConfig, runConfig, scenarioConfigs, secretResolver);
        if (!placeholderErrors.isEmpty()) {
            throw new IllegalArgumentException("YAML placeholder expansion failed:\n - "
                + String.join("\n - ", placeholderErrors));
        }

        validator.validate(envConfig, runConfig, scenarioConfigs);

        if (isDryRun()) {
            String outputDir = resolveOutputDir();
            dryRunSummaryWriter.write(envConfig, runConfig, scenarioConfigs, outputDir);
            log.info("DRY RUN: configuration validated and summary written to {}/dry-run-summary.json. "
                + "Exiting before any traffic is generated.", outputDir);
            System.exit(0);
        }

        tokenProvider = TokenProvider.fromEnvironment(envConfig, secretResolver);
        scenarioFactory = new ScenarioFactory(
            () -> java.util.UUID.randomUUID().toString(),
            () -> tokenProvider.currentToken());

        Map<String, HttpProtocolBuilder> protocolsByRef = new LinkedHashMap<>();
        int totalInheritedWeight = totalInheritedWeight(runConfig, scenarioConfigs);

        List<PopulationBuilder> populations = new ArrayList<>();
        for (ScenarioConfig scenarioCfg : scenarioConfigs) {
            String queryFile = scenarioCfg.getRequest() != null
                ? scenarioCfg.getRequest().getGraphqlQueryFile() : null;
            String graphqlQuery = queryFile != null ? configLoader.loadResource(queryFile) : "";

            List<Map<String, Object>> feederData = Collections.emptyList();
            Iterator<Map<String, Object>> streamingFeeder = null;
            if (scenarioCfg.getFeeder() != null && envConfig.getDatabases() != null) {
                String dbRef = scenarioCfg.getFeeder().getDatabaseRef();
                DatabaseConfig db = dbRef != null ? envConfig.getDatabases().get(dbRef) : null;
                if (db != null) {
                    try {
                        if (scenarioCfg.getFeeder().isPreload()) {
                            feederData = feederFactory.loadFromSql(
                                dataSourceRegistry.getOrCreate(dbRef, db),
                                scenarioCfg.getFeeder());
                            feederRowCounts.put(scenarioCfg.getId(), feederData.size());
                        } else {
                            FeederFactory.StreamingFeeder sf = feederFactory.streamFromSql(
                                dataSourceRegistry.getOrCreate(dbRef, db),
                                scenarioCfg.getFeeder());
                            streamingFeeders.add(sf);
                            streamingFeeder = sf;
                            feederRowCounts.put(scenarioCfg.getId(), "streaming");
                        }
                    } catch (Exception e) {
                        log.warn("Failed to load feeder data: {}", e.getMessage());
                    }
                }
            }

            Map<String, String> effectiveHeaders = HeaderResolver.resolveScenarioHeaders(envConfig, runConfig, scenarioCfg);
            ScenarioBuilder scenarioBuilder = scenarioFactory.build(scenarioCfg, graphqlQuery, envConfig.getHttp(), effectiveHeaders);

            if (streamingFeeder != null) {
                scenarioBuilder = scenarioBuilder.feed(streamingFeeder);
            } else if (!feederData.isEmpty()) {
                String strategy = scenarioCfg.getFeeder() != null
                    ? scenarioCfg.getFeeder().getStrategy() : "circular";
                io.gatling.javaapi.core.FeederBuilder<Object> base = listFeeder(feederData);
                io.gatling.javaapi.core.FeederBuilder<?> feeder = switch (strategy == null ? "circular" : strategy.toLowerCase(java.util.Locale.ROOT)) {
                    case "queue" -> base.queue();
                    case "shuffle" -> base.shuffle();
                    case "random" -> base.random();
                    default -> base.circular();
                };
                scenarioBuilder = scenarioBuilder.feed(feeder);
            }

            String endpointRef = scenarioCfg.getEndpointRef() != null
                ? scenarioCfg.getEndpointRef() : "default";
            HttpProtocolBuilder scenarioProtocol = protocolsByRef.computeIfAbsent(endpointRef,
                ref -> httpProtocolFactory.build(envConfig, secretResolver, ref));

            WorkloadConfig workload = WorkloadFactory.effectiveWorkload(runConfig, scenarioCfg);
            if (inheritsFromRun(scenarioCfg) && totalInheritedWeight > 0) {
                int weight = findWeight(runConfig, scenarioCfg.getId());
                if (weight > 0) {
                    workload = WorkloadFactory.scaleWorkload(workload, (double) weight / totalInheritedWeight);
                }
            }
            PopulationBuilder population;
            if (isClosedMode()) {
                ClosedInjectionStep[] injectionSteps = workloadFactory.buildClosedInjection(workload);
                population = scenarioBuilder.injectClosed(injectionSteps);
            } else {
                OpenInjectionStep[] injectionSteps = workloadFactory.buildInjection(workload);
                population = scenarioBuilder.injectOpen(injectionSteps);
            }
            populations.add(population.protocols(scenarioProtocol));
        }

        List<Assertion> assertions = AssertionFactory.buildAll(runConfig.getAssertions(), scenarioConfigs);

        setUp(populations.toArray(new PopulationBuilder[0]))
            .assertions(assertions.toArray(new Assertion[0]));
    }

    @Override
    public void before() {
        ReportingConfig reporting = runConfig.getReporting();
        if (reporting != null && reporting.isWriteRunMetadata()) {
            String outputDir = reporting.getOutputDirectory();
            if (outputDir == null || outputDir.isBlank()) {
                outputDir = "build/reports/gatling";
            }
            try {
                runMetadataWriter.write(runConfig, envConfig, activeScenarios, outputDir, startedAt, feederRowCounts);
            } catch (Exception e) {
                log.warn("Failed to write run metadata: {}", e.getMessage());
            }
            if (reporting.isIncludeConfigSnapshot()) {
                try {
                    configSnapshotWriter.write(envConfig, runConfig, activeScenarios, outputDir,
                        secretResolver, reporting.isRedactSecrets());
                } catch (Exception e) {
                    log.warn("Failed to write config snapshot: {}", e.getMessage());
                }
            }
        }
        startTokenRefresher();
        dataSourceRegistry.logStats();
    }

    @Override
    public void after() {
        if (tokenRefresher != null) tokenRefresher.close();
        for (FeederFactory.StreamingFeeder sf : streamingFeeders) {
            try { sf.close(); } catch (Exception e) { log.warn("Error closing streaming feeder: {}", e.getMessage()); }
        }
        dataSourceRegistry.logStats();
        dataSourceRegistry.close();
    }

    private void startTokenRefresher() {
        if (!(tokenProvider instanceof OAuthClientCredentialsTokenProvider oauth)) return;
        SecurityConfig sec = envConfig.getSecurity();
        if (sec == null) return;
        Long interval = sec.getRefreshIntervalSeconds();
        if (interval == null || interval <= 0) return;
        tokenRefresher = new TokenRefresher(oauth, interval, TokenRefresher.parseFailureMode(sec.getOnRefreshFailure()));
        tokenRefresher.start();
    }

    private static boolean inheritsFromRun(ScenarioConfig sc) {
        return sc.getWorkload() == null || sc.getWorkload().isInheritFromRun();
    }

    private static int totalInheritedWeight(RunConfig run, List<ScenarioConfig> scenarios) {
        if (run == null || run.getScenarios() == null) return 0;
        Map<String, ScenarioConfig> byId = new HashMap<>();
        for (ScenarioConfig sc : scenarios) {
            if (sc.getId() != null) byId.put(sc.getId(), sc);
        }
        int total = 0;
        for (ScenarioRef ref : run.getScenarios()) {
            if (!ref.isEnabled()) continue;
            ScenarioConfig sc = byId.get(ref.getId());
            if (sc != null && inheritsFromRun(sc)) {
                total += Math.max(0, ref.getWeight());
            }
        }
        return total;
    }

    private static int findWeight(RunConfig run, String scenarioId) {
        if (run == null || run.getScenarios() == null || scenarioId == null) return 0;
        for (ScenarioRef ref : run.getScenarios()) {
            if (scenarioId.equals(ref.getId())) return Math.max(0, ref.getWeight());
        }
        return 0;
    }

    private boolean isClosedMode() {
        return runConfig.getExecution() != null
            && "closed".equalsIgnoreCase(runConfig.getExecution().getMode());
    }

    private boolean isDryRun() {
        if ("true".equalsIgnoreCase(System.getProperty("loadtest.dryRun"))) return true;
        return runConfig.getSafety() != null && runConfig.getSafety().isDryRun();
    }

    private String resolveOutputDir() {
        ReportingConfig reporting = runConfig.getReporting();
        String outputDir = reporting != null ? reporting.getOutputDirectory() : null;
        return (outputDir == null || outputDir.isBlank()) ? "build/reports/gatling" : outputDir;
    }

}
