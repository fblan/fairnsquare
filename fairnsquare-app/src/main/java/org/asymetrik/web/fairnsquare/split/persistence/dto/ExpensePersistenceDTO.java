package org.asymetrik.web.fairnsquare.split.persistence.dto;

import java.math.BigDecimal;

/**
 * Sealed persistence DTO for the Expense hierarchy. Persists only the data needed to reconstruct the expense and
 * recalculate shares. Shares are derived data computed from expense amount + participant info on load.
 */
public sealed interface ExpensePersistenceDTO permits ExpenseByNightPersistenceDTO, ExpenseBySharePersistenceDTO,
        ExpenseEqualPersistenceDTO, ExpenseFreePersistenceDTO {

    String id();

    BigDecimal amount();

    String description();

    String payerId();

    String createdAt();
}
