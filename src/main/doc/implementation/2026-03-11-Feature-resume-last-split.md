# Feature: Resume Last Split from localStorage

## What, Why and Constraints

**What:** When a user visits the Home page (`/`), if they previously worked on a split, a "Resume your split" card is shown above the creation form. It lets them jump back to their split or dismiss the suggestion. The split is saved to localStorage whenever the split dashboard loads successfully.

**Why:** Users may close the tab or navigate away mid-session and return later. Without persistence, they would need to remember or search for their split URL. This reduces friction for the common single-device use case.

**Constraints:**
- The resume card is only shown if the split still exists on the server (verified via API call). A stale/deleted split silently clears the localStorage entry.
- Only the most recently visited split is stored (one entry).
- The creation form is always visible below the resume card — the user can start fresh at any time without dismissing.

## How

### Step 1 — `src/lib/stores/lastSplitStore.ts` (created)

New utility module with three functions:
- `saveLastSplit({ id, name })` — serializes to JSON under key `fairnsquare_lastSplit`
- `loadLastSplit()` — parses safely, returns `null` on missing or corrupt data
- `clearLastSplit()` — removes the key

Follows the existing `fairnsquare_*` key naming convention.

### Step 2 — `src/routes/Split.svelte` (modified)

After a successful `getSplit()` response, calls `saveLastSplit({ id: split.id, name: split.name })`. This ensures any split dashboard visit persists the split for later resume.

### Step 3 — `src/routes/Home.svelte` (modified)

On mount (`$effect`):
1. Calls `loadLastSplit()` — if nothing stored, no-op
2. Calls `getSplit(stored.id)` to verify the split still exists
3. On success: sets `resumeSplit` state → resume card rendered
4. On error (404 or network): calls `clearLastSplit()` silently, no card shown

Resume card actions:
- **Resume** button → `navigate('/splits/:splitId', { params: { splitId } })`
- **Dismiss** button → `clearLastSplit()` + sets `resumeSplit = null`

### Step 4 — Tests (created / modified)

`src/lib/stores/lastSplitStore.test.ts` (created) and `src/routes/Home.test.ts` (modified — 5 new tests added).

## Tests

### `src/lib/stores/lastSplitStore.test.ts` — 6 tests

| Group | Tests |
|---|---|
| `loadLastSplit` | returns null when nothing stored; returns null on corrupt JSON |
| `saveLastSplit / loadLastSplit` | saves and reloads correctly; overwrites previous entry |
| `clearLastSplit` | removes stored split; no-op when nothing stored |

### `src/routes/Home.test.ts` — 5 new tests (22 total)

| Test | Covers |
|---|---|
| No resume card when no split stored | `loadLastSplit` returns null → no card |
| Resume card shown with verified split name | `getSplit` resolves → card visible with name |
| No card when split not found on server | `getSplit` rejects → `clearLastSplit` called, no card |
| Navigate on Resume click | clicking Resume calls `navigate` with correct splitId |
| Dismiss hides card and clears storage | clicking Dismiss calls `clearLastSplit`, card hidden |

All 373 tests pass.
