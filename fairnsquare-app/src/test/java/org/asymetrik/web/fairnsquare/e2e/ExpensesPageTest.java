package org.asymetrik.web.fairnsquare.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ExpensesPageTest extends E2eTestBase {

    @Test
    void aSplitWithoutExpensesSaysSo() {
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Alice", 2);

        page.navigate("/splits/" + splitId + "/expenses");

        assertThat(page.getByRole(AriaRole.HEADING).filter(new Locator.FilterOptions().setHasText("No expenses yet")))
                .isVisible();
    }

    @Test
    void addsAnExpensePaidByAChosenParticipant() {
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Alice", 2);
        addParticipant(splitId, "Bob", 2);
        page.navigate("/splits/" + splitId + "/expenses");

        button("Add Expense").click();
        Locator modal = dialog("Add Expense");
        waitForFocusOnDescription(modal);
        field(modal, "Description").fill("Groceries");
        field(modal, "Amount (€)").fill("60");
        field(modal, "Payer").click();
        button(modal, "Bob").click();
        assertThat(field(modal, "Description")).hasValue("Groceries");
        assertThat(field(modal, "Amount (€)")).hasValue("60");
        assertThat(field(modal, "Payer")).hasText("Bob");
        button(modal, "Add Expense").click();

        assertThat(modal).not().isVisible();
        assertThat(toast("Expense added")).isVisible();
        assertThat(text("Groceries")).isVisible();
        assertThat(text("€60.00")).isVisible();
        assertThat(text("Paid by Bob")).isVisible();
    }

    @Test
    void editsAnExpense() {
        String splitId = createSplit("Trip");
        String alice = addParticipant(splitId, "Alice", 2);
        addExpense(splitId, "Groceries", "60", alice);
        page.navigate("/splits/" + splitId + "/expenses");

        button("Edit expense: Groceries").click();
        Locator modal = dialog("Edit Expense");
        waitForFocusOnDescription(modal);
        field(modal, "Description").fill("Dinner");
        field(modal, "Amount (€)").fill("85.50");
        button(modal, "Save Changes").click();

        assertThat(modal).not().isVisible();
        assertThat(toast("Expense updated")).isVisible();
        assertThat(text("Dinner")).isVisible();
        assertThat(text("€85.50")).isVisible();
    }

    @Test
    void deletesAnExpenseAfterConfirmation() {
        String splitId = createSplit("Trip");
        String alice = addParticipant(splitId, "Alice", 2);
        addExpense(splitId, "Groceries", "60", alice);
        addExpense(splitId, "Fuel", "30", alice);
        page.navigate("/splits/" + splitId + "/expenses");

        button("Delete expense: Fuel").click();
        button(dialog("Delete this expense?"), "Delete").click();

        assertThat(toast("Expense deleted")).isVisible();
        assertThat(button("Edit expense: Fuel")).not().isVisible();
        assertThat(button("Edit expense: Groceries")).isVisible();
    }

    @Test
    void filtersExpensesByPayer() {
        String splitId = createSplit("Trip");
        String alice = addParticipant(splitId, "Alice", 2);
        String bob = addParticipant(splitId, "Bob", 2);
        addExpense(splitId, "Groceries", "60", alice);
        addExpense(splitId, "Fuel", "30", bob);
        page.navigate("/splits/" + splitId + "/expenses");

        field("Filter by payer").selectOption(bob);

        assertThat(button("Edit expense: Fuel")).isVisible();
        assertThat(button("Edit expense: Groceries")).not().isVisible();

        button("Clear filters").first().click();
        assertThat(button("Edit expense: Groceries")).isVisible();
    }

    /**
     * The window focuses Description shortly after it opens; typing before that would follow the focus into Description
     * (#168).
     */
    private static void waitForFocusOnDescription(Locator modal) {
        assertThat(field(modal, "Description")).isFocused();
    }
}
