package org.asymetrik.web.fairnsquare.split.api.expense.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for BY_NIGHT_CUSTOM expense type. Represents expenses split proportionally by nights stayed for a subset of
 * participants.
 */
public record ExpenseByNightCustomDTO(@JsonProperty("id") String id, @JsonProperty("description") String description,
        @JsonProperty("amount") BigDecimal amount, @JsonProperty("payerId") String payerId,
        @JsonProperty("type") String type, @JsonProperty("splitMode") String splitMode,
        @JsonProperty("createdAt") String createdAt, @JsonProperty("shares") List<ShareDTO> shares,
        @JsonProperty("participantIds") List<String> participantIds) implements ExpenseDTO {

    @JsonCreator
    public ExpenseByNightCustomDTO {
    }
}
