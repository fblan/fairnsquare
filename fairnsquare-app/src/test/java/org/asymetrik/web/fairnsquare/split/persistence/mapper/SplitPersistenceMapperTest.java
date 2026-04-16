package org.asymetrik.web.fairnsquare.split.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.split.domain.expenses.Expense;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseByNight;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseFree;
import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.asymetrik.web.fairnsquare.split.domain.settlement.SettlementCalculator;
import org.asymetrik.web.fairnsquare.split.domain.settlement.SettlementPartyId;
import org.asymetrik.web.fairnsquare.split.domain.Split;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ExpenseByNightPersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ExpenseEqualPersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ExpenseFreePersistenceDTO;
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
                "2026-01-30T10:00:00Z", null,
                List.of(new ParticipantPersistenceDTO(Participant.Id.generate().value(), "Bob", 5, 1.0, null)),
                List.of(), null);

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
        ExpenseFree free = ExpenseFree.create(new BigDecimal("40.00"), "Dinner", alice.id(),
                List.of(Expense.Share.withParts(alice.id(), BigDecimal.ONE)));
        split.addExpense(byNight);
        split.addExpense(free);

        SplitPersistenceDTO dto = mapper.toPersistenceDTO(split);

        assertThat(dto.expenses()).hasSize(2);
        assertThat(dto.expenses().get(0)).isInstanceOf(ExpenseByNightPersistenceDTO.class);
        assertThat(dto.expenses().get(1)).isInstanceOf(ExpenseFreePersistenceDTO.class);

        Split roundTrip = mapper.toDomain(dto);
        assertThat(roundTrip.getExpenses()).hasSize(2);
        assertThat(roundTrip.getExpenses().get(0)).isInstanceOf(ExpenseByNight.class);
        assertThat(roundTrip.getExpenses().get(1)).isInstanceOf(ExpenseFree.class);
    }

    @Test
    void shouldConvertLegacyEqualPersistenceDTOToFreeOnLoad() {
        // Simulate loading a legacy EQUAL DTO (e.g., from an old ZIP file)
        Participant alice = Participant.create("Alice", 2);
        Participant bob = Participant.create("Bob", 3);
        String aliceParticipantId = alice.id().value();
        ExpenseEqualPersistenceDTO equalDTO = new ExpenseEqualPersistenceDTO(
                org.asymetrik.web.fairnsquare.split.domain.expenses.Expense.Id.generate().value(),
                new BigDecimal("40.00"), "Taxi", aliceParticipantId, "2026-01-01T10:00:00Z");
        SplitPersistenceDTO dto = new SplitPersistenceDTO(Split.Id.generate().value(), "Legacy Split",
                "2026-01-01T10:00:00Z", null,
                List.of(new ParticipantPersistenceDTO(alice.id().value(), "Alice", 2, 1.0, null),
                        new ParticipantPersistenceDTO(bob.id().value(), "Bob", 3, 1.0, null)),
                List.of(equalDTO), null);

        Split roundTrip = mapper.toDomain(dto);

        assertThat(roundTrip.getExpenses()).hasSize(1);
        assertThat(roundTrip.getExpenses().get(0)).isInstanceOf(ExpenseFree.class);
        ExpenseFree converted = (ExpenseFree) roundTrip.getExpenses().get(0);
        assertThat(converted.getAmount()).isEqualByComparingTo("40.00");
        assertThat(converted.getSharesWithParts()).hasSize(2)
                .allSatisfy(share -> assertThat(share.parts()).isEqualByComparingTo("1"));
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
                "2026-01-30T10:00:00Z", null, null, null, null);

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
        original.addExpense(ExpenseFree.create(new BigDecimal("100.00"), "Hotel", alice.id(),
                List.of(Expense.Share.withParts(alice.id(), BigDecimal.ONE),
                        Expense.Share.withParts(bob.id(), BigDecimal.ONE))));
        original.settle(SettlementCalculator.calculate(original));

        SplitPersistenceDTO dto = mapper.toPersistenceDTO(original);
        Split roundTrip = mapper.toDomain(dto);

        assertThat(roundTrip.getSettlement()).isNotNull();
        assertThat(roundTrip.getSettlement().balances()).hasSize(2);
        assertThat(roundTrip.getSettlement().reimbursements()).hasSize(1);
        assertThat(roundTrip.getSettlement().balances().get(0).participantId()).isEqualTo(alice.id());
        assertThat(roundTrip.getSettlement().reimbursements().get(0).from())
                .isEqualTo(new SettlementPartyId.Individual(bob.id()));
        assertThat(roundTrip.getSettlement().reimbursements().get(0).to())
                .isEqualTo(new SettlementPartyId.Individual(alice.id()));
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
