package org.asymetrik.web.fairnsquare.expense.api.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for FREE expense type. Represents expenses with manually specified shares per participant.
 */
public record ExpenseFreeDTO(@JsonProperty("id") String id, @JsonProperty("description") String description,
        @JsonProperty("amount") BigDecimal amount, @JsonProperty("payerId") String payerId,
        @JsonProperty("type") String type, @JsonProperty("splitMode") String splitMode,
        @JsonProperty("createdAt") String createdAt, @JsonProperty("shares") List<ShareDTO> shares)
        implements ExpenseDTO {

    @JsonCreator
    public ExpenseFreeDTO {
    }
}
