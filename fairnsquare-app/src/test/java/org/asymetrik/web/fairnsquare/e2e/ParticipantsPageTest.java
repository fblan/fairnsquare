package org.asymetrik.web.fairnsquare.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
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
}
