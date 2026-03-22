package org.asymetrik.web.fairnsquare.split.api.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.asymetrik.web.fairnsquare.split.api.dto.SettlementResponseDTO;
import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.asymetrik.web.fairnsquare.split.domain.settlement.Settlement;
import org.asymetrik.web.fairnsquare.split.domain.settlement.SettlementParticipant;
import org.asymetrik.web.fairnsquare.split.domain.settlement.SettlementParticipantMapper;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Mapper for converting Settlement domain objects to DTOs for REST API responses.
 */
@ApplicationScoped
public class SettlementMapper {

    /**
     * Converts a Settlement domain object to a SettlementResponseDTO, resolving participant and group names from the
     * provided participant list.
     *
     * @param settlement
     *            the domain settlement
     * @param participants
     *            the participants of the split, used to resolve names by ID
     *
     * @return the DTO representation
     *
     * @throws NullPointerException
     *             if settlement is null
     */
    public SettlementResponseDTO toDTO(Settlement settlement, List<Participant> participants) {
        if (settlement == null) {
            throw new NullPointerException("Settlement cannot be null");
        }

        // Individual participant names (for balance display and legacy reimbursements)
        Map<Participant.Id, String> individualNames = new HashMap<>();
        for (Participant p : participants) {
            individualNames.put(p.id(), p.name().value());
        }

        // Combined name lookup: individual IDs + shared-account group IDs → display name
        Map<String, String> nameById = new HashMap<>();
        for (Participant p : participants) {
            nameById.put(p.id().value(), p.name().value());
        }
        for (SettlementParticipant sp : SettlementParticipantMapper.from(participants)) {
            if (sp instanceof SettlementParticipant.SharedAccount sa) {
                nameById.put(sa.id().value(), sa.name());
            }
        }

        return new SettlementResponseDTO(
                settlement.balances().stream()
                        .map(b -> new SettlementResponseDTO.ParticipantBalanceDTO(b.participantId().value(),
                                individualNames.getOrDefault(b.participantId(), ""), b.totalPaid(), b.totalCost(),
                                b.balance()))
                        .toList(),
                settlement.reimbursements().stream()
                        .map(r -> new SettlementResponseDTO.ReimbursementDTO(r.from().value(),
                                nameById.getOrDefault(r.from().value(), ""), r.to().value(),
                                nameById.getOrDefault(r.to().value(), ""), r.amount()))
                        .toList());
    }
}
