package io.rampage.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-038 — end-to-end integration test for {@link io.rampage.simulation.RampageSimulation}.
 *
 * <p>Stands up a WireMock server, points a minimal smoke run at it via temporary YAML
 * configuration, drives Gatling in-process ({@code Gatling.fromArgs}, which runs synchronously
 * and does not call {@code System.exit}), and asserts that the simulation produced a report and
 * run metadata and that the requests Gatling sent are well-formed GraphQL envelopes.
 *
 * <p>Tagged {@code integration} so it is excluded from the fast {@code test} suite and run via the
 * dedicated {@code integrationTest} Gradle task (which adds the {@code gatling} source set, where
 * {@code RampageSimulation} lives, to the classpath).
 */
@Tag("integration")
class RampageSimulationWireMockIntegrationTest {

    private static final String SIMULATION_CLASS = "io.rampage.simulation.RampageSimulation";

    private WireMockServer wireMock;
    private String previousEnvProperty;
    private String previousRunProperty;

    @BeforeEach
    void startWireMock() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
        wireMock.stubFor(post(urlEqualTo("/graphql"))
            .willReturn(okJson("{\"data\":{\"__typename\":\"Query\"}}")));
        previousEnvProperty = System.getProperty("loadtest.env");
        previousRunProperty = System.getProperty("loadtest.run");
    }

    @AfterEach
    void stopWireMock() {
        restoreProperty("loadtest.env", previousEnvProperty);
        restoreProperty("loadtest.run", previousRunProperty);
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @Test
    void runsSmokeSimulationAgainstWireMockAndProducesReportAndMetadata(@TempDir Path tempDir) throws Exception {
        int port = wireMock.port();
        Path results = Files.createDirectories(tempDir.resolve("results"));
        Path scenariosDir = Files.createDirectories(tempDir.resolve("scenarios"));

        Path pingQuery = tempDir.resolve("ping.graphql");
        Files.writeString(pingQuery, "query Ping { __typename }\n");

        Path scenarioFile = scenariosDir.resolve("smoke.yaml");
        Files.writeString(scenarioFile, """
            id: smoke
            name: smoke
            protocol: graphql
            endpointRef: graphql
            operationName: Ping
            request:
              graphqlQueryFile: %s
              variables:
                probe: rampage
            checks:
              httpStatus: 200
            """.formatted(pingQuery.toAbsolutePath()));

        Path envFile = tempDir.resolve("environment.yaml");
        Files.writeString(envFile, """
            id: wiremock-it
            name: WireMock Integration Test
            baseUrls:
              graphql: http://localhost:%d
            http:
              connectTimeoutMillis: 2000
              requestTimeoutMillis: 5000
              followRedirects: false
              acceptHeader: application/json
              contentTypeHeader: application/json
            security:
              mode: none
            safety:
              allowProduction: false
              requireApprovalForMutatingRequests: false
            """.formatted(port));

        Path runFile = tempDir.resolve("run.yaml");
        Files.writeString(runFile, """
            id: wiremock-it-run
            name: wiremock-it-run
            environment: wiremock-it
            scenarios:
              - id: smoke
                file: %s
                enabled: true
                weight: 100
            execution:
              mode: open
              workload:
                type: smoke
                users: 5
            assertions:
              global:
                maxErrorPercentage: 100.0
            reporting:
              outputDirectory: %s
              writeRunMetadata: true
              redactSecrets: true
            safety:
              dryRun: false
            """.formatted(scenarioFile.toAbsolutePath(), results.toAbsolutePath()));

        System.setProperty("loadtest.env", envFile.toAbsolutePath().toString());
        System.setProperty("loadtest.run", runFile.toAbsolutePath().toString());

        int status = io.gatling.app.Gatling$.MODULE$.fromArgs(new String[]{
            "-s", SIMULATION_CLASS,
            "-rf", results.toAbsolutePath().toString()
        });

        assertThat(status).as("Gatling exit status").isZero();

        // run-metadata.json is written by RampageSimulation.before() into reporting.outputDirectory
        // (the results root), not the timestamped report subdirectory.
        assertThat(results.resolve("run-metadata.json"))
            .as("run metadata at the results root")
            .exists();

        // Gatling writes its HTML report into a rampagesimulation-<timestamp> subdirectory of -rf.
        try (Stream<Path> entries = Files.list(results)) {
            boolean reportExists = entries
                .filter(Files::isDirectory)
                .filter(dir -> dir.getFileName().toString().startsWith("rampagesimulation-"))
                .anyMatch(dir -> Files.exists(dir.resolve("index.html")));
            assertThat(reportExists).as("Gatling HTML report directory with index.html").isTrue();
        }

        // The smoke workload injects users:5 at once, so WireMock received the GraphQL POSTs.
        wireMock.verify(moreThanOrExactly(1), postRequestedFor(urlEqualTo("/graphql")));

        // Each request body is a valid GraphQL envelope carrying the query and a variables object.
        List<LoggedRequest> requests = wireMock.findAll(postRequestedFor(urlEqualTo("/graphql")));
        assertThat(requests).as("captured GraphQL requests").isNotEmpty();
        JsonNode body = new ObjectMapper().readTree(requests.get(0).getBodyAsString());
        assertThat(body.path("query").asText()).as("GraphQL query field").contains("__typename");
        assertThat(body.path("variables").isObject()).as("GraphQL variables object").isTrue();
        assertThat(body.path("variables").path("probe").asText()).isEqualTo("rampage");
    }

    /**
     * Regression test for the feeder-ordering bug: the JDBC feeder must be attached to the
     * scenario chain <em>before</em> the request, so its value is in the session when the
     * request body's {@code #{...}} placeholders are resolved. Before the fix the feeder was
     * appended after the request, so {@code #{userId}} resolved against an empty session,
     * every request failed to build, and zero requests reached WireMock.
     */
    @Test
    void feederValueReachesRequestBody(@TempDir Path tempDir) throws Exception {
        int port = wireMock.port();
        Path results = Files.createDirectories(tempDir.resolve("results"));
        Path scenariosDir = Files.createDirectories(tempDir.resolve("scenarios"));

        Path userQuery = tempDir.resolve("user.graphql");
        Files.writeString(userQuery, "query GetUser($userId: String) { __typename }\n");

        // H2 literal-returning feeder: column user_id is remapped to the userId session key.
        Path feederSql = tempDir.resolve("user-feeder.sql");
        Files.writeString(feederSql, "SELECT 'regression-user-42' AS user_id\n");

        Path scenarioFile = scenariosDir.resolve("fed.yaml");
        Files.writeString(scenarioFile, """
            id: fed
            name: fed
            protocol: graphql
            endpointRef: graphql
            operationName: GetUser
            request:
              graphqlQueryFile: %s
              variables:
                userId: ${feeder:userId}
            feeder:
              type: jdbc
              databaseRef: sourceData
              sqlFile: %s
              strategy: circular
              preload: true
              failIfEmpty: true
              columns:
                user_id:
                  type: string
                  required: true
                  sessionKey: userId
            checks:
              httpStatus: 200
            """.formatted(userQuery.toAbsolutePath(), feederSql.toAbsolutePath()));

        Path envFile = tempDir.resolve("environment.yaml");
        Files.writeString(envFile, """
            id: wiremock-it
            name: WireMock Integration Test
            baseUrls:
              graphql: http://localhost:%d
            http:
              connectTimeoutMillis: 2000
              requestTimeoutMillis: 5000
              followRedirects: false
              acceptHeader: application/json
              contentTypeHeader: application/json
            security:
              mode: none
            databases:
              sourceData:
                driverClassName: org.h2.Driver
                jdbcUrl: jdbc:h2:mem:feederregression;DB_CLOSE_DELAY=-1
                username:
                  source: plain
                  value: sa
                password:
                  source: plain
                  value: ""
                pool:
                  maximumPoolSize: 2
            safety:
              allowProduction: false
              requireApprovalForMutatingRequests: false
            """.formatted(port));

        Path runFile = tempDir.resolve("run.yaml");
        Files.writeString(runFile, """
            id: fed-run
            name: fed-run
            environment: wiremock-it
            scenarios:
              - id: fed
                file: %s
                enabled: true
                weight: 100
            execution:
              mode: open
              workload:
                type: smoke
                users: 3
            assertions:
              global:
                maxErrorPercentage: 100.0
            reporting:
              outputDirectory: %s
              writeRunMetadata: false
              redactSecrets: true
            safety:
              dryRun: false
            """.formatted(scenarioFile.toAbsolutePath(), results.toAbsolutePath()));

        System.setProperty("loadtest.env", envFile.toAbsolutePath().toString());
        System.setProperty("loadtest.run", runFile.toAbsolutePath().toString());

        int status = io.gatling.app.Gatling$.MODULE$.fromArgs(new String[]{
            "-s", SIMULATION_CLASS,
            "-rf", results.toAbsolutePath().toString()
        });
        assertThat(status).as("Gatling exit status").isZero();

        List<LoggedRequest> requests = wireMock.findAll(postRequestedFor(urlEqualTo("/graphql")));
        // If the feeder were attached after the request (the bug), the request would fail to
        // build and nothing would reach WireMock — so a non-empty capture is itself meaningful.
        assertThat(requests).as("captured GraphQL requests with a feeder").isNotEmpty();
        JsonNode body = new ObjectMapper().readTree(requests.get(0).getBodyAsString());
        assertThat(body.path("variables").path("userId").asText())
            .as("feeder value substituted into the request body")
            .isEqualTo("regression-user-42");
    }

    private static void restoreProperty(String key, String previousValue) {
        if (previousValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, previousValue);
        }
    }
}
