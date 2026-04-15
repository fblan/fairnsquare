# Refactor: Replace Total-Size Storage Limit with File-Count + Single-File-Size Limits

## What, Why and Constraints

**What:** Replaced the total-storage-bytes limit in `FileSystemService` with two simpler constraints:
1. **Max file count** (default 5000): rejects saves of *new* files when the count has reached the limit; updates to existing files are always allowed.
2. **Max single file size** (default 512 KB): rejects saves whose payload exceeds this threshold.

**Why:** The previous approach walked the entire storage directory and summed all file sizes on every `saveFile` call. This O(n) scan was unnecessary: if each file is bounded in size and the number of files is bounded, the total is implicitly bounded. The new approach is cheaper — the size check is O(1) (`data.length`), and the count check is still O(n) but only triggered for new files.

The admin page now exposes `splitCountLimit` alongside `totalSplits` so operators can see e.g. "3421 / 5000 splits".

**Constraints followed:**
- Infrastructure errors extend `BaseError`, live in the infrastructure package, use HTTP 507.
- `getMaxFileCount()` is a simple getter on `FileSystemService` used only by `AdminService` — no test-only backdoor added.
- `Files.walk()` streams are closed with try-with-resources throughout.

## How

1. **`StorageStats`** — removed `usedBytes`/`maxBytes` and all byte-based methods (`remainingMb`, `remainingPercent`, `formattedUsedSize`). Added `maxFileCount` and `remainingFileCount()` / `usedPercent()`.

2. **`StorageLimitExceededError`** — deleted. Replaced by:
   - `StorageFileCountLimitExceededError(int currentCount, int maxCount)` — thrown when a new file would exceed the file count limit.
   - `StorageFileSizeLimitExceededError(long fileSize, long maxFileSizeBytes)` — thrown when the payload is too large.

3. **`FileSystemService`** — removed `maxTotalSizeBytes`, `checkSizeLimitBeforeSave`, `computeTotalSize`. Added `maxFileCount`, `maxFileSizeBytes`, `checkFileSizeLimit`, `checkFileCountLimit`, `countZipFiles`. Simplified `computeStorageStats` to count files only. Added `getMaxFileCount()` for admin use. Fixed all `Files.walk()` usages to use try-with-resources.

4. **`application.properties`** — removed `max-total-size-bytes` / `FAIRNSQUARE_MAX_TOTAL_SIZE_BYTES`. Added `max-file-count` (default 5000, env `FAIRNSQUARE_MAX_FILE_COUNT`) and `max-file-size-bytes` (default 524288 = 512 KB, env `FAIRNSQUARE_MAX_FILE_SIZE_BYTES`).

5. **`TempStorageTestResource`** — renamed `maxStorageBytes` init arg to `maxFileCount`, wiring it to `fairnsquare.storage.max-file-count`.

6. **`AdminStatsResponse`** — added `splitCountLimit` field.

7. **`AdminService`** — injected `FileSystemService` to read the configured limit and pass it to the response.

## Tests

- **`StorageStatsTest`** — rewritten: verifies `remainingFileCount`, `usedPercent`, `toString` format.
- **`FileSystemServiceDirectTest`** — rewritten with `maxFileCount=3`, `maxFileSizeBytes=1024`: covers file-count limit (new vs update), file-size limit, stats, read/delete/exists.
- **`FileSystemServiceTest`** — rewritten with `maxFileCount=3`: same scenarios via CDI/QuarkusTest context; also covers cleanup and size-limit rejection with an oversized 600 KB payload.
- **`CreateSplitFileCountLimitTest`** — new integration test: pins `maxFileCount=2`, creates two splits, then verifies the third POST returns HTTP 507 with `storage-file-count-limit-exceeded` problem details.

All 329 tests pass.
