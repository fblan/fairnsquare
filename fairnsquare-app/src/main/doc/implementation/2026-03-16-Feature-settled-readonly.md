# Feature: Settled Read-Only Mode + Unsettle

## What, Why and Constraints

**What:** When a split is settled (`split.settlement != null`), the UI becomes read-only — no adding, editing, or deleting of expenses or participants is possible. An amber banner is shown on the Participants and ExpenseList pages to inform the user and link to the settlement. An **Unsettle** button appears on the Settlement page (below the Export Settlement button, visible only when reimbursements are shown) to clear the settlement and restore editing.

**Why:** A resolved split should be protected from accidental edits that would silently invalidate the settlement. The explicit Unsettle action makes reverting to an editable state intentional.

**Constraints:**
- The `DELETE /splits/{id}/settlement` endpoint was new — no existing infrastructure.
- `Split.clearSettlement()` already existed in the domain model.
- The Svelte 5 `$derived` pattern was used for `isSettled` to keep the settled state reactively consistent with the split data.
- Settlement page already called `resolveSettlement` (POST) on every load — the Unsettle flow reuses that call to refresh the calculated settlement after unsettling.

## How

### Backend

**`SplitUseCases.java`** — Added `unsettleSettlement(String splitId)`:
- Loads the split, calls `split.clearSettlement()`, saves, returns `true`.
- Returns `false` if the split is not found (used to distinguish 404 from success).

**`SplitResource.java`** — Added `DELETE /splits/{id}/settlement`:
- Validates the split ID format (400 if invalid).
- Calls `splitService.unsettleSettlement(splitId)`; returns 404 if split not found, 204 No Content on success.

### Frontend API

**`splits.ts`** — Added `unsettleSettlement(splitId)`:
- Calls `DELETE /splits/{splitId}/settlement`.
- Already imported `unsettleSettlement` in `Settlement.svelte`.

### Frontend — Settlement page

**`Settlement.svelte`** — Added Unsettle button:
- New state: `let isUnsettling = $state(false)`.
- `handleUnsettle()`: calls `unsettleSettlement`, resets `showReimbursements = false`, reloads `getSplit` + `resolveSettlement` in parallel.
- Unsettle button renders below Export Settlement, only when `showReimbursements` is true. Uses `ghost` variant with muted text, disabled while `isUnsettling`.

### Frontend — Participants page

**`Participants.svelte`** — Read-only when settled:
- `const isSettled = $derived(split?.settlement != null)`.
- Amber banner shown after the header when `isSettled`, with a "View settlement" `<button>` navigating to the settlement page.
- Add Participant button/form gated with `{#if !isSettled}`.
- Per-card Edit, Delete, and Add Expense buttons gated with `{#if !isSettled}`.

### Frontend — ExpenseList page

**`ExpenseList.svelte`** — Read-only when settled:
- `const isSettled = $derived(split?.settlement != null)`.
- Amber banner shown after the header when `isSettled`.
- Add Expense button gated with `{#if !isSettled}`.
- Per-card Edit and Delete action row gated with `{#if !isSettled}`.

## Tests

### Backend (`SettlementUseCaseTest.java`)

Four new integration tests added:
- `unsettleSettlement_afterCalculation_returns204()` — POST then DELETE → 204
- `unsettleSettlement_afterCalculation_clearsSettlement()` — POST → DELETE → GET → 404
- `unsettleSettlement_withInvalidSplitId_returns400()` — invalid ID → 400
- `unsettleSettlement_withUnknownSplitId_returns404()` — unknown split → 404

Total: 12 tests (all passing).

### Frontend

**`Settlement.test.ts`** — 6 Unsettle tests:
- Unsettle button absent before Resolve; present after Resolve and when already persisted.
- Calls `unsettleSettlement` + reloads on click.
- Resets to pre-resolve state (Resolve button shown, reimbursements hidden).
- Error toast on failure.

**`Participants.test.ts`** — 7 settled read-only tests:
- Settled banner shown/hidden correctly.
- "View settlement" link navigates to settlement page.
- Add Participant, Edit, Delete, Add Expense buttons absent when settled.

**`ExpenseList.test.ts`** — 7 settled read-only tests:
- Settled banner shown/hidden correctly.
- "View settlement" link navigates to settlement page.
- Add Expense, Edit, Delete buttons absent when settled.

Total frontend tests: 401 (all passing).