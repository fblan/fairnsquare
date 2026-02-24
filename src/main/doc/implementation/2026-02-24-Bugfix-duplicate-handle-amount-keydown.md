# Bugfix: Duplicate handleAmountKeydown in ExpenseEditModal

## What, Why and Constraints

Fixed a compile error in `ExpenseEditModal.svelte` caused by two duplicate declarations that were introduced by a double-paste.

**What broke**: All 4 test suites that import `ExpenseEditModal` (directly or transitively) failed with a `CompileError` — no tests could run in those files.

**Constraints**: No logic was changed; the first (correct) instance of each declaration was kept and the redundant copies removed.

## How

### Files modified

**`fairnsquare-app/src/main/webui/src/lib/components/expense/ExpenseEditModal.svelte`**

Two duplicates removed:
1. `function handleAmountKeydown` was declared twice (lines 229 and 248). The second identical copy was deleted.
2. `onkeydown={handleAmountKeydown}` appeared twice on the amount `<Input>` element. The duplicate attribute was deleted.

## Tests

All 10 test suites pass after the fix (319 tests total). No new tests were added — the existing test suites already provided coverage and were previously blocked from running by the compile error.