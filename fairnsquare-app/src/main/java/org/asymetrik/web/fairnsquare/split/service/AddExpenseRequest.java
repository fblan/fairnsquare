package org.asymetrik.web.fairnsquare.split.service;

import java.math.BigDecimal;

import org.asymetrik.web.fairnsquare.split.domain.expenses.SplitMode;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for adding a new expense to a split.
 */
public record AddExpenseRequest(
        @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be at least 0.01") BigDecimal amount,

        @NotBlank(message = "Description is required") @Size(max = 200, message = "Description cannot exceed 200 characters") String description,

        @NotBlank(message = "Payer is required") String payerId,

        @NotNull(message = "Split mode is required") SplitMode splitMode) {

    /**
     * Constructor with default split mode (BY_NIGHT).
     */
    public AddExpenseRequest(BigDecimal amount, String description, String payerId) {
        this(amount, description, payerId, SplitMode.BY_NIGHT);
    }
}
