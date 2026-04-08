# Bugfix: Filter Participant Zero-Amount Expenses

## What, Why and Constraints

**What:** In the participant expenses modal, expenses where the participant has a share entry with `amount = 0` (and is not the payer) were incorrectly shown. Since the participant neither spent nor owed anything, these expenses added noise without value.

**Why:** Issue #116 — the existing filter included any expense where the participant appeared in the `shares` array, regardless of the share amount. This caused false positives for split modes like `BY_NIGHT` or `BY_SHARE` where a participant with 0 nights/shares still receives a `shares` entry with `amount: 0`.

**Constraints:** Change limited to the filter predicate only; no data model or API changes required.

## How

### Files modified

- **`ParticipantExpensesModal.svelte`** — Added `&& s.amount > 0` to the shares filter condition inside the `$derived` block:
  ```js
  // Before
  e.shares.some((s) => s.participantId === participant.id)
  // After
  e.shares.some((s) => s.participantId === participant.id && s.amount > 0)
  ```
  The payer condition is unchanged: if the participant paid, `spent > 0` is always true (expense amount is always positive).

- **`ParticipantExpensesModal.test.ts`** — Added:
  - `expenseAliceZeroShare` fixture: Bob paid €80, Alice has a share entry with `amount: 0` (simulating `BY_NIGHT` with 0 nights)
  - Test: `hides expenses where participant has a zero-amount share and is not payer`

## Tests

- **Automated:** 1 new test added in `ParticipantExpensesModal.test.ts`, all 17 tests pass.
- The new test directly exercises the fixed scenario: expense with a zero-amount share does not appear in the modal for that participant.
