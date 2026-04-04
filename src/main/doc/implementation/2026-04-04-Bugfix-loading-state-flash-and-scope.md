# Bugfix: Loading State Flash and Scope

## What, Why and Constraints

**What:** Fixed two UX problems with the loading indicator on per-split pages (Split dashboard, Participants, ExpenseList, Settlement):

1. **Loading flash** — the spinner appeared immediately on every page transition, even when the API responded in under 500ms. Fixed by deferring `showLoading` to `true` only after a 500ms timer.
2. **Full-page loading scope** — the loading block replaced the entire page including the navigation header. Fixed by always rendering `SplitPageHeader` outside the loading block, using `splitId` (available immediately from route params).
3. **Title blink** — each per-split page is a separate Svelte component that mounts fresh with no split name. Navigating between tabs caused the title to flash blank on every mount. Fixed with a session-level `splitTitleCache` (plain Map) that persists the name across component re-mounts within the same session.

**Why:** Issue #103 — the full-page spinner on every tab switch was jarring, even for fast loads. The header disappearing during loading also removed the navigation context from the user.

**Constraints:**
- `splitId` is always available from `route.params.splitId` — safe to pass to `SplitPageHeader` immediately.
- Each tab is a separate Svelte component with its own `split = $state(null)` — name cannot persist between mounts without an external store. A session-level Map (`splitTitleCache.ts`) is the minimal solution.
- `split` is also no longer reset to `null` on reload — prevents blink when reloading within the same component. Content is guarded by `!isLoading` to prevent stale data from rendering.
- The timer must be cleared on both success and error paths to avoid stale state.

## How

### Files modified

All 4 per-split route components received the same treatment:

**`Split.svelte`, `Participants.svelte`, `ExpenseList.svelte`, `Settlement.svelte`**
- Added `showLoading = $state(false)` and `loadingTimer` alongside existing `isLoading`
- In `loadSplit()` / `loadSettlement()`: set `showLoading = false` at start, start a 500ms `setTimeout` that sets `showLoading = true`, clear timer and reset `showLoading = false` in `finally`
- Added `splitDisplayName = $state(getCachedSplitName(...))` — initialized from cache on mount, so the last known name is immediately available
- In `loadSplit`/`loadSettlement`: set `splitDisplayName`/`splitName` and call `setCachedSplitName()` on success
- Removed `split = null` at the start of `loadSplit` — also prevents blink when reloading within the same component
- Restructured template: `SplitPageHeader` moved above the `{#if ...}` block, always rendered when `splitId` is set
- `{#if isLoading}` in template replaced with `{#if showLoading}` — spinner only shown after 500ms
- Content guard changed from `{:else if split}` to `{:else if !isLoading && split}` — prevents stale content from rendering while loading

**`src/lib/stores/splitTitleCache.ts`** (new)
- Plain module-level `Map<string, string>` — no reactivity, no localStorage, no deps
- Exports `getCachedSplitName(id)` and `setCachedSplitName(id, name)`

### Files modified (tests)

**`Split.test.ts`, `Participants.test.ts`, `ExpenseList.test.ts`, `Settlement.test.ts`**
- Updated `shows loading state initially` → `shows loading spinner after 500ms delay`
- Tests now use `vi.useFakeTimers()` + `vi.advanceTimersByTimeAsync(500)` to verify:
  1. Spinner is NOT shown immediately after render
  2. Spinner IS shown after 500ms with a pending API call
- `vi.useRealTimers()` called at end of each test to restore timer behaviour

## Tests

| File | Tests | Coverage |
|---|---|---|
| `Split.test.ts` | updated (1) | Loading spinner deferred 500ms |
| `Participants.test.ts` | updated (1) | Loading spinner deferred 500ms |
| `ExpenseList.test.ts` | updated (1) | Loading spinner deferred 500ms |
| `Settlement.test.ts` | updated (1) | Loading spinner deferred 500ms |

**Total: 458 tests passing (full suite).**
