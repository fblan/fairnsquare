package org.asymetrik.web.fairnsquare.split.domain;

import java.math.BigDecimal;
import java.util.List;

import org.asymetrik.web.fairnsquare.split.domain.expenses.Expense;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseEqual;
import org.asymetrik.web.fairnsquare.split.domain.expenses.SplitMode;
import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ExpenseEqual share calculation logic.
 */
class ExpenseEqualTest {

    @Test
    void calculateShares_withThreeParticipants_equallySplit() {
        // Given: 3 participants, amount 90.00
        Participant alice = createParticipant("alice", 4);
        Participant bob = createParticipant("bob", 2);
        Participant charlie = createParticipant("charlie", 3);
        List<Participant> participants = List.of(alice, bob, charlie);

        ExpenseEqual expense = ExpenseEqual.create(new BigDecimal("90.00"), "Dinner", alice.id());

        // When
        List<Expense.Share> shares = expense.calculateShares(participants);

        // Then: each gets 30.00 (90 / 3)
        assertThat(shares).hasSize(3);
        assertThat(shares.get(0).amount()).isEqualByComparingTo("30.00");
        assertThat(shares.get(1).amount()).isEqualByComparingTo("30.00");
        assertThat(shares.get(2).amount()).isEqualByComparingTo("30.00");
    }

    @Test
    void calculateShares_withSingleParticipant_getsFullAmount() {
        Participant alice = createParticipant("alice", 5);
        List<Participant> participants = List.of(alice);

        ExpenseEqual expense = ExpenseEqual.create(new BigDecimal("100.00"), "Hotel", alice.id());

        List<Expense.Share> shares = expense.calculateShares(participants);

        assertThat(shares).hasSize(1);
        assertThat(shares.get(0).amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void calculateShares_withEmptyParticipants_returnsEmptyList() {
        ExpenseEqual expense = ExpenseEqual.create(new BigDecimal("100.00"), "Test", Participant.Id.generate());

        List<Expense.Share> shares = expense.calculateShares(List.of());

        assertThat(shares).isEmpty();
    }

    @Test
    void calculateShares_withNullParticipants_returnsEmptyList() {
        ExpenseEqual expense = ExpenseEqual.create(new BigDecimal("100.00"), "Test", Participant.Id.generate());

        List<Expense.Share> shares = expense.calculateShares(null);

        assertThat(shares).isEmpty();
    }

    @Test
    void calculateShares_withIndivisibleAmount_lastParticipantGetsRemainder() {
        // Given: 100.00 / 3 = 33.33... each, rounding leaves remainder for last
        Participant alice = createParticipant("alice", 1);
        Participant bob = createParticipant("bob", 1);
        Participant charlie = createParticipant("charlie", 1);
        List<Participant> participants = List.of(alice, bob, charlie);

        ExpenseEqual expense = ExpenseEqual.create(new BigDecimal("100.00"), "Dinner", alice.id());

        List<Expense.Share> shares = expense.calculateShares(participants);

        // Then: sum of shares equals original amount
        BigDecimal total = shares.stream().map(Expense.Share::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("100.00");

        // First two get 33.33, last gets 33.34
        assertThat(shares.get(0).amount()).isEqualByComparingTo("33.33");
        assertThat(shares.get(1).amount()).isEqualByComparingTo("33.33");
        assertThat(shares.get(2).amount()).isEqualByComparingTo("33.34");
    }

    @Test
    void calculateShares_ignoresNightsForEqualSplit() {
        // Given: participants have different nights but should still get equal shares
        Participant alice = createParticipant("alice", 10);
        Participant bob = createParticipant("bob", 1);
        List<Participant> participants = List.of(alice, bob);

        ExpenseEqual expense = ExpenseEqual.create(new BigDecimal("100.00"), "Shared expense", alice.id());

        List<Expense.Share> shares = expense.calculateShares(participants);

        // Then: each gets 50.00 regardless of nights
        assertThat(shares.get(0).amount()).isEqualByComparingTo("50.00");
        assertThat(shares.get(1).amount()).isEqualByComparingTo("50.00");
    }

    @Test
    void calculateShares_ac2_90WithThreeParticipants_returnsThirtyEach() {
        // AC 2: €90 with 3 participants → €30 each
        Participant alice = createParticipant("Alice", 1);
        Participant bob = createParticipant("Bob", 1);
        Participant charlie = createParticipant("Charlie", 1);
        List<Participant> participants = List.of(alice, bob, charlie);

        ExpenseEqual expense = ExpenseEqual.create(new BigDecimal("90.00"), "Dinner", alice.id());

        List<Expense.Share> shares = expense.calculateShares(participants);

        assertThat(shares).hasSize(3);
        assertThat(shares.get(0).amount()).isEqualByComparingTo("30.00");
        assertThat(shares.get(1).amount()).isEqualByComparingTo("30.00");
        assertThat(shares.get(2).amount()).isEqualByComparingTo("30.00");
    }

    @Test
    void calculateShares_ac3_100WithThreeParticipants_returnsTwoThirtyThreeAndOneThirtyFour() {
        // AC 3: €100 with 3 participants → (€33.33, €33.33, €33.34)
        Participant alice = createParticipant("Alice", 1);
        Participant bob = createParticipant("Bob", 1);
        Participant charlie = createParticipant("Charlie", 1);
        List<Participant> participants = List.of(alice, bob, charlie);

        ExpenseEqual expense = ExpenseEqual.create(new BigDecimal("100.00"), "Supplies", alice.id());

        List<Expense.Share> shares = expense.calculateShares(participants);

        assertThat(shares).hasSize(3);
        assertThat(shares.get(0).amount()).isEqualByComparingTo("33.33");
        assertThat(shares.get(1).amount()).isEqualByComparingTo("33.33");
        assertThat(shares.get(2).amount()).isEqualByComparingTo("33.34");

        // Verify sum equals expense amount exactly
        BigDecimal total = shares.stream().map(Expense.Share::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("100.00");
    }

    @Test
    void getSplitMode_returnsEqual() {
        ExpenseEqual expense = ExpenseEqual.create(new BigDecimal("100.00"), "Test", Participant.Id.generate());

        assertThat(expense.getSplitMode()).isEqualTo(SplitMode.EQUAL);
    }

    @Test
    void fromJson_createsValidExpense() {
        Expense.Id id = Expense.Id.generate();
        Participant.Id payerId = Participant.Id.generate();

        ExpenseEqual expense = ExpenseEqual.fromJson(id, new BigDecimal("150.00"), "Dinner", payerId,
                java.time.Instant.now());

        assertThat(expense.getId()).isEqualTo(id);
        assertThat(expense.getAmount()).isEqualByComparingTo("150.00");
        assertThat(expense.getDescription()).isEqualTo("Dinner");
        assertThat(expense.getPayerId()).isEqualTo(payerId);
    }

    private Participant createParticipant(String name, int nights) {
        return Participant.create(name, nights);
    }
}
