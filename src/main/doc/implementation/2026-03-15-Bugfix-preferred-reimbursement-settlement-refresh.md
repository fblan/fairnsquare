# Bugfix: Preferred Reimbursement Not Applied When Resolving Settlement

## What, Why and Constraints

**What**: When a user selected a preferred creditor on the Settlement page and then clicked "Resolve",
the displayed reimbursements did not honour the preference — they were calculated before any preference
was set and never refreshed.

**Why**: The `Settlement.svelte` page loads the settlement once on mount via `getSettlement`. When the
user changed a preferred creditor, `handlePreferredCreditorChange` correctly saved the preference to
the backend via `updateParticipant`, but did **not** reload the settlement. Clicking "Resolve" simply
flipped `showReimbursements = true`, showing the already-loaded (stale) reimbursements that were
computed without the user's preference.

The backend algorithm (`SettlementCalculator.java`) was already correct: it applies all preferred
pairings in Phase 1 before running the greedy two-pointer pass in Phase 2. The problem was
exclusively in the frontend not re-fetching after a preference change.

**Constraints**: Minimal change — only the frontend `handlePreferredCreditorChange` function needed
updating; no backend changes were required.

## How

### File modified: `Settlement.svelte`

Added a call to `getSettlement(splitId)` at the end of `handlePreferredCreditorChange`, immediately
after the participant preference is persisted and the local `preferredCreditorId` is updated:

```js
settlement = await getSettlement(splitId);
```

This ensures that as soon as a user picks a preferred creditor, the settlement is recalculated by
the backend (which now includes the new preference in Phase 1 of the algorithm) and the updated
`settlement.reimbursements` are stored in the component state. When "Resolve" is subsequently
clicked, the shown reimbursements correctly reflect the preferred pairing.

### File modified: `Settlement.test.ts`

Added a new test `'reloads settlement after preferred creditor changes to reflect updated reimbursements'`
which verifies that `getSettlement` is called a second time after the user changes a preferred
creditor in the dropdown.

## Tests

- **New test** (`Settlement.test.ts`): `'reloads settlement after preferred creditor changes to reflect updated reimbursements'` — mocks two sequential `getSettlement` responses and asserts that after the select changes, `getSettlement` is called twice.
- All 374 existing frontend tests continue to pass.
- All 266 existing backend tests continue to pass (no backend changes).
