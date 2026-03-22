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

    /**
     * Persistence record for a reimbursement.
     * <p>
     * {@code fromType} and {@code toType} distinguish individual participants from shared-account groups:
     * <ul>
     * <li>{@code null} or {@code "participant"} → {@link SettlementPartyId.Individual} (backward compatible with files
     * written before shared accounts were introduced).</li>
     * <li>{@code "group"} → {@link SettlementPartyId.Group}.</li>
     * </ul>
     */
    public record ReimbursementPersistenceDTO(String fromId, String fromType, String toId, String toType,
            BigDecimal amount) {
    }
}
