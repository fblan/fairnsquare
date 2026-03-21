package org.asymetrik.web.fairnsquare.split.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseEqual;
import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.junit.jupiter.api.Test;

/**
 * Tests that participant balances (totalPaid, totalCost, balance) are correctly recalculated by the Split aggregate
 * after every mutation to participants or expenses.
 */
class SplitParticipantBalanceTest {

    @Test
    void newParticipant_hasZeroBalances() {
        Split split = Split.create("Test");
        split.addParticipant(Participant.create("Alice", 3));

        Participant alice = split.getParticipants().get(0);
        assertThat(alice.totalPaid()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(alice.totalCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(alice.balance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void addExpense_updatesPayerTotalPaidAndAllParticipantsCost() {
        Split split = Split.create("Test");
        Participant alice = Participant.create("Alice", 3);
        Participant bob = Participant.create("Bob", 2);
        split.addParticipant(alice);
        split.addParticipant(bob);

        // Alice pays 100, split equally → each owes 50
        split.addExpense(ExpenseEqual.create(new BigDecimal("100.00"), "Dinner", alice.id()));

        Participant updatedAlice = split.getParticipant(alice.id());
        Participant updatedBob = split.getParticipant(bob.id());

        assertThat(updatedAlice.totalPaid()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(updatedAlice.totalCost()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(updatedAlice.balance()).isEqualByComparingTo(new BigDecimal("50.00"));

        assertThat(updatedBob.totalPaid()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(updatedBob.totalCost()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(updatedBob.balance()).isEqualByComparingTo(new BigDecimal("-50.00"));
    }

    @Test
    void removeExpense_recalculatesBalances() {
        Split split = Split.create("Test");
        Participant alice = Participant.create("Alice", 3);
        Participant bob = Participant.create("Bob", 2);
        split.addParticipant(alice);
        split.addParticipant(bob);
        split.addExpense(ExpenseEqual.create(new BigDecimal("100.00"), "Dinner", alice.id()));

        split.removeExpense(split.getExpenses().get(0).getId());

        Participant updatedAlice = split.getParticipant(alice.id());
        assertThat(updatedAlice.totalPaid()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(updatedAlice.totalCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(updatedAlice.balance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void updateExpense_recalculatesBalances() {
        Split split = Split.create("Test");
        Participant alice = Participant.create("Alice", 3);
        Participant bob = Participant.create("Bob", 2);
        split.addParticipant(alice);
        split.addParticipant(bob);
        split.addExpense(ExpenseEqual.create(new BigDecimal("100.00"), "Dinner", alice.id()));

        // Update to 200 — Alice still payer
        split.updateExpense(split.getExpenses().get(0).getId(), new BigDecimal("200.00"), "Dinner", alice.id(),
                org.asymetrik.web.fairnsquare.split.domain.expenses.SplitMode.EQUAL);

        Participant updatedAlice = split.getParticipant(alice.id());
        assertThat(updatedAlice.totalPaid()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(updatedAlice.totalCost()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(updatedAlice.balance()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void multipleExpenses_accumulateBalancesCorrectly() {
        Split split = Split.create("Test");
        Participant alice = Participant.create("Alice", 3);
        Participant bob = Participant.create("Bob", 2);
        split.addParticipant(alice);
        split.addParticipant(bob);

        // Alice pays 100, Bob pays 60 — both split equally
        split.addExpense(ExpenseEqual.create(new BigDecimal("100.00"), "Hotel", alice.id()));
        split.addExpense(ExpenseEqual.create(new BigDecimal("60.00"), "Food", bob.id()));

        Participant updatedAlice = split.getParticipant(alice.id());
        Participant updatedBob = split.getParticipant(bob.id());

        // Total: 160, each owes 80
        assertThat(updatedAlice.totalPaid()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(updatedAlice.totalCost()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(updatedAlice.balance()).isEqualByComparingTo(new BigDecimal("20.00"));

        assertThat(updatedBob.totalPaid()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(updatedBob.totalCost()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(updatedBob.balance()).isEqualByComparingTo(new BigDecimal("-20.00"));
    }

    @Test
    void removeParticipant_recalculatesRemainingParticipantBalances() {
        Split split = Split.create("Test");
        Participant alice = Participant.create("Alice", 3);
        Participant bob = Participant.create("Bob", 2);
        Participant charlie = Participant.create("Charlie", 1);
        split.addParticipant(alice);
        split.addParticipant(bob);
        split.addParticipant(charlie);
        // Alice pays 90 split equally among 3 → each owes 30
        split.addExpense(ExpenseEqual.create(new BigDecimal("90.00"), "Hotel", alice.id()));

        // Remove Charlie (not a payer, can be removed)
        split.removeParticipant(charlie.id());

        // Now 2 participants, Alice pays 90 → each owes 45
        Participant updatedAlice = split.getParticipant(alice.id());
        assertThat(updatedAlice.totalPaid()).isEqualByComparingTo(new BigDecimal("90.00"));
        assertThat(updatedAlice.totalCost()).isEqualByComparingTo(new BigDecimal("45.00"));
        assertThat(updatedAlice.balance()).isEqualByComparingTo(new BigDecimal("45.00"));
    }
}
