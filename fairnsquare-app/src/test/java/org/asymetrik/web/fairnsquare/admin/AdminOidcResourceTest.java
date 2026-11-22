package org.asymetrik.web.fairnsquare.admin;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

/**
 * With an OIDC provider, the admin API wants the provider's session, not the password.
 */
@QuarkusTest
@TestProfile(AdminOidcProfile.class)
class AdminOidcResourceTest {

    /** Matches the %test.admin.password-hash in application.properties. */
    private static final String TEST_PASSWORD = "test-admin-secret-fairnsquare";

    @Test
    void saysAdminsSignInWithTheProvider() {
        given().when().get("/api/admin/session").then().statusCode(200).body("method", equalTo("OIDC")).body("provider",
                equalTo("Pocket ID"));
    }

    @Test
    void refusesThePassword() {
        given().header("X-Admin-Token", TEST_PASSWORD).when().get("/api/admin/stats").then().statusCode(401);
        given().header("X-Admin-Token", TEST_PASSWORD).when().get("/api/admin/module-report").then().statusCode(401);
    }

    @Test
    void signingInGoesToTheProvider() {
        given().redirects().follow(false).when().get("/api/admin/session/login").then().statusCode(302)
                .header("Location", containsString(
                        "/auth/realms/quarkus?response_type=code&client_id=fairnsquare&scope=openid+profile+email+groups"))
                .header("Location",
                        containsString("redirect_uri=http%3A%2F%2Flocalhost%3A8081%2Fapi%2Fadmin%2Fsession%2Flogin"));
    }
}
