package org.asymetrik.web.fairnsquare.split.persistence.dto;

import java.util.List;

/**
 * Persistence DTO for Split aggregate root. Mirrors the JSON storage format exactly: value objects are stored as
 * primitives, createdAt as ISO-8601 string.
 */
public record SplitPersistenceDTO(String id, String name, String createdAt,
        List<ParticipantPersistenceDTO> participants, List<ExpensePersistenceDTO> expenses) {
}
