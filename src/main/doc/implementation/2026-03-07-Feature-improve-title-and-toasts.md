# Feature: Improve Title and Toast Detail

**Date:** 2026-03-07
**Branch:** `feature/improve-title-and-toasts`

---

## 1. What, Why and Constraints

### What
Two UI improvements:
1. **Browser tab title** — changed from the generic `webui` placeholder to `FairNSquare`.
2. **Toast notifications** — enriched all CRUD success toasts for expenses and participants with a contextual description line showing the entity's key details.

### Why
- The browser tab showed `webui`, which is confusing and unprofessional.
- Toast notifications only showed generic messages (`Expense added`, `Participant removed`, etc.) with no indication of *which* item was affected, making it hard to confirm the correct action was performed at a glance.

### Constraints
- The `Toast` interface is used across the whole frontend; adding the `description` field as optional preserves all existing callers without modification.
- The `Toaster` component layout must not break on toasts without a description — the second line is only rendered conditionally.
- No backend changes required.
- All existing and new behaviour must be covered by automated tests per `frontend-rules.md`.

---

## 2. How

### Step 1 — `index.html`
Changed `<title>webui</title>` to `<title>FairNSquare</title>`.

### Step 2 — `toastStore.svelte.ts`
Added an optional `description?: string` field to the `Toast` interface. No logic changes needed — `addToast` passes all fields through automatically.

### Step 3 — `Toaster.svelte`
Updated the toast body:
- The `message` now uses `font-medium` for visual hierarchy.
- A second `<p>` element renders `toast.description` conditionally (`{#if toast.description}`), styled as `text-xs opacity-80 mt-0.5 truncate`.
- The wrapping element changed from a plain `<p>` to a `<div class="flex-1 min-w-0">` to support the two-line layout without overflow.

### Step 4 — `ExpenseEditModal.svelte`
Enriched 4 success toast calls with a `description`:
- **Add** (BY_NIGHT / EQUAL): `{description} · €{amount} · Paid by {payerName}`
- **Update** (BY_NIGHT / EQUAL): same format
- **Add or Update** (FREE mode, single code path): same format
- **Delete**: `{expense.description} · €{expense.amount.toFixed(2)}`

Payer name is resolved inline via `participants.find(p => p.id === payerId)?.name`.

### Step 5 — `ExpenseList.svelte`
Enriched the delete success toast (direct-delete from the list view):
- Description: `{expenseToDelete.description} · €{expenseToDelete.amount.toFixed(2)} · Paid by {getPayerName(expenseToDelete.payerId)}`

`expenseToDelete` and `getPayerName` are already available at the call site.

### Step 6 — `Participants.svelte`
- **Add participant**: captured `formNights` and `formShare` into `addedNights` / `addedShare` constants *before* the form reset, then added `description: '${addedNights} nights · share ${addedShare}'` to the toast.
- **Delete participant**: added `description: deletingParticipantName` (the name is captured before deletion and is still set when the toast fires).

### Step 7 — `EditParticipantModal.svelte`
- **Update**: added `description: '${editName.trim()} · ${editNights} nights · share ${editShare}'`.
- **Delete**: added `description: participant.name` (`participant` prop is still non-null at this point).

---

## 3. Tests

All changes are covered by automated tests. 343 tests pass.

### `Toaster.test.ts` (+2 tests)
- `renders description line when description is provided` — verifies both `message` and `description` text appear in the DOM.
- `does not render description line when description is absent` — verifies only one `<p>` inside the alert when no description is given.

### `EditExpenseModal.test.ts` (2 assertions updated)
- `shows success toast after update` — assertion updated to include `description: 'Groceries · €75.00 · Paid by Bob'`.
- `shows success toast after deletion` — assertion updated to include `description: 'Groceries · €50.00'`.

### `AddExpenseModal.test.ts` (1 assertion updated + description input added)
- `shows success toast after creation` — test now fills in description field (`Lunch`); assertion updated to include `description: 'Lunch · €25.50 · Paid by Bob'`.

### `EditParticipantModal.test.ts` (+2 tests, new `Toast messages` describe block)
- `shows success toast with participant details after update` — verifies `description: 'Alice · 5 nights · share 1'`.
- `shows success toast with participant name after delete` — verifies `description: 'Alice'`.

### `ExpenseList.test.ts` (1 assertion updated)
- `calls API and updates list on confirm (AC 5)` — assertion updated to include `description: 'Groceries · €90.00 · Paid by Alice'`.

### `Participants.test.ts` (2 assertions updated)
- `calls API and refreshes list on successful submission` — assertion updated to include `description: '2 nights · share 1'`.
- `shows success toast after deletion` — assertion updated to include `description: 'Alice'`.
