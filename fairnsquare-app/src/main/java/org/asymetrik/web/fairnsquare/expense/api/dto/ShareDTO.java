package org.asymetrik.web.fairnsquare.expense.api.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for expense share information in REST API responses.
 */
public record ShareDTO(@JsonProperty("participantId") String participantId, @JsonProperty("amount") BigDecimal amount) {

    @JsonCreator
    public ShareDTO {
    }
}
