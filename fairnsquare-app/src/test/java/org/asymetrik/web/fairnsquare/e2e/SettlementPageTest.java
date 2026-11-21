package org.asymetrik.web.fairnsquare.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

import org.asymetrik.web.fairnsquare.testing.FairnsquareTest;

import org.junit.jupiter.api.Test;

import com.microsoft.playwright.options.SelectOption;

import io.restassured.http.ContentType;

@FairnsquareTest
class SettlementPageTest extends E2eTestBase {

    private static final String SHARED_ACCOUNT = "sharedAccount12345678";

    /** Alice pays 60 for two people with the same nights: Bob owes her 30. */
    private String splitWhereBobOwesAlice() {
        String splitId = createSplit("Trip");
        String alice = addParticipant(splitId, "Alice", 2);
        addParticipant(splitId, "Bob", 2);
        addExpense(splitId, "Groceries", "60", alice);
        return splitId;
    }

    /** Puts a participant (1 share) in a shared account through the API. */
    private static void joinSharedAccount(String splitId, String participantId, String name, int nights) {
        given().contentType(ContentType.JSON).body("""
                {"name": "%s", "nights": %d, "share": 1, "sharedAccountId": "%s"}
                """.formatted(name, nights, SHARED_ACCOUNT))
                .put("/api/splits/" + splitId + "/participants/" + participantId).then().statusCode(200);
    }

    private static void resolve(String splitId) {
        given().contentType(ContentType.JSON).post("/api/splits/" + splitId + "/settlement").then().statusCode(200);
    }

    @Test
    void showsTheBalancesBeforeResolving() {
        String splitId = splitWhereBobOwesAlice();

        page.navigate("/splits/" + splitId + "/settlement");

        assertThat(text("Owed €30.00")).isVisible();
        assertThat(text("Owes €30.00")).isVisible();
        assertThat(button("Resolve")).isVisible();
        assertThat(button("Unsettle")).not().isVisible();
    }

    @Test
    void resolvingShowsWhoPaysWhom() {
        String splitId = splitWhereBobOwesAlice();
        page.navigate("/splits/" + splitId + "/settlement");

        button("Resolve").click();

        assertThat(text("Pay €30.00 to Alice")).isVisible();
        assertThat(text("Receive €30.00 from Bob")).isVisible();
        assertThat(button("Unsettle")).isVisible();
        assertThat(button("Group with...")).hasCount(0);
    }

    @Test
    void anAlreadyResolvedSplitShowsItsTransfers() {
        String splitId = splitWhereBobOwesAlice();
        resolve(splitId);

        page.navigate("/splits/" + splitId + "/settlement");

        assertThat(text("Pay €30.00 to Alice")).isVisible();
        assertThat(button("Export Settlement")).isVisible();
        assertThat(button("Resolve")).not().isVisible();
    }

    @Test
    void openingThePageDoesNotResolve() {
        String splitId = splitWhereBobOwesAlice();
        page.navigate("/splits/" + splitId + "/settlement");
        assertThat(button("Resolve")).isVisible();

        given().get("/api/splits/" + splitId + "/settlement").then().statusCode(404);
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
        assertThat(text("Pay €30.00 to Alice")).not().isVisible();

        page.navigate("/splits/" + splitId + "/participants");
        assertThat(button("Edit Alice")).isVisible();
    }

    @Test
    void exportCopiesASummary() {
        page.addInitScript("""
                Object.defineProperty(navigator, 'clipboard', {
                  value: { writeText: async (text) => { window.copiedText = text; } },
                });""");
        String splitId = splitWhereBobOwesAlice();
        resolve(splitId);
        page.navigate("/splits/" + splitId + "/settlement");

        button("Export Settlement").click();

        assertThat(toast("Settlement copied to clipboard!")).isVisible();
        String copied = (String) page.evaluate("window.copiedText");
        assertThat(copied).startsWith("=== Trip ===\n1 expense — Total: €60.00\n2 participants\n")
                .contains("Bob → Alice: €30.00", "Alice receives €30.00")
                .endsWith("/splits/" + splitId + "/settlement");
    }

    @Test
    void exportWithoutClipboardShowsTheSummary() {
        page.addInitScript("Object.defineProperty(navigator, 'clipboard', { value: undefined });");
        String splitId = splitWhereBobOwesAlice();
        resolve(splitId);
        page.navigate("/splits/" + splitId + "/settlement");

        button("Export Settlement").click();

        assertThat(toast("Bob → Alice: €30.00")).isVisible();
    }

    @Test
    void groupsTwoParticipantsIntoOneBalance() {
        String splitId = splitWhereBobOwesAlice();
        page.navigate("/splits/" + splitId + "/settlement");

        button("Group with...").first().click();
        var modal = dialog("Group Alice with");
        assertThat(button(modal, "OK")).isDisabled();
        field(modal, "Partner").selectOption(new SelectOption().setLabel("Bob"));
        button(modal, "OK").click();

        assertThat(text("Alice & Bob")).isVisible();
        assertThat(text("Settled")).isVisible();
        assertThat(button("Ungroup")).isVisible();
    }

    @Test
    void joinsAnExistingGroup() {
        String splitId = createSplit("Trip");
        String alice = addParticipant(splitId, "Alice", 2);
        String bob = addParticipant(splitId, "Bob", 2);
        addParticipant(splitId, "Carol", 2);
        joinSharedAccount(splitId, alice, "Alice", 2);
        joinSharedAccount(splitId, bob, "Bob", 2);
        page.navigate("/splits/" + splitId + "/settlement");
        assertThat(text("Alice & Bob")).isVisible();

        button("Group with...").click();
        var modal = dialog("Group Carol with");
        field(modal, "Partner").selectOption(new SelectOption().setLabel("Alice & Bob"));
        button(modal, "OK").click();

        assertThat(text("Alice & Bob & Carol")).isVisible();
        assertThat(button("Group with...")).hasCount(0);
    }

    @Test
    void ungroupShowsEachBalanceAgain() {
        String splitId = splitWhereBobOwesAlice();
        var participants = given().get("/api/splits/" + splitId).then().extract().jsonPath();
        joinSharedAccount(splitId, participants.getString("participants[0].id"), "Alice", 2);
        joinSharedAccount(splitId, participants.getString("participants[1].id"), "Bob", 2);
        page.navigate("/splits/" + splitId + "/settlement");
        assertThat(text("Alice & Bob")).isVisible();

        button("Ungroup").click();

        assertThat(text("Owed €30.00")).isVisible();
        assertThat(text("Owes €30.00")).isVisible();
        assertThat(button("Group with...")).hasCount(2);
    }

    @Test
    void anUnknownSplitGoesBackHome() {
        page.navigate("/splits/unknownSplit123456789/settlement");

        assertThat(toast("Split not found — create a new one.")).isVisible();
        assertThat(page).hasURL(Pattern.compile("/$"));
    }
}
