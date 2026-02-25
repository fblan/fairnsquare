# Storage Stats Logging on Split Persist

## What, Why and Constraints

**What:** After each split is persisted, a log line is emitted showing the remaining storage space in MB, remaining space as a percentage, and the total number of stored ZIP files.

**Why:** Provides operational visibility into storage consumption at write time, making it easy to detect when the system is approaching its storage limit without waiting for a `StorageLimitExceededError`.

**Constraints:**
- Must reuse the existing `@Log` CDI interceptor infrastructure (no new logging framework).
- Stats are computed from the file system after the save completes, so they reflect the true post-save state.
- No change to any method signatures visible outside the persistence package.

## How

### `StorageStats.java` (new)
`fairnsquare-app/src/main/java/.../split/persistence/StorageStats.java`

A `record` holding `usedBytes`, `maxBytes`, and `fileCount`. Provides two derived values (`remainingMb()`, `remainingPercent()`) and a human-readable `toString()` formatted as:
```
remainingMb=450.1 remainingPct=86.1% fileCount=42
```
This string is what the `@Log` interceptor serialises as the `result=` field.

### `StorageConstraintsService.java` (modified)
Added import for `@Log` and a new public method `computeStorageStats()`:
- Annotated with `@Log` so the CDI interceptor fires on every call.
- Walks the root data directory in a single pass, accumulating total size and file count.
- Returns a `StorageStats` snapshot.
- Gracefully returns zeros when the directory does not exist yet.

### `SplitRepository.java` (modified)
- Injected `StorageConstraintsService` via constructor injection.
- After `zipFileRepository.save(...)` completes in `save()`, calls `storageConstraintsService.computeStorageStats()`.
- The CDI proxy ensures the `@Log` interceptor fires, producing a log line such as:
  ```
  method=computeStorageStats result=remainingMb=450.1 remainingPct=86.1% fileCount=42 duration=3ms
  ```

## Tests

Added three test cases in `StorageConstraintsServiceTest`:

| Test | Verifies |
|---|---|
| `shouldReturnZeroStatsWhenStorageIsEmpty` | Stats are zeroed and `remainingPercent` is 100 when no files exist |
| `shouldReturnCorrectStatsWhenFilesExist` | Correct `usedBytes` and `fileCount` when two test files are written |
| `shouldFormatStatsAsHumanReadableString` | `toString()` contains the three expected fields including `fileCount=1` |