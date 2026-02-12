package org.asymetrik.web.fairnsquare.split.service;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for adding a FREE mode expense with manually specified share parts.
 * <p>
 * Usage example:
 * </p>
 *
 * <pre>
 * // POST /api/splits/{splitId}/expenses/free
 * {
 *   "amount": 100.00,
 *   "description": "Dinner",
 *   "payerId": "alice",
 *   "shares": [
 *     { "participantId": "alice", "parts": 2 },
 *     { "participantId": "bob", "parts": 3 }
 *   ]
 * }
 * // Amounts calculated: alice = 100 × (2/5) = 40.00, bob = 100 × (3/5) = 60.00
 * </pre>
 *
 * @param amount
 *            The expense amount (minimum 0.01)
 * @param description
 *            The expense description (max 200 characters)
 * @param payerId
 *            The ID of the participant who paid
 * @param shares
 *            List of manual share parts per participant (amounts calculated proportionally)
 */
public record AddFreeExpenseRequest(
        @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be at least 0.01") BigDecimal amount,

        @NotBlank(message = "Description is required") @Size(max = 200, message = "Description cannot exceed 200 characters") String description,

        @NotBlank(message = "Payer is required") String payerId,

        @NotEmpty(message = "Shares list cannot be empty") List<@Valid ShareRequest> shares) {

    /**
     * Represents manual share parts for one participant in a FREE mode expense.
     *
     * @param participantId
     *            The participant ID
     * @param parts
     *            The share parts (minimum 0.00, zero allowed)
     */
    public record ShareRequest(@NotBlank(message = "Participant ID is required") String participantId,

            @NotNull(message = "Share parts is required") @DecimalMin(value = "0.00", inclusive = true, message = "Share parts cannot be negative") BigDecimal parts) {
    }
}
