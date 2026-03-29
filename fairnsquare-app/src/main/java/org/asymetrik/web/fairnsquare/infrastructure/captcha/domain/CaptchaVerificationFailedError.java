package org.asymetrik.web.fairnsquare.infrastructure.captcha.domain;

import org.asymetrik.web.fairnsquare.sharedkernel.error.BaseError;

/**
 * Error thrown when a CAPTCHA answer is wrong or the challenge has expired.
 */
public class CaptchaVerificationFailedError extends BaseError {

    private static final String TYPE = "https://fairnsquare.app/errors/captcha-failed";
    private static final String TITLE = "CAPTCHA Verification Failed";
    private static final int STATUS = 400;

    public CaptchaVerificationFailedError() {
        super(TYPE, TITLE, STATUS, "CAPTCHA answer is incorrect or the challenge has expired.");
    }
}
