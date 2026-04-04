# Feature: Admin View (#107)

## What, Why and Constraints

**What:** A password-protected admin view at `/admin` showing statistics about all splits stored on the server.

The admin view provides:
- **Main stats**: total number of splits, timestamp of the most recent update
- **Oldest created splits** (top 5): splits that have been around the longest
- **Oldest updated splits** (top 5): splits that haven't been touched recently
- **Full split list** (sortable): all splits with creation date, last update, participant count, expense count, and expense types
- Real split IDs are never exposed — only the first 16 hex chars of their SHA-256 hash

**Why:** Issue #107 — operators had no visibility into server usage. No way to monitor how many splits exist, when they were last touched, or whether the storage cleanup is working.

**Constraints:**
- Admin URL is not linked from any normal view (nav bar, split page header)
- Password hash is stored as `ADMIN_PASSWORD_HASH` env var (GitHub secret) — SHA-256 of the admin password. If not configured, the endpoint returns 503.
- The `Split` entity had no `updatedAt` field — added as part of this feature with backward compat for existing persisted files.
- Loading all splits on each admin request is acceptable — admin endpoint is called infrequently by a single operator.

## How

### Backend

**`Split.java`** — added `updatedAt: Instant` field
- Initialized to `createdAt` on creation
- Updated via `touch()` on every mutation: `addParticipant`, `updateParticipant`, `removeParticipant`, `addExpense`, `updateExpense`, `removeExpense`
- New two-arg constructor `Split(id, name, createdAt, updatedAt)` for the persistence layer
- `restoreUpdatedAt(Instant)` method: called by the mapper after loading to restore the persisted value (since `addParticipant`/`addExpense` during loading call `touch()`)

**`SplitPersistenceDTO.java`** — added `updatedAt` field (nullable for backward compat)

**`SplitPersistenceMapper.java`** — maps `updatedAt` on save; on load defaults to `createdAt` if null; calls `restoreUpdatedAt` after all sub-entities are loaded

**`FileSystemService.java`** — added `listAllSplitIds()`: walks the storage directory and returns split IDs (filenames without `.zip`)

**`SplitRepository.java`** — added `loadAll()`: loads all splits, silently skips any that fail to deserialize

**`admin/api/AdminProtected.java`** — `@NameBinding` annotation marking endpoints that require the admin token

**`admin/api/internal/AdminTokenFilter.java`** — JAX-RS request filter: reads `X-Admin-Token` header, computes SHA-256, compares to `admin.password-hash` config. Returns 503 if unconfigured, 401 if invalid.

**`admin/api/dto/AdminSplitSummaryDTO.java`** — per-split record: idHash, createdAt, updatedAt, participantCount, expenseCount, expenseTypes

**`admin/api/dto/AdminStatsResponse.java`** — root response: totalSplits, lastUpdated, list of summaries

**`admin/service/AdminService.java`** — loads all splits, computes stats, hashes IDs (first 16 hex chars of SHA-256)

**`admin/api/AdminResource.java`** — `GET /api/admin/stats` with `@AdminProtected`

**`application.properties`** — added `admin.password-hash=${ADMIN_PASSWORD_HASH:}`

### Frontend

**`src/lib/api/admin.ts`** — `getAdminStats(token)` calling `GET /api/admin/stats` with `X-Admin-Token` header

**`src/routes/Admin.svelte`**
- Password form: stores token in `sessionStorage` on success; auto-loads on mount if stored
- Stats dashboard: summary cards, top-5 oldest-created table, top-5 oldest-updated table, full sortable split list
- Sign out clears sessionStorage and returns to form

**`src/lib/router.ts`** — added `/admin` route (not linked from SplitPageHeader or any navigation)

### Files modified (tests)

**`SplitPersistenceMapperTest.java`** — updated two `new SplitPersistenceDTO(...)` calls for the new `updatedAt` parameter (passing `null` to test backward compat)

**`src/routes/Admin.test.ts`** (new) — 15 tests covering all 7 ACs

## Tests

| File | Tests | Coverage |
|---|---|---|
| `Admin.test.ts` | 15 (new) | Password form, API call, stats display, 401/503 errors, sign out, sort |
| `SplitPersistenceMapperTest.java` | unchanged count (updated 2) | Backward compat: null updatedAt defaults to createdAt |

**Total: 475 tests passing (full frontend suite).**

**Backend compile**: clean (Maven `compile` phase).
