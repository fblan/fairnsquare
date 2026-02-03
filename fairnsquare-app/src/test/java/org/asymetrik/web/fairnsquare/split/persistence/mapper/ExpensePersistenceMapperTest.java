package org.asymetrik.web.fairnsquare.split.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.split.domain.Expense;
import org.asymetrik.web.fairnsquare.split.domain.ExpenseByNight;
import org.asymetrik.web.fairnsquare.split.domain.ExpenseEqual;
import org.asymetrik.web.fairnsquare.split.domain.Participant;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ExpenseByNightPersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ExpenseEqualPersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ExpensePersistenceDTO;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ExpensePersistenceMapperTest {

    @Inject
    ExpensePersistenceMapper mapper;

    @Test
    void shouldMapByNightExpenseToPersistenceDTOWithoutShares() {
        Participant.Id payerId = Participant.Id.generate();

        ExpenseByNight expense = ExpenseByNight.create(new BigDecimal("100.00"), "Hotel", payerId);

        ExpensePersistenceDTO dto = mapper.toPersistenceDTO(expense);

        assertThat(dto).isInstanceOf(ExpenseByNightPersistenceDTO.class);
        assertThat(dto.id()).isEqualTo(expense.getId().value());
        assertThat(dto.amount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(dto.description()).isEqualTo("Hotel");
        assertThat(dto.payerId()).isEqualTo(payerId.value());
    }

    @Test
    void shouldMapEqualExpenseToPersistenceDTOWithoutShares() {
        Participant.Id payerId = Participant.Id.generate();

        ExpenseEqual expense = ExpenseEqual.create(new BigDecimal("50.00"), "Dinner", payerId);

        ExpensePersistenceDTO dto = mapper.toPersistenceDTO(expense);

        assertThat(dto).isInstanceOf(ExpenseEqualPersistenceDTO.class);
        assertThat(dto.description()).isEqualTo("Dinner");
    }

    @Test
    void shouldPreserveByNightExpenseInRoundTrip() {
        Participant alice = Participant.create("Alice", 3);

        ExpenseByNight original = ExpenseByNight.create(new BigDecimal("100.00"), "Taxi", alice.id());

        ExpensePersistenceDTO dto = mapper.toPersistenceDTO(original);
        Expense roundTrip = mapper.toDomain(dto);

        assertThat(roundTrip).isInstanceOf(ExpenseByNight.class);
        assertThat(roundTrip.getAmount()).isEqualByComparingTo(original.getAmount());
        assertThat(roundTrip.getDescription()).isEqualTo(original.getDescription());
        assertThat(roundTrip.getPayerId()).isEqualTo(original.getPayerId());

    }

    @Test
    void shouldPreserveEqualExpenseInRoundTrip() {
        Participant alice = Participant.create("Alice", 2);
        List<Participant> participants = List.of(alice);

        ExpenseEqual original = ExpenseEqual.create(new BigDecimal("50.00"), "Parking", alice.id());

        ExpensePersistenceDTO dto = mapper.toPersistenceDTO(original);
        Expense roundTrip = mapper.toDomain(dto);

        assertThat(roundTrip).isInstanceOf(ExpenseEqual.class);
        assertThat(roundTrip.getAmount()).isEqualByComparingTo(original.getAmount());
        assertThat(roundTrip.getDescription()).isEqualTo(original.getDescription());

    }

}
