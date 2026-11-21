package org.asymetrik.web.fairnsquare.split.api.expense.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for FREE expense type. Represents expenses with manually specified shares per participant.
 */
public record ExpenseFreeDTO(String id, String description, BigDecimal amount, String payerId, String type,
        String splitMode, String createdAt, List<ShareDTO> shares) implements ExpenseDTO {
}
