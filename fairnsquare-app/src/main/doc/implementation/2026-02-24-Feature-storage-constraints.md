# Feature: Storage Constraints

**Date:** 2026-02-24

## What, Why and Constraints

### What
Two persistence constraints were added to the ZIP file storage layer:
1. **Total size limit** — saving a split is rejected with HTTP 507 if it would push total storage beyond a configured threshold.
2. **Nightly cleanup** — a scheduled job runs every day at midnight and removes ZIP files older than a configured number of days.

### Why
Without bounds on storage, the application could grow unboundedly on disk, causing issues on constrained environments. The size limit prevents uncontrolled growth; the age-based cleanup reclaims space from abandoned splits.

### Constraints
- Configuration is via `application.properties` / environment variables, consistent with the existing `fairnsquare.data.path` pattern.
- Defaults: **500 MB** total size (`FAIRNSQUARE_MAX_TOTAL_SIZE_BYTES`), **90 days** retention (`FAIRNSQUARE_MAX_FILE_AGE_DAYS`).
- The size check is exact: the ZIP is fully serialized in memory before any size check or disk write, so a rejection never produces a partial file.
- Update saves are handled correctly: the current file's size is subtracted from the total before checking, so replacing an existing split does not count the old bytes against the limit.
- Errors follow the existing RFC 9457 Problem Details pattern via `BaseError` / `ProblemDetailMapper`.

---

## How

### Files Created

**`StorageLimitExceededError.java`** (`split/persistence/`)
Extends `BaseError` with HTTP status 507 (Insufficient Storage) and the `https://fairnsquare.app/errors/storage-limit-exceeded` type URI. Automatically handled by the existing `ProblemDetailMapper` with no changes required there.

**`StorageConstraintsService.java`** (`split/persistence/`)
`@ApplicationScoped` service injected with config properties. Two public methods:
- `checkSizeLimitBeforeSave(Path filePath, long newFileSizeBytes)` — walks the root data directory, sums ZIP file sizes, subtracts the current file size if it already exists (update case), and throws `StorageLimitExceededError` if the projected total exceeds the limit.
- `cleanOldFiles()` — walks the root data directory, filters ZIPs by last-modified time older than the cutoff, deletes them, and logs a summary.

**`StorageCleanupScheduler.java`** (`split/persistence/`)
Thin `@ApplicationScoped` bean with a single `@Scheduled(cron = "0 0 0 * * ?")` method delegating to `StorageConstraintsService.cleanOldFiles()`.

**`StorageConstraintsServiceTest.java`** (`test/.../split/persistence/`)
8 `@QuarkusTest` integration tests covering: save allowed within limit, save rejected when limit exceeded, update case (existing file not double-counted), update rejection when new version still exceeds, cleanup deletes old files, cleanup keeps recent files, cleanup deletes all old files, graceful handling when data directory does not exist.

### Files Modified

**`pom.xml`** (`fairnsquare-app/`)
Added `quarkus-scheduler` dependency, required for `@Scheduled` support.

**`application.properties`**
Added config properties with env-var overrides:
```
fairnsquare.storage.max-total-size-bytes=${FAIRNSQUARE_MAX_TOTAL_SIZE_BYTES:524288000}
fairnsquare.storage.max-file-age-days=${FAIRNSQUARE_MAX_FILE_AGE_DAYS:90}
%test.fairnsquare.storage.max-total-size-bytes=1024
%test.fairnsquare.storage.max-file-age-days=30
```

**`TenantPathResolver.java`**
Added `resolveRootDirectory()` returning `Paths.get(dataPath)` to allow walking all tenant directories when computing total storage size.

**`ZipFileRepository.java`**
Injected `StorageConstraintsService`. Modified `saveToPath()` to serialize the ZIP into a `ByteArrayOutputStream` first, call `checkSizeLimitBeforeSave()`, then write bytes to disk via `Files.write()`. This ensures no partial file is written on a rejected save.

---

## Tests

All tests are automated (`@QuarkusTest`) in `StorageConstraintsServiceTest`:

| Test | Scenario |
|---|---|
| `shouldAllowSaveWhenStorageIsEmpty` | No files present → save allowed |
| `shouldAllowSaveWhenTotalWouldStayWithinLimit` | 500 B existing + 200 B new = 700 B < 1024 B → allowed |
| `shouldRejectSaveWhenTotalWouldExceedLimit` | 900 B existing + 200 B new = 1100 B > 1024 B → 507 thrown |
| `shouldNotDoubleCountExistingFileOnUpdate` | 800 B file updated with 500 B → net change is −300 B → allowed |
| `shouldRejectUpdateWhenNewVersionWouldExceedLimit` | 600 B other + 100 B splitA updated to 800 B → 1400 B > 1024 B → 507 thrown |
| `shouldDeleteFilesOlderThanConfiguredDays` | File set to 60 days old (> 30-day test limit) → deleted |
| `shouldKeepFilesNewerThanConfiguredDays` | Recently created files → kept |
| `shouldDeleteAllFilesOlderThanLimit` | Two files both 31 days old → both deleted |
| `shouldNotFailWhenDataDirectoryDoesNotExist` | Missing data dir → no exception |

Full test suite result: **219 tests, 0 failures**.