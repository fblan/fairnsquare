package org.asymetrik.web.fairnsquare.split.api.mapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.asymetrik.web.fairnsquare.split.api.dto.SettlementResponseDTO;
import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.asymetrik.web.fairnsquare.split.domain.settlement.Settlement;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Mapper for converting Settlement domain objects to DTOs for REST API responses.
 */
@ApplicationScoped
public class SettlementMapper {

    /**
     * Converts a Settlement domain object to a SettlementResponseDTO, resolving participant names from the provided
     * participant list.
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

        Map<Participant.Id, String> names = participants.stream()
                .collect(Collectors.toMap(Participant::id, p -> p.name().value()));

        return new SettlementResponseDTO(
                settlement.balances().stream()
                        .map(b -> new SettlementResponseDTO.ParticipantBalanceDTO(b.participantId().value(),
                                names.getOrDefault(b.participantId(), ""), b.totalPaid(), b.totalCost(), b.balance()))
                        .toList(),
                settlement.reimbursements().stream()
                        .map(r -> new SettlementResponseDTO.ReimbursementDTO(r.fromId().value(),
                                names.getOrDefault(r.fromId(), ""), r.toId().value(), names.getOrDefault(r.toId(), ""),
                                r.amount()))
                        .toList());
    }
}
