package org.asymetrik.web.fairnsquare.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.restassured.RestAssured.given;

import java.util.concurrent.atomic.AtomicBoolean;

import org.asymetrik.web.fairnsquare.testing.FairnsquareTest;

import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Route;

import io.restassured.http.ContentType;

/** The split dashboard, /splits/<id>. */
@FairnsquareTest
class SplitPageTest extends E2eTestBase {

    @Test
    void showsTheExpensesTotalAndTheParticipants() {
        String splitId = createSplit("Ski week");
        String alice = addParticipant(splitId, "alice", 4);
        addParticipant(splitId, "Bob", 3);
        addExpense(splitId, "Groceries", "84.50", alice);
        addExpense(splitId, "Fuel", "15.50", alice);

        page.navigate("/splits/" + splitId);

        assertThat(text("2 expenses")).isVisible();
        assertThat(text("€100.00")).isVisible();
        assertThat(text("2 participants")).isVisible();
        assertThat(text("Alice (4n), Bob (3n)")).isVisible();
        assertThat(text("Solve")).isVisible();
    }

    @Test
    void theSettlementCardWaitsForExpenses() {
        String splitId = createSplit("Ski week");
        addParticipant(splitId, "Alice", 4);

        page.navigate("/splits/" + splitId);

        assertThat(text("0 expenses")).isVisible();
        assertThat(text("1 participant")).isVisible();
        assertThat(button("View settlement")).not().isVisible();
    }

    @Test
    void aSettledSplitSaysSo() {
        String splitId = createSplit("Ski week");
        String alice = addParticipant(splitId, "Alice", 4);
        addParticipant(splitId, "Bob", 4);
        addExpense(splitId, "Groceries", "80", alice);
        given().contentType(ContentType.JSON).post("/api/splits/" + splitId + "/settlement").then().statusCode(200);

        page.navigate("/splits/" + splitId);

        assertThat(text("Settled")).isVisible();
    }

    @Test
    void aLoadingErrorCanBeRetried() {
        String splitId = createSplit("Ski week");
        AtomicBoolean failed = new AtomicBoolean();
        page.route("**/api/splits/" + splitId, route -> {
            if (failed.compareAndSet(false, true)) {
                route.fulfill(new Route.FulfillOptions().setStatus(500).setContentType("application/json")
                        .setBody("{\"status\": 500, \"detail\": \"Disk on fire\"}"));
            } else {
                route.resume();
            }
        });
        page.navigate("/splits/" + splitId);
        assertThat(text("Error loading split")).isVisible();
        assertThat(text("Disk on fire")).isVisible();

        button("Retry").click();

        assertThat(text("0 expenses")).isVisible();
        assertThat(text("Error loading split")).not().isVisible();
    }

    @Test
    void anOpenedSplitIsOfferedOnTheHomePage() {
        String splitId = createSplit("Shared with me");

        page.navigate("/splits/" + splitId);
        assertThat(text("0 expenses")).isVisible();
        page.navigate("/");

        assertThat(text("Shared with me")).isVisible();
        assertThat(button("Resume")).isVisible();
    }
}
