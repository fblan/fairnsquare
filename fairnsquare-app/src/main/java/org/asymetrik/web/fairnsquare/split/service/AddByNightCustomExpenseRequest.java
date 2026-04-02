package org.asymetrik.web.fairnsquare.split.service;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for adding a BY_NIGHT_CUSTOM expense with a specific subset of participating participants.
 * <p>
 * Usage example:
 * </p>
 *
 * <pre>
 * // POST /api/splits/{splitId}/expenses/by-night-custom
 * {
 *   "amount": 150.00,
 *   "description": "Accommodation (van guests excluded)",
 *   "payerId": "alice",
 *   "participantIds": ["alice", "bob"]
 * }
 * // Shares calculated proportionally by nights × share weight for the listed participants only
 * </pre>
 *
 * @param amount
 *            The expense amount (minimum 0.01)
 * @param description
 *            The expense description (max 200 characters)
 * @param payerId
 *            The ID of the participant who paid
 * @param participantIds
 *            IDs of the participants who actually participate in this expense
 */
public record AddByNightCustomExpenseRequest(
        @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be at least 0.01") BigDecimal amount,

        @NotBlank(message = "Description is required") @Size(max = 200, message = "Description cannot exceed 200 characters") String description,

        @NotBlank(message = "Payer is required") String payerId,

        @NotEmpty(message = "Participant IDs list cannot be empty") List<@NotBlank(message = "Participant ID cannot be blank") String> participantIds) {
}
