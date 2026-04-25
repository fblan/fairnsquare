# Bugfix: Use SecureRandom for CAPTCHA cryptographic operations

## What, Why and Constraints

**What**: Replaced `java.util.Random` with `java.security.SecureRandom` in `CaptchaService`. The single field `private final Random random = new Random()` is now `private final SecureRandom random = new SecureRandom()`. All four call sites (AES-GCM IV generation, two `nextInt` calls for challenge/distractor values, and `Collections.shuffle`) are unchanged.

**Why**: `CaptchaService` used the same `Random` instance to generate AES-GCM initialization vectors. `java.util.Random` is a Linear Congruential Generator with 48-bit internal state — given a few outputs, an attacker can recover the seed and predict every subsequent IV. AES-GCM requires unique, unpredictable IVs per key: an IV collision under the same key catastrophically breaks both confidentiality (XOR of plaintexts leaks) and authenticity (forgeable tags via authentication-key recovery). Predictable IVs also enable IV-collision attacks via birthday bound. This fix closes GitHub issues #125 (insecure RNG for crypto IV) and #137 (predictable challenge values usable to bypass CAPTCHA).

**Constraints** (from `src/doc/rules/backend-rules.md`):
- No CDI interceptor changes, no module boundary changes, no domain mutation, no test-only code added to `src/main/java`.
- No new logging added (existing service has none).
- No persistence DTO touched, no scheduled job, no `@QuarkusTestResource` change.

## How

### Files modified

**`fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/infrastructure/captcha/service/CaptchaService.java`**
- Replaced `import java.util.Random;` with `import java.security.SecureRandom;`.
- Changed field declaration from `private final Random random = new Random();` to `private final SecureRandom random = new SecureRandom();`.
- All four usage sites compile and behave identically because `SecureRandom` extends `Random`:
  - `random.nextInt(9)` for operands a, b (generateChallenge)
  - `random.nextBytes(iv)` for the AES-GCM IV (encryptChallenge) — the security-critical site
  - `random.nextInt(17)` for distractor candidates (buildShuffledAnswers)
  - `Collections.shuffle(all, random)` for answer-area ordering

### Why no other files changed

- The existing test `encryptChallenge_twoCallsProduceDifferentTokens` already proves IVs differ across calls; with `SecureRandom`, the property now holds for cryptographic reasons, not statistical accident.
- `SecureRandom` is a drop-in subtype, so signatures, callers, persistence, and JSON serialization are unaffected.
- No test asserts the concrete RNG class — adding such an assertion would test the framework rather than behaviour.

## Tests

**Automated regression check** — `mvn test -pl fairnsquare-app -Dquarkus.quinoa.run-tests=false`:
- `CaptchaServiceTest`: 24/24 passing (challenge generation, AES-GCM round-trip, stateless verification, token validation, secret fingerprint, AnswerArea contains).
- Full backend suite: **313/313 passing**, no failures, no skips.

**Manual verification not required** — change is a transparent algorithmic upgrade with identical observable behaviour at the public API.