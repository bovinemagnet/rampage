package io.rampage.console.web;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.rampage.console.orchestrator.RunStatus;
import io.rampage.console.results.RunSource;
import io.rampage.console.results.ScenarioStat;
import io.rampage.console.results.StoredRun;
import io.rampage.console.results.StoredRunRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class HistoryCompareTest {

    @Inject
    StoredRunRepository repository;

    void seed(String id, double p95) {
        QuarkusTransaction.requiringNew().run(() -> {
            StoredRun run = new StoredRun();
            run.id = id;
            run.name = id;
            run.status = RunStatus.COMPLETED;
            run.source = RunSource.CONSOLE;
            run.startedAt = Instant.now();
            ScenarioStat s = new ScenarioStat();
            s.scenarioName = "Quick GET";
            s.p95Ms = p95;
            s.p99Ms = p95 * 1.2;
            s.meanMs = p95 * 0.6;
            s.errorPercent = 0.0;
            s.requestsPerSecond = 50.0;
            s.requestCount = 1000L;
            run.addScenarioStat(s);
            repository.persist(run);
        });
    }

    @Test
    void comparePageShowsBothPickersWithNoSelection() {
        given().when().get("/history/compare")
                .then().statusCode(200).body(containsString("Compare runs"));
    }

    @Test
    void comparePageReturns200ForUnknownRunIds() {
        given().queryParam("a", "no-such-a").queryParam("b", "no-such-b")
                .when().get("/history/compare")
                .then().statusCode(200);
    }

    @Test
    void comparePageRendersDiffAndRegressionVerdict() {
        seed("cmp-a", 100.0);
        seed("cmp-b", 150.0);

        given().queryParam("a", "cmp-a").queryParam("b", "cmp-b")
                .when().get("/history/compare")
                .then().statusCode(200)
                .body(containsString("Quick GET"))
                .body(containsString("Regression"));
    }
}
