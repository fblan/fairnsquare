# Feature: Participant Expenses Details (Issue #113)

## What, Why and Constraints

**What:** Added a "Details" button (list icon) to each participant card on the Participants page. Clicking it opens a modal that lists all expenses the participant is involved in — either as payer or as a share recipient — with columns: Name, Amount, Type, Spent, Owned, Balance.

**Why:** Users needed a way to drill into a specific participant's expense history without navigating away from the participant management page. Issue #113 requested this view with the specific columns listed above.

**Constraints:**
- All required data was already available in the `getSplit()` response (expenses array with shares). No new backend endpoint was needed.
- The Details button is always visible, even when the split is settled (read-only state), because viewing expenses is a non-destructive action.
- Frontend rules followed: scoped `within()` queries for duplicate-label buttons in tests; `flex-1 min-w-0 shrink-0` layout classes in the modal header to prevent overflow.

## How

### 1. `Participants.svelte` — Added Details button and modal wiring

- Added `List` icon to the lucide-svelte import.
- Added `ParticipantExpensesModal` import.
- Added two new state variables: `showExpensesModal` and `selectedParticipantForExpenses`.
- Added `handleDetailsClick()` and `handleExpensesModalClose()` handler functions.
- Restructured Row 1 of the participant card: moved the action buttons `div` outside the `{#if !isSettled}` guard and placed the Details button (always visible) first, followed by the mutation buttons (add expense, edit, delete) still gated by `{#if !isSettled}`.
- Added `<ParticipantExpensesModal>` usage at the bottom of the template, conditionally rendered when `split` and `selectedParticipantForExpenses` are both set.

### 2. `ParticipantExpensesModal.svelte` — New component

- Props: `open`, `participant`, `expenses`, `participants`, `onClose`.
- Derived `participantExpenses`: filters the full expenses array to those where `payerId === participant.id` OR `shares` contains the participant's id.
- Per-row computed values:
  - `spent` = full expense amount if participant is payer, else 0
  - `owned` = participant's share amount from `expense.shares`
  - `balance` = `spent - owned`
- Balance cell is coloured green (positive), red (negative), or muted (zero).
- Empty state shown when participant has no expenses.
- Keyboard: Escape closes the modal via `svelte:window onkeydown`.
- Backdrop click closes the modal.
- Modal uses `max-h-[80vh]` with `overflow-y-auto` on the content area to handle long lists on small screens.

## Tests

**File:** `src/lib/components/participant/ParticipantExpensesModal.test.ts`  
**Count:** 16 tests, all passing.

| Group | Tests |
|-------|-------|
| Rendering | Modal renders when open; does not render when closed; shows participant name in header |
| Expense filtering | Shows only relevant expenses (payer or share recipient); shows empty state when none |
| Data display (5 tests) | Description shown; split mode formatted correctly; spent=amount when payer; spent=0 when not payer; owned=share amount; positive balance in green; negative balance in red |
| Close behaviour | X button closes; footer Close button closes; Escape key closes; backdrop click closes |

Note: The X button test uses `within(header)` to scope the query per the frontend rule on scoping queries when multiple elements share the same accessible name ("Close").
