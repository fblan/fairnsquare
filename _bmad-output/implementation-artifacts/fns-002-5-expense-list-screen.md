# Story FNS-002.5: Expense List Screen

Status: done

## Story

As a **split participant**,
I want **to see all expenses in a dedicated list and delete them**,
so that **I can manage expenses individually when needed**.

## Acceptance Criteria

1. **Given** I'm viewing the dashboard
   **When** I click the expense summary card (teal card showing count + total)
   **Then** I navigate to the Expense List screen (`/splits/{splitId}/expenses`)

2. **Given** I'm on the Expense List screen
   **When** the screen loads
   **Then** I see:
   - Header: Back button (left arrow) + "Expenses" title + Add button (+ icon)
   - Summary bar (light teal background): "X total expenses" + "€XXX.XX total"
   - List of expense cards below

3. **Given** there are no expenses
   **When** the Expense List screen loads
   **Then** I see an empty state:
   - Receipt icon (gray)
   - Text: "No expenses yet"
   - Subtext: "Tap + to add your first expense"

4. **Given** there are expenses
   **When** I view the expense list
   **Then** each expense card displays:
   - Description (bold) + Amount (right-aligned, teal)
   - "Paid by [Name]" + Date (small, right-aligned)
   - Split mode icon + text (🌙 By Night, ⊜ Equal, 📊 By Share)
   - Participants: "Everyone" or participant name list
   - Edit button (pencil icon, opens Edit Expense Modal — Story FNS-002-6)
   - Delete button (trash icon, shows confirmation dialog)
   **And** expenses are ordered reverse chronological (newest first)

5. **Given** I click the Delete button on an expense card
   **When** the confirmation dialog appears: "Delete this expense? This will recalculate balances."
   **Then** on confirm: expense is removed via API, list updates, totals recalculate, toast "Expense deleted"
   **And** on cancel: dialog closes, no action taken

6. **Given** I click Delete and the API fails
   **When** the error response is received
   **Then** I see a toast with the error detail and the expense remains in the list

7. **Given** I click the Add button (+) in the header
   **When** the Add Expense Modal opens
   **Then** no payer is pre-selected (unlike dashboard where participant card pre-selects)

8. **Given** I click the Back button (left arrow)
   **When** I'm on the Expense List screen
   **Then** I navigate back to the dashboard (`/splits/{splitId}`)

9. **Given** I successfully add or delete an expense
   **When** the operation completes
   **Then** the summary bar (count + total) updates immediately

## Tasks / Subtasks

- [x] Task 1: Create ExpenseList route and register in router (AC: 1, 8)
  - [x] 1.1: Create `fairnsquare-app/src/main/webui/src/routes/ExpenseList.svelte`
  - [x] 1.2: Add route `/splits/:splitId/expenses` → `ExpenseList` in `$lib/router.ts`
  - [x] 1.3: Make expense summary card in `Split.svelte` clickable → navigate to expense list
  - [x] 1.4: Back button in header navigates to `/splits/${splitId}`

- [x] Task 2: Implement screen layout and header (AC: 2)
  - [x] 2.1: Header with back arrow, "Expenses" title, Add (+) button
  - [x] 2.2: Summary bar: expense count + total amount (light teal bg)
  - [x] 2.3: Load split data via `getSplit(splitId)` on mount

- [x] Task 3: Implement empty state (AC: 3)
  - [x] 3.1: Receipt icon (gray, use lucide-svelte `Receipt` or similar)
  - [x] 3.2: "No expenses yet" heading
  - [x] 3.3: "Tap + to add your first expense" subtext

- [x] Task 4: Implement expense card list (AC: 4)
  - [x] 4.1: Expense card inline in ExpenseList.svelte (card component with edit/delete buttons)
  - [x] 4.2: Description + amount display
  - [x] 4.3: Payer name + date (relative or absolute formatting)
  - [x] 4.4: Split mode icon + text
  - [x] 4.5: Edit button (pencil icon — initially non-functional, wired in Story FNS-002-6)
  - [x] 4.6: Delete button (trash icon)
  - [x] 4.7: Reverse chronological ordering

- [x] Task 5: Implement delete expense flow (AC: 5, 6)
  - [x] 5.1: Add `deleteExpense(splitId, expenseId)` to `$lib/api/splits.ts` API client
  - [x] 5.2: **⚠️ Backend: Add `DELETE /api/splits/{splitId}/expenses/{expenseId}` endpoint**
  - [x] 5.3: **⚠️ Backend: Add `removeExpense(expenseId)` to Split domain + SplitUseCases**
  - [x] 5.4: ConfirmDialog on delete click: "Delete this expense? This will recalculate balances."
  - [x] 5.5: On confirm: API call, remove from list, update totals, toast "Expense deleted"
  - [x] 5.6: On error: toast with error, keep expense in list
  - [x] 5.7: Reload split data after delete to get recalculated shares

- [x] Task 6: Integrate Add Expense Modal (AC: 7)
  - [x] 6.1: Import AddExpenseModal
  - [x] 6.2: Add (+) button opens modal with `preselectedPayerId = null`
  - [x] 6.3: On success: reload split data, update list and summary

- [x] Task 7: Write comprehensive tests (all ACs)
  - [x] 7.1: Test route registration and navigation from dashboard
  - [x] 7.2: Test header layout (back button, title, add button)
  - [x] 7.3: Test summary bar shows correct count and total
  - [x] 7.4: Test empty state renders when no expenses
  - [x] 7.5: Test expense cards render with correct data
  - [x] 7.6: Test reverse chronological ordering
  - [x] 7.7: Test delete confirmation flow (confirm + cancel)
  - [x] 7.8: Test delete API error handling
  - [x] 7.9: Test Add Expense Modal opens from header
  - [x] 7.10: Test back button navigation
  - [x] 7.11: Backend integration tests for DELETE expense endpoint

## Dev Notes

### ⚠️ CRITICAL: Backend Work Required

The epic states "API Integrations (already exist per Architecture)" but **the backend does NOT have expense edit/delete endpoints**. Current backend state:

**Existing endpoints:**
- `POST /api/splits/{splitId}/expenses` ✅
- `POST /api/splits/{splitId}/expenses/by-night` ✅
- `POST /api/splits/{splitId}/expenses/equal` ✅

**Missing endpoints (must be created in this story):**
- `DELETE /api/splits/{splitId}/expenses/{expenseId}` ❌
- The Split domain model has no `removeExpense()` method
- SplitUseCases has no `removeExpense()` or `deleteExpense()` method

**Backend implementation needed:**
1. Add `removeExpense(Expense.Id)` to `Split.java` domain
2. Add `removeExpense(String splitId, String expenseId)` to `SplitUseCases.java`
3. Add `DELETE /{splitId}/expenses/{expenseId}` to `SplitResource.java`
4. Add `ExpenseNotFoundError` to domain errors
5. Add integration tests for delete expense

### Current State Analysis

**ExpenseCard.svelte (existing component):**
- Location: `fairnsquare-app/src/main/webui/src/lib/components/ui/expense-card/ExpenseCard.svelte`
- Displays expense with expand/collapse for share breakdown
- Props: `expense`, `split`, `expanded`, `onToggle`
- **Does NOT have edit/delete buttons** — those need to be added or a new card component created

**ExpensesSection.svelte (current inline expense management):**
- Location: `fairnsquare-app/src/main/webui/src/routes/ExpensesSection.svelte`
- Displays expense list inline on dashboard using ExpenseCard
- Has add expense form inline
- **Does NOT have delete functionality**

**Router (needs update):**
- Location: `fairnsquare-app/src/main/webui/src/lib/router.ts`
- Currently: `/` → Home, `/splits/:splitId` → Split
- **Add:** `/splits/:splitId/expenses` → ExpenseList

**API Client (needs deleteExpense):**
- Location: `fairnsquare-app/src/main/webui/src/lib/api/splits.ts`
- Has: `createSplit`, `getSplit`, `addParticipant`, `updateParticipant`, `deleteParticipant`, `addExpense`
- **Missing:** `deleteExpense(splitId, expenseId)` function

### Implementation Approach

**Frontend:**
1. Create `ExpenseList.svelte` route component
2. Register new route in `router.ts`
3. Make expense summary card in Split.svelte clickable (navigate)
4. Create expense list card with edit/delete buttons (extend or replace ExpenseCard)
5. Add `deleteExpense` to API client
6. Integrate AddExpenseModal (reuse from FNS-002-3)

**Backend:**
1. Add `removeExpense(Expense.Id)` to `Split.java`:
   ```java
   public void removeExpense(Expense.Id expenseId) {
       boolean removed = expenses.removeIf(e -> e.getId().equals(expenseId));
       if (!removed) {
           throw new ExpenseNotFoundError(expenseId);
       }
   }
   ```
2. Add `removeExpense` to `SplitUseCases.java`
3. Add `DELETE /{splitId}/expenses/{expenseId}` to `SplitResource.java` returning 204
4. Create `ExpenseNotFoundError` extending `BaseError`
5. Add Quarkus integration test in existing test file

### Date Formatting

```typescript
function formatDate(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffHours = diffMs / (1000 * 60 * 60);

  if (diffHours < 1) return 'Just now';
  if (diffHours < 24) return `${Math.floor(diffHours)}h ago`;
  if (diffHours < 48) return 'Yesterday';

  return date.toLocaleDateString('en-IE', { month: 'short', day: 'numeric' });
}
```

### Split Mode Icons

```typescript
function splitModeIcon(mode: SplitMode): string {
  switch (mode) {
    case 'BY_NIGHT': return '🌙';
    case 'EQUAL': return '⊜';
    case 'FREE': return '📊';
  }
}
```

### Svelte 5 Patterns (MUST FOLLOW)

```typescript
// Route component
import { route, navigate } from '$lib/router';
const splitId = $derived(route.params.splitId || '');

let split = $state<Split | null>(null);
let isLoading = $state(true);

const sortedExpenses = $derived(
  split?.expenses
    ? [...split.expenses].sort((a, b) =>
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      )
    : []
);

const expenseCount = $derived(sortedExpenses.length);
const expenseTotal = $derived(sortedExpenses.reduce((sum, e) => sum + e.amount, 0));
```

### Accessibility Requirements

- Back button: `aria-label="Back to dashboard"`
- Add button: `aria-label="Add expense"`
- Delete button per card: `aria-label="Delete expense: [description]"`
- Edit button per card: `aria-label="Edit expense: [description]"`
- Touch targets: 44px minimum
- Empty state: semantic heading structure

### Previous Story Learnings (from FNS-002-3)

1. ✅ AddExpenseModal reusable — import and use with `preselectedPayerId = null`
2. ✅ ConfirmDialog proven for delete confirmations
3. ✅ Toast notifications via `addToast()` for success/error feedback
4. ✅ `$derived` for computed values (counts, totals, sorting)
5. ✅ Split.svelte reload pattern: call `loadSplit(splitId)` after mutations
6. ⚠️ Backend integration tests are the primary testing strategy (>90% coverage target)

### Project Structure Notes

**New files:**
- `fairnsquare-app/src/main/webui/src/routes/ExpenseList.svelte`
- `fairnsquare-app/src/main/webui/src/routes/ExpenseList.test.ts`
- `fairnsquare-app/src/main/java/.../split/domain/ExpenseNotFoundError.java`

**Modified files:**
- `fairnsquare-app/src/main/webui/src/lib/router.ts` — Add expense list route
- `fairnsquare-app/src/main/webui/src/lib/api/splits.ts` — Add deleteExpense function
- `fairnsquare-app/src/main/webui/src/routes/Split.svelte` — Make summary card clickable
- `fairnsquare-app/src/main/java/.../split/domain/Split.java` — Add removeExpense method
- `fairnsquare-app/src/main/java/.../split/service/SplitUseCases.java` — Add removeExpense use case
- `fairnsquare-app/src/main/java/.../split/api/SplitResource.java` — Add DELETE expense endpoint
- Backend test files — Add delete expense integration tests

### References

- [Source: _bmad-output/implementation-artifacts/epic-FNS-002-participant-centric-dashboard.md#Story 5]
- [Source: fairnsquare-app/src/main/webui/src/lib/components/ui/expense-card/ExpenseCard.svelte] — Existing expense display
- [Source: fairnsquare-app/src/main/webui/src/routes/ExpensesSection.svelte] — Current inline expense management
- [Source: fairnsquare-app/src/main/webui/src/lib/router.ts] — Router config to extend
- [Source: fairnsquare-app/src/main/webui/src/lib/api/splits.ts] — API client to extend
- [Source: fairnsquare-app/src/main/java/.../split/api/SplitResource.java] — Backend endpoints (missing DELETE)
- [Source: fairnsquare-app/src/main/java/.../split/domain/Split.java] — Domain model (missing removeExpense)
- [Source: _bmad-output/project-context.md] — Integration testing strategy, Quarkus patterns

## Dev Agent Record

### Agent Model Used
Claude Sonnet 4.5 (claude-sonnet-4-5-20250929)

### Debug Log References
- All 147 backend tests pass (20 in ExpenseUseCaseTest, 6 new for delete expense)
- All 152 frontend tests pass (18 new in ExpenseList.test.ts)
- Zero regressions

### Completion Notes List
- Task 1: Created ExpenseList.svelte, registered route `/splits/:splitId/expenses`, made expense summary card clickable in Split.svelte, back button navigates to dashboard
- Task 2: Header with ArrowLeft back button, "Expenses" title, Plus add button. Summary bar with teal background showing count + total
- Task 3: Empty state with Receipt icon (lucide-svelte), "No expenses yet" heading, "Tap + to add your first expense" subtext
- Task 4: Expense cards inline in ExpenseList.svelte showing description, amount (teal), payer name, date (relative formatting), split mode icon+text, participant names ("Everyone" when all participate), edit button (non-functional placeholder for FNS-002-6), delete button. Reverse chronological ordering via $derived sort
- Task 5: Full delete expense flow — frontend deleteExpense API function, ConfirmDialog, toast notifications. Backend: ExpenseNotFoundError + InvalidExpenseIdError error classes, Split.removeExpense() domain method, SplitUseCases.removeExpense() service method, DELETE endpoint in SplitResource returning 204
- Task 6: AddExpenseModal integrated with preselectedPayerId=null, reloads split data on success
- Task 7: 18 frontend tests covering all ACs + 6 backend integration tests for DELETE expense endpoint
- Decision: Expense cards implemented inline in ExpenseList.svelte rather than creating separate ExpenseListCard.svelte component — keeps it simple and avoids unnecessary abstraction for a single use case

### File List

**New files:**
- `fairnsquare-app/src/main/webui/src/routes/ExpenseList.svelte`
- `fairnsquare-app/src/main/webui/src/routes/ExpenseList.test.ts`
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/ExpenseNotFoundError.java`
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/InvalidExpenseIdError.java`

**Modified files:**
- `fairnsquare-app/src/main/webui/src/lib/router.ts` — Added `/splits/:splitId/expenses` route
- `fairnsquare-app/src/main/webui/src/lib/api/splits.ts` — Added `deleteExpense()` function
- `fairnsquare-app/src/main/webui/src/routes/Split.svelte` — Made expense summary card clickable (navigates to expense list)
- `fairnsquare-app/src/main/webui/src/routes/Split.test.ts` — Updated tests for clickable expense summary card
- `fairnsquare-app/src/main/webui/package.json` — Updated dependencies (test framework)
- `fairnsquare-app/src/main/webui/package-lock.json` — Lock file update
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/Split.java` — Added `removeExpense(Expense.Id)` method
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/service/SplitUseCases.java` — Added `removeExpense(splitId, expenseId)` method
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/api/SplitResource.java` — Added `DELETE /{splitId}/expenses/{expenseId}` endpoint with OpenAPI annotations
- `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/api/ExpenseUseCaseTest.java` — Added 6 delete expense integration tests
- `_bmad-output/implementation-artifacts/sprint-status.yaml` — Updated story status to review

### Code Review Fixes Applied
- Added OpenAPI annotations (`@Operation`, `@APIResponse`) to DELETE expense endpoint for API documentation consistency
- Improved error handling type safety in ExpenseList.svelte (changed `err: any` to `err: unknown` with proper ApiError casting)
- Added defensive validation in `handleDeleteClick()` to check expense validity before opening delete dialog
- Added future date handling in `formatDate()` function for defensive coding
- Updated File List to include all git-modified files (package.json, package-lock.json, Split.test.ts, sprint-status.yaml)

## Senior Developer Review (AI)

**Review Date:** 2026-02-07
**Reviewer:** Claude Sonnet 4.5 (Code Review Agent)
**Review Outcome:** ✅ **APPROVED** (with fixes applied)

### Summary
Story FNS-002-5 implemented successfully with comprehensive test coverage (18 frontend tests + 6 backend integration tests). All 9 acceptance criteria verified. All HIGH and MEDIUM issues identified during adversarial review were fixed automatically.

### Action Items
- [x] **[MEDIUM]** Add OpenAPI annotations to DELETE endpoint - FIXED (SplitResource.java:190-193)
- [x] **[MEDIUM]** Improve error handling type safety - FIXED (ExpenseList.svelte:138-142)
- [x] **[MEDIUM]** Add defensive validation in delete handler - FIXED (ExpenseList.svelte:124-129)
- [x] **[MEDIUM]** Handle future dates in formatDate function - FIXED (ExpenseList.svelte:79-92)
- [x] **[MEDIUM]** Update File List to include all modified files - FIXED (story file updated)
- [x] **[MEDIUM]** Document all git-tracked changes - FIXED (added package.json, Split.test.ts, sprint-status.yaml to File List)

**Total Issues:** 6 Medium, 3 Low
**Issues Fixed:** 6 Medium (100%)
**Tests After Fixes:** 299 total (147 backend + 152 frontend) — ALL PASSING ✅

### Strengths
- Clean domain-driven design with proper separation of concerns
- Comprehensive test coverage covering all ACs and error paths
- Proper use of Svelte 5 runes patterns
- Backend follows established error handling patterns
- Security review clean (no injection risks, XSS, CSRF issues)

### Change Log
- 2026-02-07: Implemented Expense List Screen (FNS-002-5) — full frontend + backend with comprehensive tests
- 2026-02-07: Code review fixes applied — OpenAPI annotations, type safety improvements, defensive validation, complete file list documentation
