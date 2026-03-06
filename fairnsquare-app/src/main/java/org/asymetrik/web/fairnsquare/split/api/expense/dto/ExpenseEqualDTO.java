package org.asymetrik.web.fairnsquare.split.api.expense.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for EQUAL expense type. Represents expenses split equally among participants.
 */
public record ExpenseEqualDTO(@JsonProperty("id") String id, @JsonProperty("description") String description,
        @JsonProperty("amount") BigDecimal amount, @JsonProperty("payerId") String payerId,
        @JsonProperty("type") String type, @JsonProperty("splitMode") String splitMode,
        @JsonProperty("createdAt") String createdAt, @JsonProperty("shares") List<ShareDTO> shares)
        implements ExpenseDTO {

    @JsonCreator
    public ExpenseEqualDTO {
    }
}
