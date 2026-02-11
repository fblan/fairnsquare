package org.asymetrik.web.fairnsquare.split.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.split.persistence.TenantPathResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Integration tests for Expense management use cases. Tests expense creation with different split modes (BY_NIGHT,
 * EQUAL).
 */
@QuarkusTest
class ExpenseUseCaseTest {

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
        assertThat(sharesByParticipant.get(aliceId).get("amount")).isEqualTo(80.00f);
        assertThat(sharesByParticipant.get(bobId).get("amount")).isEqualTo(40.00f);
        assertThat(sharesByParticipant.get(charlieId).get("amount")).isEqualTo(60.00f);
        java.math.BigDecimal totalShares = shares.stream()
                .map(s -> new java.math.BigDecimal(s.get("amount").toString()))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        assertThat(totalShares.compareTo(new java.math.BigDecimal("180.00")) == 0)
                .as("Expected 180.00 but got " + totalShares).isTrue();
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
        assertThat(sharesByParticipant.get(aliceId).get("amount")).isEqualTo(30.00f);
        assertThat(sharesByParticipant.get(bobId).get("amount")).isEqualTo(30.00f);
        assertThat(sharesByParticipant.get(charlieId).get("amount")).isEqualTo(30.00f);

        // Verify shares sum to 90.00
        java.math.BigDecimal totalShares = shares.stream()
                .map(s -> new java.math.BigDecimal(s.get("amount").toString()))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        assertThat(totalShares.compareTo(new java.math.BigDecimal("90.00")) == 0)
                .as("Expected 90.00 but got " + totalShares).isTrue();
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

    // ==================== TD-001.3: Type-Specific Endpoints ====================

    /**
     * TD-001.3 AC7: POST /expenses/by-night creates ExpenseByNight.
     */
    @Test
    void addExpenseByNight_createsExpenseWithByNightShares() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "BY_NIGHT Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String aliceId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 4}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        String bobId = given().contentType(ContentType.JSON).body("""
                {"name": "Bob", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        given().contentType(ContentType.JSON).body("""
                {
                    "amount": 180.00,
                    "description": "Groceries",
                    "payerId": "%s"
                }
                """.formatted(aliceId)).when().post("/api/splits/{splitId}/expenses/by-night", splitId).then()
                .statusCode(201).body("type", equalTo("BY_NIGHT")).body("amount", equalTo(180.00f))
                .body("description", equalTo("Groceries")).body("shares", hasSize(2));
    }

    /**
     * TD-001.3 AC7: POST /expenses/equal creates ExpenseEqual.
     */
    @Test
    void addExpenseEqual_createsExpenseWithEqualShares() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "EQUAL Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String aliceId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 4}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        given().contentType(ContentType.JSON).body("""
                {"name": "Bob", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201);

        given().contentType(ContentType.JSON).body("""
                {
                    "amount": 100.00,
                    "description": "Dinner",
                    "payerId": "%s"
                }
                """.formatted(aliceId)).when().post("/api/splits/{splitId}/expenses/equal", splitId).then()
                .statusCode(201).body("type", equalTo("EQUAL")).body("amount", equalTo(100.00f))
                .body("shares", hasSize(2)).body("shares[0].amount", equalTo(50.00f))
                .body("shares[1].amount", equalTo(50.00f));
    }

    /**
     * TD-001.3 AC7: New endpoints return 404 for non-existent split.
     */
    @Test
    void addExpenseByNight_toNonExistentSplit_returns404() {
        given().contentType(ContentType.JSON).body("""
                {
                    "amount": 50.00,
                    "description": "Test",
                    "payerId": "V1StGXR8_Z5jdHi6B-myT"
                }
                """).when().post("/api/splits/V1StGXR8_Z5jdHi6B-myT/expenses/by-night").then().statusCode(404);
    }

    /**
     * TD-001.3 AC7: New endpoints return 404 for non-existent split.
     */
    @Test
    void addExpenseEqual_toNonExistentSplit_returns404() {
        given().contentType(ContentType.JSON).body("""
                {
                    "amount": 50.00,
                    "description": "Test",
                    "payerId": "V1StGXR8_Z5jdHi6B-myT"
                }
                """).when().post("/api/splits/V1StGXR8_Z5jdHi6B-myT/expenses/equal").then().statusCode(404);
    }

    // ==================== Story FNS-002.5: Delete Expense Tests ====================

    /**
     * FNS-002.5 AC5: DELETE returns 204 and removes expense from split.
     */
    @Test
    void deleteExpense_withValidData_returns204() {
        // Create split + participant + expense
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Delete Expense Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String payerId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        String expenseId = given().contentType(ContentType.JSON).body("""
                {
                    "amount": 50.00,
                    "description": "Groceries",
                    "payerId": "%s"
                }
                """.formatted(payerId)).when().post("/api/splits/" + splitId + "/expenses/equal").then().statusCode(201)
                .extract().path("id");

        // Delete the expense
        given().when().delete("/api/splits/" + splitId + "/expenses/" + expenseId).then().statusCode(204);

        // Verify expense is removed
        given().when().get("/api/splits/" + splitId).then().statusCode(200).body("expenses", hasSize(0));
    }

    /**
     * FNS-002.5 AC5: DELETE persists removal to file.
     */
    @Test
    void deleteExpense_persistsRemovalToFile() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Persistence Delete Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String payerId = given().contentType(ContentType.JSON).body("""
                {"name": "Bob", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        // Add two expenses
        String expense1Id = given().contentType(ContentType.JSON).body("""
                {
                    "amount": 30.00,
                    "description": "Snacks",
                    "payerId": "%s"
                }
                """.formatted(payerId)).when().post("/api/splits/" + splitId + "/expenses/equal").then().statusCode(201)
                .extract().path("id");

        given().contentType(ContentType.JSON).body("""
                {
                    "amount": 20.00,
                    "description": "Drinks",
                    "payerId": "%s"
                }
                """.formatted(payerId)).when().post("/api/splits/" + splitId + "/expenses/equal").then()
                .statusCode(201);

        // Delete first expense
        given().when().delete("/api/splits/" + splitId + "/expenses/" + expense1Id).then().statusCode(204);

        // Verify only second expense remains
        given().when().get("/api/splits/" + splitId).then().statusCode(200).body("expenses", hasSize(1))
                .body("expenses[0].description", equalTo("Drinks"));
    }

    /**
     * FNS-002.5: DELETE returns 404 for non-existent expense.
     */
    @Test
    void deleteExpense_withNonExistentExpense_returns404() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Non-Existent Expense Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        given().when().delete("/api/splits/" + splitId + "/expenses/V1StGXR8_Z5jdHi6B-myT").then().statusCode(404)
                .body("type", containsString("expense-not-found"));
    }

    /**
     * FNS-002.5: DELETE returns 400 for invalid expense ID format.
     */
    @Test
    void deleteExpense_withInvalidExpenseIdFormat_returns400() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Invalid Expense ID Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        given().when().delete("/api/splits/" + splitId + "/expenses/invalid..id").then().statusCode(400).body("type",
                containsString("invalid-expense-id"));
    }

    /**
     * FNS-002.5: DELETE returns 404 for non-existent split.
     */
    @Test
    void deleteExpense_withNonExistentSplit_returns404() {
        given().when().delete("/api/splits/V1StGXR8_Z5jdHi6B-myT/expenses/V1StGXR8_Z5jdHi6B-myT").then()
                .statusCode(404);
    }

    /**
     * FNS-002.5: DELETE returns 400 for invalid split ID format.
     */
    @Test
    void deleteExpense_withInvalidSplitIdFormat_returns400() {
        given().when().delete("/api/splits/invalid..id/expenses/V1StGXR8_Z5jdHi6B-myT").then().statusCode(400);
    }

    /**
     * FNS-002.6: PUT successfully updates expense amount and description.
     */
    @Test
    void updateExpense_withValidRequest_returns200AndUpdatedExpense() {
        // Create split with participants
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Update Expense Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String payerId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        given().contentType(ContentType.JSON).body("""
                {"name": "Bob", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201);

        // Create initial expense
        String expenseId = given().contentType(ContentType.JSON).body("""
                {"amount": 50.00, "description": "Groceries", "payerId": "%s"}
                """.formatted(payerId)).when().post("/api/splits/" + splitId + "/expenses/by-night").then()
                .statusCode(201).extract().path("id");

        // Update expense
        given().contentType(ContentType.JSON).body("""
                {"amount": 75.00, "description": "Updated Groceries", "payerId": "%s", "splitMode": "BY_NIGHT"}
                """.formatted(payerId)).when().put("/api/splits/" + splitId + "/expenses/" + expenseId).then()
                .statusCode(200).body("amount", equalTo(75.00f)).body("description", equalTo("Updated Groceries"))
                .body("splitMode", equalTo("BY_NIGHT"));

        // Verify updated expense persisted
        given().when().get("/api/splits/" + splitId).then().statusCode(200).body("expenses", hasSize(1))
                .body("expenses[0].amount", equalTo(75.00f))
                .body("expenses[0].description", equalTo("Updated Groceries"));
    }

    /**
     * FNS-002.6: PUT successfully changes split mode from BY_NIGHT to EQUAL.
     */
    @Test
    void updateExpense_withSplitModeChange_returns200AndRecalculatesShares() {
        // Create split with participants
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Split Mode Change Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String payerId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        given().contentType(ContentType.JSON).body("""
                {"name": "Bob", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201);

        // Create BY_NIGHT expense
        String expenseId = given().contentType(ContentType.JSON).body("""
                {"amount": 100.00, "description": "Hotel", "payerId": "%s"}
                """.formatted(payerId)).when().post("/api/splits/" + splitId + "/expenses/by-night").then()
                .statusCode(201).extract().path("id");

        // Change to EQUAL split mode
        given().contentType(ContentType.JSON).body("""
                {"amount": 100.00, "description": "Hotel", "payerId": "%s", "splitMode": "EQUAL"}
                """.formatted(payerId)).when().put("/api/splits/" + splitId + "/expenses/" + expenseId).then()
                .statusCode(200).body("splitMode", equalTo("EQUAL")).body("shares", hasSize(2))
                .body("shares[0].amount", equalTo(50.00f)).body("shares[1].amount", equalTo(50.00f));
    }

    /**
     * FNS-002.6: PUT successfully changes payer.
     */
    @Test
    void updateExpense_withPayerChange_returns200AndUpdatedPayer() {
        // Create split with participants
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Payer Change Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String alice = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        String bob = given().contentType(ContentType.JSON).body("""
                {"name": "Bob", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        // Create expense with Alice as payer
        String expenseId = given().contentType(ContentType.JSON).body("""
                {"amount": 50.00, "description": "Dinner", "payerId": "%s"}
                """.formatted(alice)).when().post("/api/splits/" + splitId + "/expenses/equal").then().statusCode(201)
                .extract().path("id");

        // Change payer to Bob
        given().contentType(ContentType.JSON).body("""
                {"amount": 50.00, "description": "Dinner", "payerId": "%s", "splitMode": "EQUAL"}
                """.formatted(bob)).when().put("/api/splits/" + splitId + "/expenses/" + expenseId).then()
                .statusCode(200).body("payerId", equalTo(bob));
    }

    /**
     * FNS-002.6: PUT returns 404 for non-existent expense.
     */
    @Test
    void updateExpense_withNonExistentExpense_returns404() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Non-Existent Expense Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String payerId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        given().contentType(ContentType.JSON).body("""
                {"amount": 50.00, "description": "Test", "payerId": "%s", "splitMode": "EQUAL"}
                """.formatted(payerId)).when().put("/api/splits/" + splitId + "/expenses/V1StGXR8_Z5jdHi6B-myT").then()
                .statusCode(404).body("type", containsString("expense-not-found"));
    }

    /**
     * FNS-002.6: PUT returns 400 for invalid expense ID format.
     */
    @Test
    void updateExpense_withInvalidExpenseIdFormat_returns400() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Invalid Expense ID Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String payerId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        given().contentType(ContentType.JSON).body("""
                {"amount": 50.00, "description": "Test", "payerId": "%s", "splitMode": "EQUAL"}
                """.formatted(payerId)).when().put("/api/splits/" + splitId + "/expenses/invalid..id").then()
                .statusCode(400).body("type", containsString("invalid-expense-id"));
    }

    /**
     * FNS-002.6: PUT returns 404 for non-existent split.
     */
    @Test
    void updateExpense_withNonExistentSplit_returns404() {
        given().contentType(ContentType.JSON).body("""
                {"amount": 50.00, "description": "Test", "payerId": "V1StGXR8_Z5jdHi6B-myT", "splitMode": "EQUAL"}
                """).when().put("/api/splits/V1StGXR8_Z5jdHi6B-myT/expenses/V1StGXR8_Z5jdHi6B-myT").then()
                .statusCode(404);
    }

    /**
     * FNS-002.6: PUT returns 400 for invalid split ID format.
     */
    @Test
    void updateExpense_withInvalidSplitIdFormat_returns400() {
        given().contentType(ContentType.JSON).body("""
                {"amount": 50.00, "description": "Test", "payerId": "V1StGXR8_Z5jdHi6B-myT", "splitMode": "EQUAL"}
                """).when().put("/api/splits/invalid..id/expenses/V1StGXR8_Z5jdHi6B-myT").then().statusCode(400);
    }

    /**
     * FNS-002.6: PUT returns 400 for invalid payer (not a participant).
     */
    @Test
    void updateExpense_withInvalidPayer_returns400() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Invalid Payer Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String payerId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        // Create expense
        String expenseId = given().contentType(ContentType.JSON).body("""
                {"amount": 50.00, "description": "Test", "payerId": "%s"}
                """.formatted(payerId)).when().post("/api/splits/" + splitId + "/expenses/equal").then().statusCode(201)
                .extract().path("id");

        // Try to update with non-existent payer
        given().contentType(ContentType.JSON).body("""
                {"amount": 50.00, "description": "Test", "payerId": "V1StGXR8_Z5jdHi6B-myT", "splitMode": "EQUAL"}
                """).when().put("/api/splits/" + splitId + "/expenses/" + expenseId).then().statusCode(400).body("type",
                containsString("payer-not-found"));
    }

    // ==================== Story 4.3: FREE Mode Manual Share Specification Tests ====================

    /**
     * Story 4.3 AC6: POST /expenses/free with valid shares returns 201 and correct response.
     */
    @Test
    void addExpenseFree_withValidShares_returns201AndExpense() {
        // Create split with 2 participants
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "FREE Mode Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String aliceId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        String bobId = given().contentType(ContentType.JSON).body("""
                {"name": "Bob", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        // Add FREE expense with manual parts: Alice=2, Bob=3 (will calculate to €40 and €60)
        given().contentType(ContentType.JSON).body("""
                {
                    "amount": 100.00,
                    "description": "Custom Split Dinner",
                    "payerId": "%s",
                    "shares": [
                        {"participantId": "%s", "parts": 2.00},
                        {"participantId": "%s", "parts": 3.00}
                    ]
                }
                """.formatted(aliceId, aliceId, bobId)).when().post("/api/splits/" + splitId + "/expenses/free").then()
                .statusCode(201).body("id", notNullValue()).body("type", equalTo("FREE"))
                .body("splitMode", equalTo("FREE")).body("amount", equalTo(100.00f))
                .body("description", equalTo("Custom Split Dinner")).body("payerId", equalTo(aliceId))
                .body("shares", hasSize(2));
    }

    /**
     * Story 4.3 AC7: POST /expenses/free returns calculated shares from parts.
     */
    @Test
    void addExpenseFree_withValidShares_returnsCalculatedShares() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "FREE Shares Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String aliceId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        String bobId = given().contentType(ContentType.JSON).body("""
                {"name": "Bob", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        // Add FREE expense with parts (3 and 2 parts should split €150 as €90 and €60)
        io.restassured.response.Response response = given().contentType(ContentType.JSON).body("""
                {
                    "amount": 150.00,
                    "description": "Uneven Split",
                    "payerId": "%s",
                    "shares": [
                        {"participantId": "%s", "parts": 3.00},
                        {"participantId": "%s", "parts": 2.00}
                    ]
                }
                """.formatted(aliceId, aliceId, bobId)).when().post("/api/splits/" + splitId + "/expenses/free").then()
                .statusCode(201).body("shares", hasSize(2)).extract().response();

        // Verify shares are calculated from parts: 3 parts = €90, 2 parts = €60
        java.util.List<java.util.Map<String, Object>> shares = response.jsonPath().getList("shares");
        Map<String, Map<String, Object>> sharesByParticipant = shares.stream()
                .collect(Collectors.toMap(s -> s.get("participantId").toString(), s -> s));
        assertThat(sharesByParticipant.get(aliceId).get("amount")).isEqualTo(90.00f);
        assertThat(sharesByParticipant.get(aliceId).get("parts")).isEqualTo(3.00f);
        assertThat(sharesByParticipant.get(bobId).get("amount")).isEqualTo(60.00f);
        assertThat(sharesByParticipant.get(bobId).get("parts")).isEqualTo(2.00f);
    }

    /**
     * Story 4.3 AC8: POST /expenses/free with negative parts returns 400.
     */
    @Test
    void addExpenseFree_withNegativeParts_returns400() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Negative Parts Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String aliceId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        String bobId = given().contentType(ContentType.JSON).body("""
                {"name": "Bob", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        // Negative parts are invalid
        given().contentType(ContentType.JSON).body("""
                {
                    "amount": 100.00,
                    "description": "Invalid Negative Parts",
                    "payerId": "%s",
                    "shares": [
                        {"participantId": "%s", "parts": 5.00},
                        {"participantId": "%s", "parts": -1.00}
                    ]
                }
                """.formatted(aliceId, aliceId, bobId)).when().post("/api/splits/" + splitId + "/expenses/free").then()
                .statusCode(400);
    }

    /**
     * Story 4.3 AC8: POST /expenses/free with non-existent participant returns 400.
     */
    @Test
    void addExpenseFree_withNonExistentParticipant_returns400() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Missing Participant Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String aliceId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        // Try to add expense with non-existent participant ID in shares
        given().contentType(ContentType.JSON).body("""
                {
                    "amount": 100.00,
                    "description": "Invalid Participant",
                    "payerId": "%s",
                    "shares": [
                        {"participantId": "%s", "parts": 2.00},
                        {"participantId": "V1StGXR8_Z5jdHi6B-myT", "parts": 3.00}
                    ]
                }
                """.formatted(aliceId, aliceId)).when().post("/api/splits/" + splitId + "/expenses/free").then()
                .statusCode(400).body("type", containsString("invalid-shares"));
    }

    /**
     * Story 4.3 AC7: POST /expenses/free persists parts and calculates shares (round-trip test).
     */
    @Test
    void addExpenseFree_persistsSharesCorrectly() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Persistence Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String aliceId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        String bobId = given().contentType(ContentType.JSON).body("""
                {"name": "Bob", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        // Add FREE expense with parts (7 and 5 parts should split €120 as €70 and €50)
        given().contentType(ContentType.JSON).body("""
                {
                    "amount": 120.00,
                    "description": "Persisted Split",
                    "payerId": "%s",
                    "shares": [
                        {"participantId": "%s", "parts": 7.00},
                        {"participantId": "%s", "parts": 5.00}
                    ]
                }
                """.formatted(aliceId, aliceId, bobId)).when().post("/api/splits/" + splitId + "/expenses/free").then()
                .statusCode(201);

        // GET split and verify shares persisted and calculated correctly
        io.restassured.response.Response response = given().when().get("/api/splits/" + splitId).then().statusCode(200)
                .body("expenses", hasSize(1)).body("expenses[0].type", equalTo("FREE"))
                .body("expenses[0].amount", equalTo(120.00f)).body("expenses[0].shares", hasSize(2)).extract()
                .response();

        // Verify calculated amounts and original parts
        java.util.List<java.util.Map<String, Object>> shares = response.jsonPath().getList("expenses[0].shares");
        Map<String, Map<String, Object>> sharesByParticipant = shares.stream()
                .collect(Collectors.toMap(s -> s.get("participantId").toString(), s -> s));
        assertThat(sharesByParticipant.get(aliceId).get("amount")).isEqualTo(70.00f);
        assertThat(sharesByParticipant.get(aliceId).get("parts")).isEqualTo(7.00f);
        assertThat(sharesByParticipant.get(bobId).get("amount")).isEqualTo(50.00f);
        assertThat(sharesByParticipant.get(bobId).get("parts")).isEqualTo(5.00f);
    }

    /**
     * Story 4.3 AC8: POST /expenses/free when a split participant is missing from shares returns 400.
     */
    @Test
    void addExpenseFree_withMissingSplitParticipant_returns400() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Missing Participant Share Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String aliceId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        given().contentType(ContentType.JSON).body("""
                {"name": "Bob", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201);

        // Charlie is a participant but NOT included in shares — should be 400
        given().contentType(ContentType.JSON).body("""
                {"name": "Charlie", "nights": 4}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201);

        given().contentType(ContentType.JSON).body("""
                {
                    "amount": 100.00,
                    "description": "Only two get shares",
                    "payerId": "%s",
                    "shares": [
                        {"participantId": "%s", "parts": 2.00}
                    ]
                }
                """.formatted(aliceId, aliceId)).when().post("/api/splits/" + splitId + "/expenses/free").then()
                .statusCode(400).body("type", containsString("invalid-shares"))
                .body("detail", containsString("All participants must have a share specified"));
    }

    /**
     * Story 4.3 AC8: POST /expenses/free with empty shares list returns 400.
     */
    @Test
    void addExpenseFree_withEmptyShares_returns400() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Empty Shares Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String aliceId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        // Try to add expense with empty shares
        given().contentType(ContentType.JSON).body("""
                {
                    "amount": 100.00,
                    "description": "No Shares",
                    "payerId": "%s",
                    "shares": []
                }
                """.formatted(aliceId)).when().post("/api/splits/" + splitId + "/expenses/free").then().statusCode(400);
    }

    /**
     * Story 4.3 AC8: POST /expenses/free allows zero parts (participant owes nothing).
     */
    @Test
    void addExpenseFree_withZeroShareParts_returns201() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Zero Share Test"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String aliceId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        String bobId = given().contentType(ContentType.JSON).body("""
                {"name": "Bob", "nights": 2}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        // Alice has 5 parts, Bob has 0 parts (Bob owes nothing)
        given().contentType(ContentType.JSON).body("""
                {
                    "amount": 100.00,
                    "description": "Alice pays all",
                    "payerId": "%s",
                    "shares": [
                        {"participantId": "%s", "parts": 5.00},
                        {"participantId": "%s", "parts": 0.00}
                    ]
                }
                """.formatted(aliceId, aliceId, bobId)).when().post("/api/splits/" + splitId + "/expenses/free").then()
                .statusCode(201).body("shares", hasSize(2));
    }
}

/**
 * FNS-002.6 Code Review Fix: PUT returns 404 for non-existent split.
 */
