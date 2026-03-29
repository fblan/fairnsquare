package org.asymetrik.web.fairnsquare.infrastructure.captcha.domain;

import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;

/**
 * Error thrown when a CAPTCHA challenge is not found or has expired.
 */
public class CaptchaChallengeNotFoundError extends BaseError {

    private static final String TYPE = "https://fairnsquare.app/errors/not-found";
    private static final String TITLE = "Not Found";
    private static final int STATUS = 404;

    public CaptchaChallengeNotFoundError(String challengeId) {
        super(TYPE, TITLE, STATUS, "CAPTCHA challenge not found or expired: " + challengeId);
    }
}
