# Feature: ZIP Archive Persistence Format

## What, Why and Constraints

**What:** Replaced the plain JSON file persistence format with a ZIP archive format. Each persisted entity is now stored as a `.zip` file containing two entries: `metadata.json` (format version and deserializer code) and `data.bin` (the JSON-serialized entity data).

**Why:** This architectural change prepares the persistence layer for future encryption support. The `metadata.json` contains a `deserializer` field (currently `"clear"` for plain JSON) that will later allow switching to encrypted payloads without changing the overall archive structure. The `version` field enables forward-compatible format evolution.

**Constraints:**
- Clean break from the previous `.json` format — no backward compatibility with old files
- The `data.bin` content remains plain JSON for now (deserializer = `"clear"`)
- The Jackson `ObjectMapper` configuration is unchanged (ISO-8601 timestamps, pretty-printed, JavaTimeModule)
- No new dependencies required — uses `java.util.zip` from the JDK

## How

### Files created

| File | Purpose |
|------|---------|
| `fairnsquare-app/src/main/java/.../persistence/PersistenceMetadata.java` | Java record holding `version` (currently `"1.0"`) and `deserializer` (currently `"clear"`), with a `current()` factory method |
| `fairnsquare-app/src/main/java/.../persistence/ZipFileRepository.java` | Replacement for `JsonFileRepository` — writes/reads ZIP archives with `metadata.json` and `data.bin` entries |

### Files modified

| File | Change |
|------|--------|
| `fairnsquare-app/src/main/java/.../persistence/TenantPathResolver.java` | Changed file extension from `.json` to `.zip` |
| `fairnsquare-app/src/main/java/.../persistence/SplitRepository.java` | Updated to reference `ZipFileRepository` instead of `JsonFileRepository` |
| `fairnsquare-app/src/test/java/.../persistence/PersistenceRoundTripTest.java` | Updated to use `ZipFileRepository`, adapted file content assertions to read from ZIP entries, added metadata verification test, removed legacy JSON test |
| `fairnsquare-app/src/test/java/.../api/CreateSplitUseCaseTest.java` | Replaced `Files.readString()` with ZIP entry reading for file content assertions |
| `fairnsquare-app/src/test/java/.../api/ParticipantUseCaseTest.java` | Replaced `Files.readString()` with ZIP entry reading for file content assertions |

### Files deleted

| File | Reason |
|------|--------|
| `fairnsquare-app/src/main/java/.../persistence/JsonFileRepository.java` | Replaced by `ZipFileRepository` |

### ZIP archive structure

```
{splitId}.zip
├── metadata.json    → { "version": "1.0", "deserializer": "clear" }
└── data.bin         → JSON-serialized entity (same content as before)
```

## Tests

All 214 backend tests and 315 frontend tests pass.

**Persistence round-trip tests** (`PersistenceRoundTripTest.java`):
- `shouldPersistAndLoadEmptySplit` — round-trip with no participants/expenses
- `shouldPersistAndLoadSplitWithParticipants` — round-trip with participants
- `shouldPersistAndLoadSplitWithByNightExpense` — round-trip with BY_NIGHT expense
- `shouldPersistAndLoadSplitWithEqualExpense` — round-trip with EQUAL expense
- `shouldPersistAndLoadSplitWithMixedExpenseTypes` — round-trip with mixed expense types
- `shouldVerifyZipArchiveContainsMetadataAndData` — verifies metadata.json (version, deserializer) and data.bin content
- `shouldNotPersistSharesAndRecalculateOnLoad` — verifies shares not in data.bin and recalculated on load
- `shouldPersistAsZipFile` — verifies `.zip` extension and file existence

**Use case tests** (`CreateSplitUseCaseTest.java`, `ParticipantUseCaseTest.java`):
- 4 tests updated to read `data.bin` from ZIP archives instead of reading raw file as string