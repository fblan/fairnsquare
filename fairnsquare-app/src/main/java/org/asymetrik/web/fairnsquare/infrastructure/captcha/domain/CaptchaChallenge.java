package org.asymetrik.web.fairnsquare.infrastructure.captcha.domain;

import java.time.Instant;
import java.util.List;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

/**
 * A CAPTCHA challenge containing a simple addition question, answer areas positioned on an image, and an expiry. The
 * challenge is transient — it is never stored on the server. Instead, the correct area bounds are encrypted into an
 * opaque token returned to the client and decrypted on verification.
 */
public record CaptchaChallenge(int operandA, int operandB, List<AnswerArea> answerAreas, String correctAreaId,
        Instant expiresAt) {

    /** Width of the generated CAPTCHA image in pixels. */
    public static final int IMAGE_WIDTH = 400;

    /** Height of the generated CAPTCHA image in pixels. */
    public static final int IMAGE_HEIGHT = 160;

    /**
     * A clickable answer area on the CAPTCHA image.
     */
    public record AnswerArea(String id, int x, int y, int width, int height, int answer) {

        /**
         * Returns true if the given pixel coordinates fall within this area.
         */
        public boolean contains(int px, int py) {
            return px >= x && px <= x + width && py >= y && py <= y + height;
        }
    }

    /**
     * Generate a new NanoID-based challenge ID.
     */
    public static String generateId() {
        return NanoIdUtils.randomNanoId();
    }

    /**
     * Returns true if the provided pixel coordinates hit the correct answer area.
     */
    public boolean isCorrectAnswer(int x, int y) {
        return answerAreas.stream().filter(a -> a.id().equals(correctAreaId)).anyMatch(a -> a.contains(x, y));
    }

    /**
     * Returns true if this challenge has passed its expiry time.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    @Override
    public String toString() {
        return "CaptchaChallenge{operands=%d+%d, expiresAt=%s}".formatted(operandA, operandB, expiresAt);
    }
}
