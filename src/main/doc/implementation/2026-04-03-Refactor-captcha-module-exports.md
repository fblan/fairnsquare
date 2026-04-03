# Refactor: Remove captcha domain from module exports

## What, Why and Constraints

**What:** Removed `org.asymetrik.web.fairnsquare.infrastructure.captcha.domain` from the `@Module(exports = {...})` declaration in the captcha module's `package-info.java`.

**Why:** Issue #97 — the captcha module was exposing its internal domain objects (`CaptchaChallenge`, `CaptchaToken`, `CaptchaChallengeNotFoundError`, `CaptchaVerificationFailedError`) through the module's exported packages. These are implementation details that no other module needs to import directly. Exporting them violates the module boundary principle followed by all other modules (e.g. `split` exports nothing). Any module could have imported captcha domain classes, creating tight coupling to infrastructure internals.

**Constraints:**
- No other module imports anything from `captcha.domain` — the change is safe with zero consumer impact.
- The `captcha.api` package (containing `CaptchaProtected`, the `@NameBinding` annotation used by `SplitResource`) remains exported, as required by the existing "Module Exports for Cross-Module Annotations" backend rule.

## How

### Files modified

**`fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/infrastructure/captcha/package-info.java`**

Removed `"org.asymetrik.web.fairnsquare.infrastructure.captcha.domain"` from the `exports` array:

```java
// Before
@Module(name = "captcha", exports = {
    "org.asymetrik.web.fairnsquare.infrastructure.captcha.api",
    "org.asymetrik.web.fairnsquare.infrastructure.captcha.domain"
})

// After
@Module(name = "captcha", exports = {
    "org.asymetrik.web.fairnsquare.infrastructure.captcha.api"
})
```

**`src/doc/rules/backend-rules.md`**

Added a new rule under "Module Boundaries":
> Domain packages (`*.domain`) must never be listed in a module's `@Module(exports = {...})`. Domain objects are internal implementation details. Only packages that form an explicit cross-module contract (e.g. annotations, shared interfaces) may be exported — typically the `api` package.

## Tests

- `ModularArchitectureTest` — 1 test, passes. This test scans all module boundaries and fails the build on any export violation or illegal cross-module import.
- Full backend test suite — BUILD SUCCESS, all tests pass.
- No new tests added: this is a configuration-only change with no behavioural impact. The architecture test already enforces module boundary correctness.
