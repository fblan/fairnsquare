package org.asymetrik.web.fairnsquare.split.service;

import org.asymetrik.web.fairnsquare.sharedkernel.validation.Violations;

import java.math.BigDecimal;
import java.util.List;

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
public record AddFreeExpenseRequest(BigDecimal amount,

        String description,

        String payerId,

        List<ShareRequest> shares) implements Violations.Validated {

    @Override
    public void validate() {
        Violations violations = Violations.check().notNull(amount, "Amount is required")
                .atLeast(amount, "0.01", "Amount must be at least 0.01")
                .notBlank(description, "Description is required")
                .maxLength(description, 200, "Description cannot exceed 200 characters")
                .notBlank(payerId, "Payer is required").notEmpty(shares, "Shares list cannot be empty");
        if (shares != null) {
            for (ShareRequest share : shares) {
                violations.when(share == null, "Share is required");
                if (share != null) {
                    violations.notBlank(share.participantId(), "Participant ID is required")
                            .notNull(share.parts(), "Share parts is required")
                            .atLeast(share.parts(), "0.00", "Share parts cannot be negative");
                }
            }
        }
        violations.orThrow();
    }

    /**
     * Represents manual share parts for one participant in a FREE mode expense.
     *
     * @param participantId
     *            The participant ID
     * @param parts
     *            The share parts (minimum 0.00, zero allowed)
     */
    public record ShareRequest(String participantId,

            BigDecimal parts) {
    }
}
