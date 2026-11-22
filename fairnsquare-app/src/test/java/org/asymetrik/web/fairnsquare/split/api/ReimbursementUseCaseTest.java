package org.asymetrik.web.fairnsquare.split.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
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
 * REIMBURSEMENT expenses through the REST API: paid by one participant for one beneficiary.
 */
@QuarkusTest
@QuarkusTestResource(TempStorageTestResource.class)
class ReimbursementUseCaseTest {

    private String splitId;
    private String aliceId;
    private String bobId;

    @BeforeEach
    void createSplitWithAliceAndBob() {
        RestAssured.requestSpecification = new RequestSpecBuilder()
                .addHeader("X-Captcha-Token", CaptchaTokenTestHelper.generateToken()).build();
        splitId = given().contentType(ContentType.JSON).body("{\"name\": \"Trip\"}").post("/api/splits").then()
                .statusCode(201).extract().path("id");
        aliceId = addParticipant("Alice");
        bobId = addParticipant("Bob");
    }

    @AfterEach
    void resetRequestSpec() {
        RestAssured.requestSpecification = null;
    }

    @Test
    void addReimbursement_chargesTheWholeAmountToTheBeneficiary() {
        given().contentType(ContentType.JSON).body(reimbursement(aliceId, bobId)).post(expenses() + "/reimbursement")
                .then().statusCode(201).body("type", equalTo("REIMBURSEMENT"))
                .body("splitMode", equalTo("REIMBURSEMENT")).body("payerId", equalTo(aliceId))
                .body("beneficiaryId", equalTo(bobId)).body("shares", hasSize(1))
                .body("shares[0].participantId", equalTo(bobId)).body("shares[0].amount", equalTo(40.0f));

        given().get("/api/splits/" + splitId).then().statusCode(200)
                .body("participants.find { it.name == 'Alice' }.balance", equalTo(40.0f))
                .body("participants.find { it.name == 'Bob' }.balance", equalTo(-40.0f))
                .body("expenses[0].beneficiaryId", equalTo(bobId));
    }

    @Test
    void addReimbursement_toThePayer_returns400() {
        given().contentType(ContentType.JSON).body(reimbursement(aliceId, aliceId)).post(expenses() + "/reimbursement")
                .then().statusCode(400).body("type", containsString("invalid-beneficiary"));
    }

    @Test
    void addReimbursement_toAnUnknownParticipant_returns400() {
        given().contentType(ContentType.JSON).body(reimbursement(aliceId, "V1StGXR8_Z5jdHi6B-myT"))
                .post(expenses() + "/reimbursement").then().statusCode(400)
                .body("type", containsString("invalid-beneficiary"));
    }

    @Test
    void addReimbursement_withoutBeneficiary_returns400() {
        given().contentType(ContentType.JSON).body("""
                {"amount": 40.00, "description": "Train", "payerId": "%s"}
                """.formatted(aliceId)).post(expenses() + "/reimbursement").then().statusCode(400);
    }

    @Test
    void updateExpense_toReimbursement_keepsItsIdAndChargesTheBeneficiary() {
        String expenseId = given().contentType(ContentType.JSON).body("""
                {"amount": 40.00, "description": "Train", "payerId": "%s"}
                """.formatted(aliceId)).post(expenses() + "/by-night").then().statusCode(201).extract().path("id");

        given().contentType(ContentType.JSON).body("""
                {"amount": 40.00, "description": "Train", "payerId": "%s", "splitMode": "REIMBURSEMENT",
                 "beneficiaryId": "%s"}
                """.formatted(aliceId, bobId)).put(expenses() + "/" + expenseId).then().statusCode(200)
                .body("id", equalTo(expenseId)).body("type", equalTo("REIMBURSEMENT"))
                .body("beneficiaryId", equalTo(bobId)).body("shares[0].amount", equalTo(40.0f));
    }

    @Test
    void updateExpense_toReimbursementWithoutBeneficiary_returns400() {
        String expenseId = given().contentType(ContentType.JSON).body("""
                {"amount": 40.00, "description": "Train", "payerId": "%s"}
                """.formatted(aliceId)).post(expenses() + "/by-night").then().statusCode(201).extract().path("id");

        given().contentType(ContentType.JSON).body("""
                {"amount": 40.00, "description": "Train", "payerId": "%s", "splitMode": "REIMBURSEMENT"}
                """.formatted(aliceId)).put(expenses() + "/" + expenseId).then().statusCode(400).body("type",
                containsString("invalid-beneficiary"));
    }

    @Test
    void deleteParticipant_whoIsABeneficiary_returns409() {
        given().contentType(ContentType.JSON).body(reimbursement(aliceId, bobId)).post(expenses() + "/reimbursement")
                .then().statusCode(201);

        given().delete("/api/splits/" + splitId + "/participants/" + bobId).then().statusCode(409);
    }

    private String addParticipant(String name) {
        return given().contentType(ContentType.JSON).body("{\"name\": \"%s\", \"nights\": 2}".formatted(name))
                .post("/api/splits/" + splitId + "/participants").then().statusCode(201).extract().path("id");
    }

    private String expenses() {
        return "/api/splits/" + splitId + "/expenses";
    }

    private static String reimbursement(String payerId, String beneficiaryId) {
        return """
                {"amount": 40.00, "description": "Train ticket", "payerId": "%s", "beneficiaryId": "%s"}
                """.formatted(payerId, beneficiaryId);
    }
}
