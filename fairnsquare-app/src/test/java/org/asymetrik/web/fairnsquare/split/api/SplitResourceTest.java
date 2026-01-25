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
import java.util.Comparator;

import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.sharedkernel.persistence.TenantPathResolver;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Integration tests for SplitResource REST API. Story 2.1: Create Split Backend API (AC 1-6) Story 2.3: Access Split
 * via Link (GET endpoint) Story 3.1: Add Participant with Smart Defaults (AC 9-11)
 */
@QuarkusTest
class SplitResourceTest {

    @Inject
    TenantPathResolver pathResolver;

    @ConfigProperty(name = "fairnsquare.data.path")
    String configuredDataPath;

    @BeforeEach
    void setUp() throws IOException {
        // Clean up any existing test data
        Path defaultTenant = pathResolver.resolveDefaultTenantDirectory();
        if (Files.exists(defaultTenant)) {
            Files.walk(defaultTenant).sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException _) {
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
            Files.walk(defaultTenant).sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException _) {
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

    // ========== Story 3.1: Add Participant Tests ==========

    /**
     * Story 3.1 AC 9: POST returns 201 with valid participant data.
     */
    @Test
    void addParticipant_withValidData_returns201WithParticipantDetails() {
        // First create a split
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Participant Test Split"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        // Add a participant
        given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201)
                .body("id", notNullValue()).body("id", matchesPattern("^[A-Za-z0-9_-]{21}$"))
                .body("name", equalTo("Alice")).body("nights", equalTo(2));
    }

    /**
     * Story 3.1 AC 9: Participant is persisted in the split's JSON file.
     */
    @Test
    void addParticipant_persistsInJsonFile() throws IOException {
        // First create a split
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Persistence Test Split"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        // Add a participant
        given().contentType(ContentType.JSON).body("""
                {"name": "Bob", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201);

        // Verify file contains participant
        Path splitFile = pathResolver.resolve(splitId);
        String content = Files.readString(splitFile);
        assertTrue(content.contains("\"Bob\""), "File should contain participant name");
        assertTrue(content.contains("\"nights\""), "File should contain nights field");
    }

    /**
     * Story 3.1 AC 9: Multiple participants can be added.
     */
    @Test
    void addParticipant_multipleParticipants_allPersisted() {
        // First create a split
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Multi Participant Split"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        // Add first participant
        given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201);

        // Add second participant
        given().contentType(ContentType.JSON).body("""
                {"name": "Bob", "nights": 5}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201);

        // Verify split contains both participants
        given().when().get("/api/splits/" + splitId).then().statusCode(200).body("participants", hasSize(2));
    }

    /**
     * Story 3.1 AC 10: POST returns 400 for empty name.
     */
    @Test
    void addParticipant_withEmptyName_returns400() {
        // First create a split
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Empty Name Test Split"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        // Try to add participant with empty name
        given().contentType(ContentType.JSON).body("""
                {"name": "", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(400)
                .body("type", containsString("validation-error")).body("status", equalTo(400));
    }

    /**
     * Story 3.1 AC 10: POST returns 400 for nights less than 1.
     */
    @Test
    void addParticipant_withNightsLessThan1_returns400() {
        // First create a split
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Invalid Nights Test Split"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        // Try to add participant with nights = 0
        given().contentType(ContentType.JSON).body("""
                {"name": "Charlie", "nights": 0}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(400)
                .body("type", containsString("validation-error")).body("status", equalTo(400));
    }

    /**
     * Story 3.1 AC 11: POST returns 404 for non-existent split.
     */
    @Test
    void addParticipant_toNonExistentSplit_returns404() {
        given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 2}
                """).when().post("/api/splits/nonexistentid12345678/participants").then().statusCode(404)
                .body("type", containsString("not-found")).body("status", equalTo(404));
    }

    /**
     * Story 3.1 AC 11: POST returns 400 for invalid splitId format.
     */
    @Test
    void addParticipant_withInvalidSplitId_returns400() {
        given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 2}
                """).when().post("/api/splits/invalid..id/participants").then().statusCode(400);
    }

    /**
     * Story 3.1 AC 10: POST returns 400 for nights greater than 365.
     */
    @Test
    void addParticipant_withNightsGreaterThan365_returns400() {
        // First create a split
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Max Nights Test Split"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        // Try to add participant with nights = 366
        given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 366}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(400)
                .body("type", containsString("validation-error")).body("status", equalTo(400));
    }

    /**
     * Story 3.1 AC 10: POST returns 400 for name exceeding 50 characters.
     */
    @Test
    void addParticipant_withNameExceeding50Chars_returns400() {
        // First create a split
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Long Name Test Split"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        // Try to add participant with name > 50 chars
        String longName = "A".repeat(51);
        given().contentType(ContentType.JSON).body("""
                {"name": "%s", "nights": 2}
                """.formatted(longName)).when().post("/api/splits/" + splitId + "/participants").then().statusCode(400)
                .body("type", containsString("validation-error")).body("status", equalTo(400));
    }
}
