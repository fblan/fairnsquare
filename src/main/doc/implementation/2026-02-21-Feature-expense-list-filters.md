# Feature: Expense List Filtering by Payer and Beneficiary

## What, Why and Constraints

**What**: Added URL query parameter-based filtering to the expense list, allowing filtering by payer (`?payer=<participantId>`) and/or beneficiary (`?beneficiary=<participantId>`).

**Why**: Users need to quickly find expenses related to specific participants. By using URL query parameters, filters are shareable and linkable from other parts of the application (e.g., navigating from a participant summary directly to their expenses).

**Constraints**:
- Frontend-only implementation — no backend changes required
- Follows existing URL parameter patterns from `Participants.svelte` (`route.search` + `history.replaceState`)
- FREE mode filtering respects active shares only (shares with `parts > 0`)
- Used native `<select>` elements for the filter bar (easier to test and more accessible than custom Select component)

## How

### Files Modified

1. **`fairnsquare-app/src/main/webui/src/routes/ExpenseList.svelte`**
   - Added `selectedPayer` and `selectedBeneficiary` as `$state` variables, initialized from `route.search` URL params. Using `$state` instead of `$derived(route.search)` because `route` is an external object whose mutations are not tracked by Svelte 5's reactivity system
   - Added `filteredExpenses` derived value that applies payer and beneficiary filters on `sortedExpenses`
   - Added `totalExpenseCount` for "X of Y" display in summary bar
   - Added `updateFilterUrl()`, `handlePayerFilterChange()`, `handleBeneficiaryFilterChange()`, and `clearFilters()` helper functions
   - Added filter bar section with two `<select>` dropdowns (Payer / Beneficiary) and a clear filters button (X icon)
   - Updated summary bar to show "X of Y expenses" when filters are active
   - Updated expense card loop to iterate over `filteredExpenses`
   - Added filter-aware empty state: "No matching expenses" with "Clear filters" button (distinct from the "No expenses yet" state)

2. **`fairnsquare-app/src/main/webui/src/routes/ExpenseList.test.ts`**
   - Added `mockSplitForFilters` test fixture with 3 participants and 3 expenses with varied payer/beneficiary combinations
   - Reset `route.search` in `beforeEach` to prevent test pollution
   - Added 13 new tests in a `Filtering` describe block

### URL Format

- `/splits/:splitId/expenses?payer=p1` — expenses paid by participant p1
- `/splits/:splitId/expenses?beneficiary=p2` — expenses where p2 is a beneficiary
- `/splits/:splitId/expenses?payer=p1&beneficiary=p2` — combined filters

## Tests

**Test file**: `src/routes/ExpenseList.test.ts`

**13 new tests** added in the `Filtering` describe block:

| # | Test | What it covers |
|---|------|---------------|
| 1 | renders filter bar with payer and beneficiary selects | Filter bar UI renders |
| 2 | shows all expenses when no filters are set | Default unfiltered state |
| 3 | filters by payer via URL query param | Payer filter via `route.search` |
| 4 | filters by beneficiary via URL query param | Beneficiary filter via `route.search` |
| 5 | filters by both payer and beneficiary combined | Combined filter logic |
| 6 | updates summary bar to show filtered count | Summary shows "X of Y" |
| 7 | shows "No matching expenses" when filter has no results | Filtered empty state |
| 8 | shows clear filters button when filters are active | Clear button visibility |
| 9 | does not show clear filters button when no filters active | Clear button hidden |
| 10 | updates URL when payer select is changed | Payer select updates URL via replaceState |
| 11 | updates URL when beneficiary select is changed | Beneficiary select updates URL via replaceState |
| 12 | payer select reflects URL param value | Select syncs with URL param |
| 13 | handles FREE mode beneficiary filtering correctly | Inactive shares (parts=0) excluded |

**Total**: 31 tests (18 existing + 13 new), all passing. Full suite: 315 tests, all passing.
