package org.asymetrik.web.fairnsquare.split.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for Settlement REST API responses. Contains participant balances and reimbursement proposals.
 */
public record SettlementResponseDTO(List<ParticipantBalanceDTO> balances, List<ReimbursementDTO> reimbursements) {

    public record ParticipantBalanceDTO(String participantId, String participantName, BigDecimal totalPaid,
            BigDecimal totalCost, BigDecimal balance) {
    }

    public record ReimbursementDTO(String fromId, String fromName, String toId, String toName, BigDecimal amount) {
    }
}
