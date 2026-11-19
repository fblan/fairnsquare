package org.asymetrik.web.fairnsquare.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class SettlementPageTest extends E2eTestBase {

    /** Alice pays 60 for two people with the same nights: Bob owes her 30. */
    private String splitWhereBobOwesAlice() {
        String splitId = createSplit("Trip");
        String alice = addParticipant(splitId, "Alice", 2);
        addParticipant(splitId, "Bob", 2);
        addExpense(splitId, "Groceries", "60", alice);
        return splitId;
    }

    @Test
    void showsTheBalancesBeforeResolving() {
        String splitId = splitWhereBobOwesAlice();

        page.navigate("/splits/" + splitId + "/settlement");

        assertThat(text("Owed €30.00")).isVisible();
        assertThat(text("Owes €30.00")).isVisible();
        assertThat(button("Resolve")).isVisible();
    }

    @Test
    void resolvingShowsWhoPaysWhom() {
        String splitId = splitWhereBobOwesAlice();
        page.navigate("/splits/" + splitId + "/settlement");

        button("Resolve").click();

        assertThat(text("Pay €30.00 to Alice")).isVisible();
        assertThat(text("Receive €30.00 from Bob")).isVisible();
        assertThat(button("Unsettle")).isVisible();
    }

    @Test
    void aSettledSplitIsReadOnlyUntilUnsettled() {
        String splitId = splitWhereBobOwesAlice();
        page.navigate("/splits/" + splitId + "/settlement");
        button("Resolve").click();
        assertThat(button("Unsettle")).isVisible();

        page.navigate("/splits/" + splitId + "/participants");
        assertThat(text("This split is settled — participants are read-only.")).isVisible();
        assertThat(button("Add Single Participant")).not().isVisible();
        assertThat(button("Edit Alice")).not().isVisible();

        page.navigate("/splits/" + splitId + "/expenses");
        assertThat(text("This split is settled — expenses are read-only.")).isVisible();
        assertThat(button("Add Expense")).not().isVisible();

        button("View settlement").click();
        button("Unsettle").click();
        assertThat(button("Resolve")).isVisible();

        page.navigate("/splits/" + splitId + "/participants");
        assertThat(button("Edit Alice")).isVisible();
    }

    @Test
    void groupsTwoParticipantsIntoOneBalance() {
        String splitId = splitWhereBobOwesAlice();
        page.navigate("/splits/" + splitId + "/settlement");

        button("Group with...").first().click();
        var modal = dialog("Group with");
        field(modal, "Partner").selectOption(new com.microsoft.playwright.options.SelectOption().setLabel("Bob"));
        button(modal, "OK").click();

        assertThat(button("Ungroup")).isVisible();
    }
}
