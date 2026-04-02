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
    void getShares_withAllParticipantsIncluded_behavesLikeByNight() {
        Participant alice = Participant.create("Alice", 4);
        Participant bob = Participant.create("Bob", 2);
        Participant charlie = Participant.create("Charlie", 3);

        ExpenseByNightCustom expense = ExpenseByNightCustom.create(new BigDecimal("180.00"), "Groceries", alice.id(),
                List.of(alice.id(), bob.id(), charlie.id()));

        // Need a mock split - use a helper that returns all three participants
        var split = buildSplit(List.of(alice, bob, charlie));
        List<Expense.Share> shares = expense.getShares(split);

        assertThat(shares).hasSize(3);
        assertThat(shares.get(0).amount()).isEqualByComparingTo("80.00"); // 4/9 * 180
        assertThat(shares.get(1).amount()).isEqualByComparingTo("40.00"); // 2/9 * 180
        assertThat(shares.get(2).amount()).isEqualByComparingTo("60.00"); // remainder
    }

    @Test
    void getShares_withSubsetOfParticipants_excludesNonIncluded() {
        Participant alice = Participant.create("Alice", 4);
        Participant bob = Participant.create("Bob", 2);
        Participant charlie = Participant.create("Charlie", 3); // excluded

        ExpenseByNightCustom expense = ExpenseByNightCustom.create(new BigDecimal("180.00"), "Groceries", alice.id(),
                List.of(alice.id(), bob.id()));

        var split = buildSplit(List.of(alice, bob, charlie));
        List<Expense.Share> shares = expense.getShares(split);

        // Only alice and bob are included: 4/6 * 180 = 120, 2/6 * 180 = 60
        assertThat(shares).hasSize(2);
        assertThat(shares.stream().map(s -> s.participantId()).toList())
                .containsExactlyInAnyOrder(alice.id(), bob.id());
        assertThat(shares.get(0).amount()).isEqualByComparingTo("120.00");
        assertThat(shares.get(1).amount()).isEqualByComparingTo("60.00");
    }

    @Test
    void getShares_withSingleIncludedParticipant_getsFullAmount() {
        Participant alice = Participant.create("Alice", 4);
        Participant bob = Participant.create("Bob", 2);

        ExpenseByNightCustom expense = ExpenseByNightCustom.create(new BigDecimal("100.00"), "Solo expense", alice.id(),
                List.of(alice.id()));

        var split = buildSplit(List.of(alice, bob));
        List<Expense.Share> shares = expense.getShares(split);

        assertThat(shares).hasSize(1);
        assertThat(shares.get(0).participantId()).isEqualTo(alice.id());
        assertThat(shares.get(0).amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void getShares_sumsToExactExpenseAmount() {
        Participant alice = Participant.create("Alice", 4);
        Participant bob = Participant.create("Bob", 3);
        Participant charlie = Participant.create("Charlie", 2);

        ExpenseByNightCustom expense = ExpenseByNightCustom.create(new BigDecimal("100.00"), "Dinner", alice.id(),
                List.of(alice.id(), bob.id(), charlie.id()));

        var split = buildSplit(List.of(alice, bob, charlie));
        List<Expense.Share> shares = expense.getShares(split);

        BigDecimal total = shares.stream().map(Expense.Share::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("100.00");
    }

    @Test
    void getSplitMode_returnsByNightCustom() {
        Participant alice = Participant.create("Alice", 3);

        ExpenseByNightCustom expense = ExpenseByNightCustom.create(new BigDecimal("100.00"), "Test", alice.id(),
                List.of(alice.id()));

        assertThat(expense.getSplitMode()).isEqualTo(SplitMode.BY_NIGHT_CUSTOM);
    }

    @Test
    void create_withEmptyParticipantIds_throwsIllegalArgumentException() {
        Participant.Id payerId = Participant.Id.generate();

        assertThatThrownBy(() -> ExpenseByNightCustom.create(new BigDecimal("100.00"), "Test", payerId, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one participant");
    }

    @Test
    void create_withNullParticipantIds_throwsIllegalArgumentException() {
        Participant.Id payerId = Participant.Id.generate();

        assertThatThrownBy(() -> ExpenseByNightCustom.create(new BigDecimal("100.00"), "Test", payerId, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromJson_recreatesExpenseWithSameParticipantIds() {
        Expense.Id id = Expense.Id.generate();
        Participant alice = Participant.create("Alice", 3);
        Participant bob = Participant.create("Bob", 2);
        List<Participant.Id> includedIds = List.of(alice.id(), bob.id());

        ExpenseByNightCustom expense = ExpenseByNightCustom.fromJson(id, new BigDecimal("90.00"), "Hotel", alice.id(),
                includedIds, java.time.Instant.now());

        assertThat(expense.getId()).isEqualTo(id);
        assertThat(expense.getIncludedParticipantIds()).containsExactlyInAnyOrderElementsOf(includedIds);
    }

    @Test
    void getShares_withPersonsWeighting_appliesNightTimesPersonsFormula() {
        // Alice: couple (2 persons), 3 nights → weight 6
        // Bob: solo (1 person), 3 nights → weight 3
        // Charlie (solo, 3 nights) is excluded
        Participant alice = Participant.create("Alice", 3, 2.0);
        Participant bob = Participant.create("Bob", 3, 1.0);
        Participant charlie = Participant.create("Charlie", 3, 1.0);

        ExpenseByNightCustom expense = ExpenseByNightCustom.create(new BigDecimal("90.00"), "Accommodation", alice.id(),
                List.of(alice.id(), bob.id()));

        var split = buildSplit(List.of(alice, bob, charlie));
        List<Expense.Share> shares = expense.getShares(split);

        // Alice: 6/9 * 90 = 60, Bob: 3/9 * 90 = 30
        assertThat(shares).hasSize(2);
        assertThat(shares.get(0).amount()).isEqualByComparingTo("60.00");
        assertThat(shares.get(1).amount()).isEqualByComparingTo("30.00");
    }

    /**
     * Builds a minimal Split stub for testing, containing the provided participants.
     */
    private org.asymetrik.web.fairnsquare.split.domain.Split buildSplit(List<Participant> participants) {
        org.asymetrik.web.fairnsquare.split.domain.Split split = org.asymetrik.web.fairnsquare.split.domain.Split
                .create("Test Split");
        for (Participant p : participants) {
            split.addParticipant(p);
        }
        return split;
    }
}
