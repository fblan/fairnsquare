package org.asymetrik.web.fairnsquare.split.api.expense.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for BY_SHARE expense type. Represents expenses split proportionally by participant share.
 */
public record ExpenseByShareDTO(String id, String description, BigDecimal amount, String payerId, String type,
        String splitMode, String createdAt, List<ShareDTO> shares) implements ExpenseDTO {
}
