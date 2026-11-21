package org.asymetrik.web.fairnsquare.split.service;

import org.asymetrik.web.fairnsquare.sharedkernel.validation.Violations;

import java.math.BigDecimal;

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
public record AddTypedExpenseRequest(BigDecimal amount,

        String description,

        String payerId) implements Violations.Validated {

    @Override
    public void validate() {
        Violations.check().notNull(amount, "Amount is required").atLeast(amount, "0.01", "Amount must be at least 0.01")
                .notBlank(description, "Description is required")
                .maxLength(description, 200, "Description cannot exceed 200 characters")
                .notBlank(payerId, "Payer is required").orThrow();
    }

}
