package org.asymetrik.web.fairnsquare.split.domain.expenses;

import java.math.BigDecimal;
import java.util.List;

import org.asymetrik.web.fairnsquare.split.domain.Split;
import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ExpenseByNightCustom share calculation logic.
 */
class ExpenseByNightCustomTest {

    @Test
    void getSplitMode_returnsByNightCustom() {
        Participant alice = Participant.create("Alice", 3);
        ExpenseByNightCustom expense = ExpenseByNightCustom.create(new BigDecimal("100.00"), "Test", alice.id(),
                List.of(alice.id()));

        assertThat(expense.getSplitMode()).isEqualTo(SplitMode.BY_NIGHT_CUSTOM);
    }

    @Test
    void getShares_onlyIncludesSelectedParticipants() {
        // Given: 3 participants in the split, only 2 selected for this expense
        Participant alice = Participant.create("Alice", 4);
        Participant bob = Participant.create("Bob", 2);
        Participant vanGuest = Participant.create("VanGuest", 3);

        Split split = Split.create("Test Split");
        split.addParticipant(alice);
        split.addParticipant(bob);
        split.addParticipant(vanGuest);

        // Only alice and bob participate in this expense (vanGuest excluded)
        ExpenseByNightCustom expense = ExpenseByNightCustom.create(new BigDecimal("180.00"), "House Rent", alice.id(),
                List.of(alice.id(), bob.id()));
        split.addExpense(expense);

        List<Expense.Share> shares = expense.getShares(split);

        // Only 2 shares, vanGuest is excluded
        assertThat(shares).hasSize(2);
        assertThat(shares).extracting(s -> s.participantId().value()).containsExactlyInAnyOrder(alice.id().value(),
                bob.id().value());
    }

    @Test
    void getShares_calculatesProportionallyByNights() {
        // Alice: 4 nights, Bob: 2 nights. Total 6 nights.
        // 180 * 4/6 = 120, 180 * 2/6 = 60
        Participant alice = Participant.create("Alice", 4);
        Participant bob = Participant.create("Bob", 2);
        Participant vanGuest = Participant.create("VanGuest", 3);

        Split split = Split.create("Test Split");
        split.addParticipant(alice);
        split.addParticipant(bob);
        split.addParticipant(vanGuest);

        ExpenseByNightCustom expense = ExpenseByNightCustom.create(new BigDecimal("180.00"), "Groceries", alice.id(),
                List.of(alice.id(), bob.id()));
        split.addExpense(expense);

        List<Expense.Share> shares = expense.getShares(split);

        assertThat(shares).hasSize(2);
        BigDecimal aliceShare = shares.stream().filter(s -> s.participantId().equals(alice.id())).findFirst()
                .orElseThrow().amount();
        BigDecimal bobShare = shares.stream().filter(s -> s.participantId().equals(bob.id())).findFirst().orElseThrow()
                .amount();

        assertThat(aliceShare).isEqualByComparingTo("120.00");
        assertThat(bobShare).isEqualByComparingTo("60.00");

        // Total equals expense amount
        BigDecimal total = aliceShare.add(bobShare);
        assertThat(total).isEqualByComparingTo("180.00");
    }

    @Test
    void getShares_withSingleParticipant_getsFullAmount() {
        Participant alice = Participant.create("Alice", 5);
        Participant bob = Participant.create("Bob", 2);

        Split split = Split.create("Test Split");
        split.addParticipant(alice);
        split.addParticipant(bob);

        ExpenseByNightCustom expense = ExpenseByNightCustom.create(new BigDecimal("100.00"), "Hotel", alice.id(),
                List.of(alice.id()));
        split.addExpense(expense);

        List<Expense.Share> shares = expense.getShares(split);

        assertThat(shares).hasSize(1);
        assertThat(shares.get(0).amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void create_withNullParticipantIds_throwsException() {
        Participant alice = Participant.create("Alice", 3);

        assertThatThrownBy(() -> ExpenseByNightCustom.create(new BigDecimal("100.00"), "Test", alice.id(), null))
                .isInstanceOf(NullPointerException.class).hasMessageContaining("participantIds cannot be null");
    }

    @Test
    void create_withEmptyParticipantIds_throwsException() {
        Participant alice = Participant.create("Alice", 3);

        assertThatThrownBy(() -> ExpenseByNightCustom.create(new BigDecimal("100.00"), "Test", alice.id(), List.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("participantIds cannot be empty");
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
        Participant.Id participantId = Participant.Id.generate();

        ExpenseByNightCustom expense = ExpenseByNightCustom.fromJson(id, new BigDecimal("150.00"), "Dinner", payerId,
                List.of(participantId), java.time.Instant.now());

        assertThat(expense.getId()).isEqualTo(id);
        assertThat(expense.getAmount()).isEqualByComparingTo("150.00");
        assertThat(expense.getDescription()).isEqualTo("Dinner");
        assertThat(expense.getPayerId()).isEqualTo(payerId);
        assertThat(expense.getParticipantIds()).containsExactly(participantId);
    }

    @Test
    void getShares_withMultiplePersonsPerParticipant_weightsCorrectly() {
        // Alice: 3 nights, 2 persons → weight 6
        // Bob: 3 nights, 1 person → weight 3
        // Total weight: 9 (VanGuest excluded)
        // Expense 90: Alice 60, Bob 30
        Participant alice = Participant.create("Alice", 3, 2.0);
        Participant bob = Participant.create("Bob", 3, 1.0);
        Participant vanGuest = Participant.create("VanGuest", 3, 1.0);

        Split split = Split.create("Test Split");
        split.addParticipant(alice);
        split.addParticipant(bob);
        split.addParticipant(vanGuest);

        ExpenseByNightCustom expense = ExpenseByNightCustom.create(new BigDecimal("90.00"), "House Rent", alice.id(),
                List.of(alice.id(), bob.id()));
        split.addExpense(expense);

        List<Expense.Share> shares = expense.getShares(split);

        assertThat(shares).hasSize(2);
        BigDecimal aliceShare = shares.stream().filter(s -> s.participantId().equals(alice.id())).findFirst()
                .orElseThrow().amount();
        BigDecimal bobShare = shares.stream().filter(s -> s.participantId().equals(bob.id())).findFirst().orElseThrow()
                .amount();

        assertThat(aliceShare).isEqualByComparingTo("60.00");
        assertThat(bobShare).isEqualByComparingTo("30.00");
    }
}
