package org.asymetrik.web.fairnsquare.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class NavigationTest extends E2eTestBase {

    private Locator nav() {
        return page.getByRole(AriaRole.NAVIGATION, new Page.GetByRoleOptions().setName("Split navigation"));
    }

    @Test
    void theTabsMoveBetweenTheSplitPages() {
        String splitId = createSplit("Road trip");
        addParticipant(splitId, "Alice", 2);
        page.navigate("/splits/" + splitId);
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1))).hasText("Road trip");

        button(nav(), "Participants").click();
        assertThat(page).hasURL(Pattern.compile("/splits/" + splitId + "/participants$"));

        button(nav(), "Expenses").click();
        assertThat(page).hasURL(Pattern.compile("/splits/" + splitId + "/expenses$"));

        button(nav(), "Settlement").click();
        assertThat(page).hasURL(Pattern.compile("/splits/" + splitId + "/settlement$"));

        button(nav(), "Home").click();
        assertThat(page).hasURL(Pattern.compile("/splits/" + splitId + "$"));
    }

    @Test
    void theDashboardLinksToExpensesAndParticipants() {
        String splitId = createSplit("Road trip");
        String alice = addParticipant(splitId, "Alice", 2);
        addExpense(splitId, "Fuel", "50", alice);
        page.navigate("/splits/" + splitId);

        button("View all expenses").click();
        assertThat(page).hasURL(Pattern.compile("/expenses$"));

        page.goBack();
        button("View all participants").click();
        assertThat(page).hasURL(Pattern.compile("/participants$"));

        page.goBack();
        button("View settlement").click();
        assertThat(page).hasURL(Pattern.compile("/settlement$"));
    }

    @Test
    void expensesNeedAParticipantFirst() {
        String splitId = createSplit("Road trip");
        page.navigate("/splits/" + splitId + "/participants");

        button(nav(), "Expenses").click();

        assertThat(dialog("A participant is required")).isVisible();
        assertThat(page).hasURL(Pattern.compile("/participants$"));
    }

    @Test
    void anUnknownSplitGoesBackHome() {
        page.navigate("/splits/doesNotExist123456789");

        assertThat(toast("Split not found — create a new one.")).isVisible();
        assertThat(page).hasURL(root.toString());
    }

    @Test
    void closingASplitGoesBackHome() {
        String splitId = createSplit("Road trip");
        page.navigate("/splits/" + splitId);

        button("Close split").click();

        assertThat(page).hasURL(root.toString());
    }
}
