package org.asymetrik.web.fairnsquare.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.asymetrik.web.fairnsquare.CaptchaTokenTestHelper;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.AriaRole;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Without a CAPTCHA token, creating a split first asks for the human verification. The challenge API is stubbed: the
 * real one draws a random sum the test cannot read.
 */
@QuarkusTest
class CaptchaTest extends E2eTestBase {

    /** A plain 40×20 PNG. */
    private static final String IMAGE = "iVBORw0KGgoAAAANSUhEUgAAACgAAAAUCAIAAABwJOjsAAAAIklEQVR4nGP4P0CAYdTiUYtHLR61eNTiUYtHLR61eORYDAAP7lcogpD+ZgAAAABJRU5ErkJggg==";

    private final AtomicInteger challenges = new AtomicInteger();

    @Override
    protected boolean withCaptchaToken() {
        return false;
    }

    @Test
    void creatingASplitWithoutTokenAsksForHumanVerification() {
        page.navigate("/");

        field("Split Name").fill("Weekend Trip");
        button("Create Split").click();

        assertThat(dialog("Human Verification")).isVisible();
        assertThat(dialog("Human Verification").getByRole(AriaRole.IMG)).isVisible();
        assertThat(page).hasURL(root.toString());
    }

    @Test
    void solvingTheChallengeCreatesTheSplit() {
        stubChallenges();
        stubVerify(200, "{\"token\": \"" + CaptchaTokenTestHelper.generateToken() + "\"}");
        page.navigate("/");
        field("Split Name").fill("Weekend Trip");
        button("Create Split").click();

        image().click();

        assertThat(dialog("Human Verification")).not().isVisible();
        assertThat(page).hasURL(Pattern.compile("/splits/[^/]+/participants$"));
    }

    @Test
    void aWrongAnswerSaysSoAndShowsANewChallenge() {
        stubChallenges();
        stubVerify(400, "{\"status\": 400, \"detail\": \"wrong\"}");
        page.navigate("/");
        field("Split Name").fill("Weekend Trip");
        button("Create Split").click();
        assertThat(image()).isVisible();

        image().click();

        assertThat(text("Wrong answer — try again!")).isVisible();
        assertThat(image()).isVisible();
        assertThat(challenges.get()).isEqualTo(2);
        assertThat(page).hasURL(root.toString());
    }

    @Test
    void newChallengeLoadsAnotherOne() {
        stubChallenges();
        page.navigate("/");
        field("Split Name").fill("Weekend Trip");
        button("Create Split").click();
        assertThat(text("key: fp-1")).isVisible();

        button(dialog("Human Verification"), "New challenge").click();

        assertThat(text("key: fp-2")).isVisible();
    }

    @Test
    void escapeDoesNotCloseIt() {
        stubChallenges();
        page.navigate("/");
        field("Split Name").fill("Weekend Trip");
        button("Create Split").click();
        assertThat(image()).isVisible();

        page.keyboard().press("Escape");

        assertThat(dialog("Human Verification")).isVisible();
    }

    private Locator image() {
        return dialog("Human Verification").getByRole(AriaRole.IMG);
    }

    /** Each challenge gets a new fingerprint (fp-1, fp-2...), so a test can tell them apart. */
    private void stubChallenges() {
        page.route("**/api/captcha/challenges", route -> {
            int n = challenges.incrementAndGet();
            fulfill(route, 200,
                    "{\"challengeToken\": \"challenge-%d\", \"imageBase64\": \"%s\", \"secretFingerprint\": \"fp-%d\"}"
                            .formatted(n, IMAGE, n));
        });
    }

    private void stubVerify(int status, String body) {
        page.route("**/api/captcha/challenges/verify", route -> fulfill(route, status, body));
    }

    private static void fulfill(Route route, int status, String body) {
        route.fulfill(new Route.FulfillOptions().setStatus(status).setContentType("application/json").setBody(body));
    }
}
