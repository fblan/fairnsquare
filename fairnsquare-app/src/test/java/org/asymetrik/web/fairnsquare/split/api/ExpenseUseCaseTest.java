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

import org.asymetrik.web.fairnsquare.sharedkernel.persistence.TenantPathResolver;
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

}
