package org.asymetrik.web.fairnsquare.infrastructure.captcha.api.dto;

import org.asymetrik.web.fairnsquare.sharedkernel.validation.Violations;

/**
 * Request DTO for verifying a CAPTCHA answer. Contains the encrypted challenge token and the click coordinates.
 */
public class CaptchaVerifyRequestDTO implements Violations.Validated {

    @Override
    public void validate() {
        Violations.check().notBlank(challengeToken, "challengeToken is required").notNull(x, "x coordinate is required")
                .atLeast(x, 0, "x coordinate must be non-negative").notNull(y, "y coordinate is required")
                .atLeast(y, 0, "y coordinate must be non-negative").orThrow();
    }

    private String challengeToken;

    private Integer x;

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
