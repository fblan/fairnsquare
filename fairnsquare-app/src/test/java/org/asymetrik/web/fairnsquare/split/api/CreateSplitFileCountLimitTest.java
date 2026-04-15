package org.asymetrik.web.fairnsquare.split.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

import org.asymetrik.web.fairnsquare.CaptchaTokenTestHelper;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.TempStorageTestResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;

/**
 * Integration test verifying that split creation is rejected with HTTP 507 when the file-count storage limit is
 * reached.
 */
@QuarkusTest
@QuarkusTestResource(value = TempStorageTestResource.class, restrictToAnnotatedClass = true, initArgs = @ResourceArg(name = "maxFileCount", value = "2"))
class CreateSplitFileCountLimitTest {

    @BeforeEach
    void installCaptchaToken() {
        RestAssured.requestSpecification = new RequestSpecBuilder()
                .addHeader("X-Captcha-Token", CaptchaTokenTestHelper.generateToken()).build();
    }

    @AfterEach
    void resetRequestSpec() {
        RestAssured.requestSpecification = null;
    }

    /**
     * Once the configured file-count limit is reached, POST /api/splits must return 507 with Problem Details.
     */
    @Test
    void createSplit_returns507WhenFileCountLimitReached() {
        // Fill storage to the limit (maxFileCount = 2)
        given().contentType(ContentType.JSON).body("""
                {"name": "Split One"}
                """).when().post("/api/splits").then().statusCode(201);

        given().contentType(ContentType.JSON).body("""
                {"name": "Split Two"}
                """).when().post("/api/splits").then().statusCode(201);

        // Third split must be rejected
        given().contentType(ContentType.JSON).body("""
                {"name": "Split Three"}
                """).when().post("/api/splits").then().statusCode(507)
                .body("type", containsString("storage-file-count-limit-exceeded"))
                .body("title", equalTo("Storage File Count Limit Exceeded")).body("status", equalTo(507));
    }
}
