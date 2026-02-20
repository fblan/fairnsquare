package org.asymetrik.web.fairnsquare.split.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for Participant REST API responses. Decouples domain model from API contract.
 */
public record ParticipantDTO(@JsonProperty("id") String id, @JsonProperty("name") String name,
        @JsonProperty("nights") double nights, @JsonProperty("numberOfPersons") double numberOfPersons) {

    @JsonCreator
    public ParticipantDTO {
    }
}
