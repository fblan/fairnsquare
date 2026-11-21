package org.asymetrik.web.fairnsquare.split.persistence.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Persistence DTO for ExpenseFree domain object. FREE mode stores share parts which are used to calculate amounts
 * proportionally.
 */
public record ExpenseFreePersistenceDTO(String id, BigDecimal amount, String description, String payerId,
        String createdAt, List<SharePersistenceDTO> shares) implements ExpensePersistenceDTO {

    /**
     * Nested DTO for persisting share parts information.
     */
    public record SharePersistenceDTO(String participantId, BigDecimal parts) {
    }
}
