# Security: Split ID Log Hashing and Cache-Control Headers (#131)

## What, Why and Constraints

**What:** Two related security fixes addressing split capability URL leakage (GitHub issue #131):
1. Raw split IDs (and participant/expense IDs) were written to log files via `@LogTag`. Since the split URL is the sole credential for accessing a split, any log access was equivalent to full split access.
2. Split API responses lacked `Cache-Control`, `Strict-Transport-Security`, and `Referrer-Policy` headers, allowing the capability URL to leak through shared HTTP caches, insecure transport, and `Referer` headers on external link clicks.

**Why:** The access model is "the URL is the credential". A 21-char NanoID provides ~125 bits of entropy (not guessable), but it is fully exposed in logs on every service call. An attacker with log access (or an insider) has read/write access to every split. The cache and transport leakage vectors are lower-impact but straightforward to close.

**Constraints:**
- `Interceptors must be purely observational` (backend-rules) — hashing happens in the interceptor without altering return values or suppressing exceptions.
- `@NameBinding` pattern for JAX-RS filters — consistent with `@AdminProtected` and `@CaptchaProtected` in the codebase.
- SHA-256 truncated to 16 hex chars — same algorithm as `AdminService.hashId`, providing a stable opaque token for log correlation without reversibility.

## How

### Step 1 — `@LogTag` annotation extended with `sensitive` attribute
`sharedkernel/logging/LogTag.java`: added `boolean sensitive() default false`. Default is `false` so all existing usages are backward-compatible.

### Step 2 — `LogInterceptor` hashes sensitive values
`sharedkernel/logging/internal/LogInterceptor.java`: added a private `hashValue(Object)` method using `MessageDigest SHA-256`, truncated to 16 hex chars. In `extractTags()`, when `logTag.sensitive() == true`, the hashed value is logged instead of the raw value.

### Step 3 — `SplitUseCases` ID parameters marked sensitive
`split/service/SplitUseCases.java`: all 16 `@LogTag` annotations on `splitId`, `participantId`, `expenseId`, and `payerId` parameters updated to `sensitive = true`. Raw IDs no longer appear in any log line.

### Step 4 — `@SplitApi` name binding annotation created
`split/api/SplitApi.java`: new `@NameBinding` annotation following the project's established filter pattern (`@AdminProtected`, `@CaptchaProtected`). Applied to `SplitResource` at the class level, so all split endpoints automatically bind to any filter carrying the annotation.

### Step 5 — `SplitCacheControlFilter` response filter added
`split/api/SplitCacheControlFilter.java`: new `ContainerResponseFilter` annotated with `@Provider @SplitApi`. Sets three headers on every split API response:
- `Cache-Control: private, no-store` — prevents shared HTTP proxy caching of split URLs
- `Strict-Transport-Security: max-age=31536000` — enforces HTTPS at the HTTP layer (complements `force_https` in `fly.toml`)
- `Referrer-Policy: no-referrer` — prevents split URLs leaking in `Referer` headers to external sites

### Step 6 — `SplitResource` annotated with `@SplitApi`
`split/api/SplitResource.java`: added `@SplitApi` at the class level, binding all split endpoints to the response filter.

## Tests

**Unit / integration (LogInterceptor):**
- `LoggedTestService.withSensitiveTag()`: new test-only method with `@LogTag(value = "id", sensitive = true)`.
- `LogInterceptorTest.shouldHashSensitiveTagValues()`: verifies the raw ID is absent from the log and the expected 16-char SHA-256 hash is present.

**Integration (HTTP headers):**
- `SplitSecurityHeadersTest`: new `@QuarkusTest` verifying:
  - `Cache-Control: private, no-store` on `GET /api/splits/{id}`
  - `Strict-Transport-Security: max-age=31536000` on `GET /api/splits/{id}`
  - `Referrer-Policy: no-referrer` on `GET /api/splits/{id}`
  - Non-split endpoint (`/q/health`) does NOT receive `Cache-Control: private, no-store`

All 327 tests pass.
