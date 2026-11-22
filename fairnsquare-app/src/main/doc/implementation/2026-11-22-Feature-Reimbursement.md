# Feature: Reimbursement expenses (#187)

## What, Why and Constraints

A new split mode, **Reimbursement**: one participant pays something for one other participant only, who bears the
whole amount. Examples: Alice buys Bob's €40 train ticket; Bob hands Alice €40 back in cash (paid by Bob for Alice).
Before, this needed a Manual expense with every participant at 0 parts but one.

Constraints:

- The beneficiary is a participant of the split and is not the payer (the expense would have no effect).
- Existing split files still load unchanged: the new type is a new discriminator (`REIMBURSEMENT`), nothing else
  changes in the stored JSON.
- Editing keeps the expense's id and creation date (PUT), unlike Manual expenses, which are replaced.
- A participant who is the beneficiary of a reimbursement cannot be deleted, as for a payer (409).

## How

### Backend

- `domain/expenses/ExpenseReimbursement` (new `Expense` subtype, `SplitMode.REIMBURSEMENT`): stores the
  `beneficiaryId`; `getShares` is a single share of the whole amount for the beneficiary, so balances and the
  settlement need no change.
- `domain/expenses/InvalidBeneficiaryError` (400, `invalid-beneficiary`): missing, unknown, or the payer.
- `Split.validateBeneficiary(payer, beneficiary)`; `Split.updateExpense(..., beneficiaryId)` builds a
  reimbursement when the mode is `REIMBURSEMENT` (the former signature delegates with `null`);
  `hasExpensesForParticipant` also counts the beneficiaries.
- `POST /api/splits/{id}/expenses/reimbursement` (`AddReimbursementRequest`: amount, description, payerId,
  beneficiaryId) → `SplitUseCases.addReimbursement`.
- `UpdateExpenseRequest` gets an optional `beneficiaryId` (required by the `REIMBURSEMENT` mode); the 4-argument
  constructor stays.
- API: `ExpenseReimbursementDTO` (the other fields plus `beneficiaryId`). Persistence:
  `ExpenseReimbursementPersistenceDTO` (`"type": "REIMBURSEMENT"`, `beneficiaryId`), both mappers updated.

### Frontend

- `components/expense-dialog.js`: a fourth mode, **Reimbursement** (arrows icon, with its info text), showing a
  **Paid for** select of every participant but the payer. Changing the payer refreshes it, keeping the choice when
  possible. Choosing the mode focuses it (as Manual focuses Edit shares). With a single participant, the select is
  empty and the form shows "A reimbursement needs another participant". The toast says "Paid by Alice for Bob".
  Adding posts to `/expenses/reimbursement`; editing uses PUT with `beneficiaryId`.
- Labels: `↔️ Reimbursement` in the expense list, `Reimbursement` in a participant's expenses. The share line
  ("Bob: €40.00"), the beneficiary filter and the settlement work from the shares, unchanged.

## Tests

- `split/api/ReimbursementUseCaseTest` (7, REST): creation (shares, balances, `beneficiaryId` in the split),
  beneficiary = payer, unknown or missing beneficiary (400), update from By Night to Reimbursement keeping the id,
  update without beneficiary (400), deleting a beneficiary (409).
- `PersistenceRoundTripTest.shouldPersistAndLoadSplitWithReimbursement`: save and load, balance recomputed.
- `e2e/ExpensesPageTest` (Playwright, 4): adding one (focus, payer left out of Paid for, toast, label, share
  line), changing the payer, editing one to another beneficiary, and the single-participant error.
- Checked visually at 390×844 and 1280×800: dialog, list with a long name, info window, settlement page.
