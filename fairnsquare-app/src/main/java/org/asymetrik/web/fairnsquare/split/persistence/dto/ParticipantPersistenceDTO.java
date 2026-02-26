package org.asymetrik.web.fairnsquare.split.persistence.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * Persistence DTO for Participant entity. Mirrors the JSON storage format with primitive types (no value objects).
 */
public record ParticipantPersistenceDTO(String id, String name, double nights,
        @JsonAlias("numberOfPersons") double share) {
}
