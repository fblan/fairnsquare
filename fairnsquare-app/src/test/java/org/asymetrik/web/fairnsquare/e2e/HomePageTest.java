package org.asymetrik.web.fairnsquare.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Page;
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
}
