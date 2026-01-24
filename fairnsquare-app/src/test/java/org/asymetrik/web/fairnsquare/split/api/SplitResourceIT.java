package org.asymetrik.web.fairnsquare.split.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.sharedkernel.persistence.TenantPathResolver;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Integration tests for SplitResource REST API. Tests AC 1-6 for Story 2.1.
 */
@QuarkusTest
class SplitResourceIT {

    @Inject
    TenantPathResolver pathResolver;

    @ConfigProperty(name = "fairnsquare.data.path")
    String configuredDataPath;

    @BeforeEach
    void setUp() throws IOException {
        // Clean up any existing test data
        Path defaultTenant = pathResolver.resolveDefaultTenantDirectory();
        if (Files.exists(defaultTenant)) {
            Files.walk(defaultTenant).sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    // Ignore cleanup errors
                }
            });
        }
    }

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

        // Verify file was created (AC 2)
        Path splitFile = pathResolver.resolve(splitId);
        assertTrue(Files.exists(splitFile), "Split file should exist at " + splitFile);
    }

    /**
     * AC 2: Split file is persisted correctly with all fields.
     */
    @Test
    void createSplit_persistsFileWithAllFields() throws IOException {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Test Split"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        Path splitFile = pathResolver.resolve(splitId);
        assertTrue(Files.exists(splitFile), "Split file should exist");

        String content = Files.readString(splitFile);
        assertTrue(content.contains("\"id\""), "File should contain id field");
        assertTrue(content.contains("\"name\""), "File should contain name field");
        assertTrue(content.contains("\"createdAt\""), "File should contain createdAt field");
        assertTrue(content.contains("\"participants\""), "File should contain participants field");
        assertTrue(content.contains("\"expenses\""), "File should contain expenses field");
        assertTrue(content.contains("Test Split"), "File should contain the split name");
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
     * AC 5: Directory is created automatically when it doesn't exist.
     */
    @Test
    void createSplit_createsDirectoryAutomatically() throws IOException {
        // Ensure directory doesn't exist
        Path defaultTenant = pathResolver.resolveDefaultTenantDirectory();
        if (Files.exists(defaultTenant)) {
            Files.walk(defaultTenant).sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    // Ignore
                }
            });
        }

        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Auto Directory Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        // Verify directory and file were created
        assertTrue(Files.exists(defaultTenant), "Default tenant directory should exist");
        Path splitFile = pathResolver.resolve(splitId);
        assertTrue(Files.exists(splitFile), "Split file should exist");
    }

    /**
     * AC 1 additional: NanoID is 21 characters and URL-safe.
     */
    @Test
    void createSplit_generatesValid21CharNanoId() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "NanoID Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        // Verify NanoID characteristics
        org.junit.jupiter.api.Assertions.assertEquals(21, splitId.length(), "NanoID should be 21 characters");
        assertTrue(splitId.matches("^[A-Za-z0-9_-]+$"), "NanoID should only contain URL-safe characters");
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
        // Create a split
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Retrievable Split"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        // Retrieve it
        given().when().get("/api/splits/" + splitId).then().statusCode(200).body("id", equalTo(splitId))
                .body("name", equalTo("Retrievable Split")).body("participants", hasSize(0))
                .body("expenses", hasSize(0));
    }

    /**
     * AC 6: Configurable data path via config property. Verifies that the fairnsquare.data.path config property is used
     * by TenantPathResolver.
     */
    @Test
    void configuredDataPath_isUsedByPathResolver() {
        // Verify config property is injected (proves config mechanism works)
        org.junit.jupiter.api.Assertions.assertNotNull(configuredDataPath,
                "Config property fairnsquare.data.path should be injected");

        // Verify TenantPathResolver uses the configured path
        Path resolvedPath = pathResolver.resolve("test-split-id");
        assertTrue(resolvedPath.toString().startsWith(configuredDataPath),
                "Resolved path should start with configured data path: " + configuredDataPath);
    }

    /**
     * Security: GET endpoint rejects splitId with path traversal characters. Note: URL path traversal (/../) is handled
     * by HTTP routing layer (returns 404). This test verifies that splitIds containing dots are rejected with 400.
     */
    @Test
    void getSplit_withPathTraversalChars_returns400() {
        // SplitId containing dots should be rejected (NanoID only allows [A-Za-z0-9_-])
        given().when().get("/api/splits/..etcpasswd12345678901").then().statusCode(400);
    }

    /**
     * Security: GET endpoint rejects invalid splitId format.
     */
    @Test
    void getSplit_withInvalidSplitIdFormat_returns400() {
        // SplitId should be alphanumeric with - and _ only (NanoID format)
        given().when().get("/api/splits/invalid..id").then().statusCode(400);
        given().when().get("/api/splits/has/slash").then().statusCode(404); // Different path
        given().when().get("/api/splits/has%00null").then().statusCode(400);
    }
}
