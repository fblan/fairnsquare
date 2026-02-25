package org.asymetrik.web.fairnsquare.split.persistence.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Persistence DTO for Settlement. Mirrors the JSON storage format with primitive types.
 */
public record SettlementPersistenceDTO(List<ParticipantBalancePersistenceDTO> balances,
        List<ReimbursementPersistenceDTO> reimbursements) {

    public record ParticipantBalancePersistenceDTO(String participantId, BigDecimal totalPaid, BigDecimal totalCost,
            BigDecimal balance) {
    }

    public record ReimbursementPersistenceDTO(String fromId, String toId, BigDecimal amount) {
    }
}
