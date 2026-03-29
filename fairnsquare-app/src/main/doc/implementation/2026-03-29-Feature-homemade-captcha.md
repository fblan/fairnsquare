# Feature: Homemade CAPTCHA Human Verification

## What, Why and Constraints

**What:** A homemade, stateless CAPTCHA system that gates split creation behind human verification. The backend generates a PNG image showing a simple integer addition (e.g. "3 + 5 = ?") with four answer boxes. The correct answer area bounds and expiry are encrypted into an AES-GCM token returned to the client alongside the image as base64. The user clicks the correct box; the client sends the encrypted token and click coordinates back to the server, which decrypts, validates, and issues a signed HMAC-SHA256 proof token. The frontend stores the proof token in `localStorage` and passes it as `X-Captcha-Token` on every `POST /api/splits`. The backend filter rejects requests without a valid token.

**Why:** Prevent bots from flooding the application with automated split creation, while remaining horizontally scalable (no shared server-side state per challenge).

**Constraints followed:**
- No external CAPTCHA service dependency — fully self-contained.
- Image generation uses Java AWT (`BufferedImage` / `Graphics2D`), available in the JDK with `java.awt.headless=true`.
- Challenge token: AES-128-GCM, key derived from secret via SHA-256. Format: `base64url(iv || ciphertext+tag)`.
- Proof token: homemade HMAC-SHA256: `base64url({"exp":<epoch>}).<hmac>` — no JWT library added.
- No server-side challenge storage — fully stateless, horizontally scalable.
- Challenges carry their own expiry inside the encrypted token; replay within the TTL window is allowed by design (no single-use constraint on the challenge token).
- All domain types follow project conventions: records, value objects, `toString()` bounded.
- Token secret and TTLs configurable via SmallRye Config / environment variables.
- Module boundaries respected: `captcha.api` and `captcha.domain` exported so `split` and `infrastructure.captcha` can reference them.

---

## How

### Backend

**`captcha/package-info.java`** — module boundary annotation; exports `captcha.api` (so `split` can use `@CaptchaProtected`) and `captcha.domain` (so `infrastructure.captcha` can reference `CaptchaChallenge`).

**`captcha/domain/CaptchaChallenge.java`** — record holding `operandA`, `operandB`, list of `AnswerArea` records (id, x, y, width, height, answer), `correctAreaId`, and `expiresAt`. Removed `challengeId` — no longer needed since challenges are never stored server-side. `isCorrectAnswer(x, y)` and `AnswerArea.contains(x, y)` remain for service-level use; `isExpired()` retained for potential future use.

**`captcha/domain/CaptchaToken.java`** — unchanged. Homemade HMAC-SHA256 proof token issued after successful verification.

**`captcha/service/CaptchaService.java`** — fully rewritten as stateless:
- Removed `ConcurrentHashMap`, `@Scheduled` cleanup, `getChallenge(id)`.
- `generateChallenge()` — same layout logic, returns transient domain object (not stored).
- `encryptChallenge(challenge)` — AES-128-GCM: payload `{"exp":<epoch>,"cx":...,"cy":...,"cw":...,"ch":...}`, 12-byte random IV, result `base64url(iv || ciphertext+tag)`. Key derived: `SHA-256(secret)[0:16]`.
- `verifyAnswer(challengeToken, x, y)` — decrypts token, checks expiry, checks if click hits the stored correct area bounds, issues proof token on success.
- `validateToken(token)` — unchanged.

**`infrastructure/captcha/CaptchaImageGenerator.java`** — unchanged (uses `new Random()` for non-seeded noise lines).

**`captcha/api/CaptchaResource.java`** — simplified endpoints:
- `POST /api/captcha/challenges` → generates challenge, renders PNG, base64-encodes image, encrypts challenge token; returns `{challengeToken, imageBase64}` (201).
- Removed `GET /{id}/image` (image now inline in the create response).
- `POST /api/captcha/challenges/verify` → accepts `{challengeToken, x, y}`, decrypts and verifies, returns `{token}` (200) or 400.

**`captcha/api/dto/CaptchaChallengeResponseDTO.java`** — changed from `{challengeId}` to `{challengeToken, imageBase64}`.

**`captcha/api/dto/CaptchaVerifyRequestDTO.java`** — added `challengeToken` field (`@NotBlank`).

**`captcha/api/CaptchaProtected.java`** — unchanged.

**`captcha/api/internal/CaptchaTokenFilter.java`** — unchanged.

**`split/api/SplitResource.java`** — unchanged (still `@CaptchaProtected` on `createSplit`).

**`application.properties`** — unchanged.

### Frontend

**`src/lib/api/captcha.ts`** — updated: `CaptchaChallengeResponse` now has `{challengeToken, imageBase64}` instead of `{challengeId}`. Removed `getChallengeImageUrl()`. `verifyChallenge(challengeToken, x, y)` sends to `POST /captcha/challenges/verify`.

**`src/lib/stores/captchaStore.ts`** — unchanged.

**`src/lib/api/splits.ts`** — unchanged.

**`src/lib/components/ui/captcha/CaptchaModal.svelte`** — updated: uses `challengeToken` state instead of `challengeId`; renders image as `src="data:image/png;base64,{imageBase64}"` (inline, no separate image request); passes `challengeToken` to `verifyChallenge`. `loadChallenge` clears `errorMessage` only on successful load (not at start), ensuring the error message is visible while the next challenge is loading.

**`src/routes/Home.svelte`** — unchanged.

### Tests

**`CaptchaTokenTestHelper.java`** — new test utility in `org.asymetrik.web.fairnsquare` root package (outside any module) that generates valid CAPTCHA proof tokens using the test secret. Placed in the root to avoid module boundary violations from split test classes.

**`CreateSplitUseCaseTest.java`, `ExpenseUseCaseTest.java`, `SettlementUseCaseTest.java`, `ParticipantUseCaseTest.java`** — added `@BeforeEach`/`@AfterEach` to install a valid `X-Captcha-Token` header via `RestAssured.requestSpecification`, so `POST /api/splits` calls pass the CAPTCHA filter.

---

## Tests

### Backend

**`CaptchaServiceTest.java`** — pure unit test (fields injected via reflection):
- Challenge generation: 4 areas, operands 1–9, correct area ID in areas, correct area holds correct answer.
- Encryption: token is non-blank, two calls with same challenge produce different tokens (random IV).
- Stateless verification: correct click → valid proof token; wrong click → empty; tampered token → empty; null/blank token → empty; same token reusable (no single-use constraint).
- Token lifecycle: valid/expired/tampered/null/blank.
- `CaptchaToken` round-trip and wrong-secret rejection.
- `AnswerArea.contains`: center → true, outside → false.

### Frontend

**`captchaStore.test.ts`** — unchanged, 9 tests passing.

**`CaptchaModal.test.ts`** — updated mocks for new API shape (`challengeToken` + `imageBase64`); removed `getChallengeImageUrl` mock; updated `verifyChallenge` assertion to pass `challengeToken`; fixed "shows error message on wrong answer" test to make `createChallenge` hang on reload so the error is visible when asserted; 11 tests passing.

**`Home.test.ts`** — removed `getChallengeImageUrl` from captcha mock; 446 tests passing.
