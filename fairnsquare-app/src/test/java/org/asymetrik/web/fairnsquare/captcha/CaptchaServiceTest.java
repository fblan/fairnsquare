package org.asymetrik.web.fairnsquare.captcha;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;

import org.asymetrik.web.fairnsquare.infrastructure.captcha.domain.CaptchaChallenge;
import org.asymetrik.web.fairnsquare.infrastructure.captcha.domain.CaptchaToken;
import org.asymetrik.web.fairnsquare.infrastructure.captcha.service.CaptchaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CaptchaService — challenge generation, AES-GCM token encryption, stateless coordinate verification,
 * and CAPTCHA proof token lifecycle.
 */
class CaptchaServiceTest {

    private static final String TEST_SECRET = "test-secret";

    private CaptchaService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new CaptchaService();
        setField(service, "secret", TEST_SECRET);
        setField(service, "challengeTtlSeconds", 300L);
        setField(service, "tokenTtlSeconds", 3600L);
    }

    // --- Challenge generation ---

    @Test
    void generateChallenge_hasFourAnswerAreas() {
        CaptchaChallenge challenge = service.generateChallenge();

        assertThat(challenge.answerAreas()).hasSize(4);
    }

    @Test
    void generateChallenge_operandsAreInRange() {
        for (int i = 0; i < 20; i++) {
            CaptchaChallenge challenge = service.generateChallenge();
            assertThat(challenge.operandA()).isBetween(1, 9);
            assertThat(challenge.operandB()).isBetween(1, 9);
        }
    }

    @Test
    void generateChallenge_correctAreaIdMatchesOneAnswerArea() {
        CaptchaChallenge challenge = service.generateChallenge();

        boolean found = challenge.answerAreas().stream().anyMatch(a -> a.id().equals(challenge.correctAreaId()));
        assertThat(found).isTrue();
    }

    @Test
    void generateChallenge_correctAreaContainsCorrectAnswer() {
        CaptchaChallenge challenge = service.generateChallenge();
        int expected = challenge.operandA() + challenge.operandB();

        int actual = challenge.answerAreas().stream().filter(a -> a.id().equals(challenge.correctAreaId())).findFirst()
                .map(CaptchaChallenge.AnswerArea::answer).orElseThrow();

        assertThat(actual).isEqualTo(expected);
    }

    // --- Challenge encryption (stateless token) ---

    @Test
    void encryptChallenge_returnsNonBlankToken() {
        CaptchaChallenge challenge = service.generateChallenge();

        assertThat(service.encryptChallenge(challenge)).isNotBlank();
    }

    @Test
    void encryptChallenge_twoCallsProduceDifferentTokens() {
        CaptchaChallenge challenge = service.generateChallenge();

        String token1 = service.encryptChallenge(challenge);
        String token2 = service.encryptChallenge(challenge);

        // Different random IVs → different ciphertext every call
        assertThat(token1).isNotEqualTo(token2);
    }

    // --- Answer verification (stateless) ---

    @Test
    void verifyAnswer_correctCoordinates_returnsToken() {
        CaptchaChallenge challenge = service.generateChallenge();
        String challengeToken = service.encryptChallenge(challenge);
        CaptchaChallenge.AnswerArea correct = challenge.answerAreas().stream()
                .filter(a -> a.id().equals(challenge.correctAreaId())).findFirst().orElseThrow();

        int clickX = correct.x() + correct.width() / 2;
        int clickY = correct.y() + correct.height() / 2;

        Optional<String> token = service.verifyAnswer(challengeToken, clickX, clickY);

        assertThat(token).isPresent();
    }

    @Test
    void verifyAnswer_correctCoordinates_tokenIsValid() {
        CaptchaChallenge challenge = service.generateChallenge();
        String challengeToken = service.encryptChallenge(challenge);
        CaptchaChallenge.AnswerArea correct = challenge.answerAreas().stream()
                .filter(a -> a.id().equals(challenge.correctAreaId())).findFirst().orElseThrow();

        Optional<String> token = service.verifyAnswer(challengeToken, correct.x() + correct.width() / 2,
                correct.y() + correct.height() / 2);

        assertThat(token).isPresent();
        assertThat(service.validateToken(token.get())).isTrue();
    }

    @Test
    void verifyAnswer_wrongCoordinates_returnsEmpty() {
        CaptchaChallenge challenge = service.generateChallenge();
        String challengeToken = service.encryptChallenge(challenge);

        Optional<String> token = service.verifyAnswer(challengeToken, 0, 0);

        assertThat(token).isEmpty();
    }

    @Test
    void verifyAnswer_tamperedToken_returnsEmpty() {
        CaptchaChallenge challenge = service.generateChallenge();
        String challengeToken = service.encryptChallenge(challenge);

        // Flip a character to corrupt the ciphertext/tag
        String tampered = challengeToken.substring(0, challengeToken.length() - 1) + "X";

        Optional<String> token = service.verifyAnswer(tampered, 200, 100);

        assertThat(token).isEmpty();
    }

    @Test
    void verifyAnswer_nullToken_returnsEmpty() {
        Optional<String> token = service.verifyAnswer(null, 100, 100);

        assertThat(token).isEmpty();
    }

    @Test
    void verifyAnswer_blankToken_returnsEmpty() {
        Optional<String> token = service.verifyAnswer("", 100, 100);

        assertThat(token).isEmpty();
    }

    @Test
    void verifyAnswer_sameTokenCanBeUsedMultipleTimes() {
        // Stateless — no single-use constraint on the challenge token itself
        CaptchaChallenge challenge = service.generateChallenge();
        String challengeToken = service.encryptChallenge(challenge);
        CaptchaChallenge.AnswerArea correct = challenge.answerAreas().stream()
                .filter(a -> a.id().equals(challenge.correctAreaId())).findFirst().orElseThrow();
        int x = correct.x() + correct.width() / 2;
        int y = correct.y() + correct.height() / 2;

        Optional<String> first = service.verifyAnswer(challengeToken, x, y);
        Optional<String> second = service.verifyAnswer(challengeToken, x, y);

        assertThat(first).isPresent();
        assertThat(second).isPresent();
    }

    // --- Token validation ---

    @Test
    void validateToken_validToken_returnsTrue() {
        String token = CaptchaToken.create(Instant.now().plusSeconds(3600), TEST_SECRET).value();

        assertThat(service.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_expiredToken_returnsFalse() {
        String token = CaptchaToken.create(Instant.now().minusSeconds(1), TEST_SECRET).value();

        assertThat(service.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_tamperedToken_returnsFalse() {
        String token = CaptchaToken.create(Instant.now().plusSeconds(3600), TEST_SECRET).value();
        String tampered = token + "X";

        assertThat(service.validateToken(tampered)).isFalse();
    }

    @Test
    void validateToken_null_returnsFalse() {
        assertThat(service.validateToken(null)).isFalse();
    }

    @Test
    void validateToken_blank_returnsFalse() {
        assertThat(service.validateToken("")).isFalse();
    }

    // --- Secret fingerprint ---

    @Test
    void getSecretFingerprint_returnsFirst8HexCharsOfSha256() {
        // SHA-256("test-secret") starts with 9caf06bb
        assertThat(service.getSecretFingerprint()).isEqualTo("9caf06bb");
    }

    @Test
    void getSecretFingerprint_hasLength8() {
        assertThat(service.getSecretFingerprint()).hasSize(8);
    }

    // --- CaptchaToken unit ---

    @Test
    void captchaToken_signAndVerify_roundTrip() {
        String token = CaptchaToken.create(Instant.now().plusSeconds(3600), TEST_SECRET).value();

        assertThat(CaptchaToken.isValid(token, TEST_SECRET)).isTrue();
    }

    @Test
    void captchaToken_wrongSecret_returnsFalse() {
        String token = CaptchaToken.create(Instant.now().plusSeconds(3600), TEST_SECRET).value();

        assertThat(CaptchaToken.isValid(token, "wrong-secret")).isFalse();
    }

    // --- AnswerArea ---

    @Test
    void answerArea_contains_centerPoint_returnsTrue() {
        CaptchaChallenge.AnswerArea area = new CaptchaChallenge.AnswerArea("id1", 10, 20, 80, 40, 7);

        assertThat(area.contains(50, 40)).isTrue();
    }

    @Test
    void answerArea_contains_outsidePoint_returnsFalse() {
        CaptchaChallenge.AnswerArea area = new CaptchaChallenge.AnswerArea("id1", 10, 20, 80, 40, 7);

        assertThat(area.contains(5, 40)).isFalse();
    }

    // --- Helpers ---

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
