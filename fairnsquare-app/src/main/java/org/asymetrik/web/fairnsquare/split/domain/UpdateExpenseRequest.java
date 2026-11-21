package org.asymetrik.web.fairnsquare.split.domain;

import org.asymetrik.web.fairnsquare.sharedkernel.validation.Violations;

import java.math.BigDecimal;

import org.asymetrik.web.fairnsquare.split.domain.expenses.SplitMode;

/**
 * Request DTO for updating an existing expense in a split.
 */
public record UpdateExpenseRequest(BigDecimal amount,

        String description,

        String payerId,

        SplitMode splitMode) implements Violations.Validated {

    @Override
    public void validate() {
        Violations.check().notNull(amount, "Amount is required").atLeast(amount, "0.01", "Amount must be at least 0.01")
                .maxLength(description, 200, "Description cannot exceed 200 characters")
                .notBlank(payerId, "Payer is required").notNull(splitMode, "Split mode is required").orThrow();
    }

}
