# Bugfix: edit expense — emptying a field shows no error (#172)

## What, Why and Constraints

Regression from #171. "… is required" was moved to submit time (frontend rule: required is reserved for submission), but in the edit expense window Save stays disabled while the form is invalid, so submission never happens: clearing the description greyed Save out with no message. The Svelte window showed the message on leaving the field.

## How

`components/expense-dialog.js`, `validate()`: a field that started empty (adding an expense) still waits for submission before saying "required"; a field the person emptied (it had a value when the window opened, i.e. editing) shows the message as soon as it is left. Wrong values (e.g. an amount of 0) show on leaving the field as before.

## Tests

`ExpensesPageTest.emptyingAFieldOfAnExistingExpenseSaysItIsRequired`: edit an expense, clear the description, leave the field → "Description is required", Save disabled. It failed before the fix. The add-mode test `leavingAnEmptyFieldShowsNoErrorButAWrongValueDoes` still passes (17/17).
