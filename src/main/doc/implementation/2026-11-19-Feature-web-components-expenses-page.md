# Feature: web components step 4 — Expenses page and expense windows (#170)

Part of the migration to plain web components (#161).

## What, Why and Constraints

Moves the Expenses page and its windows (add / edit expense, Edit Shares, confirmations) to web components. The expense window is also used by the Participants page, still Svelte until step 5, so it exposes a small API both sides can use.

Constraints: same URL (`/splits/<id>/expenses`, with `?payer=…&beneficiary=…`), behaviour and look; the existing tests keep checking the same things.

## How

New files under `fairnsquare-app/src/main/resources/META-INF/resources/`:
- `biz/expenses/expenses-page.js`: `<fns-expenses-page split-id>`, replacing the body of `routes/ExpenseList.svelte` (now only renders it). A reload after a change keeps the list on screen (the Svelte page blanked it while reloading).
- `components/expense-dialog.js`: `<fns-expense-dialog>`, replacing `ExpenseEditModal.svelte`. `openAdd({splitId, participants, payerId?})`, `openEdit({splitId, participants, expense})`, and an `expensechange` event after a save or delete; the Svelte Participants page calls `openAdd` through `bind:this` and listens with `onexpensechange`. Contains the split-mode info windows.
- `components/share-dialog.js`: `<fns-share-dialog>`, replacing `ShareEditModal.svelte`; `edit(...)` resolves with the edited shares or null.
- `components/confirm-dialog.js`: `<fns-confirm-dialog>`, `ask({title, description, confirmLabel, cancelLabel, onConfirm})`; stays open (buttons disabled, spinner) while `onConfirm` runs and if it fails. The Svelte `confirm-dialog.svelte` stays for the Participants page until step 5.
- `components/base-styles.js`: shared modal styles (`dialog.modal`, `.sheet` bottom sheet on phones, head / body / foot), destructive and icon buttons, select fields. `components/icons.js`: the icons these pages need.
- Removed: `ExpenseEditModal.svelte`, `ShareEditModal.svelte`, the custom `select`, `radio-group` and `checkbox` Svelte components, the unused `ExpenseCard.svelte`, and six Svelte icons.

Behaviour changes, on purpose:
- The payer is a native `<select>` (accessible by default, the phone's own picker) instead of a custom dropdown that screen readers saw as plain buttons.
- Description gets focus as the window opens (`autofocus` on a native modal `<dialog>`) instead of 50 ms later from a timer — the cause of the flaky test in #168.
- Bugs of the Svelte window, fixed:
  - clicking a split mode's label did not select it (only the small radio did);
  - an untouched window, opened from the Expenses page, still asked "Discard changes?" on close (the default payer counted as a change);
  - submitting an empty add form still called the server (the inline errors showed, then an error toast);
  - leaving an empty field showed "… is required" right away, against the frontend rule that reserves it for submission; the message also moved the window's content under the finger, so a tap on a split-mode ⓘ right after could land on the row instead. Errors for wrong values (e.g. an amount of 0) still show when the field is left.

Caught on the way: `ExpensesPage` and `ExpenseDialog` had a method named `remove()`, and `<fns-toaster>` one named `remove(toast)`. They hid the DOM's `Element.remove()`, which Svelte calls to take a page off screen: leaving the Expenses page opened "Delete this expense?" instead, and the page stayed. Found by `SettlementPageTest`; renamed to `deleteExpense()` and `removeToast()`.

## Tests

- `ExpensesPageTest`: 16 tests (5 existing + 11). The payer is now chosen with `selectOption`; the add test also checks Description has focus. New: beneficiary filter kept in the address (and restored on reload); clearing a filter without results; empty form shows its errors and sends nothing; Manual mode with Edit shares and Copy nights shares; clicking a split-mode label selects it; Escape closes an untouched window; closing after changes asks to discard (Keep editing / Discard); delete from the edit window; split-mode info window; arrow keys step the amount by €0.50; leaving an empty field shows no error but a wrong value does.
- `ParticipantsPageTest`: + `addsAnExpenseForAParticipantFromTheirCard` (payer preselected, expense saved).
- Checked visually with before/after screenshots at 390×844 and 1280×800: list, filtered list, add window, Manual mode, Edit Shares, errors, info window, discard confirmation.
