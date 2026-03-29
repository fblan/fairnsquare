package org.asymetrik.web.fairnsquare.infrastructure.captcha.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for a newly created CAPTCHA challenge. Contains the encrypted challenge token (opaque to the client),
 * the CAPTCHA image as a base64-encoded PNG, and a short fingerprint of the signing secret for ops verification.
 */
public record CaptchaChallengeResponseDTO(@JsonProperty("challengeToken") String challengeToken,
        @JsonProperty("imageBase64") String imageBase64, @JsonProperty("secretFingerprint") String secretFingerprint) {

    @JsonCreator
    public CaptchaChallengeResponseDTO {
    }
}
