package org.asymetrik.web.fairnsquare.admin;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * GET /api/admin/module-report: modular's report of this build, for the admin only.
 */
@QuarkusTest
class ModuleReportResourceTest {

    /** Matches the %test.admin.password-hash in application.properties. */
    private static final String TEST_PASSWORD = "test-admin-secret-fairnsquare";

    @Test
    void givesTheModuleReportOfTheBuildToTheAdmin() {
        given().header("X-Admin-Token", TEST_PASSWORD).when().get("/api/admin/module-report").then().statusCode(200)
                .contentType(containsString("text/html")).body(containsString("<h1>Module report</h1>"))
                .body(containsString("<h3>split</h3>"), containsString("<h3>admin</h3>"))
                .header("Content-Security-Policy", equalTo("sandbox allow-scripts; default-src 'none'; "
                        + "script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:"));
    }

    @Test
    void refusesWithoutTheAdminToken() {
        given().when().get("/api/admin/module-report").then().statusCode(401);
        given().header("X-Admin-Token", "wrong").when().get("/api/admin/module-report").then().statusCode(401);
    }
}
