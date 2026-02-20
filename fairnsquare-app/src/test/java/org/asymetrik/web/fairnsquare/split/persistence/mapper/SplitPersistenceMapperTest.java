package org.asymetrik.web.fairnsquare.split.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseByNight;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseEqual;
import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.asymetrik.web.fairnsquare.split.domain.settlement.SettlementCalculator;
import org.asymetrik.web.fairnsquare.split.domain.Split;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ExpenseByNightPersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ExpenseEqualPersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ParticipantPersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.SplitPersistenceDTO;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class SplitPersistenceMapperTest {

    @Inject
    SplitPersistenceMapper mapper;

    @Test
    void shouldMapDomainToPersistenceDTO() {
        Split split = Split.create("Weekend Trip");
        Participant alice = Participant.create("Alice", 3);
        split.addParticipant(alice);

        SplitPersistenceDTO dto = mapper.toPersistenceDTO(split);

        assertThat(dto.id()).isEqualTo(split.getId().value());
        assertThat(dto.name()).isEqualTo("Weekend Trip");
        assertThat(dto.createdAt()).isEqualTo(split.getCreatedAt().toString());
        assertThat(dto.participants()).hasSize(1);
        assertThat(dto.participants().getFirst().name()).isEqualTo("Alice");
        assertThat(dto.expenses()).isEmpty();
    }

    @Test
    void shouldMapPersistenceDTOToDomain() {
        SplitPersistenceDTO dto = new SplitPersistenceDTO(Split.Id.generate().value(), "Beach House",
                "2026-01-30T10:00:00Z",
                List.of(new ParticipantPersistenceDTO(Participant.Id.generate().value(), "Bob", 5, 1.0)), List.of(),
                null);

        Split split = mapper.toDomain(dto);

        assertThat(split.getId().value()).isEqualTo(dto.id());
        assertThat(split.getName().value()).isEqualTo("Beach House");
        assertThat(split.getParticipants()).hasSize(1);
        assertThat(split.getParticipants().getFirst().name().value()).isEqualTo("Bob");
        assertThat(split.getExpenses()).isEmpty();
    }

    @Test
    void shouldPreserveDataInRoundTrip() {
        Split original = Split.create("Round Trip Test");
        Participant alice = Participant.create("Alice", 3);
        Participant bob = Participant.create("Bob", 5);
        original.addParticipant(alice);
        original.addParticipant(bob);

        ExpenseByNight expense = ExpenseByNight.create(new BigDecimal("100.00"), "Hotel", alice.id());
        original.addExpense(expense);

        SplitPersistenceDTO dto = mapper.toPersistenceDTO(original);
        Split roundTrip = mapper.toDomain(dto);

        assertThat(roundTrip.getId()).isEqualTo(original.getId());
        assertThat(roundTrip.getName()).isEqualTo(original.getName());
        assertThat(roundTrip.getParticipants()).hasSize(2);
        assertThat(roundTrip.getExpenses()).hasSize(1);
        assertThat(roundTrip.getExpenses().getFirst()).isInstanceOf(ExpenseByNight.class);
        assertThat(roundTrip.getExpenses().getFirst().getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        // Shares recalculated from participants: Alice 3/8*100=37.50, Bob 5/8*100=62.50

    }

    @Test
    void shouldMapSplitWithBothExpenseTypes() {
        Split split = Split.create("Mixed Expenses");
        Participant alice = Participant.create("Alice", 2);
        split.addParticipant(alice);

        ExpenseByNight byNight = ExpenseByNight.create(new BigDecimal("80.00"), "Hotel", alice.id());
        ExpenseEqual equal = ExpenseEqual.create(new BigDecimal("40.00"), "Dinner", alice.id());
        split.addExpense(byNight);
        split.addExpense(equal);

        SplitPersistenceDTO dto = mapper.toPersistenceDTO(split);

        assertThat(dto.expenses()).hasSize(2);
        assertThat(dto.expenses().get(0)).isInstanceOf(ExpenseByNightPersistenceDTO.class);
        assertThat(dto.expenses().get(1)).isInstanceOf(ExpenseEqualPersistenceDTO.class);

        Split roundTrip = mapper.toDomain(dto);
        assertThat(roundTrip.getExpenses()).hasSize(2);
        assertThat(roundTrip.getExpenses().get(0)).isInstanceOf(ExpenseByNight.class);
        assertThat(roundTrip.getExpenses().get(1)).isInstanceOf(ExpenseEqual.class);
    }

    @Test
    void shouldMapEmptySplit() {
        Split split = Split.create("Empty");

        SplitPersistenceDTO dto = mapper.toPersistenceDTO(split);
        Split roundTrip = mapper.toDomain(dto);

        assertThat(roundTrip.getId()).isEqualTo(split.getId());
        assertThat(roundTrip.getName()).isEqualTo(split.getName());
        assertThat(roundTrip.getParticipants()).isEmpty();
        assertThat(roundTrip.getExpenses()).isEmpty();
    }

    @Test
    void shouldMapDTOWithNullCollections() {
        SplitPersistenceDTO dto = new SplitPersistenceDTO(Split.Id.generate().value(), "Null Collections",
                "2026-01-30T10:00:00Z", null, null, null);

        Split split = mapper.toDomain(dto);

        assertThat(split.getParticipants()).isEmpty();
        assertThat(split.getExpenses()).isEmpty();
    }

    @Test
    void shouldMapSettlementInRoundTrip() {
        Split original = Split.create("Settlement Round Trip");
        Participant alice = Participant.create("Alice", 3);
        Participant bob = Participant.create("Bob", 2);
        original.addParticipant(alice);
        original.addParticipant(bob);
        original.addExpense(ExpenseEqual.create(new BigDecimal("100.00"), "Hotel", alice.id()));
        original.settle(SettlementCalculator.calculate(original));

        SplitPersistenceDTO dto = mapper.toPersistenceDTO(original);
        Split roundTrip = mapper.toDomain(dto);

        assertThat(roundTrip.getSettlement()).isNotNull();
        assertThat(roundTrip.getSettlement().balances()).hasSize(2);
        assertThat(roundTrip.getSettlement().reimbursements()).hasSize(1);
        assertThat(roundTrip.getSettlement().balances().get(0).participantName()).isEqualTo("Alice");
        assertThat(roundTrip.getSettlement().reimbursements().get(0).fromName()).isEqualTo("Bob");
        assertThat(roundTrip.getSettlement().reimbursements().get(0).toName()).isEqualTo("Alice");
        assertThat(roundTrip.getSettlement().reimbursements().get(0).amount()).isEqualByComparingTo("50.00");
    }

    @Test
    void shouldMapNullSettlementInRoundTrip() {
        Split original = Split.create("No Settlement");
        original.addParticipant(Participant.create("Alice", 3));

        SplitPersistenceDTO dto = mapper.toPersistenceDTO(original);
        Split roundTrip = mapper.toDomain(dto);

        assertThat(roundTrip.getSettlement()).isNull();
    }
}
