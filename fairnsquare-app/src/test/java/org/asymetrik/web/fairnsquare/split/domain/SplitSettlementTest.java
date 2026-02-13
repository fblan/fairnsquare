package org.asymetrik.web.fairnsquare.split.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseByNight;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseEqual;
import org.asymetrik.web.fairnsquare.split.domain.expenses.SplitMode;
import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.asymetrik.web.fairnsquare.split.domain.settlement.ParticipantBalance;
import org.asymetrik.web.fairnsquare.split.domain.settlement.Reimbursement;
import org.asymetrik.web.fairnsquare.split.domain.settlement.Settlement;
import org.asymetrik.web.fairnsquare.split.domain.settlement.SettlementCalculator;
import org.junit.jupiter.api.Test;

/**
 * Tests for Settlement persistence in Split aggregate: settle, getSettlement, and clearSettlement on mutations.
 */
class SplitSettlementTest {

    @Test
    void settle_storesSettlement() {
        Split split = createSplitWithParticipantsAndExpense();
        Settlement settlement = SettlementCalculator.calculate(split);

        split.settle(settlement);

        assertThat(split.getSettlement()).isNotNull();
        assertThat(split.getSettlement()).isSameAs(settlement);
    }

    @Test
    void getSettlement_returnsNullByDefault() {
        Split split = Split.create("Test");

        assertThat(split.getSettlement()).isNull();
    }

    @Test
    void clearSettlement_removesSettlement() {
        Split split = createSplitWithParticipantsAndExpense();
        split.settle(SettlementCalculator.calculate(split));
        assertThat(split.getSettlement()).isNotNull();

        split.clearSettlement();

        assertThat(split.getSettlement()).isNull();
    }

    // --- clearSettlement on mutations ---

    @Test
    void addParticipant_clearsSettlement() {
        Split split = createSplitWithParticipantsAndExpense();
        split.settle(SettlementCalculator.calculate(split));
        assertThat(split.getSettlement()).isNotNull();

        split.addParticipant(Participant.create("Charlie", 2));

        assertThat(split.getSettlement()).isNull();
    }

    @Test
    void updateParticipant_clearsSettlement() {
        Split split = createSplitWithParticipantsAndExpense();
        Participant alice = split.getParticipants().get(0);
        split.settle(SettlementCalculator.calculate(split));
        assertThat(split.getSettlement()).isNotNull();

        split.updateParticipant(alice.id(), "Alice Updated", 5);

        assertThat(split.getSettlement()).isNull();
    }

    @Test
    void removeParticipant_clearsSettlement() {
        Split split = Split.create("Test");
        Participant alice = Participant.create("Alice", 3);
        Participant bob = Participant.create("Bob", 2);
        split.addParticipant(alice);
        split.addParticipant(bob);
        // Add expense paid by Alice only so Bob can be removed
        split.addExpense(ExpenseEqual.create(new BigDecimal("100.00"), "Dinner", alice.id()));
        split.settle(SettlementCalculator.calculate(split));
        assertThat(split.getSettlement()).isNotNull();

        // Bob has no expenses as payer, so can be removed
        split.removeParticipant(bob.id());

        assertThat(split.getSettlement()).isNull();
    }

    @Test
    void addExpense_clearsSettlement() {
        Split split = createSplitWithParticipantsAndExpense();
        split.settle(SettlementCalculator.calculate(split));
        assertThat(split.getSettlement()).isNotNull();

        Participant alice = split.getParticipants().get(0);
        split.addExpense(ExpenseEqual.create(new BigDecimal("50.00"), "Lunch", alice.id()));

        assertThat(split.getSettlement()).isNull();
    }

    @Test
    void updateExpense_clearsSettlement() {
        Split split = createSplitWithParticipantsAndExpense();
        split.settle(SettlementCalculator.calculate(split));
        assertThat(split.getSettlement()).isNotNull();

        split.updateExpense(split.getExpenses().get(0).getId(), new BigDecimal("200.00"), "Updated Hotel",
                split.getParticipants().get(0).id(), SplitMode.EQUAL);

        assertThat(split.getSettlement()).isNull();
    }

    @Test
    void removeExpense_clearsSettlement() {
        Split split = createSplitWithParticipantsAndExpense();
        split.settle(SettlementCalculator.calculate(split));
        assertThat(split.getSettlement()).isNotNull();

        split.removeExpense(split.getExpenses().get(0).getId());

        assertThat(split.getSettlement()).isNull();
    }

    private Split createSplitWithParticipantsAndExpense() {
        Split split = Split.create("Test Split");
        Participant alice = Participant.create("Alice", 3);
        Participant bob = Participant.create("Bob", 2);
        split.addParticipant(alice);
        split.addParticipant(bob);
        split.addExpense(ExpenseEqual.create(new BigDecimal("100.00"), "Hotel", alice.id()));
        return split;
    }
}
