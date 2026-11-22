package org.asymetrik.web.fairnsquare.admin;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Without an OIDC provider, admins sign in with the password.
 */
@QuarkusTest
class AdminPasswordSessionTest {

    @Test
    void saysAdminsSignInWithThePassword() {
        given().when().get("/api/admin/session").then().statusCode(200).body("method", equalTo("PASSWORD"))
                .body("provider", nullValue());
    }

    @Test
    void hasNoProviderSignIn() {
        given().redirects().follow(false).when().get("/api/admin/session/login").then().statusCode(401);
    }
}
