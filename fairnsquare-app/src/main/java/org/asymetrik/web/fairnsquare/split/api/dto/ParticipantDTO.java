package org.asymetrik.web.fairnsquare.split.api.dto;

import java.math.BigDecimal;

/**
 * DTO for Participant REST API responses. Decouples domain model from API contract.
 * <p>
 * The fields {@code totalPaid}, {@code totalCost}, and {@code balance} are computed values, never persisted.
 */
public record ParticipantDTO(String id, String name, double nights, double share, String sharedAccountId,
        BigDecimal totalPaid, BigDecimal totalCost, BigDecimal balance) {
}
