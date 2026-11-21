package org.asymetrik.web.fairnsquare.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import static io.restassured.RestAssured.given;

import org.asymetrik.web.fairnsquare.testing.FairnsquareTest;

import org.junit.jupiter.api.Test;

import com.microsoft.playwright.options.AriaRole;

import io.restassured.http.ContentType;

@FairnsquareTest
class ParticipantsPageTest extends E2eTestBase {

    @Test
    void anEmptySplitInvitesToAddAParticipant() {
        String splitId = createSplit("Empty");

        page.navigate("/splits/" + splitId + "/participants");

        assertThat(text("Add your first participant to get started")).isVisible();
    }

    @Test
    void addsASingleParticipant() {
        String splitId = createSplit("Trip");
        page.navigate("/splits/" + splitId + "/participants");

        button("Add Single Participant").click();
        field("Name").fill("Alice");
        field("Nights").fill("3");
        button("Add").click();

        assertThat(toast("Alice successfully added")).isVisible();
        assertThat(button("Edit Alice")).isVisible();
        assertThat(text("Add your first participant to get started")).not().isVisible();
    }

    @Test
    void addsAFamilyWithItsMembers() {
        String splitId = createSplit("Trip");
        page.navigate("/splits/" + splitId + "/participants");

        button("Add Family Participant").click();
        field("Name").fill("Martins");
        field("Nights").fill("2");
        field("Members").fill("2.5");
        button("Add").click();

        assertThat(toast("Martins successfully added")).isVisible();
        assertThat(button("Edit Martins")).isVisible();
    }

    @Test
    void aDuplicateNameShowsAnInlineError() {
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Alice", 2);
        page.navigate("/splits/" + splitId + "/participants");

        button("Add Single Participant").click();
        field("Name").fill("Alice");

        assertThat(text("A participant with this name already exists")).isVisible();
    }

    @Test
    void anEmptyNameIsRejectedOnSubmit() {
        String splitId = createSplit("Trip");
        page.navigate("/splits/" + splitId + "/participants");

        button("Add Single Participant").click();
        button("Add").click();

        assertThat(text("Name is required")).isVisible();
    }

    @Test
    void cancellingTheFormBringsBackTheAddButtons() {
        String splitId = createSplit("Trip");
        page.navigate("/splits/" + splitId + "/participants");

        button("Add Single Participant").click();
        button("Cancel").click();

        assertThat(button("Add Single Participant")).isVisible();
    }

    @Test
    void editsAParticipant() {
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Alice", 2);
        page.navigate("/splits/" + splitId + "/participants");

        button("Edit Alice").click();
        var modal = dialog("Edit Participant");
        field(modal, "Name").fill("Alicia");
        field(modal, "Nights").fill("4");
        button(modal, "Save Changes").click();

        assertThat(modal).not().isVisible();
        assertThat(button("Edit Alicia")).isVisible();
        assertThat(button("Edit Alice")).not().isVisible();
    }

    @Test
    void removesAParticipantAfterConfirmation() {
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Alice", 2);
        addParticipant(splitId, "Bob", 2);
        page.navigate("/splits/" + splitId + "/participants");

        button("Delete Bob").click();
        var confirm = dialog("Remove Bob?");
        assertThat(confirm).isVisible();
        button(confirm, "Remove").click();

        assertThat(toast("Participant removed successfully")).isVisible();
        assertThat(button("Edit Bob")).not().isVisible();
        assertThat(button("Edit Alice")).isVisible();
    }

    @Test
    void aParticipantWithExpensesCannotBeRemoved() {
        String splitId = createSplit("Trip");
        String alice = addParticipant(splitId, "Alice", 2);
        addExpense(splitId, "Groceries", "40", alice);
        page.navigate("/splits/" + splitId + "/participants");

        button("Delete Alice").click();
        button(dialog("Remove Alice?"), "Remove").click();

        assertThat(toast("Cannot remove Alice")).isVisible();
        assertThat(button("Edit Alice")).isVisible();
    }

    @Test
    void addsAnExpenseForAParticipantFromTheirCard() {
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Alice", 2);
        String bob = addParticipant(splitId, "Bob", 2);
        page.navigate("/splits/" + splitId + "/participants");

        button("Add expense for Bob").click();
        var modal = dialog("Add Expense");
        assertThat(field(modal, "Payer")).hasValue(bob);
        field(modal, "Description").fill("Ferry");
        field(modal, "Amount (€)").fill("40");
        button(modal, "Add Expense").click();

        assertThat(toast("Expense added")).isVisible();
        assertThat(modal).not().isVisible();
        page.navigate("/splits/" + splitId + "/expenses");
        assertThat(text("Paid by Bob")).isVisible();
    }

    @Test
    void aFamilyNeedsAtLeastHalfAMember() {
        String splitId = createSplit("Trip");
        page.navigate("/splits/" + splitId + "/participants");

        button("Add Family Participant").click();
        field("Name").fill("Martins");
        field("Members").fill("0.2");

        assertThat(text("Must be at least 0.5")).isVisible();
        button("Add").click();
        assertThat(button("Edit Martins")).not().isVisible();
    }

    @Test
    void theNextParticipantGetsTheSameNights() {
        String splitId = createSplit("Trip");
        page.navigate("/splits/" + splitId + "/participants");
        button("Add Single Participant").click();
        field("Name").fill("Alice");
        field("Nights").fill("6");
        button("Add").click();
        assertThat(toast("Alice successfully added")).isVisible();

        button("Add Single Participant").click();

        assertThat(field("Name")).isFocused();
        assertThat(field("Nights")).hasValue("6");
    }

    @Test
    void theHelpAndFieldInfoWindowsExplain() {
        String splitId = createSplit("Trip");
        page.navigate("/splits/" + splitId + "/participants");

        button("Help").click();
        assertThat(dialog("How to add participants").getByText("Mixed scenario")).isVisible();
        button(dialog("How to add participants"), "Got it").click();
        assertThat(dialog("How to add participants")).not().isVisible();

        button("Add Family Participant").click();
        button("Info about Members").click();
        assertThat(dialog("Members").getByText("Adults count as 1 each.",
                new com.microsoft.playwright.Locator.GetByTextOptions())).isVisible();
        page.keyboard().press("Escape");
        assertThat(dialog("Members")).not().isVisible();
    }

    @Test
    void theCardShowsBadgesAndBalance() {
        String splitId = createSplit("Trip");
        String alice = addParticipant(splitId, "alice", 4);
        given().contentType(ContentType.JSON).body("{\"name\": \"Martins\", \"nights\": 4, \"share\": 3}")
                .post("/api/splits/" + splitId + "/participants").then().statusCode(201);
        addExpense(splitId, "Groceries", "80", alice);
        page.navigate("/splits/" + splitId + "/participants");

        var aliceCard = page.getByRole(AriaRole.LISTITEM)
                .filter(new com.microsoft.playwright.Locator.FilterOptions().setHas(button("Edit alice")));
        assertThat(aliceCard.getByText("Alice", new com.microsoft.playwright.Locator.GetByTextOptions().setExact(true)))
                .isVisible();
        assertThat(aliceCard.getByText("4 nights")).isVisible();
        assertThat(aliceCard.getByText("1 expense",
                new com.microsoft.playwright.Locator.GetByTextOptions().setExact(true))).isVisible();
        assertThat(aliceCard.getByTitle("Balance")).hasText("€60.00");

        var martinsCard = page.getByRole(AriaRole.LISTITEM)
                .filter(new com.microsoft.playwright.Locator.FilterOptions().setHas(button("Edit Martins")));
        assertThat(martinsCard.getByText("3 shares")).isVisible();
        assertThat(martinsCard.getByTitle("Balance")).hasText("€60.00");
        assertThat(martinsCard.getByTitle("Cost")).hasText("€60.00");
    }

    @Test
    void showsTheExpensesOfAParticipant() {
        String splitId = createSplit("Trip");
        String alice = addParticipant(splitId, "Alice", 2);
        String bob = addParticipant(splitId, "Bob", 2);
        addExpense(splitId, "Groceries", "60", alice);
        addExpense(splitId, "Fuel", "40", bob);
        page.navigate("/splits/" + splitId + "/participants");

        button("View expenses for Alice").click();
        var window = dialog("Alice's Expenses");
        assertThat(window.getByText("Groceries")).isVisible();
        assertThat(window.getByText("Fuel")).isVisible();
        button(window, "Close").last().click();
        assertThat(window).not().isVisible();
    }

    @Test
    void aParticipantWithoutExpensesSaysSo() {
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Carol", 2);
        page.navigate("/splits/" + splitId + "/participants");

        button("View expenses for Carol").click();

        assertThat(dialog("Carol's Expenses").getByText("No expenses found for Carol.")).isVisible();
    }

    @Test
    void theEditWindowAsksBeforeDiscardingChanges() {
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Alice", 2);
        page.navigate("/splits/" + splitId + "/participants");
        button("Edit Alice").click();
        var modal = dialog("Edit Participant");
        assertThat(field(modal, "Name")).isFocused();
        assertThat(button(modal, "Save Changes")).isDisabled();

        field(modal, "Nights").fill("5");
        button(modal, "Cancel edit").click();
        button(dialog("Discard changes?"), "Discard").click();

        assertThat(modal).not().isVisible();
        page.reload();
        button("Edit Alice").click();
        assertThat(field(dialog("Edit Participant"), "Nights")).hasValue("2");
    }

    @Test
    void emptyingTheNameSaysItIsRequired() {
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Alice", 2);
        page.navigate("/splits/" + splitId + "/participants");
        button("Edit Alice").click();
        var modal = dialog("Edit Participant");

        field(modal, "Name").fill("");
        field(modal, "Nights").focus();

        assertThat(modal.getByText("Name is required")).isVisible();
        assertThat(button(modal, "Save Changes")).isDisabled();
    }

    @Test
    void aParticipantWhoPaidCannotBeDeletedFromTheEditWindow() {
        String splitId = createSplit("Trip");
        String alice = addParticipant(splitId, "Alice", 2);
        addParticipant(splitId, "Bob", 2);
        addExpense(splitId, "Groceries", "60", alice);
        page.navigate("/splits/" + splitId + "/participants");

        button("Edit Alice").click();
        assertThat(button(dialog("Edit Participant"), "Delete Participant")).isDisabled();
        assertThat(button(dialog("Edit Participant"), "Delete Participant")).hasAttribute("title",
                "Remove expenses first");
        page.keyboard().press("Escape");

        button("Edit Bob").click();
        button(dialog("Edit Participant"), "Delete Participant").click();
        button(dialog("Delete Bob?"), "Remove").click();

        assertThat(toast("Participant removed")).isVisible();
        assertThat(button("Edit Bob")).not().isVisible();
    }

    @Test
    void theEditedParticipantIsHighlighted() {
        String splitId = createSplit("Trip");
        addParticipant(splitId, "Alice", 2);
        page.navigate("/splits/" + splitId + "/participants");
        button("Edit Alice").click();
        field(dialog("Edit Participant"), "Share").press("ArrowUp");
        assertThat(field(dialog("Edit Participant"), "Share")).hasValue("1.5");
        button(dialog("Edit Participant"), "Save Changes").click();

        assertThat(toast("Participant updated")).isVisible();
        var card = page.getByRole(AriaRole.LISTITEM)
                .filter(new com.microsoft.playwright.Locator.FilterOptions().setHas(button("Edit Alice")));
        assertThat(card).hasClass(java.util.regex.Pattern.compile("highlight"));
        assertThat(card.getByText("1.5 shares")).isVisible();
    }
}
