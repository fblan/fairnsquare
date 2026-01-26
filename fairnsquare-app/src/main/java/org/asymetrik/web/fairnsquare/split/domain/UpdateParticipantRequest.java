package org.asymetrik.web.fairnsquare.split.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing participant in a split.
 */
public record UpdateParticipantRequest(
        @NotBlank(message = "Name is required") @Size(max = 50, message = "Name cannot exceed 50 characters") String name,

        @Min(value = 1, message = "Nights must be at least 1") @Max(value = 365, message = "Nights cannot exceed 365") int nights) {
}
