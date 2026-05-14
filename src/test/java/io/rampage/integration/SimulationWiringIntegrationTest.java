package io.rampage.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.rampage.config.model.*;
import io.rampage.factory.*;
import io.rampage.reporting.ConfigSnapshotWriter;
import io.rampage.reporting.DryRunSummaryWriter;
import io.rampage.reporting.RunMetadataWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end wiring integration: drives every factory + writer in the M1-M4 stack
 * against an in-memory JDK HTTP server (acting as the OAuth token endpoint) and an
 * H2-backed feeder. Does not invoke Gatling's actual injection lifecycle — that
 * requires the Gatling runner and is out of scope for the unit test source set.
 */
class SimulationWiringIntegrationTest {
    private HttpServer tokenServer;
    private int tokenPort;
    private FeederFactory feederFactory;
    private DataSourceRegistry registry;

    @BeforeEach
    void setUp() throws Exception {
        tokenServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        tokenPort = tokenServer.getAddress().getPort();
        tokenServer.createContext("/token", exchange -> {
            byte[] body = "{\"access_token\":\"integ-token-xyz\",\"expires_in\":3600}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
        });
        tokenServer.start();

        feederFactory = new FeederFactory();
        registry = new DataSourceRegistry(new SecretResolver());

        Class.forName("org.h2.Driver");
        try (var conn = java.sql.DriverManager.getConnection(
                "jdbc:h2:mem:integration;DB_CLOSE_DELAY=-1", "sa", "")) {
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS users (id VARCHAR(36), email VARCHAR(255))");
            conn.createStatement().execute("DELETE FROM users");
            conn.createStatement().execute("INSERT INTO users VALUES ('u-1', 'alice@example.com')");
            conn.createStatement().execute("INSERT INTO users VALUES ('u-2', 'bob@example.com')");
        }
    }

    @AfterEach
    void tearDown() {
        if (tokenServer != null) tokenServer.stop(0);
        if (registry != null) registry.close();
    }

    private DatabaseConfig h2Db() {
        DatabaseConfig db = new DatabaseConfig();
        db.setDriverClassName("org.h2.Driver");
        db.setJdbcUrl("jdbc:h2:mem:integration;DB_CLOSE_DELAY=-1");
        CredentialConfig user = new CredentialConfig();
        user.setSource("plain");
        user.setValue("sa");
        db.setUsername(user);
        CredentialConfig pwd = new CredentialConfig();
        pwd.setSource("plain");
        pwd.setValue("");
        db.setPassword(pwd);
        return db;
    }

    private EnvironmentConfig buildEnv() {
        EnvironmentConfig env = new EnvironmentConfig();
        env.setId("integration");
        env.setName("Integration");
        env.setBaseUrls(Map.of("graphql", "http://localhost:9999/graphql"));
        HttpConfig http = new HttpConfig();
        http.setConnectTimeoutMillis(2000);
        http.setRequestTimeoutMillis(5000);
        env.setHttp(http);
        SafetyConfig safety = new SafetyConfig();
        safety.setAllowProduction(false);
        env.setSafety(safety);
        env.setDatabases(Map.of("sourceData", h2Db()));

        SecurityConfig sec = new SecurityConfig();
        sec.setMode("oauth-client-credentials");
        sec.setTokenUrl("http://127.0.0.1:" + tokenPort + "/token");
        CredentialConfig cid = new CredentialConfig();
        cid.setSource("plain");
        cid.setValue("my-client");
        sec.setClientId(cid);
        CredentialConfig csec = new CredentialConfig();
        csec.setSource("plain");
        csec.setValue("my-secret");
        sec.setClientSecret(csec);
        env.setSecurity(sec);

        ObservabilityConfig obs = new ObservabilityConfig();
        obs.setCorrelationIdHeader("X-Correlation-Id");
        env.setObservability(obs);
        return env;
    }

    private RunConfig buildRun() {
        RunConfig run = new RunConfig();
        run.setId("integration-run");
        run.setName("Integration Run");
        ScenarioRef ref = new ScenarioRef();
        ref.setId("get-users");
        ref.setEnabled(true);
        ref.setWeight(100);
        run.setScenarios(List.of(ref));
        ExecutionConfig exec = new ExecutionConfig();
        WorkloadConfig wl = new WorkloadConfig();
        wl.setType("ramp-and-hold");
        RateConfig rate = new RateConfig();
        rate.setFrom(0);
        rate.setTo(10);
        wl.setRate(rate);
        wl.setRampUp("10s");
        wl.setHoldFor("30s");
        exec.setWorkload(wl);
        run.setExecution(exec);
        AssertionsConfig assertions = new AssertionsConfig();
        GlobalAssertionConfig g = new GlobalAssertionConfig();
        g.setMaxResponseTimeP95Millis(2000);
        g.setMaxErrorPercentage(1.0);
        assertions.setGlobal(g);
        run.setAssertions(assertions);
        ReportingConfig reporting = new ReportingConfig();
        reporting.setWriteRunMetadata(true);
        reporting.setIncludeConfigSnapshot(true);
        reporting.setRedactSecrets(true);
        run.setReporting(reporting);
        return run;
    }

    private ScenarioConfig buildScenario() {
        ScenarioConfig sc = new ScenarioConfig();
        sc.setId("get-users");
        sc.setName("Get Users");
        sc.setProtocol("graphql");
        sc.setEndpointRef("graphql");
        sc.setOperationName("GetUser");
        sc.setHeaders(Map.of("X-Scenario-Id", "get-users"));
        RequestConfig req = new RequestConfig();
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("userId", "${feeder:userId}");
        vars.put("includeInactive", false);
        req.setVariables(vars);
        sc.setRequest(req);

        FeederConfig feeder = new FeederConfig();
        feeder.setType("jdbc");
        feeder.setDatabaseRef("sourceData");
        ColumnConfig col = new ColumnConfig();
        col.setRequired(true);
        col.setSessionKey("userId");
        feeder.setColumns(Map.of("id", col));
        sc.setFeeder(feeder);

        ChecksConfig checks = new ChecksConfig();
        checks.setHttpStatus(200);
        sc.setChecks(checks);
        sc.setTags(List.of("graphql", "user"));
        return sc;
    }

    @Test
    void fullWiring_loadsValidatesAndProducesArtifacts(@TempDir Path tempDir) throws Exception {
        EnvironmentConfig env = buildEnv();
        RunConfig run = buildRun();
        ScenarioConfig sc = buildScenario();
        SecretResolver secrets = new SecretResolver();

        // 1. Placeholder expansion (no placeholders here, but the pass must succeed).
        var phErrors = PlaceholderSubstitutor.expandInPlace(env, run, List.of(sc), secrets);
        assertTrue(phErrors.isEmpty());

        // 2. Validation.
        new ConfigValidator(secrets).validate(env, run, List.of(sc));

        // 3. Header layering + AuthZ protection.
        Map<String, String> headers = HeaderResolver.resolveScenarioHeaders(env, run, sc);
        assertEquals("get-users", headers.get("X-Scenario-Id"));

        // 4. OAuth token fetched once.
        TokenProvider provider = TokenProvider.fromEnvironment(env, secrets);
        assertEquals("integ-token-xyz", provider.currentToken());

        // 5. Feeder preload with column validation + sessionKey remap.
        FeederConfig sqlFeeder = new FeederConfig();
        sqlFeeder.setSqlFile(writeSql(tempDir, "SELECT id, email FROM users ORDER BY id"));
        ColumnConfig idCol = new ColumnConfig();
        idCol.setRequired(true);
        idCol.setSessionKey("userId");
        ColumnConfig emailCol = new ColumnConfig();
        emailCol.setSensitive(true);
        sqlFeeder.setColumns(Map.of("id", idCol, "email", emailCol));

        FeederFactory tracking = new FeederFactory(secrets);
        List<Map<String, Object>> rows = tracking.loadFromSql(
            registry.getOrCreate("integration", h2Db()), sqlFeeder);
        assertEquals(2, rows.size());
        assertTrue(rows.get(0).containsKey("userId"), "Remapped to sessionKey");
        assertTrue(secrets.getSensitiveValues().contains("alice@example.com"),
            "Sensitive feeder values tracked for redaction");

        // 6. Scenario body building (typed variables preserved).
        String body = ScenarioFactory.buildRequestBody(sc, "query GetUser($userId: ID!, $includeInactive: Boolean!) { user(id: $userId) { id } }");
        Map<String, Object> parsed = new ObjectMapper().readValue(body, new TypeReference<>() {});
        assertEquals("GetUser", parsed.get("operationName"));
        Map<?, ?> vars = (Map<?, ?>) parsed.get("variables");
        assertEquals("#{userId}", vars.get("userId"));
        assertEquals(Boolean.FALSE, vars.get("includeInactive"));

        // 7. Workload + assertions.
        WorkloadConfig effective = WorkloadFactory.effectiveWorkload(run, sc);
        assertEquals("ramp-and-hold", effective.getType());
        var assertionList = AssertionFactory.buildAll(run.getAssertions(), List.of(sc));
        assertEquals(2, assertionList.size());

        // 8. Run metadata + config snapshot writers.
        new RunMetadataWriter().write(run, env, List.of(sc), tempDir.toString(),
            Instant.now(), Map.of("get-users", rows.size()));
        new ConfigSnapshotWriter().write(env, run, List.of(sc), tempDir.toString(), secrets, true);

        Path metadata = tempDir.resolve("run-metadata.json");
        assertTrue(Files.exists(metadata));
        String metadataJson = Files.readString(metadata);
        assertTrue(metadataJson.contains("\"feederRowCount\" : 2"));
        assertTrue(metadataJson.contains("\"effectiveWorkload\""));
        assertTrue(metadataJson.contains("ramp-and-hold"));

        Path snapshot = tempDir.resolve("config-snapshot.yaml");
        assertTrue(Files.exists(snapshot));
        String yaml = Files.readString(snapshot);
        assertTrue(yaml.contains("environment:"));
        assertTrue(yaml.contains("***REDACTED***"),
            "Sensitive feeder values must be redacted from the snapshot");
        assertFalse(yaml.contains("alice@example.com"));

        // 9. Dry-run summary writer also produces useful output for the same config.
        new DryRunSummaryWriter().write(env, run, List.of(sc), tempDir.toString());
        assertTrue(Files.exists(tempDir.resolve("dry-run-summary.json")));
    }

    private String writeSql(Path tempDir, String sql) throws IOException {
        Path file = tempDir.resolve("feeder.sql");
        Files.writeString(file, sql);
        return file.toString();
    }
}
