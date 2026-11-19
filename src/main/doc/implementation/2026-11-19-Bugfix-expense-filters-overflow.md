# Bugfix: expense filters overflow the screen on mobile (#158)

## What, Why and Constraints

On the Expenses page the payer and beneficiary filters are native `<select>`s in a flex row. A flex item cannot shrink below its content width by default (`min-width: auto`), and a select's content width is its longest option. With a long participant name (e.g. `Alexandrina-Konstantinopoulou`; names allow 50 characters), on a 390 px wide phone the beneficiary filter ended 156 px past the screen edge and the whole page scrolled sideways. Found by `LayoutTest` (#157).

## How

- `routes/ExpenseList.svelte`: both filter selects get `min-w-0`, so they share the row equally and the browser truncates the selected name inside the select.
- `e2e/LayoutTest.java`: the shared layout fixture uses the long name again (it had been shortened until this fix), and a new test selects the long name in both filters on a phone, with the Clear filters button showing.

## Tests

- `LayoutTest`: 13 tests. Without the fix, 2 fail (expenses at 390×844, and the new filter test); with the fix, all pass.
- Checked visually with a 390×844 screenshot: both filters truncate the name, the Clear button stays on screen.
