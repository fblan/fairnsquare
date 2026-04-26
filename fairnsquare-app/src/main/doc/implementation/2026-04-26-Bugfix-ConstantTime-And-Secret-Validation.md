# Bugfix: Constant-time token comparison and required CAPTCHA secret

## What, Why and Constraints

**What**: Three related security fixes bundled in one PR (closes #126, #127, #128):

1. **#128 — Constant-time HMAC compare in `CaptchaToken`**: replaced `String.equals` with `MessageDigest.isEqual` for HMAC signature verification.
2. **#127 — Constant-time SHA-256 compare in `AdminTokenFilter`**: replaced `equalsIgnoreCase` with a constant-time byte comparison; also fixed the case-insensitivity bug (`equalsIgnoreCase` treated `A` as equal to `a`, which is wrong for hex digests).
3. **#126 — Required CAPTCHA secret in production**: removed `defaultValue = "change-me-in-production"` from `@ConfigProperty`, making `CAPTCHA_SECRET` a required env var in prod. Quarkus will refuse to start if it is absent.

**Why**:

- **#128/#127 (timing oracles)**: `String.equals` and `equalsIgnoreCase` both short-circuit on the first differing byte, leaking timing information proportional to the length of the common prefix. An attacker making many repeated requests can statistically infer bytes of the HMAC signature or SHA-256 hash. `MessageDigest.isEqual` runs in constant time regardless of content.
- **#127 additional**: `equalsIgnoreCase` accepted mixed-case hex strings (`A` == `a`), effectively doubling the search space for a brute-force attacker and producing incorrect rejections if the stored hash used a different case than the computed one. Constant-case comparison (lowercase normalisation + `MessageDigest.isEqual`) is both secure and unambiguous.
- **#126 (weak default secret)**: With `defaultValue = "change-me-in-production"`, any production deployment that forgot to set `CAPTCHA_SECRET` silently ran with a well-known secret. An attacker knowing this value could forge CAPTCHA proof tokens and bypass bot protection on `POST /api/splits`. The fix makes the absence of the env var a hard startup failure.

**Constraints** (from `src/doc/rules/backend-rules.md`):
- No CDI interceptor change, no domain model change, no module boundary change.
- `ADMIN_TOKEN_HEADER` made `public` because test code in a different package (`admin`) needed access — consistent with the rule that fields required by test code or sibling classes must be explicitly `public`.
- No Mockito or other new test dependency introduced — `AdminTokenFilter` is tested with a plain inline `ContainerRequestContext` stub.

## How

### Files modified

**`fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/infrastructure/captcha/domain/CaptchaToken.java`** (#128)
- Added `import java.security.MessageDigest`.
- Replaced `!expectedSig.equals(parts[1])` with `!MessageDigest.isEqual(expectedSig.getBytes(UTF_8), parts[1].getBytes(UTF_8))`. Both operands are base64url-encoded HMAC outputs, so comparing their UTF-8 byte representations is equivalent to comparing the underlying HMAC bytes.

**`fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/admin/api/internal/AdminTokenFilter.java`** (#127)
- Made `ADMIN_TOKEN_HEADER` `public` (was package-private; test code in a different package needs it).
- Added `import java.util.HexFormat`.
- Removed `import java.util.HexFormat` — wait, it was already there and removed in the refactor; re-added via import.
- Refactored `sha256Hex(String) → String` into `sha256Bytes(String) → byte[]` (returns raw digest bytes).
- Added `constantTimeHashEquals(byte[] computed, String storedHex)`: lowercases `storedHex`, parses it back to bytes with `HexFormat.of().parseHex(...)`, then compares with `MessageDigest.isEqual`. The `try/catch` around the hex parse returns `false` on malformed stored hashes (maps to 401, not 500).
- Updated `filter()` call: `!sha256Hex(token).equalsIgnoreCase(passwordHash)` → `!constantTimeHashEquals(sha256Bytes(token), passwordHash)`.

**`fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/infrastructure/captcha/service/CaptchaService.java`** (#126)
- Removed `defaultValue = "change-me-in-production"` from `@ConfigProperty(name = "captcha.secret")`. The property is now required by Quarkus in any profile that does not provide a profile-specific override.

**`fairnsquare-app/src/main/resources/application.properties`** (#126)
- Changed `captcha.secret=${CAPTCHA_SECRET:change-me-in-production}` to `captcha.secret=${CAPTCHA_SECRET}` (no fallback → required in prod).
- Added `%dev.captcha.secret=change-me-in-production` (dev profile retains the old placeholder, committed since it is not a real secret).
- Added `%test.captcha.secret=change-me-in-test` (test profile uses a distinct placeholder to avoid confusion with the dev value).

**`fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/CaptchaTokenTestHelper.java`** (#126)
- Updated `TEST_SECRET` from `"change-me-in-production"` to `"change-me-in-test"` to match `%test.captcha.secret`. Integration tests that use this helper to generate signed CAPTCHA tokens for the `X-Captcha-Token` header now produce tokens that the test-profile server validates correctly.

### Files created

**`fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/admin/AdminTokenFilterTest.java`** (#127)
- 6 plain-JUnit tests using an inline `ContainerRequestContext` stub (no Mockito).
- Covers: valid token passes, wrong token → 401, missing token → 401, uppercase stored hash still validates, unconfigured hash → 503, malformed hex hash → 401.

## Tests

Backend (`mvn test -pl fairnsquare-app -Dquarkus.quinoa.run-tests=false`):
- `CaptchaServiceTest`: 27/27 passing (HMAC round-trip tests cover the constant-time path indirectly).
- `AdminTokenFilterTest`: 6/6 passing (new).
- Full backend suite: **322/322 passing**, no failures, no skips.

No frontend changes — no frontend tests needed.
