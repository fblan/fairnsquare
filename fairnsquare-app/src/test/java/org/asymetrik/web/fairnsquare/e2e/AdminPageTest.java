package org.asymetrik.web.fairnsquare.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.AriaRole;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class AdminPageTest extends E2eTestBase {

    /** Matches the %test.admin.password-hash in application.properties. */
    private static final String TEST_PASSWORD = "test-admin-secret-fairnsquare";

    /** Three splits: a created first, b updated last, c updated first. Long dates, as in real data. */
    private static final String STATS = """
            {"totalSplits": 3, "splitCountLimit": 1000, "usedStorageBytes": 1536,
             "maxStorageBytes": 524288000, "lastUpdated": "2026-12-01T22:22:22Z", "splits": [
              {"idHash": "bbbbbbbbbbbbbbbb", "createdAt": "2026-10-11T22:22:22Z", "updatedAt": "2026-12-01T22:22:22Z",
               "participantCount": 2, "expenseCount": 1, "expenseTypes": ["BY_NIGHT"]},
              {"idHash": "aaaaaaaaaaaaaaaa", "createdAt": "2026-10-10T22:22:22Z", "updatedAt": "2026-11-20T22:22:22Z",
               "participantCount": 2, "expenseCount": 1, "expenseTypes": ["BY_NIGHT"]},
              {"idHash": "cccccccccccccccc", "createdAt": "2026-10-12T22:22:22Z", "updatedAt": "2026-11-15T22:22:22Z",
               "participantCount": 2, "expenseCount": 1, "expenseTypes": ["BY_NIGHT"]}
            ]}""";

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
        page.reload();
        assertThat(field("Password")).isVisible();
    }

    @Test
    void theModuleReportOpensInANewTab() {
        page.navigate("/admin");
        field("Password").fill(TEST_PASSWORD);
        button("Sign in").click();

        Page report = page.context().waitForPage(() -> button("Module report ↗").click());

        assertThat(report).hasTitle("Module report · FairNSquare");
        assertThat(report.locator("iframe")).hasAttribute("sandbox", "allow-scripts");
        assertThat(report.frameLocator("iframe").getByRole(AriaRole.HEADING,
                new FrameLocator.GetByRoleOptions().setName("Module report").setExact(true).setLevel(1))).isVisible();
        assertThat(report.frameLocator("iframe").getByText("No violation of the module rules",
                new FrameLocator.GetByTextOptions())).isVisible();
        // Sandboxed, the report has its own origin: it can't read the admin token kept in this one's storage.
        Object storage = report.frames().get(1).evaluate(
                "() => { try { return sessionStorage.getItem('admin_token'); } catch (e) { return e.name; } }");
        assertThat(storage).isEqualTo("SecurityError");
        report.close();
    }

    @Test
    void aModuleReportThatFailsClosesItsTabAndSaysSo() {
        page.route("**/api/admin/module-report", route -> route.fulfill(new Route.FulfillOptions().setStatus(500)));
        page.navigate("/admin");
        field("Password").fill(TEST_PASSWORD);
        button("Sign in").click();

        Page report = page.context().waitForPage(() -> button("Module report ↗").click());

        assertThat(toast("Failed to load the module report.")).isVisible();
        page.waitForCondition(report::isClosed);
        assertThat(page.context().pages()).containsExactly(page);
    }

    @Test
    void saysWhenBackupsAreOff() {
        page.navigate("/admin");
        field("Password").fill(TEST_PASSWORD);
        button("Sign in").click();

        assertThat(text("Backups are off: no S3 bucket is configured.")).isVisible();
        assertThat(button("Back up now")).isHidden();
    }

    @Test
    void theSignInButtonWaitsForAPassword() {
        page.navigate("/admin");

        assertThat(button("Sign in")).isDisabled();
        field("Password").fill("x");
        assertThat(button("Sign in")).isEnabled();
    }

    @Test
    void saysWhenAdminAccessIsNotConfigured() {
        stubStats(503, "{\"status\": 503, \"detail\": \"not configured\"}");
        page.navigate("/admin");

        field("Password").fill("anything");
        button("Sign in").click();

        assertThat(text("Admin access is not configured on this server.")).isVisible();
    }

    @Test
    void showsTheServerMessageOnOtherErrors() {
        stubStats(500, "{\"status\": 500, \"detail\": \"Disk on fire\"}");
        page.navigate("/admin");

        field("Password").fill("anything");
        button("Sign in").click();

        assertThat(text("Disk on fire")).isVisible();
    }

    @Test
    void theTablesSortTheSplitsByAge() {
        stubStats(200, STATS);
        page.navigate("/admin");
        field("Password").fill("anything");
        button("Sign in").click();

        assertThat(text("3")).isVisible();
        assertThat(text("1.50 KB")).isVisible();
        assertThat(hashesIn("Oldest created splits")).containsExactly("aaaaaaaaaaaaaaaa", "bbbbbbbbbbbbbbbb",
                "cccccccccccccccc");
        assertThat(hashesIn("Oldest updated splits")).containsExactly("cccccccccccccccc", "aaaaaaaaaaaaaaaa",
                "bbbbbbbbbbbbbbbb");
        assertThat(hashesIn("Most recently updated splits")).containsExactly("bbbbbbbbbbbbbbbb", "aaaaaaaaaaaaaaaa",
                "cccccccccccccccc");
    }

    @Test
    void theStatsFitAPhoneScreen() {
        stubStats(200, STATS);
        page.setViewportSize(390, 844);
        page.navigate("/admin");
        field("Password").fill("anything");
        button("Sign in").click();
        assertThat(text("Most recently updated splits")).isVisible();

        Object overflow = page
                .evaluate("() => document.documentElement.scrollWidth - document.documentElement.clientWidth");
        assertThat(((Number) overflow).intValue()).as("horizontal overflow in px").isLessThanOrEqualTo(0);
    }

    /** Answers the admin stats call with this status and body, without reaching the server. */
    private void stubStats(int status, String body) {
        page.route("**/api/admin/stats", route -> route.fulfill(
                new Route.FulfillOptions().setStatus(status).setContentType("application/json").setBody(body)));
    }

    /** The id hashes listed in the table under this heading, in order. */
    private List<String> hashesIn(String heading) {
        Locator section = page.getByRole(AriaRole.REGION, new Page.GetByRoleOptions().setName(heading));
        Locator rows = section.getByRole(AriaRole.ROW);
        assertThat(rows).hasCount(4);
        return rows.all().stream().skip(1).map(row -> row.getByRole(AriaRole.CELL).first().textContent().trim())
                .toList();
    }
}
