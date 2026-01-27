package org.asymetrik.web.fairnsquare.split.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.asymetrik.web.fairnsquare.sharedkernel.persistence.JsonFileRepository;
import org.asymetrik.web.fairnsquare.sharedkernel.persistence.TenantPathResolver;
import org.asymetrik.web.fairnsquare.split.domain.Expense;
import org.asymetrik.web.fairnsquare.split.domain.Participant;
import org.asymetrik.web.fairnsquare.split.domain.Split;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Integration tests for SplitResource REST API. Story 2.1: Create Split Backend API (AC 1-6) Story 2.3: Access Split
 * via Link (GET endpoint) Story 3.1: Add Participant with Smart Defaults (AC 9-11) Story 3.2: Edit Participant Inline
 * (AC 7-10)
 */
@QuarkusTest
class SplitResourceTest {

    @Inject
    TenantPathResolver pathResolver;

    @Inject
    JsonFileRepository repository;

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

    // ========== Story 3.2: Update Participant Tests ==========

    /**
     * Story 3.2 AC 7: PUT returns 200 with valid data.
     */
    @Test
    void updateParticipant_withValidData_returns200WithUpdatedParticipant() {
        // Create a split and add a participant
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Update Test Split"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String participantId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        // Update the participant
        given().contentType(ContentType.JSON).body("""
                {"name": "Bob", "nights": 4}
                """).when().put("/api/splits/" + splitId + "/participants/" + participantId).then().statusCode(200)
                .body("id", equalTo(participantId)).body("name", equalTo("Bob")).body("nights", equalTo(4));
    }

    /**
     * Story 3.2 AC 7: Updated participant is persisted in JSON file.
     */
    @Test
    void updateParticipant_persistsChangesInJsonFile() throws IOException {
        // Create a split and add a participant
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Update Persistence Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String participantId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        // Update the participant
        given().contentType(ContentType.JSON).body("""
                {"name": "UpdatedName", "nights": 7}
                """).when().put("/api/splits/" + splitId + "/participants/" + participantId).then().statusCode(200);

        // Verify file contains updated values
        Path splitFile = pathResolver.resolve(splitId);
        String content = Files.readString(splitFile);
        assertTrue(content.contains("\"UpdatedName\""), "File should contain updated participant name");
        assertTrue(content.contains("7"), "File should contain updated nights value");
    }

    /**
     * Story 3.2 AC 8: PUT returns 400 for empty name.
     */
    @Test
    void updateParticipant_withEmptyName_returns400() {
        // Create a split and add a participant
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Update Validation Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String participantId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        // Try to update with empty name
        given().contentType(ContentType.JSON).body("""
                {"name": "", "nights": 4}
                """).when().put("/api/splits/" + splitId + "/participants/" + participantId).then().statusCode(400)
                .body("type", containsString("validation-error")).body("status", equalTo(400));
    }

    /**
     * Story 3.2 AC 8: PUT returns 400 for nights less than 1.
     */
    @Test
    void updateParticipant_withNightsLessThan1_returns400() {
        // Create a split and add a participant
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Update Nights Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String participantId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        // Try to update with nights = 0
        given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 0}
                """).when().put("/api/splits/" + splitId + "/participants/" + participantId).then().statusCode(400)
                .body("type", containsString("validation-error")).body("status", equalTo(400));
    }

    /**
     * Story 3.2 AC 8: PUT returns 400 for nights greater than 365.
     */
    @Test
    void updateParticipant_withNightsGreaterThan365_returns400() {
        // Create a split and add a participant
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Update Max Nights Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String participantId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        // Try to update with nights = 366
        given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 366}
                """).when().put("/api/splits/" + splitId + "/participants/" + participantId).then().statusCode(400)
                .body("type", containsString("validation-error")).body("status", equalTo(400));
    }

    /**
     * Story 3.2 AC 10: PUT returns 404 for non-existent split.
     */
    @Test
    void updateParticipant_toNonExistentSplit_returns404() {
        // Use valid 21-char NanoID format for both IDs to avoid 400 validation errors
        given().contentType(ContentType.JSON).body("""
                {"name": "Bob", "nights": 4}
                """).when().put("/api/splits/V1StGXR8_Z5jdHi6B-myT/participants/X2YtGXR8_Z5jdHi6B-myU").then()
                .statusCode(404).body("type", containsString("not-found")).body("status", equalTo(404));
    }

    /**
     * Story 3.2 AC 9: PUT returns 404 for non-existent participant.
     */
    @Test
    void updateParticipant_nonExistentParticipant_returns404() {
        // Create a split
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Update Non-Existent Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        // Try to update a non-existent participant
        given().contentType(ContentType.JSON).body("""
                {"name": "Bob", "nights": 4}
                """).when().put("/api/splits/" + splitId + "/participants/nonexistentid12345678").then().statusCode(404)
                .body("type", containsString("participant-not-found")).body("status", equalTo(404));
    }

    /**
     * Story 3.2 AC 8: PUT returns 400 for invalid splitId format.
     */
    @Test
    void updateParticipant_withInvalidSplitId_returns400() {
        given().contentType(ContentType.JSON).body("""
                {"name": "Bob", "nights": 4}
                """).when().put("/api/splits/invalid..id/participants/X2YtGXR8_Z5jdHi6B-myU").then().statusCode(400);
    }

    /**
     * Story 3.2 AC 8: PUT returns 400 for invalid participantId format.
     */
    @Test
    void updateParticipant_withInvalidParticipantId_returns400() {
        // Create a split
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Invalid Participant ID Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        // Try to update with invalid participant ID format
        given().contentType(ContentType.JSON).body("""
                {"name": "Bob", "nights": 4}
                """).when().put("/api/splits/" + splitId + "/participants/invalid..id").then().statusCode(400);
    }

    // ========== Story 3.3: Delete Participant Tests ==========

    /**
     * Story 3.3 AC 6: DELETE returns 204 when participant has no expenses.
     */
    @Test
    void deleteParticipant_withNoExpenses_returns204() {
        // Create a split and add a participant
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Delete Test Split"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String participantId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        // Delete the participant
        given().when().delete("/api/splits/" + splitId + "/participants/" + participantId).then().statusCode(204);
    }

    /**
     * Story 3.3 AC 6: Participant is removed from JSON file after deletion.
     */
    @Test
    void deleteParticipant_removesFromJsonFile() throws IOException {
        // Create a split and add a participant
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Delete Persistence Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String participantId = given().contentType(ContentType.JSON).body("""
                {"name": "ToBeDeleted", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        // Verify participant exists
        Path splitFile = pathResolver.resolve(splitId);
        String contentBefore = Files.readString(splitFile);
        assertTrue(contentBefore.contains("\"ToBeDeleted\""), "File should contain participant before deletion");

        // Delete the participant
        given().when().delete("/api/splits/" + splitId + "/participants/" + participantId).then().statusCode(204);

        // Verify participant is removed from file
        String contentAfter = Files.readString(splitFile);
        org.junit.jupiter.api.Assertions.assertFalse(contentAfter.contains("\"ToBeDeleted\""),
                "File should not contain participant after deletion");
    }

    /**
     * Story 3.3 AC 7: DELETE returns 409 when participant is a payer on an expense.
     */
    @Test
    void deleteParticipant_withExpenses_returns409() {
        // Create a split and add a participant
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Delete With Expenses Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String participantId = given().contentType(ContentType.JSON).body("""
                {"name": "HasExpenses", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        // Load split, add expense, and save back using repository
        Split split = repository.load(splitId, Split.class).orElseThrow();
        Expense expense = Expense.create(new java.math.BigDecimal("50.00"), "Test Expense",
                Participant.Id.of(participantId), org.asymetrik.web.fairnsquare.split.domain.SplitMode.EQUAL,
                java.util.Collections.emptyList());
        split.addExpense(expense);
        repository.save(splitId, split);

        // Try to delete the participant - should return 409
        given().when().delete("/api/splits/" + splitId + "/participants/" + participantId).then().statusCode(409);
    }

    /**
     * Story 3.3 AC 7: 409 response follows Problem Details format with correct type.
     */
    @Test
    void deleteParticipant_withExpenses_returnsProblemDetailsFormat() {
        // Create a split and add a participant
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Delete Problem Details Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String participantId = given().contentType(ContentType.JSON).body("""
                {"name": "HasExpenses", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        // Load split, add expense, and save back using repository
        Split split = repository.load(splitId, Split.class).orElseThrow();
        Expense expense = Expense.create(new java.math.BigDecimal("50.00"), "Test Expense",
                Participant.Id.of(participantId), org.asymetrik.web.fairnsquare.split.domain.SplitMode.EQUAL,
                java.util.Collections.emptyList());
        split.addExpense(expense);
        repository.save(splitId, split);

        // Verify Problem Details format
        given().when().delete("/api/splits/" + splitId + "/participants/" + participantId).then().statusCode(409)
                .body("type", containsString("participant-has-expenses"))
                .body("title", equalTo("Participant Has Expenses")).body("status", equalTo(409))
                .body("detail", containsString("Cannot remove participant with associated expenses"));
    }

    /**
     * Story 3.3 AC 8: DELETE returns 404 for non-existent participant.
     */
    @Test
    void deleteParticipant_nonExistent_returns404() {
        // Create a split
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Delete Non-Existent Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        // Try to delete a non-existent participant
        given().when().delete("/api/splits/" + splitId + "/participants/nonexistentid12345678").then().statusCode(404)
                .body("type", containsString("participant-not-found")).body("status", equalTo(404));
    }

    /**
     * Story 3.3 AC 8: DELETE returns 404 for non-existent split.
     */
    @Test
    void deleteParticipant_toNonExistentSplit_returns404() {
        // Use valid 21-char NanoID format for both IDs to avoid 400 validation errors
        given().when().delete("/api/splits/V1StGXR8_Z5jdHi6B-myT/participants/X2YtGXR8_Z5jdHi6B-myU").then()
                .statusCode(404).body("type", containsString("not-found")).body("status", equalTo(404));
    }

    /**
     * Story 3.3 AC 9: DELETE returns 400 for invalid splitId format.
     */
    @Test
    void deleteParticipant_withInvalidSplitId_returns400() {
        given().when().delete("/api/splits/invalid..id/participants/X2YtGXR8_Z5jdHi6B-myU").then().statusCode(400);
    }

    /**
     * Story 3.3 AC 9: DELETE returns 400 for invalid participantId format.
     */
    @Test
    void deleteParticipant_withInvalidParticipantId_returns400() {
        // Create a split
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Invalid Participant ID Delete Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        // Try to delete with invalid participant ID format
        given().when().delete("/api/splits/" + splitId + "/participants/invalid..id").then().statusCode(400);
    }

    // ==================== Story 4.1: Add Expense Tests ====================

    /**
     * Story 4.1 AC 11: POST returns 201 with valid expense data (BY_NIGHT mode).
     */
    @Test
    void addExpense_withValidData_returns201AndExpense() {
        // Create a split and add participants
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Expense Test Split"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String payerId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 4}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        given().contentType(ContentType.JSON).body("""
                {"name": "Bob", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201);

        given().contentType(ContentType.JSON).body("""
                {"name": "Charlie", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201);

        // Add expense
        given().contentType(ContentType.JSON).body("""
                {
                    "amount": 180.00,
                    "description": "Groceries",
                    "payerId": "%s",
                    "splitMode": "BY_NIGHT"
                }
                """.formatted(payerId)).when().post("/api/splits/" + splitId + "/expenses").then().statusCode(201)
                .body("id", notNullValue()).body("id.length()", equalTo(21)).body("amount", equalTo(180.0f))
                .body("description", equalTo("Groceries")).body("payerId", equalTo(payerId))
                .body("splitMode", equalTo("BY_NIGHT")).body("createdAt", notNullValue()).body("shares", hasSize(3));
    }

    /**
     * Story 4.1 AC 11: Response contains calculated shares for BY_NIGHT mode.
     */
    @Test
    void addExpense_byNightMode_calculatesSharesProportionally() {
        // Create a split with 3 participants: Alice (4 nights), Bob (2 nights), Charlie (3 nights) = 9 total nights
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "By Night Share Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String aliceId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 4}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        String bobId = given().contentType(ContentType.JSON).body("""
                {"name": "Bob", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        String charlieId = given().contentType(ContentType.JSON).body("""
                {"name": "Charlie", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        // Add expense of €180.00 - BY_NIGHT should split: Alice=80, Bob=40, Charlie=60
        io.restassured.response.Response response = given().contentType(ContentType.JSON).body("""
                {
                    "amount": 180.00,
                    "description": "Dinner",
                    "payerId": "%s",
                    "splitMode": "BY_NIGHT"
                }
                """.formatted(aliceId)).when().post("/api/splits/" + splitId + "/expenses").then().statusCode(201)
                .body("shares", hasSize(3)).extract().response();

        // Verify shares sum to 180.00
        java.util.List<java.util.Map<String, Object>> shares = response.jsonPath().getList("shares");
        Map<String, Map<String, Object>> sharesByParticipant = shares.stream()
                .collect(Collectors.toMap(s -> s.get("participantId").toString(), s -> s));
        Assertions.assertThat(sharesByParticipant.get(aliceId).get("amount")).isEqualTo(80.00f);
        Assertions.assertThat(sharesByParticipant.get(bobId).get("amount")).isEqualTo(40.00f);
        Assertions.assertThat(sharesByParticipant.get(charlieId).get("amount")).isEqualTo(60.00f);
        java.math.BigDecimal totalShares = shares.stream()
                .map(s -> new java.math.BigDecimal(s.get("amount").toString()))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        org.junit.jupiter.api.Assertions.assertTrue(totalShares.compareTo(new java.math.BigDecimal("180.00")) == 0,
                "Expected 180.00 but got " + totalShares);
    }

    /**
     * Story 4.1 AC 4: POST with EQUAL mode calculates equal shares.
     */
    @Test
    void addExpense_equalMode_calculatesEqualShares() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Equal Share Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String aliceId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 4}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        String bobId = given().contentType(ContentType.JSON).body("""
                {"name": "Bob", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        String charlieId = given().contentType(ContentType.JSON).body("""
                {"name": "Charlie", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        // Add expense of €90.00 EQUAL mode - should be €30 each
        io.restassured.response.Response response = given().contentType(ContentType.JSON).body("""
                {
                    "amount": 90.00,
                    "description": "Taxi",
                    "payerId": "%s",
                    "splitMode": "EQUAL"
                }
                """.formatted(aliceId)).when().post("/api/splits/" + splitId + "/expenses").then().statusCode(201)
                .body("splitMode", equalTo("EQUAL")).body("shares", hasSize(3)).extract().response();

        java.util.List<java.util.Map<String, Object>> shares = response.jsonPath().getList("shares");

        Map<String, Map<String, Object>> sharesByParticipant = shares.stream()
                .collect(Collectors.toMap(s -> s.get("participantId").toString(), s -> s));
        Assertions.assertThat(sharesByParticipant.get(aliceId).get("amount")).isEqualTo(30.00f);
        Assertions.assertThat(sharesByParticipant.get(bobId).get("amount")).isEqualTo(30.00f);
        Assertions.assertThat(sharesByParticipant.get(charlieId).get("amount")).isEqualTo(30.00f);

        // Verify shares sum to 90.00
        java.math.BigDecimal totalShares = shares.stream()
                .map(s -> new java.math.BigDecimal(s.get("amount").toString()))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        org.junit.jupiter.api.Assertions.assertTrue(totalShares.compareTo(new java.math.BigDecimal("90.00")) == 0,
                "Expected 90.00 but got " + totalShares);
    }

    /**
     * Story 4.1 AC 11: Test expense is persisted in split's JSON file.
     */
    @Test
    void addExpense_persistsToSplitFile() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Persistence Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String payerId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        // Add expense
        given().contentType(ContentType.JSON).body("""
                {
                    "amount": 50.00,
                    "description": "Snacks",
                    "payerId": "%s",
                    "splitMode": "EQUAL"
                }
                """.formatted(payerId)).when().post("/api/splits/" + splitId + "/expenses").then().statusCode(201);

        // Verify expense persisted by getting the split
        given().when().get("/api/splits/" + splitId).then().statusCode(200).body("expenses", hasSize(1))
                .body("expenses[0].description", equalTo("Snacks")).body("expenses[0].amount", equalTo(50.0f));
    }

    /**
     * Story 4.1 AC 12: POST returns 400 for invalid payerId (not in split).
     */
    // TODO : refactor to do assertj.
    // TODO : reorganize tests by use cases.
    // TODO : test persistence by using // testing (do not rely on the fact that it is a json file on disk)
    // TODO : refactor split calculator and Expense. Expense should be a sealed interface/abstract class with each one a
    // calculation function that take the parent split as parameter.
    // TODO : add test that verifies the coverage?
    // TODO: add modifications in documentation.
    @Test
    void addExpense_withInvalidPayerId_returns400() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Invalid Payer Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201);

        // Try to add expense with non-existent payer
        given().contentType(ContentType.JSON).body("""
                {
                    "amount": 50.00,
                    "description": "Test",
                    "payerId": "V1StGXR8_Z5jdHi6B-myT",
                    "splitMode": "EQUAL"
                }
                """).when().post("/api/splits/" + splitId + "/expenses").then().statusCode(400)
                .body("type", containsString("payer-not-found")).body("title", equalTo("Payer Not Found"));
    }

    /**
     * Story 4.1 AC 11: POST returns 400 for missing/invalid amount.
     */
    @Test
    void addExpense_withMissingAmount_returns400() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Missing Amount Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String payerId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        given().contentType(ContentType.JSON).body("""
                {
                    "description": "Test",
                    "payerId": "%s",
                    "splitMode": "EQUAL"
                }
                """.formatted(payerId)).when().post("/api/splits/" + splitId + "/expenses").then().statusCode(400);
    }

    /**
     * Story 4.1 AC 11: POST returns 400 for amount less than 0.01.
     */
    @Test
    void addExpense_withAmountBelowMinimum_returns400() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Below Min Amount Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String payerId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        given().contentType(ContentType.JSON).body("""
                {
                    "amount": 0.001,
                    "description": "Test",
                    "payerId": "%s",
                    "splitMode": "EQUAL"
                }
                """.formatted(payerId)).when().post("/api/splits/" + splitId + "/expenses").then().statusCode(400);
    }

    /**
     * Story 4.1 AC 11: POST returns 400 for empty description.
     */
    @Test
    void addExpense_withEmptyDescription_returns400() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Empty Description Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String payerId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        given().contentType(ContentType.JSON).body("""
                {
                    "amount": 50.00,
                    "description": "",
                    "payerId": "%s",
                    "splitMode": "EQUAL"
                }
                """.formatted(payerId)).when().post("/api/splits/" + splitId + "/expenses").then().statusCode(400);
    }

    /**
     * Story 4.1 AC 13: POST returns 404 for non-existent split.
     */
    @Test
    void addExpense_toNonExistentSplit_returns404() {
        given().contentType(ContentType.JSON).body("""
                {
                    "amount": 50.00,
                    "description": "Test",
                    "payerId": "V1StGXR8_Z5jdHi6B-myT",
                    "splitMode": "EQUAL"
                }
                """).when().post("/api/splits/V1StGXR8_Z5jdHi6B-myT/expenses").then().statusCode(404);
    }

    /**
     * Story 4.1 AC 13: POST returns 400 for invalid splitId format.
     */
    @Test
    void addExpense_withInvalidSplitIdFormat_returns400() {
        given().contentType(ContentType.JSON).body("""
                {
                    "amount": 50.00,
                    "description": "Test",
                    "payerId": "V1StGXR8_Z5jdHi6B-myT",
                    "splitMode": "EQUAL"
                }
                """).when().post("/api/splits/invalid..id/expenses").then().statusCode(400);
    }

    /**
     * Story 4.1 Code Review: Test backward compatibility for minimal Expense JSON from Story 3.3.
     */
    @Test
    void expense_deserializesMinimalJsonForBackwardCompatibility() throws Exception {
        // Ensure old JSON format with only payerId still works
        String minimalExpenseJson = """
                {"payerId": "V1StGXR8_Z5jdHi6B-myT"}
                """;

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        Expense expense = mapper.readValue(minimalExpenseJson, Expense.class);

        assertThat(expense.getPayerId()).isNotNull();
        assertThat(expense.getPayerId().value()).isEqualTo("V1StGXR8_Z5jdHi6B-myT");
    }
}
