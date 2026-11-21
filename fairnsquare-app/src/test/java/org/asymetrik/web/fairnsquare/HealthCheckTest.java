package org.asymetrik.web.fairnsquare;

import org.asymetrik.web.fairnsquare.testing.FairnsquareTest;

import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

/**
 * Integration tests for health check endpoints. Verifies AC5: Health endpoints return 200 OK.
 */
@FairnsquareTest
class HealthCheckTest {

    @Test
    void healthEndpointReturnsUp() {
        given().when().get("/api/health").then().statusCode(200).body("status", is("UP"));
    }

    @Test
    void livenessEndpointReturnsUp() {
        given().when().get("/api/health/live").then().statusCode(200).body("status", is("UP"));
    }

    @Test
    void readinessEndpointReturnsUp() {
        given().when().get("/api/health/ready").then().statusCode(200).body("status", is("UP"));
    }
}
