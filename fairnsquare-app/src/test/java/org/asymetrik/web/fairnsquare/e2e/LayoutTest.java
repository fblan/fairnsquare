package org.asymetrik.web.fairnsquare.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Every page fits the screen width on a phone and on a desktop: nothing makes the page scroll sideways.
 */
@QuarkusTest
class LayoutTest extends E2eTestBase {

    @ParameterizedTest(name = "{0} at {1}x{2}")
    @CsvSource({ "/, 390, 844", "/, 1280, 800", //
            "dashboard, 390, 844", "dashboard, 1280, 800", //
            "participants, 390, 844", "participants, 1280, 800", //
            "expenses, 390, 844", "expenses, 1280, 800", //
            "settlement, 390, 844", "settlement, 1280, 800", //
            "/admin, 390, 844", "/admin, 1280, 800" })
    void thePageFitsTheScreenWidth(String target, int width, int height) {
        String splitId = createSplit("A split with a rather long name to check wrapping");
        String alice = addParticipant(splitId, "Alexandrina-Konstantinopoulou", 3);
        addParticipant(splitId, "Bob", 2);
        addExpense(splitId, "Groceries for the whole week at the big supermarket", "123.45", alice);
        page.setViewportSize(width, height);

        page.navigate(switch (target) {
            case "dashboard" -> "/splits/" + splitId;
            case "participants", "expenses", "settlement" -> "/splits/" + splitId + "/" + target;
            default -> target;
        });
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1))).isVisible();
        page.waitForLoadState();

        assertNoHorizontalOverflow();
    }

    @Test
    void theExpenseFiltersFitAPhoneWithALongNameSelected() {
        String splitId = createSplit("Trip");
        String longName = addParticipant(splitId, "Alexandrina-Konstantinopoulou", 3);
        addParticipant(splitId, "Bob", 2);
        addExpense(splitId, "Groceries", "60", longName);
        page.setViewportSize(390, 844);
        page.navigate("/splits/" + splitId + "/expenses");

        field("Filter by payer").selectOption(longName);
        field("Filter by beneficiary").selectOption(longName);

        assertThat(button("Clear filters")).isVisible();
        assertNoHorizontalOverflow();
    }

    private void assertNoHorizontalOverflow() {
        Object overflow = page
                .evaluate("() => document.documentElement.scrollWidth - document.documentElement.clientWidth");
        assertThat(((Number) overflow).intValue()).as("horizontal overflow in px").isLessThanOrEqualTo(0);
    }
}
