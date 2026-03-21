package org.asymetrik.web.fairnsquare.split.api.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for Participant REST API responses. Decouples domain model from API contract.
 * <p>
 * The fields {@code totalPaid}, {@code totalCost}, and {@code balance} are computed values, never persisted.
 */
public record ParticipantDTO(@JsonProperty("id") String id, @JsonProperty("name") String name,
        @JsonProperty("nights") double nights, @JsonProperty("share") double share,
        @JsonProperty("preferredCreditorId") String preferredCreditorId,
        @JsonProperty("totalPaid") BigDecimal totalPaid, @JsonProperty("totalCost") BigDecimal totalCost,
        @JsonProperty("balance") BigDecimal balance) {

    @JsonCreator
    public ParticipantDTO {
    }
}
