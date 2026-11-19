package org.asymetrik.web.fairnsquare.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class AdminPageTest extends E2eTestBase {

    /** Matches the %test.admin.password-hash in application.properties. */
    private static final String TEST_PASSWORD = "test-admin-secret-fairnsquare";

    @Test
    void aWrongPasswordIsRefused() {
        page.navigate("/admin");

        field("Password").fill("wrong");
        button("Sign in").click();

        assertThat(text("Invalid password.")).isVisible();
    }

    @Test
    void signingInShowsTheStatsUntilSignedOut() {
        createSplit("Counted");
        page.navigate("/admin");

        field("Password").fill(TEST_PASSWORD);
        button("Sign in").click();

        assertThat(text("Total splits")).isVisible();
        assertThat(text("Storage used")).isVisible();

        page.reload();
        assertThat(text("Total splits")).isVisible();

        button("Sign out").click();
        assertThat(field("Password")).isVisible();
    }
}
