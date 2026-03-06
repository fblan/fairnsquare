package org.asymetrik.web.fairnsquare.split.api.expense.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for expense share information in REST API responses. For FREE mode, parts is present; for BY_NIGHT/EQUAL, parts
 * is null.
 */
public record ShareDTO(@JsonProperty("participantId") String participantId, @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("parts") BigDecimal parts) {

    @JsonCreator
    public ShareDTO {
    }

    /**
     * Creates a ShareDTO with calculated amount (no parts).
     */
    public static ShareDTO withAmount(String participantId, BigDecimal amount) {
        return new ShareDTO(participantId, amount, null);
    }

    /**
     * Creates a ShareDTO with both amount and parts.
     */
    public static ShareDTO withAmountAndParts(String participantId, BigDecimal amount, BigDecimal parts) {
        return new ShareDTO(participantId, amount, parts);
    }
}
