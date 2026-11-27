package org.asymetrik.web.fairnsquare.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.io.IOException;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.admin.AdminBackupsProfile;
import org.asymetrik.web.fairnsquare.backup.internal.InMemoryArchiveStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/**
 * The admin page's backups, on, into a bucket in memory.
 */
@QuarkusTest
@TestProfile(AdminBackupsProfile.class)
class AdminBackupsPageTest extends E2eTestBase {

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
    void backingUpThenComparingWithTheArchive() {
        signIn();
        assertThat(text("No archive yet.")).isVisible();

        button("Back up now").click();

        assertThat(toast("Backup saved")).isVisible();
        assertThat(text("1 archive in the bucket.")).isVisible();
        createSplit("Created after the backup");

        compareButtons().first().click();

        Locator comparison = page.getByRole(AriaRole.STATUS)
                .filter(new Locator.FilterOptions().setHasText("Since the archive of"));
        assertThat(comparison).isVisible();
        assertThat(comparison.locator("div").filter(new Locator.FilterOptions().setHasText("New"))).hasText("New1");
        assertThat(comparison.locator("div").filter(new Locator.FilterOptions().setHasText("Deleted")))
                .hasText("Deleted0");
        assertThat(comparison.locator("div")
                .filter(new Locator.FilterOptions().setHasText("Changed").setHasNotText("Unchanged")))
                        .hasText("Changed0");
    }

    @Test
    void anUnreachableBucketIsShownAndTheBackupFailsWithItsReason() {
        bucket.failWith(new IOException("Could not list the archives in the S3 bucket: connection refused"));
        signIn();

        assertThat(text("Could not list the archives in the S3 bucket: connection refused")).isVisible();
        button("Back up now").click();
        assertThat(toast("connection refused")).isVisible();
        assertThat(button("Back up now")).isEnabled();
    }

    private void signIn() {
        page.navigate("/admin");
        field("Password").fill(TEST_PASSWORD);
        button("Sign in").click();
        assertThat(text("Total splits")).isVisible();
    }

    /** The number under a total's name, e.g. New. */
    private static Locator total(Locator comparison, String name) {
        return comparison.getByRole(AriaRole.DEFINITION).and(comparison.locator("dt:text-is('" + name + "') + dd"));
    }

    private Locator compareButtons() {
        return page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(Pattern.compile("^Compare with the archive of ")));
    }
}
