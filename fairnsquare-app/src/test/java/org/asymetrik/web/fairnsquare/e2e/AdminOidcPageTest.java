package org.asymetrik.web.fairnsquare.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.asymetrik.web.fairnsquare.admin.AdminOidcProfile;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.AriaRole;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/**
 * The admin page when admins sign in with the OIDC provider (WireMock's: its login form signs alice in).
 */
@QuarkusTest
@TestProfile(AdminOidcProfile.class)
class AdminOidcPageTest extends E2eTestBase {

    @Test
    void signingInWithTheProviderShowsTheStatsUntilSignedOut() {
        page.navigate("/admin");
        assertThat(field("Password")).not().isAttached();

        signInWithTheProvider();

        assertThat(page).hasURL(root + "admin");
        assertThat(text("Total splits")).isVisible();
        page.reload();
        assertThat(text("Total splits")).isVisible();

        button("Sign out").click();
        assertThat(providerSignIn()).isVisible();
        page.reload();
        assertThat(providerSignIn()).isVisible();
        assertThat(text("Total splits")).not().isVisible();
    }

    @Test
    void theModuleReportOpensWithTheProvidersSession() {
        page.navigate("/admin");
        signInWithTheProvider();

        Page report = page.context().waitForPage(() -> button("Module report ↗").click());

        assertThat(report).hasTitle("Module report · FairNSquare");
        assertThat(report.frameLocator("iframe").getByText("No violation of the module rules")).isVisible();
        report.close();
    }

    @Test
    void anAccountThatIsNotAnAdminIsSignedOut() {
        page.route("**/api/admin/stats", route -> route.fulfill(new Route.FulfillOptions().setStatus(403)
                .setContentType("application/json").setBody("{\"status\": 403, \"detail\": \"not an admin\"}")));
        page.navigate("/admin");
        signInWithTheProvider();

        assertThat(text("This account is not an admin.")).isVisible();
        assertThat(providerSignIn()).isVisible();
        page.unrouteAll();
        page.reload();
        // The session was ended: the stats need a new sign-in.
        assertThat(providerSignIn()).isVisible();
        assertThat(text("Total splits")).not().isVisible();
    }

    private void signInWithTheProvider() {
        providerSignIn().click();
        page.locator("#username").fill("alice");
        page.locator("#login").click();
    }

    private Locator providerSignIn() {
        return page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Sign in with Pocket ID"));
    }
}
