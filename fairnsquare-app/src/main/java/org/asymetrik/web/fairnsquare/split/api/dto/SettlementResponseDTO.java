package org.asymetrik.web.fairnsquare.split.api.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for Settlement REST API responses. Contains participant balances and reimbursement proposals.
 */
public record SettlementResponseDTO(@JsonProperty("balances") List<ParticipantBalanceDTO> balances,
        @JsonProperty("reimbursements") List<ReimbursementDTO> reimbursements) {

    @JsonCreator
    public SettlementResponseDTO {
    }

    public record ParticipantBalanceDTO(@JsonProperty("participantId") String participantId,
            @JsonProperty("participantName") String participantName, @JsonProperty("totalPaid") BigDecimal totalPaid,
            @JsonProperty("totalCost") BigDecimal totalCost, @JsonProperty("balance") BigDecimal balance) {

        @JsonCreator
        public ParticipantBalanceDTO {
        }
    }

    public record ReimbursementDTO(@JsonProperty("fromId") String fromId, @JsonProperty("fromName") String fromName,
            @JsonProperty("toId") String toId, @JsonProperty("toName") String toName,
            @JsonProperty("amount") BigDecimal amount) {

        @JsonCreator
        public ReimbursementDTO {
        }
    }
}
