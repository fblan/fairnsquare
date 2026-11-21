package org.asymetrik.web.fairnsquare.split.api.expense.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for BY_NIGHT expense type. Represents expenses split proportionally by nights stayed.
 */
public record ExpenseByNightDTO(String id, String description, BigDecimal amount, String payerId, String type,
        String splitMode, String createdAt, List<ShareDTO> shares) implements ExpenseDTO {
}
