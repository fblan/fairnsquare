package org.asymetrik.web.fairnsquare.split.service;

import org.asymetrik.web.fairnsquare.sharedkernel.validation.Violations;

import java.math.BigDecimal;

import org.asymetrik.web.fairnsquare.split.domain.expenses.SplitMode;

/**
 * Request DTO for adding a new expense to a split.
 */
public record AddExpenseRequest(BigDecimal amount,

        String description,

        String payerId,

        SplitMode splitMode) implements Violations.Validated {

    @Override
    public void validate() {
        Violations.check().notNull(amount, "Amount is required").atLeast(amount, "0.01", "Amount must be at least 0.01")
                .notBlank(description, "Description is required")
                .maxLength(description, 200, "Description cannot exceed 200 characters")
                .notBlank(payerId, "Payer is required").notNull(splitMode, "Split mode is required").orThrow();
    }

    /**
     * Constructor with default split mode (BY_NIGHT).
     */
    public AddExpenseRequest(BigDecimal amount, String description, String payerId) {
        this(amount, description, payerId, SplitMode.BY_NIGHT);
    }
}
