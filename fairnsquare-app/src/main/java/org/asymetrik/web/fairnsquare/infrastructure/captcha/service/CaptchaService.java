package org.asymetrik.web.fairnsquare.infrastructure.captcha.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jakarta.enterprise.context.ApplicationScoped;

import org.asymetrik.web.fairnsquare.infrastructure.captcha.domain.CaptchaChallenge;
import org.asymetrik.web.fairnsquare.infrastructure.captcha.domain.CaptchaToken;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Stateless CAPTCHA service. Challenges are generated in-memory and never stored on the server. The correct answer area
 * bounds and expiry are encrypted into an AES-GCM token returned to the client alongside the image. On verification the
 * token is decrypted server-side to check coordinates and expiry.
 */
@ApplicationScoped
public class CaptchaService {

    // Answer area layout constants
    private static final int ANSWER_AREA_COUNT = 4;
    private static final int BOX_MARGIN = 20;
    private static final int BOX_HEIGHT = 50;
    private static final int BOX_Y = CaptchaChallenge.IMAGE_HEIGHT - BOX_HEIGHT - BOX_MARGIN;

    // AES-GCM constants
    private static final String AES_CIPHER = "AES/GCM/NoPadding";
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private static final Base64.Encoder B64_ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_DEC = Base64.getUrlDecoder();

    private final Random random = new Random();

    @ConfigProperty(name = "captcha.secret", defaultValue = "change-me-in-production")
    String secret;

    @ConfigProperty(name = "captcha.challenge-ttl-seconds", defaultValue = "300")
    long challengeTtlSeconds;

    @ConfigProperty(name = "captcha.token-ttl-seconds", defaultValue = "3600")
    long tokenTtlSeconds;

    /**
     * Generate a new CAPTCHA challenge with a random addition question and shuffled answer areas. The challenge is
     * transient and not stored on the server.
     *
     * @return the generated challenge
     */
    public CaptchaChallenge generateChallenge() {
        int a = 1 + random.nextInt(9);
        int b = 1 + random.nextInt(9);
        int correctAnswer = a + b;

        List<Integer> allAnswers = buildShuffledAnswers(correctAnswer);

        int boxWidth = (CaptchaChallenge.IMAGE_WIDTH - (ANSWER_AREA_COUNT + 1) * BOX_MARGIN) / ANSWER_AREA_COUNT;
        List<CaptchaChallenge.AnswerArea> areas = new ArrayList<>();
        String correctAreaId = null;

        for (int i = 0; i < ANSWER_AREA_COUNT; i++) {
            int x = BOX_MARGIN + i * (boxWidth + BOX_MARGIN);
            int answer = allAnswers.get(i);
            String id = CaptchaChallenge.generateId().substring(0, 8);
            areas.add(new CaptchaChallenge.AnswerArea(id, x, BOX_Y, boxWidth, BOX_HEIGHT, answer));
            if (answer == correctAnswer && correctAreaId == null) {
                correctAreaId = id;
            }
        }

        Instant expiresAt = Instant.now().plus(Duration.ofSeconds(challengeTtlSeconds));
        return new CaptchaChallenge(a, b, areas, correctAreaId, expiresAt);
    }

    /**
     * Encrypt the correct answer area bounds and expiry into an AES-GCM token. The token is opaque to the client and
     * carries all data needed for stateless verification.
     *
     * @param challenge
     *            the challenge to encrypt
     *
     * @return base64url-encoded AES-GCM ciphertext: {@code base64url(iv || ciphertext+tag)}
     */
    public String encryptChallenge(CaptchaChallenge challenge) {
        CaptchaChallenge.AnswerArea correct = challenge.answerAreas().stream()
                .filter(a -> a.id().equals(challenge.correctAreaId())).findFirst()
                .orElseThrow(() -> new IllegalStateException("Correct area not found in challenge"));

        String payload = "{\"exp\":" + challenge.expiresAt().getEpochSecond() + ",\"cx\":" + correct.x() + ",\"cy\":"
                + correct.y() + ",\"cw\":" + correct.width() + ",\"ch\":" + correct.height() + "}";

        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[GCM_IV_BYTES + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_BYTES);
            System.arraycopy(ciphertext, 0, combined, GCM_IV_BYTES, ciphertext.length);

            return B64_ENC.encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt CAPTCHA challenge", e);
        }
    }

    /**
     * Stateless verification: decrypt the challenge token, check expiry, then check click coordinates against the
     * stored correct area bounds.
     *
     * @param challengeToken
     *            the encrypted token returned by {@link #encryptChallenge}
     * @param x
     *            click x coordinate (relative to image)
     * @param y
     *            click y coordinate (relative to image)
     *
     * @return a signed CAPTCHA proof token if correct, empty if wrong or expired
     */
    public Optional<String> verifyAnswer(String challengeToken, int x, int y) {
        if (challengeToken == null || challengeToken.isBlank()) {
            return Optional.empty();
        }
        try {
            byte[] combined = B64_DEC.decode(challengeToken);
            if (combined.length <= GCM_IV_BYTES) {
                return Optional.empty();
            }
            byte[] iv = Arrays.copyOf(combined, GCM_IV_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_BYTES, combined.length);

            Cipher cipher = Cipher.getInstance(AES_CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            String json = new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);

            long exp = parseLong(json, "exp");
            if (Instant.now().getEpochSecond() > exp) {
                return Optional.empty();
            }

            int cx = parseInt(json, "cx");
            int cy = parseInt(json, "cy");
            int cw = parseInt(json, "cw");
            int ch = parseInt(json, "ch");

            if (x < cx || x > cx + cw || y < cy || y > cy + ch) {
                return Optional.empty();
            }

            Instant tokenExpiry = Instant.now().plus(Duration.ofSeconds(tokenTtlSeconds));
            return Optional.of(CaptchaToken.create(tokenExpiry, secret).value());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Validate a CAPTCHA token's signature and expiry.
     *
     * @param token
     *            the raw token string from the request header
     *
     * @return true if the token is valid and not expired
     */
    public boolean validateToken(String token) {
        return CaptchaToken.isValid(token, secret);
    }

    /**
     * Return the first 8 hex characters of SHA-256(secret) as a stable fingerprint. Safe to expose publicly — reveals
     * nothing about the secret itself.
     */
    public String getSecretFingerprint() {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha.digest(secret.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute secret fingerprint", e);
        }
    }

    private SecretKeySpec deriveKey() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = Arrays.copyOf(sha.digest(secret.getBytes(StandardCharsets.UTF_8)), 16);
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static long parseLong(String json, String field) {
        Matcher m = Pattern.compile("\"" + field + "\":(-?\\d+)").matcher(json);
        if (!m.find()) {
            throw new IllegalArgumentException("Field not found in token payload: " + field);
        }
        return Long.parseLong(m.group(1));
    }

    private static int parseInt(String json, String field) {
        return (int) parseLong(json, field);
    }

    private List<Integer> buildShuffledAnswers(int correct) {
        List<Integer> wrongs = new ArrayList<>();
        while (wrongs.size() < ANSWER_AREA_COUNT - 1) {
            int candidate = 2 + random.nextInt(17);
            if (candidate != correct && !wrongs.contains(candidate)) {
                wrongs.add(candidate);
            }
        }
        List<Integer> all = new ArrayList<>(wrongs);
        all.add(correct);
        Collections.shuffle(all, random);
        return all;
    }
}
