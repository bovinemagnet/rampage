package io.rampage.console.sample;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
class EchoGraphQLTest {

    @Test
    void echoQueryReturnsSuppliedMessage() {
        String body = "{\"query\":\"query Echo($msg: String!) { echo(msg: $msg) }\","
                + "\"variables\":{\"msg\":\"ping\"}}";
        given().header("Content-Type", "application/json")
                .body(body)
                .when().post("/verification/graphql")
                .then().statusCode(200)
                .body("data.echo", equalTo("ping"));
    }

    @Test
    void usersQueryReturnsRequestedCount() {
        String body = "{\"query\":\"query Users($n: Int!) { users(count: $n) { id name email } }\","
                + "\"variables\":{\"n\":3}}";
        given().header("Content-Type", "application/json")
                .body(body)
                .when().post("/verification/graphql")
                .then().statusCode(200)
                .body("data.users", hasSize(3))
                .body("data.users[0].id", equalTo("user-0"))
                .body("data.users[0].email", equalTo("user-0@example.com"));
    }
}
