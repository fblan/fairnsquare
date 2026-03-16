# Backward Compatibility Tests for Split Persistence

## What, Why and Constraints

**What:** Added a dedicated test class (`PersistenceBackwardCompatibilityTest`) that verifies old ZIP split files can still be loaded correctly by the current deserialization stack. Each test loads a frozen fixture file representing a specific old storage format.

**Why:** The persistence format evolves over time — new fields are added, field names are renamed, and enum values are aliased. Without explicit tests pinning each old format, a future refactor could silently break deserialization of data written by older versions of the application. This test suite provides a safety net: if a backward-compatibility annotation (e.g. `@JsonAlias`, `@JsonSubTypes` alias) is accidentally removed, these tests will fail immediately.

**Constraints:**
- Fixtures are real `.zip` binary files (not raw JSON) — they include the `metadata.json` ZIP entry with a pinned format version, exactly as they would appear on disk in production.
- The format version passed to `ZipSerializer.toZip()` when generating fixtures must be an explicit literal string (e.g. `"1.0"`), never `ZipMetadata.CURRENT_VERSION`, so the fixture remains pinned even if the current version is later bumped.
- Fixtures must be static, committed files — they must never be regenerated automatically, as they represent a point-in-time snapshot of a real on-disk format.
- Test code must not live in `src/main/java` (backend-rules.md).
- `@QuarkusTestResource` must not use `restrictToAnnotatedClass = true` without a conflicting `initArgs` (backend-rules.md).
- Participant and Split IDs must be exactly 21 URL-safe characters to satisfy domain value object validation.

---

## How

### Files created

**`fairnsquare-app/src/test/java/.../split/persistence/FixtureZipGenerator.java`**
Plain JUnit (no `@QuarkusTest`) utility class. Instantiates `ZipSerializer` directly, builds each old-format JSON string, wraps it with `zipSerializer.toZip(jsonBytes, "1.0")` (explicit version — not `CURRENT_VERSION`), and writes the result to `src/test/resources/fixtures/compat/*.zip`. Run once to regenerate fixtures; never runs in CI.

**`fairnsquare-app/src/test/resources/fixtures/compat/v1-no-preferred-creditor.zip`**
ZIP fixture for splits saved before feature #71. Participants JSON omits `preferredCreditorId`. Metadata version: `"1.0"`.

**`fairnsquare-app/src/test/resources/fixtures/compat/v1-number-of-persons.zip`**
ZIP fixture for splits saved before the participant-share refactor. Participant JSON uses `numberOfPersons` instead of `share`. Metadata version: `"1.0"`.

**`fairnsquare-app/src/test/resources/fixtures/compat/v1-by-person-expense.zip`**
ZIP fixture for splits saved before the BY_SHARE rename. Expense JSON uses `"type": "BY_PERSON"`. Metadata version: `"1.0"`.

**`fairnsquare-app/src/test/java/.../split/persistence/PersistenceBackwardCompatibilityTest.java`**
QuarkusTest class with three tests. Each test:
1. Reads the fixture `.zip` bytes from the classpath via `ClassLoader.getResourceAsStream`.
2. Saves the raw bytes to the temp storage directory via `FileSystemService.saveFile()` — no re-wrapping.
3. Calls `SplitRepository.load(splitId)` and asserts the resulting domain object is correct.

The `loadFixtureIntoStorage` helper throws a descriptive `AssertionError` if the resource is not found. `ZipSerializer` is not injected — the fixture is already a valid ZIP.

### Convention for adding new fixtures

When a new backward-compatibility mechanism is introduced (new nullable field, renamed field, aliased enum value):
1. Add the old-format JSON string to `FixtureZipGenerator` with the format version that was current at the time.
2. Run `FixtureZipGenerator.generateFixtures()` and commit the new `.zip` file.
3. Add a test method in `PersistenceBackwardCompatibilityTest` with a Javadoc comment explaining what old format it covers and why.

---

## Tests

Three automated tests added in `PersistenceBackwardCompatibilityTest`:

| Test | Fixture | Backward-compat mechanism verified |
|---|---|---|
| `shouldLoadSplitWhoseParticipantsHaveNoPreferredCreditorId` | `v1-no-preferred-creditor.json` | Missing `preferredCreditorId` → `null` |
| `shouldLoadSplitWithLegacyNumberOfPersonsField` | `v1-number-of-persons.json` | `@JsonAlias("numberOfPersons")` on `share` |
| `shouldLoadSplitWithLegacyByPersonExpenseType` | `v1-by-person-expense.json` | `BY_PERSON` alias → `ExpenseByShare` |

All three tests pass (`Tests run: 3, Failures: 0, Errors: 0`).