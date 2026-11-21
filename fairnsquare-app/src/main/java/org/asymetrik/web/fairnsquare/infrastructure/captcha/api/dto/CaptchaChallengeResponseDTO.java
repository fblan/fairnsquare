package org.asymetrik.web.fairnsquare.infrastructure.captcha.api.dto;

/**
 * Response DTO for a newly created CAPTCHA challenge. Contains the encrypted challenge token (opaque to the client),
 * the CAPTCHA image as a base64-encoded PNG, and a short fingerprint of the signing secret for ops verification.
 */
public record CaptchaChallengeResponseDTO(String challengeToken, String imageBase64, String secretFingerprint) {
}
