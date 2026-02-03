package org.asymetrik.web.fairnsquare.split.persistence.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Persistence DTO for ExpenseByNight domain object. Shares are not persisted — they are recalculated from participant
 * nights on load.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExpenseByNightPersistenceDTO(String id, BigDecimal amount, String description, String payerId,
        String createdAt) implements ExpensePersistenceDTO {
}
