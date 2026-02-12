package org.asymetrik.web.fairnsquare.split.service;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for adding a new expense to a split via type-specific endpoints. The split mode is determined by the
 * endpoint path, not the request body.
 * <p>
 * Usage example:
 * </p>
 *
 * <pre>
 * // POST /api/splits/{splitId}/expenses/by-night
 * {
 *   "amount": 150.00,
 *   "description": "Hotel",
 *   "payerId": "alice"
 * }
 *
 * // POST /api/splits/{splitId}/expenses/equal
 * {
 *   "amount": 50.00,
 *   "description": "Groceries",
 *   "payerId": "bob"
 * }
 * </pre>
 *
 * @param amount
 *            The expense amount (minimum 0.01)
 * @param description
 *            The expense description (max 200 characters)
 * @param payerId
 *            The ID of the participant who paid
 */
public record AddTypedExpenseRequest(
        @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be at least 0.01") BigDecimal amount,

        @NotBlank(message = "Description is required") @Size(max = 200, message = "Description cannot exceed 200 characters") String description,

        @NotBlank(message = "Payer is required") String payerId) {
}
