package org.asymetrik.web.fairnsquare.split.domain;

import java.math.BigDecimal;

import org.asymetrik.web.fairnsquare.split.domain.expenses.SplitMode;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing expense in a split.
 */
public record UpdateExpenseRequest(
        @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be at least 0.01") BigDecimal amount,

        @Size(max = 200, message = "Description cannot exceed 200 characters") String description,

        @NotBlank(message = "Payer is required") String payerId,

        @NotNull(message = "Split mode is required") SplitMode splitMode) {
}
