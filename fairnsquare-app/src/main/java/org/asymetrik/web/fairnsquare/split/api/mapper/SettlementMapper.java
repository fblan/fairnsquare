package org.asymetrik.web.fairnsquare.split.api.mapper;

import org.asymetrik.web.fairnsquare.split.api.dto.SettlementResponseDTO;
import org.asymetrik.web.fairnsquare.split.domain.settlement.Settlement;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Mapper for converting Settlement domain objects to DTOs for REST API responses.
 */
@ApplicationScoped
public class SettlementMapper {

    /**
     * Converts a Settlement domain object to a SettlementResponseDTO.
     *
     * @param settlement
     *            the domain settlement
     *
     * @return the DTO representation
     *
     * @throws NullPointerException
     *             if settlement is null
     */
    public SettlementResponseDTO toDTO(Settlement settlement) {
        if (settlement == null) {
            throw new NullPointerException("Settlement cannot be null");
        }

        return new SettlementResponseDTO(
                settlement.balances().stream()
                        .map(b -> new SettlementResponseDTO.ParticipantBalanceDTO(b.participantId().value(),
                                b.participantName(), b.totalPaid(), b.totalCost(), b.balance()))
                        .toList(),
                settlement.reimbursements().stream()
                        .map(r -> new SettlementResponseDTO.ReimbursementDTO(r.fromId().value(), r.fromName(),
                                r.toId().value(), r.toName(), r.amount()))
                        .toList());
    }
}
