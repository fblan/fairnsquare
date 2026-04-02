package org.asymetrik.web.fairnsquare.split.service;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for adding a BY_NIGHT_CUSTOM mode expense with a specified subset of participants.
 *
 * @param amount
 *            The expense amount (minimum 0.01)
 * @param description
 *            The expense description (max 200 characters)
 * @param payerId
 *            The ID of the participant who paid
 * @param participantIds
 *            The IDs of participants included in the split (must not be null or empty)
 */
public record AddByNightCustomExpenseRequest(
        @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be at least 0.01") BigDecimal amount,

        @Size(max = 200, message = "Description cannot exceed 200 characters") String description,

        @NotBlank(message = "Payer is required") String payerId,

        @NotNull(message = "Participant IDs are required") @NotEmpty(message = "Participant IDs cannot be empty") List<String> participantIds) {
}
