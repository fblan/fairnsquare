package org.asymetrik.web.fairnsquare.split.persistence.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * Persistence DTO for Participant entity. Mirrors the JSON storage format with primitive types (no value objects).
 * <p>
 * {@code sharedAccountId} is nullable; existing files that predate this field are deserialized with {@code null},
 * preserving backward compatibility. The former {@code preferredCreditorId} field is silently ignored on read (Jackson
 * discards unknown fields by default).
 */
public record ParticipantPersistenceDTO(String id, String name, double nights,
        @JsonAlias("numberOfPersons") double share, String sharedAccountId) {
}
