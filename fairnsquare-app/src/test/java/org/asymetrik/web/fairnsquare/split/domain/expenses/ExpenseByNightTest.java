package org.asymetrik.web.fairnsquare.split.domain.expenses;

import java.math.BigDecimal;
import java.util.List;

import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ExpenseByNight share calculation logic.
 */
class ExpenseByNightTest {

    @Test
    void calculateShares_withThreeParticipants_proportionalToNights() {
        // Given: 3 participants with different nights (4, 2, 3 = 9 total)
        Participant alice = createParticipant("alice", 4);
        Participant bob = createParticipant("bob", 2);
        Participant charlie = createParticipant("charlie", 3);
        List<Participant> participants = List.of(alice, bob, charlie);

        ExpenseByNight expense = ExpenseByNight.create(new BigDecimal("180.00"), "Groceries", alice.id());

        // When
        List<Expense.Share> shares = expense.calculateShares(participants);

        // Then: shares proportional to nights (4/9, 2/9, 3/9 of 180)
        assertThat(shares).hasSize(3);
        assertThat(shares.get(0).amount()).isEqualByComparingTo("80.00"); // Alice: 4/9 * 180 = 80
        assertThat(shares.get(1).amount()).isEqualByComparingTo("40.00"); // Bob: 2/9 * 180 = 40
        assertThat(shares.get(2).amount()).isEqualByComparingTo("60.00"); // Charlie: remainder = 60
    }

    @Test
    void calculateShares_withSingleParticipant_getsFullAmount() {
        Participant alice = createParticipant("alice", 5);
        List<Participant> participants = List.of(alice);

        ExpenseByNight expense = ExpenseByNight.create(new BigDecimal("100.00"), "Hotel", alice.id());

        List<Expense.Share> shares = expense.calculateShares(participants);

        assertThat(shares).hasSize(1);
        assertThat(shares.get(0).amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void calculateShares_withEmptyParticipants_returnsEmptyList() {
        ExpenseByNight expense = ExpenseByNight.create(new BigDecimal("100.00"), "Test", Participant.Id.generate());

        List<Expense.Share> shares = expense.calculateShares(List.of());

        assertThat(shares).isEmpty();
    }

    @Test
    void calculateShares_withNullParticipants_returnsEmptyList() {
        ExpenseByNight expense = ExpenseByNight.create(new BigDecimal("100.00"), "Test", Participant.Id.generate());

        List<Expense.Share> shares = expense.calculateShares(null);

        assertThat(shares).isEmpty();
    }

    @Test
    void calculateShares_withZeroTotalNights_returnsEmptyList() {
        // Participant validation requires nights >= 1, so we test empty list instead
        // This effectively tests the "0 total nights" scenario
        ExpenseByNight expense = ExpenseByNight.create(new BigDecimal("100.00"), "Test", Participant.Id.generate());

        List<Expense.Share> shares = expense.calculateShares(List.of());

        assertThat(shares).isEmpty();
    }

    @Test
    void calculateShares_withIndivisibleAmount_lastParticipantGetsRemainder() {
        // Given: 100.00 / 3 = 33.33... each, rounding leaves remainder
        Participant alice = createParticipant("alice", 1);
        Participant bob = createParticipant("bob", 1);
        Participant charlie = createParticipant("charlie", 1);
        List<Participant> participants = List.of(alice, bob, charlie);

        ExpenseByNight expense = ExpenseByNight.create(new BigDecimal("100.00"), "Dinner", alice.id());

        List<Expense.Share> shares = expense.calculateShares(participants);

        // Then: sum of shares equals original amount
        BigDecimal total = shares.stream().map(Expense.Share::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("100.00");
    }

    @Test
    void calculateShares_ac1_180WithFourTwoThreeNights_returnsEightyFortySixty() {
        // AC 1: €180 with (4, 2, 3) nights → (€80, €40, €60)
        Participant alice = createParticipant("Alice", 4);
        Participant bob = createParticipant("Bob", 2);
        Participant charlie = createParticipant("Charlie", 3);
        List<Participant> participants = List.of(alice, bob, charlie);

        ExpenseByNight expense = ExpenseByNight.create(new BigDecimal("180.00"), "Accommodation", alice.id());

        List<Expense.Share> shares = expense.calculateShares(participants);

        assertThat(shares).hasSize(3);
        assertThat(shares.get(0).amount()).isEqualByComparingTo("80.00");
        assertThat(shares.get(1).amount()).isEqualByComparingTo("40.00");
        assertThat(shares.get(2).amount()).isEqualByComparingTo("60.00");

        // Verify sum equals expense amount exactly
        BigDecimal total = shares.stream().map(Expense.Share::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("180.00");
    }

    @Test
    void getSplitMode_returnsByNight() {
        ExpenseByNight expense = ExpenseByNight.create(new BigDecimal("100.00"), "Test", Participant.Id.generate());

        assertThat(expense.getSplitMode()).isEqualTo(SplitMode.BY_NIGHT);
    }

    @Test
    void fromJson_createsValidExpense() {
        Expense.Id id = Expense.Id.generate();
        Participant.Id payerId = Participant.Id.generate();

        ExpenseByNight expense = ExpenseByNight.fromJson(id, new BigDecimal("150.00"), "Dinner", payerId,
                java.time.Instant.now());

        assertThat(expense.getId()).isEqualTo(id);
        assertThat(expense.getAmount()).isEqualByComparingTo("150.00");
        assertThat(expense.getDescription()).isEqualTo("Dinner");
        assertThat(expense.getPayerId()).isEqualTo(payerId);
    }

    @Test
    void calculateShares_withDifferentNumberOfPersons_weightsProportionally() {
        // Alice: 4 nights, 2 persons → weight 8
        // Bob: 4 nights, 1 person → weight 4
        // Total weight: 12
        // Expense: €120
        Participant alice = Participant.create("Alice", 4, 2.0);
        Participant bob = Participant.create("Bob", 4, 1.0);
        List<Participant> participants = List.of(alice, bob);

        ExpenseByNight expense = ExpenseByNight.create(new BigDecimal("120.00"), "Hotel", alice.id());

        List<Expense.Share> shares = expense.calculateShares(participants);

        assertThat(shares).hasSize(2);
        assertThat(shares.get(0).amount()).isEqualByComparingTo("80.00"); // Alice: 8/12 * 120 = 80
        assertThat(shares.get(1).amount()).isEqualByComparingTo("40.00"); // Bob: 4/12 * 120 = 40
    }

    @Test
    void calculateShares_withHalfPersonChild_weightsCorrectly() {
        // Family scenario: Alice (2 adults + 1 child = 2.5 persons, 3 nights) → weight 7.5
        // Bob (1 person, 3 nights) → weight 3
        // Total weight: 10.5
        // Expense: €210
        Participant aliceFamily = Participant.create("Alice Family", 3, 2.5);
        Participant bob = Participant.create("Bob", 3, 1.0);
        List<Participant> participants = List.of(aliceFamily, bob);

        ExpenseByNight expense = ExpenseByNight.create(new BigDecimal("210.00"), "Accommodation", aliceFamily.id());

        List<Expense.Share> shares = expense.calculateShares(participants);

        assertThat(shares).hasSize(2);
        assertThat(shares.get(0).amount()).isEqualByComparingTo("150.00"); // 7.5/10.5 * 210 = 150
        assertThat(shares.get(1).amount()).isEqualByComparingTo("60.00"); // 3/10.5 * 210 = 60

        // Verify sum equals total
        BigDecimal total = shares.stream().map(Expense.Share::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("210.00");
    }

    @Test
    void calculateShares_withDefaultOnePersonPerParticipant_sameAsOldBehavior() {
        // When all participants have 1 person, behavior is identical to the original nights-only formula
        Participant alice = Participant.create("Alice", 4); // default 1.0 person
        Participant bob = Participant.create("Bob", 2); // default 1.0 person
        List<Participant> participants = List.of(alice, bob);

        ExpenseByNight expense = ExpenseByNight.create(new BigDecimal("180.00"), "Hotel", alice.id());

        List<Expense.Share> shares = expense.calculateShares(participants);

        assertThat(shares).hasSize(2);
        assertThat(shares.get(0).amount()).isEqualByComparingTo("120.00"); // 4/6 * 180
        assertThat(shares.get(1).amount()).isEqualByComparingTo("60.00"); // 2/6 * 180
    }

    private Participant createParticipant(String name, double nights) {
        return Participant.create(name, nights);
    }
}
