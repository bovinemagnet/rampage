package io.rampage.console.web;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.rampage.console.orchestrator.RunStatus;
import io.rampage.console.results.RunSource;
import io.rampage.console.results.StoredRun;
import io.rampage.console.results.StoredRunRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@QuarkusTest
class HistoryTaggingTest {

    @Inject
    StoredRunRepository repository;

    /** Seed must commit in its own transaction so the running app sees it. */
    void seed(String id) {
        QuarkusTransaction.requiringNew().run(() -> {
            StoredRun run = new StoredRun();
            run.id = id;
            run.name = "Seed";
            run.status = RunStatus.COMPLETED;
            run.source = RunSource.CONSOLE;
            run.startedAt = Instant.now();
            repository.persist(run);
        });
    }

    @Test
    void addThenRemoveTagUpdatesTheStoredRun() {
        seed("tag-run-1");

        given().formParam("tag", "nightly")
                .when().post("/history/tag-run-1/tags")
                .then().statusCode(200).body(containsString("nightly"));

        assertThat(reload("tag-run-1").tags).contains("nightly");

        given().when().delete("/history/tag-run-1/tags/nightly")
                .then().statusCode(200);

        assertThat(reload("tag-run-1").tags).doesNotContain("nightly");
    }

    @Test
    void savingNotesPersistsThem() {
        seed("note-run-1");

        given().formParam("notes", "Investigated the P95 spike")
                .when().post("/history/note-run-1/notes")
                .then().statusCode(200);

        assertThat(reload("note-run-1").notes).isEqualTo("Investigated the P95 spike");
    }

    @Test
    void unknownRunReturns404() {
        given().formParam("tag", "x")
                .when().post("/history/no-such-run/tags")
                .then().statusCode(404);
    }

    @Test
    void rejectsTagContainingASlash() {
        seed("tag-run-2");

        given().formParam("tag", "ci/cd")
                .when().post("/history/tag-run-2/tags")
                .then().statusCode(200).body(not(containsString("ci/cd")));

        assertThat(reload("tag-run-2").tags).doesNotContain("ci/cd");
    }

    StoredRun reload(String id) {
        return QuarkusTransaction.requiringNew().call(() -> repository.findById(id));
    }
}
