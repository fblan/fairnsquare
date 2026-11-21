package org.asymetrik.web.fairnsquare.infrastructure.captcha.api.dto;

/**
 * Response DTO containing the signed CAPTCHA token issued after a successful verification.
 */
public record CaptchaTokenResponseDTO(String token) {
}
