package org.asymetrik.web.fairnsquare.split.persistence.dto;

import java.math.BigDecimal;

/**
 * Persistence DTO for ExpenseByNight domain object. Shares are not persisted — they are recalculated from participant
 * nights on load.
 */
public record ExpenseByNightPersistenceDTO(String id, BigDecimal amount, String description, String payerId,
        String createdAt) implements ExpensePersistenceDTO {
}
