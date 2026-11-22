package org.asymetrik.web.fairnsquare.split.api.expense.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for REIMBURSEMENT expense type: paid by one participant for one beneficiary, who bears the whole amount.
 */
public record ExpenseReimbursementDTO(@JsonProperty("id") String id, @JsonProperty("description") String description,
        @JsonProperty("amount") BigDecimal amount, @JsonProperty("payerId") String payerId,
        @JsonProperty("type") String type, @JsonProperty("splitMode") String splitMode,
        @JsonProperty("createdAt") String createdAt, @JsonProperty("shares") List<ShareDTO> shares,
        @JsonProperty("beneficiaryId") String beneficiaryId) implements ExpenseDTO {

    @JsonCreator
    public ExpenseReimbursementDTO {
    }
}
