package org.asymetrik.web.fairnsquare.infrastructure.captcha.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * A homemade signed CAPTCHA token proving the user solved a challenge. Format:
 * {@code base64url(payload).base64url(hmac-sha256)} where payload is {@code {"exp":<epoch_seconds>}}.
 */
public record CaptchaToken(String value) {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder B64_ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_DEC = Base64.getUrlDecoder();

    /**
     * Create and sign a new token expiring at the given instant.
     *
     * @param expiresAt
     *            when this token expires
     * @param secret
     *            HMAC signing secret
     *
     * @return a new signed CaptchaToken
     */
    public static CaptchaToken create(Instant expiresAt, String secret) {
        String payload = B64_ENC
                .encodeToString(("{\"exp\":" + expiresAt.getEpochSecond() + "}").getBytes(StandardCharsets.UTF_8));
        String sig = hmac(payload, secret);
        return new CaptchaToken(payload + "." + sig);
    }

    /**
     * Verify the signature and expiry of a raw token string.
     *
     * @param token
     *            the raw token value
     * @param secret
     *            HMAC signing secret
     *
     * @return true if the signature is valid and the token is not expired
     */
    public static boolean isValid(String token, String secret) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String[] parts = token.split("\\.", 2);
        if (parts.length != 2) {
            return false;
        }
        // Constant-time signature comparison — prevents timing oracle on HMAC bytes
        String expectedSig = hmac(parts[0], secret);
        if (!MessageDigest.isEqual(expectedSig.getBytes(StandardCharsets.UTF_8),
                parts[1].getBytes(StandardCharsets.UTF_8))) {
            return false;
        }
        // Verify expiry
        try {
            String json = new String(B64_DEC.decode(parts[0]), StandardCharsets.UTF_8);
            long exp = Long.parseLong(json.replaceAll(".*\"exp\":(\\d+).*", "$1"));
            return Instant.now().getEpochSecond() <= exp;
        } catch (Exception e) {
            return false;
        }
    }

    private static String hmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return B64_ENC.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC computation failed", e);
        }
    }

    @Override
    public String toString() {
        return "CaptchaToken{value=<redacted>}";
    }
}
