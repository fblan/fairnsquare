package org.asymetrik.web.fairnsquare;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

/**
 * Integration tests for health check endpoints. Verifies AC5: Health endpoints return 200 OK.
 */
@QuarkusTest
class HealthCheckTest {

    @Test
    void healthEndpointReturnsUp() {
        given().when().get("/q/health").then().statusCode(200).body("status", is("UP"));
    }

    @Test
    void livenessEndpointReturnsUp() {
        given().when().get("/q/health/live").then().statusCode(200).body("status", is("UP"));
    }

    @Test
    void readinessEndpointReturnsUp() {
        given().when().get("/q/health/ready").then().statusCode(200).body("status", is("UP"));
    }
}
