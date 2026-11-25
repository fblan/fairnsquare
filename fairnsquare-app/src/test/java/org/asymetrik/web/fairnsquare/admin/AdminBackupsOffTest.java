package org.asymetrik.web.fairnsquare.admin;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Without a bucket, backups are off.
 */
@QuarkusTest
class AdminBackupsOffTest {

    /** Matches the %test.admin.password-hash in application.properties. */
    private static final String TEST_PASSWORD = "test-admin-secret-fairnsquare";

    @Test
    void saysBackupsAreOff() {
        given().header("X-Admin-Token", TEST_PASSWORD).when().get("/api/admin/backups").then().statusCode(200)
                .body("enabled", equalTo(false)).body("problem", nullValue()).body("archives", hasSize(0));
    }

    @Test
    void refusesToBackUp() {
        given().header("X-Admin-Token", TEST_PASSWORD).when().post("/api/admin/backups").then().statusCode(409)
                .body("detail", equalTo("Backups are off: no S3 bucket is configured."));
    }
}
