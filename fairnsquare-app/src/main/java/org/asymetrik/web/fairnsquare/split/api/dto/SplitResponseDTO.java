package org.asymetrik.web.fairnsquare.split.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.asymetrik.web.fairnsquare.expense.api.dto.ExpenseDTO;

/**
 * DTO for Split REST API responses. Decouples domain model from API contract.
 */
public record SplitResponseDTO(@JsonProperty("id") String id, @JsonProperty("name") String name,
        @JsonProperty("createdAt") String createdAt, @JsonProperty("participants") List<ParticipantDTO> participants,
        @JsonProperty("expenses") List<ExpenseDTO> expenses,
        @JsonProperty("settlement") SettlementResponseDTO settlement) {

    @JsonCreator
    public SplitResponseDTO {
    }
}
