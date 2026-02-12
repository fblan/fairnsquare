package org.asymetrik.web.fairnsquare.split.domain.expenses;

import java.math.BigDecimal;

import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for Expense sealed abstract class and its factory methods.
 */
class ExpenseTest {

    @Test
    @SuppressWarnings("deprecation")
    void create_withByNightMode_returnsExpenseByNight() {
        Participant.Id payerId = Participant.Id.generate();

        Expense expense = Expense.create(new BigDecimal("100.00"), "Test", payerId, SplitMode.BY_NIGHT);

        assertThat(expense).isInstanceOf(ExpenseByNight.class);
        assertThat(expense.getSplitMode()).isEqualTo(SplitMode.BY_NIGHT);
    }

    @Test
    @SuppressWarnings("deprecation")
    void create_withEqualMode_returnsExpenseEqual() {
        Participant.Id payerId = Participant.Id.generate();
        Expense expense = Expense.create(new BigDecimal("100.00"), "Test", payerId, SplitMode.EQUAL);

        assertThat(expense).isInstanceOf(ExpenseEqual.class);
        assertThat(expense.getSplitMode()).isEqualTo(SplitMode.EQUAL);
    }

    @Test
    @SuppressWarnings("deprecation")
    void create_withFreeMode_throwsUnsupportedOperationException() {
        Participant.Id payerId = Participant.Id.generate();

        assertThatThrownBy(() -> Expense.create(new BigDecimal("100.00"), "Test", payerId, SplitMode.FREE))
                .isInstanceOf(UnsupportedOperationException.class).hasMessageContaining("FREE mode requires shares");
    }

    @Test
    @SuppressWarnings("deprecation")
    void create_withNullAmount_throwsIllegalArgumentException() {
        Participant.Id payerId = Participant.Id.generate();

        assertThatThrownBy(() -> Expense.create(null, "Test", payerId, SplitMode.BY_NIGHT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Amount cannot be null");
    }

    @Test
    @SuppressWarnings("deprecation")
    void create_withZeroAmount_throwsIllegalArgumentException() {
        Participant.Id payerId = Participant.Id.generate();

        assertThatThrownBy(() -> Expense.create(BigDecimal.ZERO, "Test", payerId, SplitMode.BY_NIGHT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Amount must be positive");
    }

    @Test
    @SuppressWarnings("deprecation")
    void create_withNegativeAmount_throwsIllegalArgumentException() {
        Participant.Id payerId = Participant.Id.generate();

        assertThatThrownBy(() -> Expense.create(new BigDecimal("-10.00"), "Test", payerId, SplitMode.BY_NIGHT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Amount must be positive");
    }

    @Test
    @SuppressWarnings("deprecation")
    void create_withNullDescription_throwsIllegalArgumentException() {
        Participant.Id payerId = Participant.Id.generate();

        assertThatThrownBy(() -> Expense.create(new BigDecimal("100.00"), null, payerId, SplitMode.BY_NIGHT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Description cannot be blank");
    }

    @Test
    @SuppressWarnings("deprecation")
    void create_withBlankDescription_throwsIllegalArgumentException() {
        Participant.Id payerId = Participant.Id.generate();

        assertThatThrownBy(() -> Expense.create(new BigDecimal("100.00"), "   ", payerId, SplitMode.BY_NIGHT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Description cannot be blank");
    }

    @Test
    @SuppressWarnings("deprecation")
    void create_withTooLongDescription_throwsIllegalArgumentException() {
        Participant.Id payerId = Participant.Id.generate();
        String longDescription = "x".repeat(201);

        assertThatThrownBy(() -> Expense.create(new BigDecimal("100.00"), longDescription, payerId, SplitMode.BY_NIGHT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Description cannot exceed 200 characters");
    }

    @Test
    void fromJson_minimalExpense_createsExpenseWithOnlyPayerId() {
        Participant.Id payerId = Participant.Id.generate();

        Expense expense = Expense.fromJson(null, null, null, payerId, null, null);

        assertThat(expense).isInstanceOf(ExpenseByNight.class);
        assertThat(expense.getPayerId()).isEqualTo(payerId);
        assertThat(expense.getId()).isNull();
        assertThat(expense.getAmount()).isNull();
    }

    @Test
    void fromJson_withByNightMode_createsExpenseByNight() {
        Expense.Id id = Expense.Id.generate();
        Participant.Id payerId = Participant.Id.generate();

        Expense expense = Expense.fromJson(id, new BigDecimal("100.00"), "Test", payerId, SplitMode.BY_NIGHT,
                java.time.Instant.now());

        assertThat(expense).isInstanceOf(ExpenseByNight.class);
    }

    @Test
    void fromJson_withEqualMode_createsExpenseEqual() {
        Expense.Id id = Expense.Id.generate();
        Participant.Id payerId = Participant.Id.generate();

        Expense expense = Expense.fromJson(id, new BigDecimal("100.00"), "Test", payerId, SplitMode.EQUAL,
                java.time.Instant.now());

        assertThat(expense).isInstanceOf(ExpenseEqual.class);
    }

    @Test
    void fromJson_withFreeMode_throwsUnsupportedOperationException() {
        Expense.Id id = Expense.Id.generate();
        Participant.Id payerId = Participant.Id.generate();

        assertThatThrownBy(() -> Expense.fromJson(id, new BigDecimal("100.00"), "Test", payerId, SplitMode.FREE,
                java.time.Instant.now())).isInstanceOf(UnsupportedOperationException.class)
                        .hasMessageContaining("FREE mode requires shares");
    }

    @Test
    void fromJson_withNullSplitMode_defaultsToByNight() {
        Expense.Id id = Expense.Id.generate();
        Participant.Id payerId = Participant.Id.generate();

        Expense expense = Expense.fromJson(id, new BigDecimal("100.00"), "Test", payerId, null,
                java.time.Instant.now());

        assertThat(expense).isInstanceOf(ExpenseByNight.class);
    }

    // ==================== Expense.Id value object tests ====================

    @Test
    void expenseId_isValid_returnsTrueForValidId() {
        String validId = "V1StGXR8_Z5jdHi6B-myT";
        assertThat(Expense.Id.isValid(validId)).isTrue();
    }

    @Test
    void expenseId_isValid_returnsFalseForNull() {
        assertThat(Expense.Id.isValid(null)).isFalse();
    }

    @Test
    void expenseId_isValid_returnsFalseForBlank() {
        assertThat(Expense.Id.isValid("")).isFalse();
        assertThat(Expense.Id.isValid("   ")).isFalse();
    }

    @Test
    void expenseId_isValid_returnsFalseForInvalidCharacters() {
        assertThat(Expense.Id.isValid("V1StGXR8_Z5jdHi6B-my!")).isFalse();
    }

    @Test
    void expenseId_isValid_returnsFalseForWrongLength() {
        assertThat(Expense.Id.isValid("short")).isFalse();
        assertThat(Expense.Id.isValid("V1StGXR8_Z5jdHi6B-myTT")).isFalse(); // 22 chars
    }

    @Test
    void expenseId_of_createsIdFromString() {
        String value = "V1StGXR8_Z5jdHi6B-myT";
        Expense.Id id = Expense.Id.of(value);
        assertThat(id.value()).isEqualTo(value);
    }

    @Test
    void expenseId_fromJson_createsIdFromString() {
        String value = "V1StGXR8_Z5jdHi6B-myT";
        Expense.Id id = Expense.Id.fromJson(value);
        assertThat(id.value()).isEqualTo(value);
    }

    @Test
    void expenseId_generate_createsValidId() {
        Expense.Id id = Expense.Id.generate();
        assertThat(Expense.Id.isValid(id.value())).isTrue();
    }

    @Test
    void expenseId_constructor_throwsForNull() {
        assertThatThrownBy(() -> new Expense.Id(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or blank");
    }

    @Test
    void expenseId_constructor_throwsForBlank() {
        assertThatThrownBy(() -> new Expense.Id("   ")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or blank");
    }

    @Test
    void expenseId_constructor_throwsForInvalidPattern() {
        assertThatThrownBy(() -> new Expense.Id("V1StGXR8_Z5jdHi6B-my!")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid characters");
    }

    @Test
    void expenseId_constructor_throwsForWrongLength() {
        assertThatThrownBy(() -> new Expense.Id("short")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly 21 characters");
    }

    // ==================== Expense.Share value object tests ====================

    @Test
    void share_constructor_throwsForNullParticipantId() {
        assertThatThrownBy(() -> new Expense.Share(null, new BigDecimal("10.00"), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("participantId cannot be null");
    }

    @Test
    void share_withAmount_throwsForNegativeAmount() {
        assertThatThrownBy(() -> Expense.Share.withAmount(Participant.Id.generate(), new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("amount cannot be negative");
    }

    @Test
    void share_withParts_throwsForNegativeParts() {
        assertThatThrownBy(() -> Expense.Share.withParts(Participant.Id.generate(), new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("parts cannot be negative");
    }

    @Test
    void share_fromJson_createsValidShareWithAmount() {
        Participant.Id participantId = Participant.Id.generate();
        BigDecimal amount = new BigDecimal("25.50");

        Expense.Share share = Expense.Share.fromJson(participantId, amount, null);

        assertThat(share.participantId()).isEqualTo(participantId);
        assertThat(share.amount()).isEqualByComparingTo("25.50");
        assertThat(share.parts()).isNull();
    }

    @Test
    void share_fromJson_createsValidShareWithParts() {
        Participant.Id participantId = Participant.Id.generate();
        BigDecimal parts = new BigDecimal("2.5");

        Expense.Share share = Expense.Share.fromJson(participantId, null, parts);

        assertThat(share.participantId()).isEqualTo(participantId);
        assertThat(share.amount()).isNull();
        assertThat(share.parts()).isEqualByComparingTo("2.5");
    }
}
