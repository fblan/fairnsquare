# Security: CORS Defaults and Test Admin Hash

## What, Why and Constraints

**What**: Two security hardening changes bundled in one PR (fixes #130 and #135).

1. **CORS defaults (#130)** — The base `application.properties` previously defaulted `FAIRNSQUARE_CORS_ORIGINS` to `http://localhost:5173,http://localhost:8080`, meaning any deployment that omitted the env var would silently accept requests from localhost origins in production. Additionally, no `quarkus.http.cors.methods` restriction was set, leaving all HTTP methods allowed by CORS.

2. **Weak test admin hash (#135)** — The `%test.admin.password-hash` was the SHA-256 of `"password"` — a well-known dictionary word. Although this hash is only used in the test profile, having it committed in source enables an attacker who discovers it to immediately reverse it using rainbow tables, potentially influencing development assumptions.

**Constraints**: Followed the same pattern established by `captcha.secret` in PR #145: no default in the base property (required env var for prod), profile-specific values in `application.properties` for dev and test.

## How

### #130 — CORS

**`fairnsquare-app/src/main/resources/application.properties`**

- Removed the `:http://localhost:5173,http://localhost:8080` fallback from the base `quarkus.http.cors.origins` property. The env var `FAIRNSQUARE_CORS_ORIGINS` is now required in production; Quarkus will refuse to start if it is absent.
- Added `%dev.quarkus.http.cors.origins=http://localhost:5173,http://localhost:8080` so dev mode continues to work without any env var.
- Added `quarkus.http.cors.methods=GET,POST,DELETE,PATCH,OPTIONS` to restrict the CORS pre-flight allowlist to the methods the API actually uses, rather than the Quarkus default of all methods.

### #135 — Test admin hash

**`fairnsquare-app/src/main/resources/application.properties`**

- Replaced `%test.admin.password-hash` value (SHA-256 of `"password"`) with the SHA-256 of `"test-admin-secret-fairnsquare"` — a non-dictionary string that cannot be reversed with a rainbow table.
- Updated the inline comment to document the new test password.

**`fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/admin/AdminTokenFilterTest.java`**

- Updated `PASSWORD` constant from `"password"` to `"test-admin-secret-fairnsquare"` so the unit test continues to compute and compare the correct hash.

## Tests

- `AdminTokenFilterTest` (6 unit tests) — all pass with the new password string. The test computes the hash inline via `sha256Hex(PASSWORD)` so it always stays in sync with the constant.
- Full backend suite: **322 tests, 0 failures**.
