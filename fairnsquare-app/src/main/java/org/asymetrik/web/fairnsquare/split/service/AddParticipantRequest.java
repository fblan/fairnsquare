package org.asymetrik.web.fairnsquare.split.service;

import org.asymetrik.web.fairnsquare.sharedkernel.validation.Violations;

/**
 * Request DTO for adding a participant to a split.
 */
public record AddParticipantRequest(String name,

        double nights,

        Double share) implements Violations.Validated {

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
