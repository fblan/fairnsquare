# Refactor: Modular Architecture Enforcement

## What, Why and Constraints

**What:** Introduced `org.asymetrik.modular` module boundary enforcement for 5 modules: `split`, `filesystem`, `zipfile`, `error`, `logging`. Internal (non-exported) classes were moved to `internal` sub-packages to restrict cross-module access.

**Why:** Enforce architectural boundaries at build time so that no code can accidentally import internal implementation details from another module. The `ModularArchitectureTest` now fails the build if a module accesses a non-exported package of another module.

**Constraints:**
- `@Module` annotations cannot be nested (parent and child packages cannot both be modules)
- Modules are declared at leaf-domain level: `split`, `filesystem`, `zipfile`, `error`, `logging`
- `sharedkernel.validation` was skipped (empty package, no classes to protect)

## How

### Step 1 — Add dependencies (fairnsquare-app/pom.xml)
- `org.asymetrik.modular:api` (compile scope) — provides `@Module` annotation
- `org.asymetrik.modular:verification` (test scope) — provides `ModularVerifier`

### Step 2 — Declare modules via package-info.java

| File | Action | Exports |
|------|--------|---------|
| `sharedkernel/error/package-info.java` | Modified | `"."` |
| `sharedkernel/logging/package-info.java` | Created | `"."` |
| `split/package-info.java` | Created | `{}` (nothing) |
| `infrastructure/filesystem/package-info.java` | Created | `"."` |
| `infrastructure/zipfile/package-info.java` | Created | `"."` |

### Step 3 — Write ModularArchitectureTest
- `src/test/.../ModularArchitectureTest.java` — plain JUnit test (no `@QuarkusTest`)
- Scans `org.asymetrik.web.fairnsquare`, fails with a descriptive message on any violation

### Step 4 — Move internal classes to unexported sub-packages

**`filesystem.internal`** (new sub-package, not in exports):
- Moved: `PathId`, `TenantPathResolver`, `StorageCleanupScheduler`, `StorageLimitExceededError`, `StorageStats`
- Updated: `FileSystemService` — added imports from `internal`
- Visibility fixes: `TenantPathResolver.DEFAULT_TENANT` and `dataPath` made `public` (previously package-private, now different package)

**`zipfile.internal`**:
- Moved: `ZipMetadata`
- Updated: `ZipSerializer` — added import from `internal`

**`error.internal`**:
- Moved: `ProblemDetailMapper`
- Updated: explicit import for `BaseError` added

**`logging.internal`**:
- Moved: `LogInterceptor`
- Updated: explicit imports for `Log`, `LogTag` added

### Test files updated (import updates only)
- `FileSystemServiceTest.java`, `FileSystemServiceDirectTest.java`, `StorageStatsTest.java`
- `ExpenseUseCaseTest.java`, `ParticipantUseCaseTest.java`, `CreateSplitUseCaseTest.java`, `InfrastructureUseCaseTest.java`, `PersistenceRoundTripTest.java`

## Tests

Full Maven test suite passed (`mvn test`, exit code 0).

`ModularArchitectureTest` verifies:
- No nested module violations
- No export violations (cross-module access to non-exported packages)