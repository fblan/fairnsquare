# Feature: Edit Preferred Creditor After Settlement

## What, Why and Constraints

**What:** The preferred creditor dropdown on the Settlement page was disabled once the user clicked "Resolve". It is now always enabled, so users can change their reimbursement preference at any time — including after the settlement has been resolved.

**Why:** Users may want to adjust who they reimburse after seeing the full settlement. Previously, clicking "Resolve" permanently locked the dropdown, making the preference uneditable without navigating away and back.

**Constraints:**
- The backend already cleared the settlement on `updateParticipant` (via `Split.clearSettlement()`) and always recalculates on `GET /settlement` — no backend changes were needed.
- After a preference change, the settlement must be invalidated in the frontend view: `showReimbursements` is reset to `false` and the settlement is reloaded, so the user must click "Resolve" again to see the updated reimbursements.

## How

### `Settlement.svelte`
- Removed `disabled={showReimbursements}` from the preferred creditor `<select>` element.
- Removed the now-unused `disabled:opacity-50 disabled:cursor-not-allowed` Tailwind classes from the same element.
- In `handlePreferredCreditorChange`, after a successful `updateParticipant` call: added `showReimbursements = false` and `await loadSettlement(splitId)` to reload the recalculated settlement.

### `Settlement.test.ts`
- Replaced `'disables the select after clicking Resolve'` with `'keeps the select enabled after clicking Resolve'`.
- Added `'reloads the settlement and hides reimbursements when preferred creditor changes after Resolve'` — verifies that `getSettlement` is called a second time and that reimbursements are hidden after the preference change.

## Tests

File: `src/routes/Settlement.test.ts`

**Modified test (1):**
- `Preferred Creditor > keeps the select enabled after clicking Resolve` — asserts `select.disabled === false` after clicking Resolve.

**New test (1):**
- `Preferred Creditor > reloads the settlement and hides reimbursements when preferred creditor changes after Resolve` — asserts `getSettlement` is called twice and that reimbursement details disappear after the preference change.

All 374 frontend tests pass.