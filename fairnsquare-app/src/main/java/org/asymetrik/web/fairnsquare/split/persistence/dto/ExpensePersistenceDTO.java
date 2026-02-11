package org.asymetrik.web.fairnsquare.split.persistence.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Sealed persistence DTO for the Expense hierarchy. Persists only the data needed to reconstruct the expense and
 * recalculate shares. Shares are derived data computed from expense amount + participant info on load.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", defaultImpl = ExpenseByNightPersistenceDTO.class)
@JsonSubTypes({ @JsonSubTypes.Type(value = ExpenseByNightPersistenceDTO.class, name = "BY_NIGHT"),
        @JsonSubTypes.Type(value = ExpenseEqualPersistenceDTO.class, name = "EQUAL"),
        @JsonSubTypes.Type(value = ExpenseFreePersistenceDTO.class, name = "FREE") })
public sealed interface ExpensePersistenceDTO
        permits ExpenseByNightPersistenceDTO, ExpenseEqualPersistenceDTO, ExpenseFreePersistenceDTO {

    String id();

    BigDecimal amount();

    String description();

    String payerId();

    String createdAt();
}
