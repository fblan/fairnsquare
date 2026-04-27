# Backend Development Rules

## Domain Model

- In mutation methods on aggregate roots, `validate()` must always be the last method call. Any side effects (e.g. `clearSettlement()`) must happen before validation.

- When a domain record holds computed fields derived from the aggregate's state (e.g. denormalized calculations), the following conventions must apply:
  - They must be documented in the Javadoc as "computed, never persisted".
  - They must be initialized to a sensible zero/empty value in all factory methods.
  - The aggregate root is responsible for recalculating them after every mutation. The recalculation call must happen **after** `clearSettlement()` and **before** `validate()`.
  - Persistence mappers must initialize them to their zero value; the aggregate's mutation methods will restore correct values as the entity is reconstituted from storage.

## Dev Infrastructure

- Dev-only beans (seeders, data fixtures, etc.) must be placed in a dedicated `*.dev` sub-package and annotated with `@IfBuildProfile("dev")`. They must never be referenced from production code.

## Scheduled Jobs

- Scheduled jobs (classes annotated with `@Scheduled`) must be placed in the same package as the service they delegate to, and must be thin wrappers — they must contain no business logic themselves. All logic belongs in the injected service.

## Infrastructure Errors

- Errors related to infrastructure constraints (storage, rate limits, etc.) must extend `BaseError` and be placed in the relevant infrastructure package (e.g. `persistence/`), not in the domain layer. They must use appropriate HTTP status codes (e.g. 507 for storage limits) rather than generic 4xx codes.

## CDI Interceptors

- Cross-cutting concern interceptors (`@InterceptorBinding` and their `@Interceptor` implementations) must be placed in the `sharedkernel` package (e.g. `sharedkernel/logging/`). Interceptors must be purely observational — they must not alter the return value or suppress exceptions.

## Domain Model toString()

- All domain entities (aggregate roots, entities, value objects) must implement `toString()` with a concise, bounded-length summary of their identity and key state (ID, counts, flags). Avoid dumping full collections or large fields. This ensures meaningful and readable log output when entities appear in interceptor or debug logs.

## Application Configuration — Environment-Specific URLs

- URLs that vary per developer or environment (tunnel URLs, external service endpoints, etc.) must not be hardcoded in `application.properties`. Use SmallRye Config's `${ENV_VAR:default}` substitution syntax. Local overrides go in `fairnsquare-app/.env` (gitignored).

## Logging Conventions

- Use `org.jboss.logging.Logger` with format methods (`infof`, `errorf`, etc.). Log entries for service calls must use structured key=value format (e.g. `method=X splitId=Y result=Z duration=Nms`) to enable log parsing and filtering.

## Module Boundaries

- Modules are declared at leaf-domain level using `@Module` on `package-info.java` (via `org.asymetrik.modular:api`). Parent and child packages cannot both be annotated as modules.
- Non-exported implementation details must be placed in a `<module>.internal` sub-package. Only classes that are intentionally part of the module's public API should remain in the root module package.
- `ModularArchitectureTest` (plain JUnit, no `@QuarkusTest`) must scan `org.asymetrik.web.fairnsquare` and fail the build on any export violation or nested module violation.
- When moving a class to an `internal` sub-package, Java package-private access is broken. Any fields or constants that test code or sibling classes need must be explicitly made `public`.
- Domain packages (`*.domain`) must never be listed in a module's `@Module(exports = {...})`. Domain objects are internal implementation details. Only packages that form an explicit cross-module contract (e.g. annotations, shared interfaces) may be exported — typically the `api` package.

## Test-Only Code Must Not Live in Production Sources

- Methods, utilities, or helpers that exist solely to support tests must not be added to `src/main/java`. They belong in `src/test/java`.
- Before each commit, verify that no production class has been polluted with test-only methods (e.g. `cleanAll`, `reset`, `seed`, fixture helpers). A method is test-only if it is called exclusively from test code.
- If a test needs access to internal state, prefer injecting config properties (e.g. `@ConfigProperty`) or using the public API rather than adding a backdoor to the production service.

## Quarkus Test Context Sharing

- `@QuarkusTestResource` must not use `restrictToAnnotatedClass = true` unless the class sets custom `initArgs` that would conflict with other test classes (e.g. a non-default `maxFileCount` or `maxFileSizeBytes`). Using `restrictToAnnotatedClass = true` without a conflicting `initArgs` forces an unnecessary extra Quarkus application context per class, significantly increasing build time.
- Test isolation must be achieved through unique resource identifiers (NanoID) rather than per-class filesystem isolation. Tests must never enumerate all stored resources.

## API Layer Placement

- DTOs and mappers for a domain resource must be co-located under the owning domain's `api/` package (e.g. `split/api/expense/dto/`, `split/api/expense/mapper/`). They must not live in a top-level domain package unrelated to the resource that exposes them.

## Domain Polymorphism — Sealed Interfaces

- When a domain concept has multiple structurally distinct variants handled exhaustively (e.g. in a `switch` or `instanceof` chain), model it as a `sealed interface` with `record` implementations (inner or top-level). This gives compile-time exhaustiveness guarantees and prevents unchecked casts. Example: `SettlementParticipant` (`Standard`, `SharedAccount`, `SharedAccountMember`) and `SettlementPartyId` (`Individual`, `Group`).

## Runtime-Computed Domain Projections

- When a domain concept is derived from persisted entities at runtime and never stored itself (e.g. `SettlementParticipant` computed from `Participant` records), place it in the domain package alongside the entities it derives from. Mark it as "not persisted" in its Javadoc. Its mapper must be a stateless utility class with no injectable dependencies — a single static `from(...)` method is sufficient.

## Cross-Module Test Helpers

- When integration tests in module X need to use types from unexported packages of module Y (e.g., generating signed tokens in split tests), create the helper class in the root package `org.asymetrik.web.fairnsquare` (no `@Module` annotation), not inside any module package. Root-package classes fall outside the `ModularVerifier` boundary check and can access all modules freely.

## Module Exports for Cross-Module Annotations

- When a `@NameBinding` or other annotation is designed to be applied by other modules (e.g., `@CaptchaProtected` on a split endpoint), its package must be explicitly listed in the provider module's `@Module(exports = {...})`. Omitting it causes `ModularArchitectureTest` to fail for every consuming module. Export the minimal set of packages needed (typically the `api` package containing the annotation).

## Backward-Compatible Persistence Discriminators

- When adding a type discriminator field to a persistence DTO (e.g. `fromType` on `ReimbursementPersistenceDTO`), use `null` as the legacy default value rather than introducing a migration or a sentinel string. Null (or absent) must map to the original/default type, and the persistence mapper must apply this default explicitly on load. This allows all files written before the discriminator existed to continue loading without modification.

- When **removing** a domain type that has been persisted as a JSON discriminator (e.g. `"type": "EQUAL"` in ZIP archives), do NOT delete the persistence DTO class. Keep it exclusively in the persistence layer so Jackson can still deserialize legacy archives. The persistence mapper must transparently convert the legacy DTO to the nearest current type on load. The domain mapper must guard against receiving the legacy DTO directly (throw `UnsupportedOperationException`) to enforce that conversion happens at the correct layer.

## Sealed Classes — Removing a Permitted Subtype

- Removing a type from a `sealed` class or interface `permits` clause is a compile-breaking change for the removed subtype. Ensure the subtype file is deleted (or its `extends`/`implements` declaration removed) in the same commit. Leaving a `permits`-removed subtype on disk causes a compile error that manifests as an architecture test failure rather than a clear compiler diagnostic.

## Expense Update — SplitMode Constraints

- `split.updateExpense(id, amount, description, payer, SplitMode)` delegates to `Expense.fromJson()`, which cannot construct a `FREE` expense (FREE requires explicit per-participant shares that are not part of the update request signature). Tests exercising `updateExpense` must use `BY_NIGHT` or `BY_SHARE`. The `updateExpense` method must document this constraint with a `@throws UnsupportedOperationException` if `SplitMode.FREE` is passed.

## Timing-Safe Comparisons

- All equality checks on secret material — HMAC signatures, password hashes, API tokens, signed values — must use `MessageDigest.isEqual(byte[], byte[])`. `String.equals`, `String.equalsIgnoreCase`, and `Arrays.equals` must not be used for this purpose: they short-circuit on the first differing byte and leak timing information proportional to the length of the common prefix.
- To compare two strings (e.g. hex or base64 representations), convert both to bytes with the same encoding first: `MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8))`.
- The rule applies anywhere the compared value was supplied (directly or indirectly) by an external caller.

## Capability Credentials in Logs

- Parameters that function as capability credentials (e.g. NanoID-based identifiers where knowing the value alone grants access to a resource) must never be logged in raw form. Annotate them with `@LogTag(value = "...", sensitive = true)` so the interceptor logs a truncated SHA-256 hash instead. The hash is stable enough for log correlation but not reversible.
- The rule applies to any identifier whose knowledge alone grants access — not just split IDs, but participant IDs, expense IDs, or any future token-like identifier used as an access key.

## JAX-RS Response Filters — Scoping

- Response filters that apply security headers to a subset of endpoints must use a `@NameBinding` annotation rather than path-string matching inside `ContainerResponseFilter.filter()`. Apply the binding annotation to the resource class (not individual methods) to cover all current and future methods automatically.

## Cryptographic Randomness

- Any randomness consumed by a cryptographic operation — AES/GCM IVs, salts, nonces, key material, signed tokens, CAPTCHA challenges, anti-CSRF values — must come from `java.security.SecureRandom`. `java.util.Random`, `Math.random()`, and `ThreadLocalRandom` are forbidden in security-sensitive paths because they expose predictable internal state (LCG, 48-bit seed) that an attacker can recover from a handful of outputs.
- A single `SecureRandom` instance per service is sufficient — it is thread-safe and avoids per-call seeding cost.
- Non-cryptographic randomness (test fixtures, dev seeders, UI animations, jitter for backoff) may continue to use `java.util.Random` or `ThreadLocalRandom`. The boundary is "would predictability give an attacker leverage?", not "is this code in `infrastructure/`?".