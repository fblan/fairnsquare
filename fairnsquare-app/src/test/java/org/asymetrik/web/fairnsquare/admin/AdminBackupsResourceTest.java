package org.asymetrik.web.fairnsquare.admin;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;

import java.io.IOException;

import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.backup.internal.InMemoryArchiveStore;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.TempStorageTestResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/**
 * /api/admin/backups with backups on, into a bucket in memory.
 */
@QuarkusTest
@TestProfile(AdminBackupsProfile.class)
@QuarkusTestResource(TempStorageTestResource.class)
class AdminBackupsResourceTest {

    /** Matches the %test.admin.password-hash in application.properties. */
    private static final String TEST_PASSWORD = "test-admin-secret-fairnsquare";

    @Inject
    InMemoryArchiveStore bucket;

    @BeforeEach
    @AfterEach
    void emptyTheBucket() {
        bucket.failWith(null);
        bucket.objects().clear();
    }

    @Test
    void backsUpListsAndCompares() {
        String name = given().header("X-Admin-Token", TEST_PASSWORD).when().post("/api/admin/backups").then()
                .statusCode(201).body("name", matchesPattern("splits-.*Z\\.fnsbackup")).extract().path("name");

        given().header("X-Admin-Token", TEST_PASSWORD).when().get("/api/admin/backups").then().statusCode(200)
                .body("enabled", equalTo(true)).body("problem", nullValue()).body("archives", hasSize(1))
                .body("archives[0].name", equalTo(name));

        int current = given().header("X-Admin-Token", TEST_PASSWORD).when()
                .get("/api/admin/backups/" + name + "/comparison").then().statusCode(200).body("added", equalTo(0))
                .body("deleted", equalTo(0)).body("changed", equalTo(0)).extract().path("current");
        given().header("X-Admin-Token", TEST_PASSWORD).when().get("/api/admin/backups/" + name + "/comparison").then()
                .body("archived", equalTo(current)).body("unchanged", equalTo(current));
    }

    @Test
    void anUnreachableBucketIsTheProblem() {
        bucket.failWith(new IOException("Could not list the archives in the S3 bucket: connection refused"));

        given().header("X-Admin-Token", TEST_PASSWORD).when().get("/api/admin/backups").then().statusCode(200)
                .body("enabled", equalTo(true))
                .body("problem", equalTo("Could not list the archives in the S3 bucket: connection refused"))
                .body("archives", hasSize(0));
        given().header("X-Admin-Token", TEST_PASSWORD).when().post("/api/admin/backups").then().statusCode(502)
                .body("detail", matchesPattern(".*connection refused.*"));
    }

    @Test
    void anUnknownArchiveIsNotFound() {
        given().header("X-Admin-Token", TEST_PASSWORD).when()
                .get("/api/admin/backups/splits-2020-01-01T00-00-00-000Z.fnsbackup/comparison").then().statusCode(404);
    }

    @Test
    void isForAdminsOnly() {
        given().when().get("/api/admin/backups").then().statusCode(401);
        given().when().post("/api/admin/backups").then().statusCode(401);
        given().header("X-Admin-Token", "wrong").when().get("/api/admin/backups/x/comparison").then().statusCode(401);
    }
}
