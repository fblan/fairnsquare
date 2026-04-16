package org.asymetrik.web.fairnsquare.split.persistence.mapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.split.domain.Split;
import org.asymetrik.web.fairnsquare.split.domain.expenses.Expense;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseFree;
import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.asymetrik.web.fairnsquare.split.domain.settlement.ParticipantBalance;
import org.asymetrik.web.fairnsquare.split.domain.settlement.Reimbursement;
import org.asymetrik.web.fairnsquare.split.domain.settlement.Settlement;
import org.asymetrik.web.fairnsquare.split.domain.settlement.SettlementPartyId;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ExpenseEqualPersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ExpensePersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ParticipantPersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.SettlementPersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.SplitPersistenceDTO;

/**
 * Bidirectional mapper between Split aggregate root and SplitPersistenceDTO. Delegates to ParticipantPersistenceMapper
 * and ExpensePersistenceMapper for nested entities.
 */
@ApplicationScoped
public class SplitPersistenceMapper {

    private final ParticipantPersistenceMapper participantMapper;
    private final ExpensePersistenceMapper expenseMapper;

    @Inject
    public SplitPersistenceMapper(ParticipantPersistenceMapper participantMapper,
            ExpensePersistenceMapper expenseMapper) {
        this.participantMapper = participantMapper;
        this.expenseMapper = expenseMapper;
    }

    public SplitPersistenceDTO toPersistenceDTO(Split split) {
        List<ParticipantPersistenceDTO> participants = split.getParticipants().stream()
                .map(participantMapper::toPersistenceDTO).toList();

        List<ExpensePersistenceDTO> expenses = split.getExpenses().stream().map(expenseMapper::toPersistenceDTO)
                .toList();

        SettlementPersistenceDTO settlementDTO = settlementToPersistenceDTO(split.getSettlement());

        return new SplitPersistenceDTO(split.getId().value(), split.getName().value(), split.getCreatedAt().toString(),
                split.getUpdatedAt().toString(), participants, expenses, settlementDTO);
    }

    public Split toDomain(SplitPersistenceDTO dto) {
        Instant createdAt = Instant.parse(dto.createdAt());
        Instant updatedAt = dto.updatedAt() != null ? Instant.parse(dto.updatedAt()) : createdAt;
        Split split = new Split(Split.Id.of(dto.id()), new Split.Name(dto.name()), createdAt, updatedAt);

        if (dto.participants() != null) {
            dto.participants().forEach(p -> split.addParticipant(participantMapper.toDomain(p)));
        }

        if (dto.expenses() != null) {
            List<Participant> participants = split.getParticipants();
            dto.expenses().forEach(e -> split.addExpense(toExpenseDomain(e, participants)));
        }

        if (dto.settlement() != null) {
            split.settle(settlementToDomain(dto.settlement()));
        }

        // Restore the persisted updatedAt — addParticipant/addExpense/settle call touch() during loading
        split.restoreUpdatedAt(updatedAt);

        return split;
    }

    /**
     * Converts a persistence DTO to a domain Expense, applying legacy migrations.
     * <p>
     * EQUAL expenses (no longer creatable from the UI) are transparently converted to {@link ExpenseFree} with
     * {@code parts = 1} for every participant, preserving identical split semantics.
     */
    private Expense toExpenseDomain(ExpensePersistenceDTO dto, List<Participant> participants) {
        if (dto instanceof ExpenseEqualPersistenceDTO equal) {
            Expense.Id id = equal.id() != null ? Expense.Id.of(equal.id()) : null;
            Participant.Id payerId = equal.payerId() != null ? Participant.Id.of(equal.payerId()) : null;
            Instant createdAt = equal.createdAt() != null ? Instant.parse(equal.createdAt()) : null;
            List<Expense.Share> shares = participants.stream().map(p -> Expense.Share.withParts(p.id(), BigDecimal.ONE))
                    .toList();
            return ExpenseFree.fromJson(id, equal.amount(), equal.description(), payerId, shares, createdAt);
        }
        return expenseMapper.toDomain(dto);
    }

    private SettlementPersistenceDTO settlementToPersistenceDTO(Settlement settlement) {
        if (settlement == null) {
            return null;
        }
        List<SettlementPersistenceDTO.ParticipantBalancePersistenceDTO> balances = settlement.balances().stream()
                .map(b -> new SettlementPersistenceDTO.ParticipantBalancePersistenceDTO(b.participantId().value(),
                        b.totalPaid(), b.totalCost(), b.balance()))
                .toList();
        List<SettlementPersistenceDTO.ReimbursementPersistenceDTO> reimbursements = settlement.reimbursements().stream()
                .map(r -> new SettlementPersistenceDTO.ReimbursementPersistenceDTO(r.from().value(),
                        partyType(r.from()), r.to().value(), partyType(r.to()), r.amount()))
                .toList();
        return new SettlementPersistenceDTO(balances, reimbursements);
    }

    private Settlement settlementToDomain(SettlementPersistenceDTO dto) {
        List<ParticipantBalance> balances = dto.balances().stream()
                .map(b -> new ParticipantBalance(Participant.Id.of(b.participantId()), b.totalPaid(), b.totalCost(),
                        b.balance()))
                .toList();
        List<Reimbursement> reimbursements = dto.reimbursements().stream()
                .map(r -> new Reimbursement(toPartyId(r.fromId(), r.fromType()), toPartyId(r.toId(), r.toType()),
                        r.amount()))
                .toList();
        return new Settlement(balances, reimbursements);
    }

    /**
     * Returns the persistence type tag for a {@link SettlementPartyId}. {@code null} is used for individuals to keep
     * files written by older versions readable (Jackson ignores absent fields and treats them as {@code null}).
     */
    private static String partyType(SettlementPartyId partyId) {
        return switch (partyId) {
            case SettlementPartyId.Individual ignored -> null;
            case SettlementPartyId.Group ignored -> "group";
        };
    }

    /**
     * Reconstructs a {@link SettlementPartyId} from persisted values. Absent or {@code "participant"} type tags map to
     * {@link SettlementPartyId.Individual} for backward compatibility with pre-shared-account files.
     */
    private static SettlementPartyId toPartyId(String id, String type) {
        if ("group".equals(type)) {
            return new SettlementPartyId.Group(Participant.SharedAccountId.of(id));
        }
        return new SettlementPartyId.Individual(Participant.Id.of(id));
    }
}
