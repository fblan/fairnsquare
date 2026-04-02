package org.asymetrik.web.fairnsquare.split.persistence.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Persistence DTO for ExpenseByNightCustom domain object. Stores the participant IDs for the custom subset.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExpenseByNightCustomPersistenceDTO(String id, BigDecimal amount, String description, String payerId,
        String createdAt, List<String> participantIds) implements ExpensePersistenceDTO {
}
