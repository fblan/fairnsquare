package org.asymetrik.web.fairnsquare.split.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;

import org.asymetrik.web.fairnsquare.CaptchaTokenTestHelper;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.TempStorageTestResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;

/**
 * Integration tests for the settlement endpoints:
 * <ul>
 * <li>{@code GET /api/splits/{id}/settlement} — returns the persisted settlement, or 404 if none computed yet</li>
 * <li>{@code POST /api/splits/{id}/settlement} — calculates, persists, and returns the settlement</li>
 * </ul>
 */
@QuarkusTest
@QuarkusTestResource(TempStorageTestResource.class)
class SettlementUseCaseTest {

    @BeforeEach
    void installCaptchaToken() {
        RestAssured.requestSpecification = new RequestSpecBuilder()
                .addHeader("X-Captcha-Token", CaptchaTokenTestHelper.generateToken()).build();
    }

    @AfterEach
    void resetRequestSpec() {
        RestAssured.requestSpecification = null;
    }

    // --- GET /api/splits/{id}/settlement ---

    @Test
    void getSettlement_withNoPersistedSettlement_returns404() {
        String splitId = createSplitWithParticipantAndExpense();

        given().when().get("/api/splits/" + splitId + "/settlement").then().statusCode(404);
    }

    @Test
    void getSettlement_afterCalculation_returnsPersistedSettlement() {
        String splitId = createSplitWithParticipantAndExpense();

        // Calculate first
        given().contentType(ContentType.JSON).when().post("/api/splits/" + splitId + "/settlement").then()
                .statusCode(200);

        // GET should now return the persisted settlement without recalculating
        given().when().get("/api/splits/" + splitId + "/settlement").then().statusCode(200)
                .body("balances", notNullValue()).body("reimbursements", notNullValue());
    }

    @Test
    void getSettlement_withInvalidSplitId_returns400() {
        given().when().get("/api/splits/not-a-valid-id/settlement").then().statusCode(400);
    }

    @Test
    void getSettlement_withUnknownSplitId_returns404() {
        given().when().get("/api/splits/nonExistentSplitId001/settlement").then().statusCode(404);
    }

    // --- POST /api/splits/{id}/settlement ---

    @Test
    void calculateSettlement_returnsBalancesAndReimbursements() {
        String splitId = createSplitWithParticipantAndExpense();

        given().contentType(ContentType.JSON).when().post("/api/splits/" + splitId + "/settlement").then()
                .statusCode(200).body("balances", hasSize(2)).body("reimbursements", notNullValue());
    }

    @Test
    void calculateSettlement_persistsSettlementRetrievableByGet() {
        String splitId = createSplitWithParticipantAndExpense();

        // Before POST: GET returns 404
        given().when().get("/api/splits/" + splitId + "/settlement").then().statusCode(404);

        // POST calculates and persists
        given().contentType(ContentType.JSON).when().post("/api/splits/" + splitId + "/settlement").then()
                .statusCode(200);

        // After POST: GET returns the persisted result
        given().when().get("/api/splits/" + splitId + "/settlement").then().statusCode(200);
    }

    @Test
    void calculateSettlement_withInvalidSplitId_returns400() {
        given().contentType(ContentType.JSON).when().post("/api/splits/not-a-valid-id/settlement").then()
                .statusCode(400);
    }

    @Test
    void calculateSettlement_withUnknownSplitId_returns404() {
        given().contentType(ContentType.JSON).when().post("/api/splits/nonExistentSplitId001/settlement").then()
                .statusCode(404);
    }

    // --- DELETE /api/splits/{id}/settlement ---

    @Test
    void unsettleSettlement_afterCalculation_returns204() {
        String splitId = createSplitWithParticipantAndExpense();

        given().contentType(ContentType.JSON).when().post("/api/splits/" + splitId + "/settlement").then()
                .statusCode(200);

        given().when().delete("/api/splits/" + splitId + "/settlement").then().statusCode(204);
    }

    @Test
    void unsettleSettlement_afterCalculation_clearsSettlement() {
        String splitId = createSplitWithParticipantAndExpense();

        given().contentType(ContentType.JSON).when().post("/api/splits/" + splitId + "/settlement").then()
                .statusCode(200);

        // Unsettle
        given().when().delete("/api/splits/" + splitId + "/settlement").then().statusCode(204);

        // GET should now return 404 again
        given().when().get("/api/splits/" + splitId + "/settlement").then().statusCode(404);
    }

    @Test
    void unsettleSettlement_withInvalidSplitId_returns400() {
        given().when().delete("/api/splits/not-a-valid-id/settlement").then().statusCode(400);
    }

    @Test
    void unsettleSettlement_withUnknownSplitId_returns404() {
        given().when().delete("/api/splits/nonExistentSplitId001/settlement").then().statusCode(404);
    }

    // --- helpers ---

    /**
     * Creates a split with two participants and one expense, returning the split ID. This gives the settlement
     * calculator enough data to produce meaningful balances and reimbursements.
     */
    private String createSplitWithParticipantAndExpense() {
        String splitId = given().contentType(ContentType.JSON).body("""
                {"name": "Settlement Test Split"}
                """).when().post("/api/splits").then().statusCode(201).extract().path("id");

        String aliceId = given().contentType(ContentType.JSON).body("""
                {"name": "Alice", "nights": 3}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract()
                .path("id");

        given().contentType(ContentType.JSON).body("""
                {"name": "Bob", "nights": 5}
                """).when().post("/api/splits/" + splitId + "/participants").then().statusCode(201);

        given().contentType(ContentType.JSON).body("""
                {"amount": 160.00, "description": "Hotel", "payerId": "%s"}
                """.formatted(aliceId)).when().post("/api/splits/" + splitId + "/expenses/by-night").then()
                .statusCode(201);

        return splitId;
    }
}
