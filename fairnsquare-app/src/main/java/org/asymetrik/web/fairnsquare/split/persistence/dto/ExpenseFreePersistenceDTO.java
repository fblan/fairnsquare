package org.asymetrik.web.fairnsquare.split.persistence.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Persistence DTO for ExpenseFree domain object. FREE mode stores share parts which are used to calculate amounts
 * proportionally.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExpenseFreePersistenceDTO(String id, BigDecimal amount, String description, String payerId,
        String createdAt, List<SharePersistenceDTO> shares) implements ExpensePersistenceDTO {

    /**
     * Nested DTO for persisting share parts information.
     */
    public record SharePersistenceDTO(String participantId, BigDecimal parts) {
    }
}
