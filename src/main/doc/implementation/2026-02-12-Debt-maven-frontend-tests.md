# Debt: Enable Frontend Tests in Maven Build

## What, Why and Constraints

### What
Integrated Vitest frontend tests into the Maven build lifecycle so that `mvn test` runs both backend (JUnit) and frontend (Vitest) tests. Also fixed 2 pre-existing failing frontend tests.

### Why
Frontend tests (Vitest) were not executed during `mvn test`. Quinoa only ran `npm run build` (compile check) but not `npm run test`. This meant broken frontend tests went undetected in the Maven build, reducing confidence in the CI pipeline.

### Constraints
- Used the Quinoa testing library (`quarkus-quinoa-testing`) with the JUnit wrapper approach (`@TestProfile(QuinoaTestProfiles.EnableAndRunTests.class)`)
- Kept Quinoa disabled in test mode by default — only the `WebUITest` class activates it via its test profile
- Fixed test expectations to match actual component/backend behavior (200-char description limit, not 100)

## How

### Step 1: Fix 2 failing frontend tests

- **`EditExpenseModal.test.ts`** — Two tests expected a 100-character description limit, but the component validates at 200 characters (matching backend `MAX_DESCRIPTION_LENGTH = 200`). Changed test inputs from `'a'.repeat(101)` to `'a'.repeat(201)` and expectations from `/100 characters/i` to `/200 characters/i`.

- **`AddExpenseModal.test.ts`** — The "form reset clears share inputs" test created two separate `render()` calls instead of using a single render with `rerender()`. This meant the `$effect` watching the `open` prop never fired on the original instance. Fixed to use single render + `rerender({ open: false })` then `rerender({ open: true })` to properly trigger the reset cycle. Also fixed deprecated `rerender({ props: {...} })` API to `rerender({...})`.

### Step 2: Add Quinoa testing integration

- **`pom.xml` (parent)** — Added `quarkus-quinoa-testing` to `<dependencyManagement>` with version `${quarkus-quinoa.version}`.
- **`fairnsquare-app/pom.xml`** — Added `quarkus-quinoa-testing` as a test-scoped dependency.
- **`WebUITest.java`** — Created JUnit test class annotated with `@QuarkusTest` and `@TestProfile(QuinoaTestProfiles.EnableAndRunTests.class)`. The empty test method triggers Quinoa to run `npm run test` during the Quarkus test lifecycle.

### Step 3: Verification

- Ran `mvn test` — 187 Java tests pass, Quinoa executes Vitest (247 frontend tests pass), BUILD SUCCESS.

## Tests

### Fixed tests
- `EditExpenseModal.test.ts`: 2 tests corrected (description length validation)
- `AddExpenseModal.test.ts`: 1 test corrected (form reset lifecycle)

### Integration
- `WebUITest.java`: 1 JUnit test that triggers Quinoa to run all 247 Vitest frontend tests during `mvn test`
