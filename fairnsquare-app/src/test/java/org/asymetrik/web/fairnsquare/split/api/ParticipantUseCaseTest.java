package org.asymetrik.web.fairnsquare.split.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.split.persistence.TenantPathResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Integration tests for Participant management use cases. Tests add, update, and delete participant endpoints.
 */
@QuarkusTest
class ParticipantUseCaseTest {

    @Inject
    TenantPathResolver pathResolver;

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
                .body("name", equalTo("Alice")).body("nights", equalTo(2.0f));
    }

    @Test
    void addParticipant_withTwiceSameName_returnsError() {
        // First create a split
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Participant Test Split"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        // Add a participant
        given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201)
                .body("id", notNullValue()).body("id", matchesPattern("^[A-Za-z0-9_-]{21}$"))
                .body("name", equalTo("Alice")).body("nights", equalTo(2.0f));
        // Add the same participant
        given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(400);

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
        assertThat(content.contains("\"Bob\"")).as("File should contain participant name").isTrue();
        assertThat(content.contains("\"nights\"")).as("File should contain nights field").isTrue();
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
                .body("id", equalTo(participantId)).body("name", equalTo("Bob")).body("nights", equalTo(4.0f));
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
        assertThat(content.contains("\"UpdatedName\"")).as("File should contain updated participant name").isTrue();
        assertThat(content.contains("7")).as("File should contain updated nights value").isTrue();
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
        assertThat(contentBefore.contains("\"ToBeDeleted\"")).as("File should contain participant before deletion")
                .isTrue();

        // Delete the participant
        given().when().delete("/api/splits/" + splitId + "/participants/" + participantId).then().statusCode(204);

        // Verify participant is removed from file
        String contentAfter = Files.readString(splitFile);
        assertThat(contentAfter.contains("\"ToBeDeleted\"")).as("File should not contain participant after deletion")
                .isFalse();
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

        // Add expense via API
        given().contentType(ContentType.JSON).body("""
                {"amount": 50.00, "description": "Test Expense", "payerId": "%s"}
                """.formatted(participantId)).when().post("/api/splits/" + splitId + "/expenses/equal").then()
                .statusCode(201);

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

        // Add expense via API
        given().contentType(ContentType.JSON).body("""
                {"amount": 50.00, "description": "Test Expense", "payerId": "%s"}
                """.formatted(participantId)).when().post("/api/splits/" + splitId + "/expenses/equal").then()
                .statusCode(201);

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

}
