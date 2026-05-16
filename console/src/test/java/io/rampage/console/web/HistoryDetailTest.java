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
class HistoryDetailTest {

    @Inject
    StoredRunRepository repository;

    void seed() {
        QuarkusTransaction.requiringNew().run(() -> {
            StoredRun run = new StoredRun();
            run.id = "detail-run-1";
            run.name = "Detail Run";
            run.environmentId = "local";
            run.status = RunStatus.COMPLETED;
            run.source = RunSource.IMPORTED;
            run.startedAt = Instant.now();
            ScenarioStat s = new ScenarioStat();
            s.scenarioName = "Quick GET";
            s.p95Ms = 142.0;
            s.requestCount = 300L;
            run.addScenarioStat(s);
            repository.persist(run);
        });
    }

    @Test
    void detailPageShowsRunNameAndScenarioStats() {
        seed();
        given().when().get("/history/detail-run-1")
                .then().statusCode(200)
                .body(containsString("Detail Run"))
                .body(containsString("Quick GET"));
    }

    @Test
    void unknownRunReturns404() {
        given().when().get("/history/no-such-run").then().statusCode(404);
    }
}
