# Feature: Resolve Settlement on Button Click

## What, Why and Constraints

**What:** Changed the settlement page so that the settlement is calculated on page load (showing balances immediately), and reimbursement details are only revealed when the user explicitly clicks the "Resolve" button.

**Why:** Previously, the settlement was calculated eagerly whenever the user navigated to the settlement page. The user wanted explicit control over when the settlement is "resolved" (i.e., when reimbursements are committed and shown). Balances (Paid / Cost per participant) are informational and can be shown immediately; the Resolve action commits the result.

**Constraints:**
- The backend already had a single `GET /splits/{id}/settlement` endpoint that both recalculated and returned the settlement. This needed to be split into a read-only `GET` (returns persisted settlement only, 404 if none) and a mutating `POST` (calculates, persists, and returns).
- The frontend must call `resolveSettlement` (POST) on page load to get balance data, but only show reimbursements after the user clicks Resolve.
- If the split already has a persisted settlement (`split.settlement != null`), reimbursements are shown directly without requiring the Resolve click.
- The Resolve button is placed before the balance cards, consistent with "Add Participant" / "Add Expense" button placement in other views.

---

## How

### Step 1 — Backend: new `SettlementNotFoundError`

Created `SettlementNotFoundError` (404) to distinguish "no settlement computed yet" from other errors. Used by the new `GET /splits/{id}/settlement` endpoint.

**File created:** `fairnsquare-app/src/main/java/.../split/domain/SettlementNotFoundError.java`

### Step 2 — Backend: split GET / POST settlement endpoints

`SplitUseCases`:
- Added `getPersistedSettlement(splitId)` — reads the persisted settlement from the split without recalculating. Returns `Optional.empty()` if none.
- `calculateSettlement(splitId)` is unchanged; it calculates, persists, and returns.

`SplitResource`:
- `GET /splits/{id}/settlement` — returns the persisted settlement (404 if not yet computed).
- `POST /splits/{id}/settlement` — calculates, persists, and returns the settlement.

**Files modified:** `SplitUseCases.java`, `SplitResource.java`

### Step 3 — Backend: new integration tests

Created `SettlementUseCaseTest` (8 tests):
- GET returns 404 when no settlement is persisted.
- GET returns the persisted settlement after a POST.
- POST calculates and persists the settlement.
- Error cases: 400 for invalid split ID format, 404 for unknown split.

**File created:** `fairnsquare-app/src/test/java/.../split/api/SettlementUseCaseTest.java`

### Step 4 — Frontend API: `resolveSettlement`

Added `resolveSettlement(splitId)` to `splits.ts` — calls `POST /splits/{id}/settlement`.

**File modified:** `fairnsquare-app/src/main/webui/src/lib/api/splits.ts`

### Step 5 — Frontend: Settlement.svelte

Revised the settlement page:
- On page load: `getSplit` and `resolveSettlement` (POST) are called in parallel. Balance cards are shown immediately from the POST result.
- `showReimbursements` starts as `false`. Reimbursement details inside each balance card are hidden until the user clicks Resolve.
- If `split.settlement != null` (already persisted), `showReimbursements = true` on load — reimbursements visible directly, Export button shown.
- The Resolve / Export button is placed **before the balance cards** (after the header), consistent with "Add Participant" / "Add Expense" button placement in other views.
- Removed unused `isResolving` state.
- Removed unused `getSettlement` import.

**File modified:** `fairnsquare-app/src/main/webui/src/routes/Settlement.svelte`

### Step 6 — Frontend: Split.svelte

Removed the `sessionStorage.setItem('settlement-resolved', 'true')` call that was previously used to signal the settlement page to auto-show reimbursements. This is now handled by checking `split.settlement != null` directly.

**File modified:** `fairnsquare-app/src/main/webui/src/routes/Split.svelte`

### Step 7 — Frontend: Settlement.test.ts

Rewrote the test suite to match the new behaviour (33 tests):
- `resolveSettlement` is mocked in `beforeEach` (called on load, not on button click).
- Balance cards and preferred creditor selects are asserted visible after page load (no Resolve click required).
- Resolve button click is tested to show reimbursement details without triggering an additional API call.
- `getSettlement` removed from mock (no longer used).

**File modified:** `fairnsquare-app/src/main/webui/src/routes/Settlement.test.ts`

### Step 8 — Frontend: Split.test.ts

Removed the assertion `expect(sessionStorage.getItem('settlement-resolved')).toBe('true')` that no longer applies.

**File modified:** `fairnsquare-app/src/main/webui/src/routes/Split.test.ts`

### Step 9 — Backend: backward compatibility tests (separate branch)

Added frozen ZIP fixture files and `PersistenceBackwardCompatibilityTest` to ensure old persisted splits remain loadable after format changes. (Committed separately on branch `feat/backward-compat-tests`.)

---

## Tests

### Backend
- **`SettlementUseCaseTest`** — 8 integration tests covering GET (404 when none, returns persisted after POST), POST (calculates and persists), and error cases.
- **`PersistenceBackwardCompatibilityTest`** — 3 tests loading frozen ZIP fixtures for format variations (missing `preferredCreditorId`, legacy `numberOfPersons`, legacy `BY_PERSON` expense type).

### Frontend
- **`Settlement.test.ts`** — 33 unit tests covering: loading state, header, error handling, page load behaviour (balance cards visible, Resolve button shown), Resolve button action (reimbursements revealed, no extra API call), already-persisted settlement auto-reveal, balance card colour/label display, Export Settlement clipboard flow, and Preferred Creditor select behaviour.
- **`Split.test.ts`** — 29 unit tests; removed the obsolete `sessionStorage` assertion.