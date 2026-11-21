package org.asymetrik.web.fairnsquare.split.domain;

import org.asymetrik.web.fairnsquare.sharedkernel.validation.Violations;

/**
 * Request DTO for updating an existing participant in a split.
 */
public record UpdateParticipantRequest(String name,

        double nights,

        Double share,

        /** ID of the shared-account group this participant belongs to. Null removes the participant from any group. */
        String sharedAccountId) implements Violations.Validated {

    @Override
    public void validate() {
        Violations.check().notBlank(name, "Name is required").maxLength(name, 50, "Name cannot exceed 50 characters")
                .atLeast(nights, 1, "Nights must be at least 1").atMost(nights, 365, "Nights cannot exceed 365")
                .wholeNumber(nights, "Nights must be a whole number").atLeast(share, 0.5, "Share must be at least 0.5")
                .atMost(share, 50, "Share cannot exceed 50").orThrow();
    }

    /**
     * Returns the share, defaulting to 1.0 if not specified.
     */
    public double shareOrDefault() {
        return share != null ? share : 1.0;
    }
}
