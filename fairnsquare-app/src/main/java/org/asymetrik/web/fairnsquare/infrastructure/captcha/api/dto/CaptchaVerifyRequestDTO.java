package org.asymetrik.web.fairnsquare.infrastructure.captcha.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for verifying a CAPTCHA answer. Contains the encrypted challenge token and the click coordinates.
 */
public class CaptchaVerifyRequestDTO {

    @NotBlank(message = "challengeToken is required")
    private String challengeToken;

    @NotNull(message = "x coordinate is required")
    @Min(value = 0, message = "x coordinate must be non-negative")
    private Integer x;

    @NotNull(message = "y coordinate is required")
    @Min(value = 0, message = "y coordinate must be non-negative")
    private Integer y;

    public CaptchaVerifyRequestDTO() {
    }

    public CaptchaVerifyRequestDTO(String challengeToken, Integer x, Integer y) {
        this.challengeToken = challengeToken;
        this.x = x;
        this.y = y;
    }

    public String getChallengeToken() {
        return challengeToken;
    }

    public void setChallengeToken(String challengeToken) {
        this.challengeToken = challengeToken;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }
}
