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
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class HistoryTrendsTest {

    @Inject
    StoredRunRepository repository;

    void seed(String id, Instant started, double p95) {
        QuarkusTransaction.requiringNew().run(() -> {
            StoredRun run = new StoredRun();
            run.id = id;
            run.name = id;
            run.status = RunStatus.COMPLETED;
            run.source = RunSource.CONSOLE;
            run.runConfigKey = "local::smoke";
            run.startedAt = started;
            ScenarioStat s = new ScenarioStat();
            s.scenarioName = "Quick GET";
            s.p95Ms = p95;
            s.errorPercent = 0.0;
            s.requestsPerSecond = 50.0;
            run.addScenarioStat(s);
            repository.persist(run);
        });
    }

    @Test
    void trendDataBuildsAJsonBundleInTimeOrder() {
        StoredRun a = new StoredRun();
        a.startedAt = Instant.parse("2026-05-10T10:00:00Z");
        ScenarioStat s = new ScenarioStat();
        s.p95Ms = 120.0;
        s.requestsPerSecond = 40.0;
        s.errorPercent = 1.0;
        a.addScenarioStat(s);

        String json = TrendData.toJson(List.of(a));

        assertThat(json).contains("\"p95\":[120.0]");
        assertThat(json).contains("\"x\":[" + Instant.parse("2026-05-10T10:00:00Z").getEpochSecond());
    }

    @Test
    void trendDataEmitsNullRpsForRunWithNoScenarioStats() {
        StoredRun a = new StoredRun();
        a.startedAt = Instant.parse("2026-05-10T10:00:00Z");
        // No scenario stats at all — a run that produced no report.

        String json = TrendData.toJson(List.of(a));

        assertThat(json).contains("\"rps\":[null]");
        assertThat(json).contains("\"p95\":[null]");
    }

    @Test
    void trendsPageRendersEmptyStateWhenNoConfigSelected() {
        given().when().get("/history/trends")
                .then().statusCode(200)
                .body(containsString("Trends need at least one"));
    }

    @Test
    void trendsPageRendersChartForAConfigKey() {
        seed("trend-a", Instant.parse("2026-05-09T10:00:00Z"), 110.0);
        seed("trend-b", Instant.parse("2026-05-11T10:00:00Z"), 140.0);

        given().queryParam("runConfigKey", "local::smoke")
                .when().get("/history/trends")
                .then().statusCode(200)
                .body(containsString("trend-chart"))
                .body(containsString("\"p95\""));
    }
}
