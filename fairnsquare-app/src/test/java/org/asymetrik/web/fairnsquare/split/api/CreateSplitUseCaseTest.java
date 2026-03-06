package org.asymetrik.web.fairnsquare.split.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;

import org.asymetrik.web.fairnsquare.infrastructure.filesystem.TempStorageTestResource;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Integration tests for Split creation and retrieval use cases. Tests POST /api/splits and GET /api/splits/{id}
 * endpoints.
 */
@QuarkusTest
@QuarkusTestResource(value = TempStorageTestResource.class, restrictToAnnotatedClass = true)
class CreateSplitUseCaseTest {

    /**
     * AC 1: Successful split creation returns 201 with correct response body.
     */
    @Test
    void createSplit_withValidName_returns201WithSplitDetails() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Bordeaux Weekend 2026"}
                """).when().post("/api/splits").then().statusCode(201)
                .header("Location", containsString("/api/splits/")).body("id", notNullValue())
                .body("id", matchesPattern("^[A-Za-z0-9_-]{21}$")) // NanoID pattern
                .body("name", equalTo("Bordeaux Weekend 2026")).body("createdAt", notNullValue())
                .body("participants", hasSize(0)).body("expenses", hasSize(0)).extract().path("id");

        // Verify split is persisted and retrievable (AC 2)
        given().when().get("/api/splits/" + splitId).then().statusCode(200);
    }

    /**
     * AC 2: Split is persisted correctly with all fields.
     */
    @Test
    void createSplit_persistsAllFields() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Test Split"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        given().when().get("/api/splits/" + splitId).then().statusCode(200).body("id", equalTo(splitId))
                .body("name", equalTo("Test Split")).body("createdAt", notNullValue()).body("participants", hasSize(0))
                .body("expenses", hasSize(0));
    }

    /**
     * AC 3: Empty name returns 400 with Problem Details format.
     */
    @Test
    void createSplit_withEmptyName_returns400ProblemDetails() {
        given().contentType(ContentType.JSON).body("""
                {"name": ""}
                """).when().post("/api/splits").then().statusCode(400).body("type", containsString("validation-error"))
                .body("title", equalTo("Validation Error")).body("status", equalTo(400)).body("detail", notNullValue());
    }

    /**
     * AC 4: Missing name returns 400 with Problem Details format.
     */
    @Test
    void createSplit_withMissingName_returns400ProblemDetails() {
        given().contentType(ContentType.JSON).body("{}").when().post("/api/splits").then().statusCode(400)
                .body("type", containsString("validation-error")).body("title", equalTo("Validation Error"))
                .body("status", equalTo(400)).body("detail", notNullValue());
    }

    /**
     * AC 5: Split is retrievable immediately after creation even when storage directory did not exist yet.
     */
    @Test
    void createSplit_createsDirectoryAutomatically() {
        // Directory is already clean from @BeforeEach — verify server creates it on demand
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Auto Directory Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        given().when().get("/api/splits/" + splitId).then().statusCode(200);
    }

    /**
     * AC 1 additional: NanoID is 21 characters and URL-safe.
     */
    @Test
    void createSplit_generatesValid21CharNanoId() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "NanoID Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        assertThat(splitId.length()).as("NanoID should be 21 characters").isEqualTo(21);
        assertThat(splitId.matches("^[A-Za-z0-9_-]+$")).as("NanoID should only contain URL-safe characters").isTrue();
    }

    /**
     * GET endpoint returns 404 with Problem Details for non-existent split (Story 2.3 AC 5).
     */
    @Test
    void getSplit_nonExistent_returns404WithProblemDetails() {
        given().when().get("/api/splits/nonexistentid12345678").then().statusCode(404)
                .body("type", containsString("not-found")).body("title", equalTo("Not Found"))
                .body("status", equalTo(404)).body("detail", containsString("Split not found"));
    }

    /**
     * GET endpoint returns created split.
     */
    @Test
    void getSplit_afterCreate_returnsTheSplit() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Retrievable Split"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        given().when().get("/api/splits/" + splitId).then().statusCode(200).body("id", equalTo(splitId))
                .body("name", equalTo("Retrievable Split")).body("participants", hasSize(0))
                .body("expenses", hasSize(0));
    }

    /**
     * Security: GET endpoint rejects splitId with path traversal characters. Note: URL path traversal (/../) is handled
     * by HTTP routing layer (returns 404). This test verifies that splitIds containing dots are rejected with 400.
     */
    @Test
    void getSplit_withPathTraversalChars_returns400() {
        given().when().get("/api/splits/..etcpasswd12345678901").then().statusCode(400);
    }

    /**
     * Security: GET endpoint rejects invalid splitId format.
     */
    @Test
    void getSplit_withInvalidSplitIdFormat_returns400() {
        given().when().get("/api/splits/invalid..id").then().statusCode(400);
        given().when().get("/api/splits/has/slash").then().statusCode(404); // Different path
        given().when().get("/api/splits/has%00null").then().statusCode(400);
    }
}
