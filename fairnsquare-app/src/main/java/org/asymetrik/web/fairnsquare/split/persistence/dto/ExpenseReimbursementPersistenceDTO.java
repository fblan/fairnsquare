package org.asymetrik.web.fairnsquare.split.persistence.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Persistence DTO for ExpenseReimbursement: the beneficiary is stored, the share is computed on load.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExpenseReimbursementPersistenceDTO(String id, BigDecimal amount, String description, String payerId,
        String createdAt, String beneficiaryId) implements ExpensePersistenceDTO {
}
