package org.asymetrik.web.fairnsquare;

import java.time.Instant;

import org.asymetrik.web.fairnsquare.infrastructure.captcha.domain.CaptchaToken;

/**
 * Test helper for generating valid CAPTCHA tokens in integration tests. Placed in the root test package (not inside any
 * module) to avoid violating module boundary constraints.
 */
public final class CaptchaTokenTestHelper {

    /** The secret used in tests (matches %test.captcha.secret in application.properties). */
    private static final String TEST_SECRET = "change-me-in-test";

    private CaptchaTokenTestHelper() {
    }

    /**
     * Generate a valid, non-expired CAPTCHA token signed with the test secret.
     *
     * @return raw token string suitable for use in the {@code X-Captcha-Token} header
     */
    public static String generateToken() {
        return CaptchaToken.create(Instant.now().plusSeconds(3600), TEST_SECRET).value();
    }
}
