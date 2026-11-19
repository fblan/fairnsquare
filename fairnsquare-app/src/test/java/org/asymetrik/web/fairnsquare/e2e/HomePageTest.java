package org.asymetrik.web.fairnsquare.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.asymetrik.web.fairnsquare.CaptchaTokenTestHelper;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.AriaRole;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class HomePageTest extends E2eTestBase {

    @Test
    void creatingASplitOpensItsParticipantsPage() {
        page.navigate("/");

        field("Split Name").fill("Weekend Trip");
        button("Create Split").click();

        assertThat(page).hasURL(Pattern.compile("/splits/[^/]+/participants$"));
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1))).hasText("Weekend Trip");
    }

    @Test
    void anEmptyNameShowsAnInlineError() {
        page.navigate("/");

        button("Create Split").click();

        assertThat(text("Split name is required")).isVisible();
        assertThat(page).hasURL(root.toString());
    }

    @Test
    void aCreatedSplitIsListedInRecentSplitsAndCanBeResumed() {
        page.navigate("/");
        field("Split Name").fill("Ski week");
        button("Create Split").click();
        assertThat(page).hasURL(Pattern.compile("/participants$"));
        String splitUrl = page.url().replace("/participants", "");

        page.navigate("/");
        assertThat(text("Recent splits")).isVisible();
        assertThat(text("Ski week")).isVisible();

        button("Resume").click();
        assertThat(page).hasURL(splitUrl);
    }

    @Test
    void aDismissedRecentSplitDisappears() {
        page.navigate("/");
        field("Split Name").fill("Old trip");
        button("Create Split").click();
        assertThat(page).hasURL(Pattern.compile("/participants$"));

        page.navigate("/");
        assertThat(text("Old trip")).isVisible();
        button("Dismiss").click();

        assertThat(text("Old trip")).not().isVisible();
        page.reload();
        assertThat(text("Create a New Split")).isVisible();
        assertThat(text("Old trip")).not().isVisible();
    }

    @Test
    void aSplitThatNoLongerExistsDropsOutOfRecentSplits() {
        String kept = createSplit("Still here");
        page.navigate("/");
        page.evaluate("kept => localStorage.setItem('fairnsquare_lastSplits', JSON.stringify("
                + "[{id: 'goneSplitId0123456789', name: 'Gone'}, {id: kept, name: 'Still here'}]))", kept);

        page.reload();

        assertThat(text("Still here")).isVisible();
        assertThat(text("Gone")).not().isVisible();
        assertThat(page.evaluate("() => localStorage.getItem('fairnsquare_lastSplits')").toString())
                .doesNotContain("goneSplitId0123456789");
    }

    @Test
    void aServerErrorOnCreateShowsAnErrorToast() {
        page.route("**/api/splits", route -> fulfill(route, 500, "{\"status\": 500, \"detail\": \"Storage is full\"}"));
        page.navigate("/");

        field("Split Name").fill("Weekend Trip");
        button("Create Split").click();

        assertThat(toast("Storage is full")).isVisible();
        assertThat(page).hasURL(root.toString());
        assertThat(button("Create Split")).isEnabled();
    }

    @Test
    void aRefusedTokenAsksForVerificationAgainThenCreates() {
        AtomicBoolean refused = new AtomicBoolean();
        page.route("**/api/splits", route -> {
            if (refused.compareAndSet(false, true)) {
                fulfill(route, 401, "{\"status\": 401, \"detail\": \"expired\"}");
            } else {
                route.resume();
            }
        });
        page.route("**/api/captcha/challenges/verify",
                route -> fulfill(route, 200, "{\"token\": \"" + CaptchaTokenTestHelper.generateToken() + "\"}"));
        page.navigate("/");
        field("Split Name").fill("Weekend Trip");
        button("Create Split").click();

        dialog("Human Verification").getByRole(AriaRole.IMG).click();

        assertThat(page).hasURL(Pattern.compile("/splits/[^/]+/participants$"));
    }

    private static void fulfill(Route route, int status, String body) {
        route.fulfill(new Route.FulfillOptions().setStatus(status).setContentType("application/json").setBody(body));
    }
}
