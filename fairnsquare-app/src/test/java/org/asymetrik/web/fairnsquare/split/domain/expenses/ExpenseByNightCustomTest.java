package org.asymetrik.web.fairnsquare.split.domain.expenses;

import java.math.BigDecimal;
import java.util.List;

import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ExpenseByNightCustom share calculation logic.
 */
class ExpenseByNightCustomTest {

    @Test
    void calculateShares_withSubsetOfParticipants_onlySelectedParticipantsHaveShares() {
        // Given: 3 participants but only 2 are selected for this expense
        Participant alice = Participant.create("Alice", 4);
        Participant bob = Participant.create("Bob", 2);
        Participant charlie = Participant.create("Charlie", 3);
        List<Participant> selected = List.of(alice, bob);

        ExpenseByNightCustom expense = ExpenseByNightCustom.create(new BigDecimal("180.00"), "Groceries", alice.id(),
                List.of(alice.id(), bob.id()));

        // When
        List<Expense.Share> shares = expense.calculateShares(selected);

        // Then: only Alice and Bob have shares, proportional to their nights (4:2 = 2:1)
        assertThat(shares).hasSize(2);
        // Alice: 4/(4+2) * 180 = 120
        assertThat(shares.get(0).amount()).isEqualByComparingTo("120.00");
        // Bob: 2/(4+2) * 180 = 60
        assertThat(shares.get(1).amount()).isEqualByComparingTo("60.00");
    }

    @Test
    void calculateShares_withSingleParticipant_getsFullAmount() {
        Participant alice = Participant.create("Alice", 5);

        ExpenseByNightCustom expense = ExpenseByNightCustom.create(new BigDecimal("100.00"), "Hotel", alice.id(),
                List.of(alice.id()));

        List<Expense.Share> shares = expense.calculateShares(List.of(alice));

        assertThat(shares).hasSize(1);
        assertThat(shares.get(0).amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void calculateShares_withEmptyParticipants_returnsEmptyList() {
        Participant alice = Participant.create("Alice", 5);

        ExpenseByNightCustom expense = ExpenseByNightCustom.create(new BigDecimal("100.00"), "Hotel", alice.id(),
                List.of(alice.id()));

        List<Expense.Share> shares = expense.calculateShares(List.of());

        assertThat(shares).isEmpty();
    }

    @Test
    void calculateShares_sumEqualsExpenseAmount() {
        // Verify no rounding errors: sum of shares must equal total amount
        Participant alice = Participant.create("Alice", 3);
        Participant bob = Participant.create("Bob", 3);
        Participant charlie = Participant.create("Charlie", 3);
        List<Participant> selected = List.of(alice, bob, charlie);

        ExpenseByNightCustom expense = ExpenseByNightCustom.create(new BigDecimal("100.00"), "Dinner", alice.id(),
                List.of(alice.id(), bob.id(), charlie.id()));

        List<Expense.Share> shares = expense.calculateShares(selected);

        BigDecimal total = shares.stream().map(Expense.Share::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("100.00");
    }

    @Test
    void calculateShares_withDifferentNights_proportionalToNightsWeightedByShare() {
        // Alice: 4 nights, 2 persons → weight 8
        // Bob: 4 nights, 1 person → weight 4
        // Total weight: 12
        // Expense: €120
        Participant alice = Participant.create("Alice", 4, 2.0);
        Participant bob = Participant.create("Bob", 4, 1.0);
        List<Participant> selected = List.of(alice, bob);

        ExpenseByNightCustom expense = ExpenseByNightCustom.create(new BigDecimal("120.00"), "Hotel", alice.id(),
                List.of(alice.id(), bob.id()));

        List<Expense.Share> shares = expense.calculateShares(selected);

        assertThat(shares).hasSize(2);
        assertThat(shares.get(0).amount()).isEqualByComparingTo("80.00"); // Alice: 8/12 * 120 = 80
        assertThat(shares.get(1).amount()).isEqualByComparingTo("40.00"); // Bob: 4/12 * 120 = 40
    }

    @Test
    void getSplitMode_returnsByNightCustom() {
        Participant alice = Participant.create("Alice", 3);

        ExpenseByNightCustom expense = ExpenseByNightCustom.create(new BigDecimal("100.00"), "Test", alice.id(),
                List.of(alice.id()));

        assertThat(expense.getSplitMode()).isEqualTo(SplitMode.BY_NIGHT_CUSTOM);
    }

    @Test
    void getParticipantIds_returnsStoredIds() {
        Participant alice = Participant.create("Alice", 3);
        Participant bob = Participant.create("Bob", 2);

        ExpenseByNightCustom expense = ExpenseByNightCustom.create(new BigDecimal("100.00"), "Test", alice.id(),
                List.of(alice.id(), bob.id()));

        assertThat(expense.getParticipantIds()).containsExactly(alice.id(), bob.id());
    }

    @Test
    void fromJson_createsValidExpense() {
        Expense.Id id = Expense.Id.generate();
        Participant.Id payerId = Participant.Id.generate();
        Participant.Id participantId1 = Participant.Id.generate();
        Participant.Id participantId2 = Participant.Id.generate();

        ExpenseByNightCustom expense = ExpenseByNightCustom.fromJson(id, new BigDecimal("150.00"), "Dinner", payerId,
                List.of(participantId1, participantId2), java.time.Instant.now());

        assertThat(expense.getId()).isEqualTo(id);
        assertThat(expense.getAmount()).isEqualByComparingTo("150.00");
        assertThat(expense.getDescription()).isEqualTo("Dinner");
        assertThat(expense.getPayerId()).isEqualTo(payerId);
        assertThat(expense.getParticipantIds()).containsExactly(participantId1, participantId2);
    }

    @Test
    void create_withNullParticipantIds_throwsIllegalArgumentException() {
        Participant.Id payerId = Participant.Id.generate();

        assertThatThrownBy(() -> ExpenseByNightCustom.create(new BigDecimal("100.00"), "Test", payerId,
                (java.util.List<Participant.Id>) null)).isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Participant IDs cannot be null");
    }

    @Test
    void create_withEmptyParticipantIds_throwsIllegalArgumentException() {
        Participant.Id payerId = Participant.Id.generate();

        assertThatThrownBy(() -> ExpenseByNightCustom.create(new BigDecimal("100.00"), "Test", payerId, List.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at least one participant");
    }
}
