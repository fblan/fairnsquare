# Refactor: Reduce Quarkus Test Contexts

## What, Why and Constraints

**What:** Removed `restrictToAnnotatedClass = true` from `@QuarkusTestResource` annotations on five `@QuarkusTest` classes so they can share a single Quarkus application context.

**Why:** Each class annotated with `restrictToAnnotatedClass = true` forces an independent Quarkus application startup (~10–15s each). With six such classes, the build was starting seven separate Quarkus contexts, adding ~75s of overhead. Removing the flag allows Quarkus to reuse one context across all classes with the same resource configuration.

**Constraints:**
- Test isolation must be preserved without per-class temp directories.
- Only classes using the default `TempStorageTestResource` configuration (no custom `initArgs`) are safe to share.
- `FileSystemServiceTest` is kept isolated (`restrictToAnnotatedClass = true`) because it sets a custom `initArgs = {maxStorageBytes=1024}` that would conflict with other classes if shared.
- Test isolation relies on NanoID-based unique resource IDs: tests never enumerate all stored files, so sharing a temp directory across tests is safe.

**Result:** Build time reduced from 4m21s to 3m06s (−75s, −29%).

## How

**Files modified:**

- `fairnsquare-app/src/test/java/.../split/api/CreateSplitUseCaseTest.java`
  Removed `restrictToAnnotatedClass = true`. Also removed unused `TenantPathResolver` and `jakarta.inject.Inject` imports that were artifacts of the previous isolation approach.

- `fairnsquare-app/src/test/java/.../split/api/ParticipantUseCaseTest.java`
  Simplified `@QuarkusTestResource` annotation by removing `restrictToAnnotatedClass = true`.

- `fairnsquare-app/src/test/java/.../split/api/ExpenseUseCaseTest.java`
  Same as above.

- `fairnsquare-app/src/test/java/.../split/persistence/PersistenceRoundTripTest.java`
  Same as above.

- `fairnsquare-app/src/test/java/.../infrastructure/filesystem/InfrastructureUseCaseTest.java`
  Same as above.

- `fairnsquare-app/src/test/java/.../infrastructure/filesystem/TempStorageTestResource.java`
  Updated Javadoc to document the default sharing model and explain when `restrictToAnnotatedClass = true` remains appropriate (custom `initArgs`).

**Files added:**

- `Makefile` (project root)
  Provides four developer-facing targets: `test` (full sequential build), `test-backend` (Quarkus only, skips vitest), `test-frontend` (vitest only), `test-parallel` (backend + frontend concurrently). The `test-parallel` target launches vitest in the background while Maven runs, then waits for both and fails if either exits non-zero.

## Tests

All existing tests pass after the refactor. No new tests were added — this is a pure configuration change. The correctness of test isolation was verified by:
1. Running `mvn clean install` successfully (all tests pass, coverage checks met).
2. Confirming that test classes using unique NanoID-based resource identifiers never interfere across shared temp storage.
