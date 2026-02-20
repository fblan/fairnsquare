package org.asymetrik.web.fairnsquare.split.domain;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing participant in a split.
 */
public record UpdateParticipantRequest(
        @NotBlank(message = "Name is required") @Size(max = 50, message = "Name cannot exceed 50 characters") String name,

        @DecimalMin(value = "0.5", message = "Nights must be at least 0.5") @DecimalMax(value = "365", message = "Nights cannot exceed 365") double nights,

        @DecimalMin(value = "0.5", message = "Number of persons must be at least 0.5") @DecimalMax(value = "50", message = "Number of persons cannot exceed 50") Double numberOfPersons) {

    /**
     * Returns the number of persons, defaulting to 1.0 if not specified.
     */
    public double numberOfPersonsOrDefault() {
        return numberOfPersons != null ? numberOfPersons : 1.0;
    }
}
