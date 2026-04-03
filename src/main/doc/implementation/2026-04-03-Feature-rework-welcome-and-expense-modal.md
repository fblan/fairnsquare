# Feature: Rework Welcome Page and Expense Modal

## What, Why and Constraints

**What:** Three UX improvements from issue #94:
1. The welcome page now stores and displays up to 10 recent splits (instead of 1), each with its own Resume/Dismiss action.
2. The first-participant form has been removed from the welcome page — creating a split now only requires a name, then navigates directly to the participants page.
3. The expense modal now shows Description before Amount.

**Why:** The single "last split" resume card was limiting for users who work with multiple splits. The first-participant form on the home page was adding unnecessary friction to split creation. The description/amount order reflects natural input flow (name the expense first, then enter the cost).

**Constraints:**
- `saveLastSplit()` signature was kept identical so `Split.svelte` (the only other caller) required no changes.
- The store key was renamed from `fairnsquare_lastSplit` (single object) to `fairnsquare_lastSplits` (array) — old stored data is silently ignored (parse fails gracefully, returns `[]`).
- Recent splits are verified against the API on mount using `Promise.allSettled` — stale splits (404) are removed from storage automatically.

## How

### Files modified

**`fairnsquare-app/src/main/webui/src/lib/stores/lastSplitStore.ts`**

Complete rewrite:
- `saveLastSplit(split)` — prepends to array, deduplicates by id, caps at 10
- `loadLastSplits()` — returns `LastSplit[]` (new plural function)
- `removeLastSplit(id)` — removes one specific split by id (replaces `clearLastSplit`)
- `clearLastSplit` removed (no longer needed)

**`fairnsquare-app/src/main/webui/src/routes/Home.svelte`**

- Removed: participant name, nights, share fields; related state, validation, and `addParticipant` / `saveNightsDefault` calls
- `doCreateSplit()` now only calls `createSplit()` then navigates to the participants page
- Single resume card replaced by a list of recent splits (each with Resume + Dismiss)
- Recent splits verified via `Promise.allSettled` on mount; stale ones auto-removed
- Layout: Create Split form first, recent splits list below

**`fairnsquare-app/src/main/webui/src/lib/components/expense/ExpenseEditModal.svelte`**

- Description field moved before Amount field in the form template

### Files modified (tests)

**`fairnsquare-app/src/main/webui/src/lib/stores/lastSplitStore.test.ts`**

Fully rewritten to cover the new API: `saveLastSplit`, `loadLastSplits`, `removeLastSplit` — including deduplication, cap-at-10, and stale-entry handling.

**`fairnsquare-app/src/main/webui/src/routes/Home.test.ts`**

Fully rewritten:
- Removed all participant form tests and two-step create flow tests
- Added tests for split-name-only form, simplified create flow, recent splits list, individual dismiss, stale split removal, and CAPTCHA integration

## Tests

| File | Tests | Coverage |
|---|---|---|
| `lastSplitStore.test.ts` | 11 | saveLastSplit (prepend, dedup, cap), loadLastSplits (empty/corrupt/not-array), removeLastSplit (found/not-found/empty) |
| `Home.test.ts` | 18 | Form rendering (AC1), validation (AC2), create flow (AC3), recent splits list (AC4), individual dismiss (AC5), error handling (AC6), CAPTCHA (AC7) |
| `EditExpenseModal.test.ts` | 47 | Existing — all pass, field-order swap had no impact |
| `AddExpenseModal.test.ts` | 62 | Existing — all pass, field-order swap had no impact |

**Total: 449 tests passing (full suite).**
