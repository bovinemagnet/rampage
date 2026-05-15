package io.rampage.console.sample;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class EchoResourceTest {

    @Test
    void echoGetReturnsMsgAndTimestamp() {
        given().queryParam("msg", "hello")
                .when().get("/verification/rest/echo")
                .then().statusCode(200)
                .body("msg", equalTo("hello"))
                .body("timestamp", notNullValue());
    }

    @Test
    void echoPostRoundTripsBody() {
        given().header("Content-Type", "application/json")
                .body("{\"msg\":\"round-trip\"}")
                .when().post("/verification/rest/echo")
                .then().statusCode(200)
                .body("msg", equalTo("round-trip"))
                .body("timestamp", notNullValue());
    }

    @Test
    void slowSleepsForRequestedMs() {
        long t0 = System.currentTimeMillis();
        given().queryParam("ms", "120")
                .when().get("/verification/rest/slow")
                .then().statusCode(200)
                .body("msg", equalTo("slept-120ms"));
        long elapsed = System.currentTimeMillis() - t0;
        assert elapsed >= 100 : "expected ~120ms, observed " + elapsed;
    }

    @Test
    void failReturnsRequestedStatus() {
        given().queryParam("code", "503")
                .when().get("/verification/rest/fail")
                .then().statusCode(503);
    }

    @Test
    void failClampsInvalidStatusTo500() {
        given().queryParam("code", "9999")
                .when().get("/verification/rest/fail")
                .then().statusCode(500);
    }

    @Test
    void throttleReturns429OnceCapExceeded() {
        // The throttle counts per wall-clock second, so a single quick burst can
        // span a second boundary and reset. Fire enough requests that — even in
        // the worst-case split across two seconds — the cap is exceeded.
        int hits429 = 0;
        for (int i = 0; i < 8; i++) {
            int code = given().queryParam("qps", "1").when().get("/verification/rest/throttle").statusCode();
            if (code == 429) hits429++;
        }
        assertThat(hits429)
                .as("at least some of 8 requests at qps=1 should be throttled")
                .isGreaterThanOrEqualTo(3);
    }
}
