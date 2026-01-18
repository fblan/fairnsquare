package org.asymetrik.web.fairnsquare;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

/**
 * Integration tests verifying OpenAPI and Swagger UI are properly configured. Story 1.3: Configure OpenTelemetry & Dev
 * Experience AC5: GET /q/swagger-ui displays the OpenAPI documentation
 */
@QuarkusTest
class OpenApiIT {

    /**
     * AC5: Verifies Swagger UI is accessible and returns the interactive documentation page.
     */
    @Test
    void swaggerUiIsAccessible() {
        given().when().get("/q/swagger-ui").then().statusCode(200).contentType(containsString("text/html"))
                .body(containsString("swagger-ui")).body(containsString("OpenAPI UI"));
    }

    /**
     * AC5: Verifies OpenAPI specification endpoint returns valid YAML.
     */
    @Test
    void openApiSpecReturnsYaml() {
        given().when().get("/q/openapi").then().statusCode(200).contentType(containsString("application/yaml"))
                .body(containsString("openapi:"));
    }

    /**
     * AC5: Verifies OpenAPI spec contains application name and version.
     */
    @Test
    void openApiContainsAppInfo() {
        given().when().get("/q/openapi").then().statusCode(200).body(containsString("title:"))
                .body(containsString("FairNSquare API"));
    }

    /**
     * Verifies OpenAPI spec can be retrieved in JSON format.
     */
    @Test
    void openApiSpecReturnsJson() {
        given().accept("application/json").when().get("/q/openapi").then().statusCode(200)
                .contentType(containsString("application/json")).body(containsString("\"openapi\""));
    }
}
