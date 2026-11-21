package org.asymetrik.web.fairnsquare.split.api.expense.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Polymorphic DTO for Expense REST API responses. Decouples domain model from API contract. Uses Jackson type
 * discriminator matching domain structure (BY_NIGHT, BY_SHARE, FREE).
 */
public sealed interface ExpenseDTO permits ExpenseByNightDTO, ExpenseByShareDTO, ExpenseFreeDTO {

    String id();

    String description();

    BigDecimal amount();

    String payerId();

    String type();

    String splitMode();

    String createdAt();

    List<ShareDTO> shares();
}
