package org.asymetrik.web.fairnsquare.split.domain.expenses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.junit.jupiter.api.Test;

class ExpenseFreeTest {

    private static final Participant.Id ALICE_ID = Participant.Id.of("alice1234567890123456");
    private static final Participant.Id BOB_ID = Participant.Id.of("bob123456789012345678");
    private static final Participant.Id CHARLIE_ID = Participant.Id.of("charlie12345678901234");

    @Test
    void create_validShares_succeeds() {
        // Arrange - parts can be any positive values (e.g., 2, 3, 1)
        BigDecimal amount = new BigDecimal("100.00");
        List<Expense.Share> shares = List.of(Expense.Share.withParts(ALICE_ID, new BigDecimal("2")),
                Expense.Share.withParts(BOB_ID, new BigDecimal("3")),
                Expense.Share.withParts(CHARLIE_ID, new BigDecimal("1")));

        // Act
        ExpenseFree expense = ExpenseFree.create(amount, "Groceries", ALICE_ID, shares);

        // Assert
        assertThat(expense).isNotNull();
        assertThat(expense.getAmount()).isEqualByComparingTo(amount);
        assertThat(expense.getDescription()).isEqualTo("Groceries");
        assertThat(expense.getPayerId()).isEqualTo(ALICE_ID);
        assertThat(expense.getSplitMode()).isEqualTo(SplitMode.FREE);
    }

    @Test
    void getShares_calculatesAmountsFromParts() {
        // Arrange - 2 parts and 3 parts should split €100 as €40 and €60
        List<Expense.Share> shares = List.of(Expense.Share.withParts(ALICE_ID, new BigDecimal("2")),
                Expense.Share.withParts(BOB_ID, new BigDecimal("3")));
        ExpenseFree expense = ExpenseFree.create(new BigDecimal("100.00"), "Dinner", ALICE_ID, shares);

        // Act
        List<Expense.Share> result = expense.getShares(null); // Split not needed for FREE mode

        // Assert - getShares() returns shares with BOTH calculated amounts AND original parts
        assertThat(result).hasSize(2);
        assertThat(result.get(0).participantId()).isEqualTo(ALICE_ID);
        assertThat(result.get(0).amount()).isEqualByComparingTo("40.00"); // 100 * (2/5)
        assertThat(result.get(0).parts()).isEqualByComparingTo("2");
        assertThat(result.get(1).participantId()).isEqualTo(BOB_ID);
        assertThat(result.get(1).amount()).isEqualByComparingTo("60.00"); // 100 * (3/5)
        assertThat(result.get(1).parts()).isEqualByComparingTo("3");
    }

    @Test
    void create_allZeroParts_throwsException() {
        // Arrange - All zero parts is invalid (at least one participant must have positive parts)
        BigDecimal amount = new BigDecimal("100.00");
        List<Expense.Share> shares = List.of(Expense.Share.withParts(ALICE_ID, BigDecimal.ZERO),
                Expense.Share.withParts(BOB_ID, BigDecimal.ZERO));

        // Act & Assert
        assertThatThrownBy(() -> ExpenseFree.create(amount, "All zero", ALICE_ID, shares))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one share must have positive parts");
    }

    @Test
    void create_emptyShares_throwsException() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");
        List<Expense.Share> shares = List.of();

        // Act & Assert
        assertThatThrownBy(() -> ExpenseFree.create(amount, "No shares", ALICE_ID, shares))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must have at least one share");
    }

    @Test
    void create_nullShares_throwsException() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");

        // Act & Assert
        assertThatThrownBy(() -> ExpenseFree.create(amount, "Null shares", ALICE_ID, (List<Expense.Share>) null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Shares cannot be null");
    }

    @Test
    void create_negativeShareParts_throwsException() {
        // Arrange - Share validation happens in Share record constructor
        // Act & Assert
        assertThatThrownBy(() -> Expense.Share.withParts(ALICE_ID, new BigDecimal("-10.00")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Share parts cannot be negative");
    }

    @Test
    void create_zeroShareParts_succeeds() {
        // Arrange - Zero parts are allowed (participant doesn't owe anything)
        BigDecimal amount = new BigDecimal("100.00");
        List<Expense.Share> shares = List.of(Expense.Share.withParts(ALICE_ID, new BigDecimal("5")),
                Expense.Share.withParts(BOB_ID, BigDecimal.ZERO)); // Bob doesn't owe anything

        // Act
        ExpenseFree expense = ExpenseFree.create(amount, "Alice pays all", ALICE_ID, shares);

        // Assert
        assertThat(expense).isNotNull();
        List<Expense.Share> result = expense.getShares(null);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).participantId()).isEqualTo(ALICE_ID);
        assertThat(result.get(0).amount()).isEqualByComparingTo("100.00"); // All 5 parts = 100%
        assertThat(result.get(0).parts()).isEqualByComparingTo("5");
        assertThat(result.get(1).participantId()).isEqualTo(BOB_ID);
        assertThat(result.get(1).amount()).isEqualByComparingTo("0.00"); // 0 parts = 0%
        assertThat(result.get(1).parts()).isEqualByComparingTo("0");
    }

    @Test
    void create_nullAmount_throwsException() {
        // Arrange
        List<Expense.Share> shares = List.of(Expense.Share.withParts(ALICE_ID, new BigDecimal("50.00")));

        // Act & Assert
        assertThatThrownBy(() -> ExpenseFree.create(null, "No amount", ALICE_ID, shares))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Amount cannot be null");
    }

    @Test
    void create_negativeAmount_throwsException() {
        // Arrange - use positive shares since we're testing amount validation, not share validation
        List<Expense.Share> shares = List.of(Expense.Share.withParts(ALICE_ID, new BigDecimal("50.00")));

        // Act & Assert
        assertThatThrownBy(() -> ExpenseFree.create(new BigDecimal("-50.00"), "Negative", ALICE_ID, shares))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Amount must be positive");
    }

    @Test
    void create_blankDescription_throwsException() {
        // Arrange
        List<Expense.Share> shares = List.of(Expense.Share.withParts(ALICE_ID, new BigDecimal("50.00")));

        // Act & Assert
        assertThatThrownBy(() -> ExpenseFree.create(new BigDecimal("50.00"), "", ALICE_ID, shares))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Description cannot be blank");
    }

    @Test
    void fromJson_reconstitutesExpense() {
        // Arrange - Store parts (3 and 2 parts)
        Expense.Id id = Expense.Id.generate();
        BigDecimal amount = new BigDecimal("100.00");
        List<Expense.Share> shares = List.of(Expense.Share.withParts(ALICE_ID, new BigDecimal("3")),
                Expense.Share.withParts(BOB_ID, new BigDecimal("2")));
        java.time.Instant createdAt = java.time.Instant.now();

        // Act
        ExpenseFree expense = ExpenseFree.fromJson(id, amount, "Reconstituted", ALICE_ID, shares, createdAt);

        // Assert
        assertThat(expense.getId()).isEqualTo(id);
        assertThat(expense.getAmount()).isEqualByComparingTo(amount);
        assertThat(expense.getDescription()).isEqualTo("Reconstituted");
        assertThat(expense.getPayerId()).isEqualTo(ALICE_ID);
        assertThat(expense.getCreatedAt()).isEqualTo(createdAt);

        // getShares() returns shares with calculated amounts AND original parts
        List<Expense.Share> result = expense.getShares(null);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).participantId()).isEqualTo(ALICE_ID);
        assertThat(result.get(0).amount()).isEqualByComparingTo("60.00"); // 100 * (3/5)
        assertThat(result.get(0).parts()).isEqualByComparingTo("3");
        assertThat(result.get(1).participantId()).isEqualTo(BOB_ID);
        assertThat(result.get(1).amount()).isEqualByComparingTo("40.00"); // 100 * (2/5)
        assertThat(result.get(1).parts()).isEqualByComparingTo("2");
    }
}
