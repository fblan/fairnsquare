package org.asymetrik.web.fairnsquare.split.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.asymetrik.web.fairnsquare.split.domain.expenses.Expense;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseByNight;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseFree;
import org.asymetrik.web.fairnsquare.split.domain.expenses.SplitMode;
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
        split.addExpense(ExpenseFree.create(new BigDecimal("100.00"), "Dinner", alice.id(),
                List.of(Expense.Share.withParts(alice.id(), BigDecimal.ONE),
                        Expense.Share.withParts(bob.id(), BigDecimal.ONE))));

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
        split.addExpense(ExpenseFree.create(new BigDecimal("100.00"), "Dinner", alice.id(),
                List.of(Expense.Share.withParts(alice.id(), BigDecimal.ONE),
                        Expense.Share.withParts(bob.id(), BigDecimal.ONE))));

        split.removeExpense(split.getExpenses().get(0).getId());

        Participant updatedAlice = split.getParticipant(alice.id());
        assertThat(updatedAlice.totalPaid()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(updatedAlice.totalCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(updatedAlice.balance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void updateExpense_recalculatesBalances() {
        Split split = Split.create("Test");
        // Use equal nights so BY_NIGHT splits evenly
        Participant alice = Participant.create("Alice", 1);
        Participant bob = Participant.create("Bob", 1);
        split.addParticipant(alice);
        split.addParticipant(bob);
        split.addExpense(ExpenseByNight.create(new BigDecimal("100.00"), "Dinner", alice.id()));

        // Update to 200 — Alice still payer, BY_NIGHT with equal nights → each owes 100
        split.updateExpense(split.getExpenses().get(0).getId(), new BigDecimal("200.00"), "Dinner", alice.id(),
                SplitMode.BY_NIGHT);

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
        split.addExpense(ExpenseFree.create(new BigDecimal("100.00"), "Hotel", alice.id(),
                List.of(Expense.Share.withParts(alice.id(), BigDecimal.ONE),
                        Expense.Share.withParts(bob.id(), BigDecimal.ONE))));
        split.addExpense(ExpenseFree.create(new BigDecimal("60.00"), "Food", bob.id(),
                List.of(Expense.Share.withParts(alice.id(), BigDecimal.ONE),
                        Expense.Share.withParts(bob.id(), BigDecimal.ONE))));

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
        // Use equal nights so removing a participant recalculates shares evenly
        Participant alice = Participant.create("Alice", 1);
        Participant bob = Participant.create("Bob", 1);
        Participant charlie = Participant.create("Charlie", 1);
        split.addParticipant(alice);
        split.addParticipant(bob);
        split.addParticipant(charlie);
        // Alice pays 90 split BY_NIGHT among 3 equal nights → each owes 30
        split.addExpense(ExpenseByNight.create(new BigDecimal("90.00"), "Hotel", alice.id()));

        // Remove Charlie (not a payer, can be removed)
        split.removeParticipant(charlie.id());

        // Now 2 participants with equal nights, Alice pays 90 → each owes 45
        Participant updatedAlice = split.getParticipant(alice.id());
        assertThat(updatedAlice.totalPaid()).isEqualByComparingTo(new BigDecimal("90.00"));
        assertThat(updatedAlice.totalCost()).isEqualByComparingTo(new BigDecimal("45.00"));
        assertThat(updatedAlice.balance()).isEqualByComparingTo(new BigDecimal("45.00"));
    }
}
