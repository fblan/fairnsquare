package org.asymetrik.web.fairnsquare.split.persistence.mapper;

import java.time.Instant;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.split.domain.Split;
import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.asymetrik.web.fairnsquare.split.domain.settlement.ParticipantBalance;
import org.asymetrik.web.fairnsquare.split.domain.settlement.Reimbursement;
import org.asymetrik.web.fairnsquare.split.domain.settlement.Settlement;
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
                participants, expenses, settlementDTO);
    }

    public Split toDomain(SplitPersistenceDTO dto) {
        Split split = new Split(Split.Id.of(dto.id()), new Split.Name(dto.name()), Instant.parse(dto.createdAt()));

        if (dto.participants() != null) {
            dto.participants().forEach(p -> split.addParticipant(participantMapper.toDomain(p)));
        }

        if (dto.expenses() != null) {
            dto.expenses().forEach(e -> split.addExpense(expenseMapper.toDomain(e)));
        }

        if (dto.settlement() != null) {
            split.settle(settlementToDomain(dto.settlement()));
        }

        return split;
    }

    private SettlementPersistenceDTO settlementToPersistenceDTO(Settlement settlement) {
        if (settlement == null) {
            return null;
        }
        List<SettlementPersistenceDTO.ParticipantBalancePersistenceDTO> balances = settlement.balances().stream()
                .map(b -> new SettlementPersistenceDTO.ParticipantBalancePersistenceDTO(b.participantId().value(),
                        b.participantName(), b.totalPaid(), b.totalCost(), b.balance()))
                .toList();
        List<SettlementPersistenceDTO.ReimbursementPersistenceDTO> reimbursements = settlement.reimbursements().stream()
                .map(r -> new SettlementPersistenceDTO.ReimbursementPersistenceDTO(r.fromId().value(), r.fromName(),
                        r.toId().value(), r.toName(), r.amount()))
                .toList();
        return new SettlementPersistenceDTO(balances, reimbursements);
    }

    private Settlement settlementToDomain(SettlementPersistenceDTO dto) {
        List<ParticipantBalance> balances = dto.balances().stream()
                .map(b -> new ParticipantBalance(Participant.Id.of(b.participantId()), b.participantName(),
                        b.totalPaid(), b.totalCost(), b.balance()))
                .toList();
        List<Reimbursement> reimbursements = dto.reimbursements().stream()
                .map(r -> new Reimbursement(Participant.Id.of(r.fromId()), r.fromName(), Participant.Id.of(r.toId()),
                        r.toName(), r.amount()))
                .toList();
        return new Settlement(balances, reimbursements);
    }
}
