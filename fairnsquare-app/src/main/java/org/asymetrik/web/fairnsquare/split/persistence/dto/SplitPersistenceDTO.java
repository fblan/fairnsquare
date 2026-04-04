package org.asymetrik.web.fairnsquare.split.persistence.dto;

import java.util.List;

/**
 * Persistence DTO for Split aggregate root. Mirrors the JSON storage format exactly: value objects are stored as
 * primitives, createdAt/updatedAt as ISO-8601 strings. updatedAt may be null in older persisted files (defaults to
 * createdAt on load).
 */
public record SplitPersistenceDTO(String id, String name, String createdAt, String updatedAt,
        List<ParticipantPersistenceDTO> participants, List<ExpensePersistenceDTO> expenses,
        SettlementPersistenceDTO settlement) {
}
