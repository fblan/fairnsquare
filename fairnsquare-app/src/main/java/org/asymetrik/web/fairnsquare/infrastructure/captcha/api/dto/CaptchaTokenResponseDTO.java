package org.asymetrik.web.fairnsquare.infrastructure.captcha.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO containing the signed CAPTCHA token issued after a successful verification.
 */
public record CaptchaTokenResponseDTO(@JsonProperty("token") String token) {

    @JsonCreator
    public CaptchaTokenResponseDTO {
    }
}
