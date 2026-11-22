package org.asymetrik.web.fairnsquare.split.service;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for adding a REIMBURSEMENT expense: paid by one participant for one beneficiary.
 *
 * <pre>
 * // POST /api/splits/{splitId}/expenses/reimbursement
 * { "amount": 40.00, "description": "Train ticket", "payerId": "alice", "beneficiaryId": "bob" }
 * </pre>
 */
public record AddReimbursementRequest(
        @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be at least 0.01") BigDecimal amount,

        @NotBlank(message = "Description is required") @Size(max = 200, message = "Description cannot exceed 200 characters") String description,

        @NotBlank(message = "Payer is required") String payerId,

        @NotBlank(message = "Beneficiary is required") String beneficiaryId) {
}
