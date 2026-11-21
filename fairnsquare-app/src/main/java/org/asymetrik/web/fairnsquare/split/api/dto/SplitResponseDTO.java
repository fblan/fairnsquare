package org.asymetrik.web.fairnsquare.split.api.dto;

import java.util.List;

import org.asymetrik.web.fairnsquare.split.api.expense.dto.ExpenseDTO;

/**
 * DTO for Split REST API responses. Decouples domain model from API contract.
 */
public record SplitResponseDTO(String id, String name, String createdAt, List<ParticipantDTO> participants,
        List<ExpenseDTO> expenses, SettlementResponseDTO settlement) {
}
