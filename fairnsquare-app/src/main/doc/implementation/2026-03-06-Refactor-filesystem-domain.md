# Refactor: Extract Filesystem Technical Domain

**Date**: 2026-03-06

---

## 1. What, Why and Constraints

### What
Extracted a new `filesystem` technical domain from the `split/persistence` package. This domain owns all raw file system operations and storage constraint enforcement, exposing a clean public API centered on `PathId`, `Filename`, and `byte[]`.

The `ZipFileRepository.save()` method was also refactored to take `byte[]` data + an optional version string instead of a generic entity `T`. Serialization responsibility moved to `SplitRepository`.

### Why
The `split/persistence` package was doing too much: it handled both domain persistence logic (ZIP format, entity mapping) and low-level filesystem operations (path resolution, size limits, file cleanup). These are distinct concerns:
- **Filesystem domain**: knows about directories, file I/O, storage limits, file age — purely technical
- **Split persistence**: knows about ZIP format, metadata, entity serialization — business-aligned infrastructure

The split improves cohesion, makes each layer independently testable, and avoids the split domain depending on path resolution details.

### Constraints
- No API changes to `SplitRepository` (the public persistence boundary for the split domain)
- All existing tests must pass
- Backend rules followed: scheduler in same package as service, errors extend `BaseError`, interceptors in `sharedkernel`

---

## 2. How

### New package: `org.asymetrik.web.fairnsquare.infrastructure.filesystem`

**Created:**
- `PathId.java` — value object wrapping a directory path segment (e.g. tenant identifier)
- `Filename.java` — value object wrapping a file name (with extension)
- `StorageStats.java` — moved from `split/persistence/`; snapshot of storage usage
- `StorageLimitExceededError.java` — moved from `split/persistence/`; extends `BaseError`, HTTP 507
- `TenantPathResolver.java` — moved from `split/persistence/`; made public (used in tests across packages); API updated to accept `PathId`/`Filename` with backward-compatible `resolve(String splitId)` convenience method
- `FileSystemService.java` — new `@ApplicationScoped` service with public API:
  - `saveFile(Filename, byte[])` / `saveFile(PathId, Filename, byte[])` — writes file, checks size limit, logs stats
  - `readFile(Filename)` / `readFile(PathId, Filename)` → `Optional<byte[]>`
  - `deleteFile(Filename)` / `deleteFile(PathId, Filename)`
  - `existsFile(Filename)` / `existsFile(PathId, Filename)` → `boolean`
  - `computeStorageStats()` → `StorageStats` (annotated `@Log`)
  - `cleanOldFiles()` — deletes ZIP files older than configured retention
  - `checkSizeLimitBeforeSave()` — private; called inside `saveFile()`
- `StorageCleanupScheduler.java` — moved from `split/persistence/`; delegates to `FileSystemService.cleanOldFiles()` (per backend rule: scheduler lives in same package as service)

### Modified: `split/persistence`

**Modified:**
- `ZipFileRepository.java` — refactored:
  - Removed: generic `<T>` entity save/load; `ObjectMapper`-based serialization; `StorageConstraintsService` injection
  - Added: `save(String splitId, byte[] data)` (default version) and `save(String splitId, byte[] data, String version)` (explicit version)
  - Added: `load(String splitId)` → `Optional<byte[]>` (returns raw `data.bin` bytes after ZIP extraction and validation)
  - Injects `FileSystemService` for all I/O
  - Retains: ZIP format creation (`createZip`), metadata validation (`extractDataFromZip`), `PersistenceMetadata` usage
- `SplitRepository.java` — refactored:
  - Added: `ObjectMapper` (same configuration as before: `JavaTimeModule`, `INDENT_OUTPUT`)
  - `save()`: serializes entity DTO to JSON bytes, delegates to `zipFileRepository.save(splitId, bytes)`
  - `load()`: delegates to `zipFileRepository.load(splitId)`, deserializes bytes → DTO → domain
  - Removed: `StorageConstraintsService` injection and `computeStorageStats()` call (now called automatically inside `FileSystemService.saveFile()`)

**Deleted:**
- `StorageConstraintsService.java` — all logic distributed to `FileSystemService`
- `StorageCleanupScheduler.java` (old location)
- `TenantPathResolver.java` (old location)
- `StorageStats.java` (old location)
- `StorageLimitExceededError.java` (old location)

### Tests

**Created in `filesystem` test package:**
- `FileSystemServiceTest.java` (17 tests, `@QuarkusTest`) — replaces `StorageConstraintsServiceTest`; tests size enforcement via `saveFile()`, stats, cleanup, CRUD operations
- `FileSystemServiceDirectTest.java` (12 tests, no CDI) — replaces `StorageConstraintsServiceDirectTest`; direct instantiation for JaCoCo coverage
- `StorageStatsTest.java` (6 tests) — moved from `split/persistence`; no logic change

**Updated:**
- `PersistenceRoundTripTest.java` — switched from `ZipFileRepository`+mapper to `SplitRepository`; updated `TenantPathResolver` import
- `CreateSplitUseCaseTest`, `ParticipantUseCaseTest`, `ExpenseUseCaseTest`, `InfrastructureUseCaseTest` — updated `TenantPathResolver` import

**Deleted (old locations):**
- `split/persistence/StorageConstraintsServiceTest.java`
- `split/persistence/StorageConstraintsServiceDirectTest.java`
- `split/persistence/StorageStatsTest.java`

---

## 3. Tests

All 259 tests pass (`BUILD SUCCESS`).

New tests added: 35 (17 in `FileSystemServiceTest` + 12 in `FileSystemServiceDirectTest` + 6 in `StorageStatsTest`)

Key scenarios covered by new tests:
- Storage size limit enforcement (allow, reject, update without double-counting)
- Old file cleanup (delete files past retention, keep recent files)
- Storage stats computation (empty dir, with files, format strings)
- File CRUD: save, read, delete, exists
- Graceful handling of missing directories
