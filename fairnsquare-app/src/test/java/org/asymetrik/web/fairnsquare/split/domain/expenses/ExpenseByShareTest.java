package org.asymetrik.web.fairnsquare.split.domain.expenses;

import java.math.BigDecimal;
import java.util.List;

import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ExpenseByShare share calculation logic.
 */
class ExpenseByShareTest {

    @Test
    void calculateShares_withThreeParticipantsEqualShare_splitsEqually() {
        // Given: 3 participants each with 1 share (default)
        Participant alice = createParticipant("Alice", 4, 1.0);
        Participant bob = createParticipant("Bob", 2, 1.0);
        Participant charlie = createParticipant("Charlie", 3, 1.0);
        List<Participant> participants = List.of(alice, bob, charlie);

        ExpenseByShare expense = ExpenseByShare.create(new BigDecimal("90.00"), "Groceries", alice.id());

        // When
        List<Expense.Share> shares = expense.calculateShares(participants);

        // Then: equal split (1/3 each), nights are ignored
        assertThat(shares).hasSize(3);
        assertThat(shares.get(0).amount()).isEqualByComparingTo("30.00");
        assertThat(shares.get(1).amount()).isEqualByComparingTo("30.00");
        assertThat(shares.get(2).amount()).isEqualByComparingTo("30.00");
    }

    @Test
    void calculateShares_withDifferentShares_proportionalToShare() {
        // Alice: 2 share, Bob: 1 share → Alice pays 2/3, Bob pays 1/3
        // Nights are completely ignored
        Participant alice = createParticipant("Alice", 1, 2.0);
        Participant bob = createParticipant("Bob", 10, 1.0);
        List<Participant> participants = List.of(alice, bob);

        ExpenseByShare expense = ExpenseByShare.create(new BigDecimal("90.00"), "Dinner", alice.id());

        // When
        List<Expense.Share> shares = expense.calculateShares(participants);

        // Then: proportional to share only
        assertThat(shares).hasSize(2);
        assertThat(shares.get(0).amount()).isEqualByComparingTo("60.00"); // Alice: 2/3 * 90
        assertThat(shares.get(1).amount()).isEqualByComparingTo("30.00"); // Bob: remainder
    }

    @Test
    void calculateShares_nightsAreIgnored_onlyShareCounts() {
        // Alice: 1 night, 2 share → 2 weight units
        // Bob: 10 nights, 1 share → 1 weight unit
        // BY_NIGHT would give Alice 2*10/(2*10+10*1)=2/3... but BY_SHARE ignores nights
        // BY_SHARE gives Alice 2/3 regardless of nights
        Participant alice = createParticipant("Alice", 1, 2.0);
        Participant bob = createParticipant("Bob", 10, 1.0);
        List<Participant> participants = List.of(alice, bob);

        ExpenseByShare expense = ExpenseByShare.create(new BigDecimal("120.00"), "Hotel", alice.id());

        List<Expense.Share> shares = expense.calculateShares(participants);

        assertThat(shares).hasSize(2);
        assertThat(shares.get(0).amount()).isEqualByComparingTo("80.00"); // Alice: 2/3 * 120
        assertThat(shares.get(1).amount()).isEqualByComparingTo("40.00"); // Bob: 1/3 * 120
    }

    @Test
    void calculateShares_withHalfShare_weightsCorrectly() {
        // Family: 2.5 share; Single: 1 share → total 3.5
        // €70 expense
        Participant family = createParticipant("Family", 3, 2.5);
        Participant single = createParticipant("Single", 3, 1.0);
        List<Participant> participants = List.of(family, single);

        ExpenseByShare expense = ExpenseByShare.create(new BigDecimal("70.00"), "Groceries", family.id());

        List<Expense.Share> shares = expense.calculateShares(participants);

        assertThat(shares).hasSize(2);
        assertThat(shares.get(0).amount()).isEqualByComparingTo("50.00"); // 2.5/3.5 * 70
        assertThat(shares.get(1).amount()).isEqualByComparingTo("20.00"); // remainder

        BigDecimal total = shares.stream().map(Expense.Share::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("70.00");
    }

    @Test
    void calculateShares_withIndivisibleAmount_lastParticipantGetsRemainder() {
        // €100 / 3 shares = 33.33... — remainder goes to last participant
        Participant alice = createParticipant("Alice", 1, 1.0);
        Participant bob = createParticipant("Bob", 1, 1.0);
        Participant charlie = createParticipant("Charlie", 1, 1.0);
        List<Participant> participants = List.of(alice, bob, charlie);

        ExpenseByShare expense = ExpenseByShare.create(new BigDecimal("100.00"), "Dinner", alice.id());

        List<Expense.Share> shares = expense.calculateShares(participants);

        BigDecimal total = shares.stream().map(Expense.Share::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("100.00");
        assertThat(shares.get(0).amount()).isEqualByComparingTo("33.33");
        assertThat(shares.get(1).amount()).isEqualByComparingTo("33.33");
        assertThat(shares.get(2).amount()).isEqualByComparingTo("33.34");
    }

    @Test
    void calculateShares_withSingleParticipant_getsFullAmount() {
        Participant alice = createParticipant("Alice", 5, 2.0);
        List<Participant> participants = List.of(alice);

        ExpenseByShare expense = ExpenseByShare.create(new BigDecimal("100.00"), "Hotel", alice.id());

        List<Expense.Share> shares = expense.calculateShares(participants);

        assertThat(shares).hasSize(1);
        assertThat(shares.get(0).amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void calculateShares_withEmptyParticipants_returnsEmptyList() {
        ExpenseByShare expense = ExpenseByShare.create(new BigDecimal("100.00"), "Test", Participant.Id.generate());

        List<Expense.Share> shares = expense.calculateShares(List.of());

        assertThat(shares).isEmpty();
    }

    @Test
    void calculateShares_withNullParticipants_returnsEmptyList() {
        ExpenseByShare expense = ExpenseByShare.create(new BigDecimal("100.00"), "Test", Participant.Id.generate());

        List<Expense.Share> shares = expense.calculateShares(null);

        assertThat(shares).isEmpty();
    }

    @Test
    void getSplitMode_returnsByShare() {
        ExpenseByShare expense = ExpenseByShare.create(new BigDecimal("100.00"), "Test", Participant.Id.generate());

        assertThat(expense.getSplitMode()).isEqualTo(SplitMode.BY_SHARE);
    }

    @Test
    void fromJson_createsValidExpense() {
        Expense.Id id = Expense.Id.generate();
        Participant.Id payerId = Participant.Id.generate();

        ExpenseByShare expense = ExpenseByShare.fromJson(id, new BigDecimal("150.00"), "Dinner", payerId,
                java.time.Instant.now());

        assertThat(expense.getId()).isEqualTo(id);
        assertThat(expense.getAmount()).isEqualByComparingTo("150.00");
        assertThat(expense.getDescription()).isEqualTo("Dinner");
        assertThat(expense.getPayerId()).isEqualTo(payerId);
        assertThat(expense.getSplitMode()).isEqualTo(SplitMode.BY_SHARE);
    }

    private Participant createParticipant(String name, double nights, double share) {
        return Participant.create(name, nights, share);
    }
}
