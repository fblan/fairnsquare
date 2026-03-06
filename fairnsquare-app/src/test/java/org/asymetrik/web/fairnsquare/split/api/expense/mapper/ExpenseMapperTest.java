package org.asymetrik.web.fairnsquare.split.api.expense.mapper;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;

import org.asymetrik.web.fairnsquare.split.api.expense.dto.ExpenseByNightDTO;
import org.asymetrik.web.fairnsquare.split.api.expense.dto.ExpenseDTO;
import org.asymetrik.web.fairnsquare.split.api.expense.dto.ExpenseEqualDTO;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseByNight;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseEqual;
import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.asymetrik.web.fairnsquare.split.domain.Split;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ExpenseMapperTest {

    @Inject
    ExpenseMapper mapper;

    @Test
    void shouldMapExpenseByNightToDTO() {
        Participant.Id payerId = Participant.Id.generate();
        ExpenseByNight expense = ExpenseByNight.create(BigDecimal.valueOf(100.50), "Hotel", payerId);
        Split split = Split.create("Trip");
        Participant p = Participant.create("Alice", 2);
        split.addParticipant(p);

        ExpenseDTO dto = mapper.toDTO(expense, split);

        assertThat(dto).isInstanceOf(ExpenseByNightDTO.class);
        assertThat(dto.id()).isEqualTo(expense.getId().value());
        assertThat(dto.description()).isEqualTo("Hotel");
        assertThat(dto.amount()).isEqualByComparingTo(BigDecimal.valueOf(100.50));
        assertThat(dto.payerId()).isEqualTo(payerId.value());
        assertThat(dto.type()).isEqualTo("BY_NIGHT");
        assertThat(dto.splitMode()).isEqualTo("BY_NIGHT");
    }

    @Test
    void shouldMapExpenseEqualToDTO() {
        Participant.Id payerId = Participant.Id.generate();
        ExpenseEqual expense = ExpenseEqual.create(BigDecimal.valueOf(50.00), "Dinner", payerId);
        Split split = Split.create("Trip");
        Participant p = Participant.create("Bob", 1);
        split.addParticipant(p);

        ExpenseDTO dto = mapper.toDTO(expense, split);

        assertThat(dto).isInstanceOf(ExpenseEqualDTO.class);
        assertThat(dto.id()).isEqualTo(expense.getId().value());
        assertThat(dto.description()).isEqualTo("Dinner");
        assertThat(dto.amount()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
        assertThat(dto.payerId()).isEqualTo(payerId.value());
        assertThat(dto.type()).isEqualTo("EQUAL");
        assertThat(dto.splitMode()).isEqualTo("EQUAL");
    }

    @Test
    void shouldHandleNullInput() {
        assertThatNullPointerException().isThrownBy(() -> mapper.toDTO(null, null))
                .withMessage("Expense cannot be null");
    }

    @Test
    void shouldReturnEmptySharesWhenSplitIsNull() {
        Participant.Id payerId = Participant.Id.generate();
        ExpenseByNight expense = ExpenseByNight.create(BigDecimal.valueOf(100.00), "Hotel", payerId);

        ExpenseDTO dto = mapper.toDTO(expense, null);

        assertThat(dto.shares()).isEmpty();
    }
}
