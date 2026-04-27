package org.asymetrik.web.fairnsquare.split.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.equalTo;

import org.asymetrik.web.fairnsquare.CaptchaTokenTestHelper;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.TempStorageTestResource;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Verifies that responses to /api/splits/** carry the required security headers from {@link SplitCacheControlFilter}.
 */
@QuarkusTest
@QuarkusTestResource(TempStorageTestResource.class)
class SplitSecurityHeadersTest {

    @Test
    void splitEndpointsShouldCarryCacheControlHeader() {
        String splitId = createSplit();

        given().when().get("/api/splits/" + splitId).then().statusCode(200).header("Cache-Control",
                "private, no-store");
    }

    @Test
    void splitEndpointsShouldCarryStrictTransportSecurityHeader() {
        String splitId = createSplit();

        given().when().get("/api/splits/" + splitId).then().statusCode(200).header("Strict-Transport-Security",
                "max-age=31536000");
    }

    @Test
    void splitEndpointsShouldCarryReferrerPolicyHeader() {
        String splitId = createSplit();

        given().when().get("/api/splits/" + splitId).then().statusCode(200).header("Referrer-Policy", "no-referrer");
    }

    @Test
    void nonSplitEndpointsShouldNotCarrySplitCacheControlHeader() {
        given().when().get("/q/health").then().header("Cache-Control", not(equalTo("private, no-store")));
    }

    private String createSplit() {
        return given().contentType(ContentType.JSON).header("X-Captcha-Token", CaptchaTokenTestHelper.generateToken())
                .body("""
                        {"name": "Security Header Test Split"}
                        """).when().post("/api/splits").then().statusCode(201).extract().path("id");
    }
}
