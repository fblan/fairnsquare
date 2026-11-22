package org.asymetrik.web.fairnsquare.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.restassured.RestAssured.given;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

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
        assertThat(field(modal, "Description")).isFocused();
        field(modal, "Description").fill("Groceries");
        field(modal, "Amount (€)").fill("60");
        field(modal, "Payer").selectOption(new SelectOption().setLabel("Bob"));
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
        assertThat(button(modal, "Save Changes")).isDisabled();
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

    @Test
    void filtersByBeneficiaryAndKeepsTheFiltersInTheAddress() {
        String splitId = createSplit("Trip");
        String alice = addParticipant(splitId, "Alice", 2);
        String bob = addParticipant(splitId, "Bob", 2);
        addExpense(splitId, "Groceries", "60", alice);
        addFreeExpense(splitId, "Alice's book", "20", alice, alice, bob);
        page.navigate("/splits/" + splitId + "/expenses");

        field("Filter by beneficiary").selectOption(bob);

        assertThat(button("Edit expense: Groceries")).isVisible();
        assertThat(button("Edit expense: Alice's book")).not().isVisible();
        assertThat(text("1 of 2 expenses")).isVisible();
        assertThat(page).hasURL(Pattern.compile("beneficiary=" + bob));

        page.reload();
        assertThat(field("Filter by beneficiary")).hasValue(bob);
        assertThat(button("Edit expense: Alice's book")).not().isVisible();
    }

    @Test
    void aFilterWithoutResultsOffersToClearIt() {
        String splitId = createSplit("Trip");
        String alice = addParticipant(splitId, "Alice", 2);
        String bob = addParticipant(splitId, "Bob", 2);
        addExpense(splitId, "Groceries", "60", alice);
        page.navigate("/splits/" + splitId + "/expenses?payer=" + bob);

        assertThat(text("No matching expenses")).isVisible();
        button("Clear filters").last().click();

        assertThat(button("Edit expense: Groceries")).isVisible();
        assertThat(field("Filter by payer")).hasValue("");
    }

    @Test
    void anEmptyFormShowsItsErrorsAndSendsNothing() {
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Alice", 2);
        page.navigate("/splits/" + splitId + "/expenses");

        button("Add Expense").click();
        Locator modal = dialog("Add Expense");
        button(modal, "Add Expense").click();

        assertThat(modal.getByText("Description is required")).isVisible();
        assertThat(modal.getByText("Amount must be at least €0.01")).isVisible();
        assertThat(field(modal, "Description")).hasAttribute("aria-invalid", "true");
        assertThat(page.getByRole(AriaRole.ALERT)).hasCount(0);
        assertThat(modal).isVisible();
    }

    @Test
    void manualModeSharesTheExpenseByParts() {
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Alice", 2);
        addParticipant(splitId, "Bob", 3);
        page.navigate("/splits/" + splitId + "/expenses");
        button("Add Expense").click();
        Locator modal = dialog("Add Expense");
        field(modal, "Description").fill("Boat");
        field(modal, "Amount (€)").fill("50");

        field(modal, "Manual").check();
        assertThat(modal.getByText("Alice: 1", new Locator.GetByTextOptions().setExact(true))).isVisible();
        button(modal, "Edit shares").click();
        Locator shares = dialog("Edit Shares");
        button(shares, "Copy nights shares").click();
        button(shares, "Apply").click();
        assertThat(field(shares, "Alice parts")).hasValue("2");
        assertThat(field(shares, "Bob parts")).hasValue("3");
        button(shares, "Confirm").click();

        assertThat(shares).not().isVisible();
        assertThat(modal.getByText("Alice: 2, Bob: 3", new Locator.GetByTextOptions().setExact(true))).isVisible();
        button(modal, "Add Expense").click();

        assertThat(toast("Expense added")).isVisible();
        assertThat(text("✏️ Manual")).isVisible();
        assertThat(text("Alice: €20.00 · Bob: €30.00")).isVisible();
    }

    @Test
    void manualModeCopiesThePersonsShares() {
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Alice", 3, 2);
        addParticipant(splitId, "Bob", 2, 1);
        page.navigate("/splits/" + splitId + "/expenses");
        button("Add Expense").click();
        Locator modal = dialog("Add Expense");
        field(modal, "Description").fill("Restaurant");
        field(modal, "Amount (€)").fill("90");
        field(modal, "Manual").check();
        button(modal, "Edit shares").click();
        Locator shares = dialog("Edit Shares");

        button(shares, "Copy persons shares").click();
        assertThat(shares.getByText("share (number of persons)", new Locator.GetByTextOptions().setExact(true)))
                .isVisible();
        button(shares, "Apply").click();
        assertThat(field(shares, "Alice parts")).hasValue("2");
        assertThat(field(shares, "Bob parts")).hasValue("1");
        assertThat(field(shares, "Include Bob")).isChecked();
        button(shares, "Confirm").click();
        assertThat(modal.getByText("Alice: 2, Bob: 1", new Locator.GetByTextOptions().setExact(true))).isVisible();
        button(modal, "Add Expense").click();

        assertThat(toast("Expense added")).isVisible();
        assertThat(text("Alice: €60.00 · Bob: €30.00")).isVisible();
    }

    @Test
    void cancellingACopyKeepsTheParts() {
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Alice", 3, 2);
        page.navigate("/splits/" + splitId + "/expenses");
        button("Add Expense").click();
        Locator modal = dialog("Add Expense");
        field(modal, "Manual").check();
        button(modal, "Edit shares").click();
        Locator shares = dialog("Edit Shares");

        button(shares, "Copy persons shares").click();
        button(shares.getByRole(AriaRole.ALERT), "Cancel").click();

        assertThat(shares.getByRole(AriaRole.ALERT)).not().isVisible();
        assertThat(field(shares, "Alice parts")).hasValue("1");
    }

    @Test
    void choosingManualBringsTheEditSharesButtonIntoView() {
        page.setViewportSize(390, 844);
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Alice", 2);
        page.navigate("/splits/" + splitId + "/expenses");
        button("Add Expense").click();
        Locator modal = dialog("Add Expense");

        modal.getByText("Manual", new Locator.GetByTextOptions().setExact(true)).click();

        assertThat(button(modal, "Edit shares")).isFocused();
        assertThat(button(modal, "Edit shares")).isInViewport();
    }

    @Test
    void aReimbursementChargesTheWholeAmountToOneParticipant() {
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Alice", 2);
        addParticipant(splitId, "Bob", 2);
        addParticipant(splitId, "Carol", 2);
        page.navigate("/splits/" + splitId + "/expenses");
        button("Add Expense").click();
        Locator modal = dialog("Add Expense");
        field(modal, "Description").fill("Train ticket");
        field(modal, "Amount (€)").fill("40");

        modal.getByText("Reimbursement", new Locator.GetByTextOptions().setExact(true)).click();
        Locator paidFor = field(modal, "Paid for");
        assertThat(paidFor).isFocused();
        assertThat(paidFor.locator("option")).hasText(new String[] { "Bob", "Carol" });
        paidFor.selectOption(new SelectOption().setLabel("Carol"));
        button(modal, "Add Expense").click();

        assertThat(toast("Expense added")).isVisible();
        assertThat(text("Train ticket · €40.00 · Paid by Alice for Carol")).isVisible();
        assertThat(text("↔️ Reimbursement")).isVisible();
        assertThat(text("Carol: €40.00")).isVisible();
    }

    @Test
    void changingThePayerTakesThemOutOfPaidFor() {
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Alice", 2);
        addParticipant(splitId, "Bob", 2);
        page.navigate("/splits/" + splitId + "/expenses");
        button("Add Expense").click();
        Locator modal = dialog("Add Expense");
        field(modal, "Reimbursement").check();
        assertThat(field(modal, "Paid for").locator("option")).hasText(new String[] { "Bob" });

        field(modal, "Payer").selectOption(new SelectOption().setLabel("Bob"));

        assertThat(field(modal, "Paid for").locator("option")).hasText(new String[] { "Alice" });
    }

    @Test
    void aReimbursementOpensForEditingWithItsBeneficiary() {
        String splitId = createSplit("Trip");
        String aliceId = addParticipant(splitId, "Alice", 2);
        addParticipant(splitId, "Bob", 2);
        String carolId = addParticipant(splitId, "Carol", 2);
        given().contentType(ContentType.JSON).body("""
                {"amount": 40.00, "description": "Train ticket", "payerId": "%s", "beneficiaryId": "%s"}
                """.formatted(aliceId, carolId)).post("/api/splits/" + splitId + "/expenses/reimbursement").then()
                .statusCode(201);
        page.navigate("/splits/" + splitId + "/expenses");

        button("Edit expense: Train ticket").click();
        Locator modal = dialog("Edit Expense");
        assertThat(field(modal, "Reimbursement")).isChecked();
        assertThat(field(modal, "Paid for")).hasValue(carolId);
        field(modal, "Paid for").selectOption(new SelectOption().setLabel("Bob"));
        button(modal, "Save Changes").click();

        assertThat(toast("Expense updated")).isVisible();
        assertThat(text("Bob: €40.00")).isVisible();
    }

    @Test
    void aReimbursementNeedsAnotherParticipant() {
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Alice", 2);
        page.navigate("/splits/" + splitId + "/expenses");
        button("Add Expense").click();
        Locator modal = dialog("Add Expense");
        field(modal, "Description").fill("Train ticket");
        field(modal, "Amount (€)").fill("40");

        field(modal, "Reimbursement").check();
        button(modal, "Add Expense").click();

        assertThat(modal.getByText("A reimbursement needs another participant")).isVisible();
        assertThat(modal).isVisible();
    }

    @Test
    void clickingASplitModeLabelSelectsIt() {
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Alice", 2);
        page.navigate("/splits/" + splitId + "/expenses");
        button("Add Expense").click();
        Locator modal = dialog("Add Expense");

        modal.getByText("By Participant members", new Locator.GetByTextOptions().setExact(true)).click();

        assertThat(field(modal, "By Participant members")).isChecked();
        assertThat(field(modal, "By Night")).not().isChecked();
    }

    @Test
    void anUntouchedWindowClosesWithEscape() {
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Alice", 2);
        page.navigate("/splits/" + splitId + "/expenses");
        button("Add Expense").click();
        Locator modal = dialog("Add Expense");
        assertThat(field(modal, "Description")).isFocused();

        page.keyboard().press("Escape");

        assertThat(modal).not().isVisible();
        assertThat(dialog("Discard changes?")).not().isVisible();
    }

    @Test
    void closingAfterChangesAsksToDiscardThem() {
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Alice", 2);
        page.navigate("/splits/" + splitId + "/expenses");
        button("Add Expense").click();
        Locator modal = dialog("Add Expense");
        field(modal, "Description").fill("Groceries");

        button(modal, "Cancel").click();
        Locator discard = dialog("Discard changes?");
        button(discard, "Keep editing").click();
        assertThat(discard).not().isVisible();
        assertThat(field(modal, "Description")).hasValue("Groceries");

        button(modal, "Cancel").click();
        button(discard, "Discard").click();
        assertThat(modal).not().isVisible();
    }

    @Test
    void deletesFromTheEditWindow() {
        String splitId = createSplit("Trip");
        String alice = addParticipant(splitId, "Alice", 2);
        addExpense(splitId, "Groceries", "60", alice);
        page.navigate("/splits/" + splitId + "/expenses");

        button("Edit expense: Groceries").click();
        button(dialog("Edit Expense"), "Delete expense").click();
        button(dialog("Delete this expense?"), "Delete").click();

        assertThat(toast("Expense deleted")).isVisible();
        assertThat(dialog("Edit Expense")).not().isVisible();
        assertThat(text("No expenses yet")).isVisible();
    }

    @Test
    void eachSplitModeExplainsItself() {
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Alice", 2);
        page.navigate("/splits/" + splitId + "/expenses");
        button("Add Expense").click();

        button(dialog("Add Expense"), "Info about Manual").click();
        Locator info = dialog("Manual");
        assertThat(info.getByText("You define the exact parts for each participant", new Locator.GetByTextOptions()))
                .isVisible();
        button(info, "Got it").click();

        assertThat(info).not().isVisible();
        assertThat(dialog("Add Expense")).isVisible();
    }

    @Test
    void arrowKeysStepTheAmountByFiftyCents() {
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Alice", 2);
        page.navigate("/splits/" + splitId + "/expenses");
        button("Add Expense").click();
        Locator amount = field(dialog("Add Expense"), "Amount (€)");

        amount.fill("12.30");
        amount.press("ArrowUp");
        assertThat(amount).hasValue("12.8");
        amount.press("ArrowDown");
        amount.press("ArrowDown");
        assertThat(amount).hasValue("11.8");
    }

    @Test
    void leavingAnEmptyFieldShowsNoErrorButAWrongValueDoes() {
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Alice", 2);
        page.navigate("/splits/" + splitId + "/expenses");
        button("Add Expense").click();
        Locator modal = dialog("Add Expense");

        field(modal, "Amount (€)").focus();
        assertThat(modal.getByText("Description is required")).not().isVisible();
        field(modal, "Amount (€)").fill("0");
        field(modal, "Description").focus();

        assertThat(modal.getByText("Amount must be at least €0.01")).isVisible();
        assertThat(modal.getByText("Description is required")).not().isVisible();
    }

    @Test
    void emptyingAFieldOfAnExistingExpenseSaysItIsRequired() {
        String splitId = createSplit("Trip");
        String alice = addParticipant(splitId, "Alice", 2);
        addExpense(splitId, "Groceries", "60", alice);
        page.navigate("/splits/" + splitId + "/expenses");
        button("Edit expense: Groceries").click();
        Locator modal = dialog("Edit Expense");

        field(modal, "Description").fill("");
        field(modal, "Amount (€)").focus();

        assertThat(modal.getByText("Description is required")).isVisible();
        assertThat(button(modal, "Save Changes")).isDisabled();
    }

    /** Adds a Manual (FREE) expense where only the payer takes a part (the other participant gets 0). */
    private static void addFreeExpense(String splitId, String description, String amount, String payerId,
            String onlyBeneficiary, String other) {
        given().contentType(ContentType.JSON).body("""
                {"amount": %s, "description": "%s", "payerId": "%s",
                 "shares": [{"participantId": "%s", "parts": 1}, {"participantId": "%s", "parts": 0}]}
                """.formatted(amount, description, payerId, onlyBeneficiary, other))
                .post("/api/splits/" + splitId + "/expenses/free").then().statusCode(201);
    }
}
