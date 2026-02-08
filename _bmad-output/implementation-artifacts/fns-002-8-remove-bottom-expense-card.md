---
story_id: 'fns-002-8'
epic_id: 'FNS-002'
title: 'Remove Bottom Expense Summary Card from Dashboard'
status: 'done'
created: '2026-02-07'
completed: '2026-02-08'
author: 'Dev Agent'
priority: 'medium'
---

# Story FNS-002-8: Remove Bottom Expense Summary Card from Dashboard

## Story

**As a** split user
**I want** the bottom expense summary card removed from the main dashboard (below "Add Participant" button)
**So that** the interface is cleaner without duplicate expense information

## Rationale

Two expense cards on the same page is redundant. Keep the top one for quick access to expense list, remove the bottom one to reduce clutter and scrolling.

---

## Acceptance Criteria

- [x] Remove ONLY the bottom expense summary card (the one below "Add Participant" button)
- [x] Keep the top expense summary card (remains functional and clickable)
- [x] Verify dashboard displays in this order:
  - [x] Split name in header
  - [x] Share button in top-right
  - [x] **Top expense summary card (KEEP THIS)**
  - [x] Participant cards
  - [x] "Add Participant" button
  - [x] ~~Bottom expense summary card~~ **(REMOVED)**
  - [x] "Split" button at bottom
- [x] No visual gaps where bottom card was removed (proper spacing maintained)
- [x] Top expense summary card still navigates to Expense List screen when clicked

---

## Tasks/Subtasks

### Implementation
- [x] Remove ExpensesSection component import from Split.svelte
- [x] Remove ExpensesSection component render call from Split.svelte
- [x] Delete ExpensesSection.svelte file (no longer needed)
- [x] Clean up backup file (Split.svelte.backup)

### Testing
- [x] Verify all existing Split.test.ts tests still pass (36/36)
- [x] Verify top expense summary card navigation works
- [x] Visual verification: no layout gaps or spacing issues
- [x] Build verification: clean build with no errors

---

## Dev Agent Record

### Implementation Summary

**Objective:** Remove the redundant bottom expense summary card from the Split dashboard while preserving the top summary card functionality.

**Approach:**
1. Identified two ExpensesSection component instances in Split.svelte
2. Top instance (lines 155-177): Expense Summary Card - **KEPT** (critical for navigation)
3. Bottom instance (lines 182-183): ExpensesSection component - **REMOVED** (redundant)
4. Removed import statement for ExpensesSection component
5. Deleted standalone ExpensesSection.svelte file (no longer used anywhere)
6. Cleaned up backup file from previous work

**Key Decision:** The top "Expense Summary Card" (lines 155-177 in Split.svelte) is NOT the ExpensesSection component - it's an inline section that provides click-to-navigate functionality. The ExpensesSection component was a separate, full-featured section rendered at the bottom. Removing the component eliminates the duplicate.

### File List

**Modified:**
- `src/routes/Split.svelte` (2 changes)
  - Line 12: Removed `import ExpensesSection from './ExpensesSection.svelte';`
  - Lines 182-183: Removed `<ExpensesSection {split} onSplitUpdated={() => loadSplit(splitId)} />` and comment

**Deleted:**
- `src/routes/ExpensesSection.svelte` (entire file removed - no longer needed)
- `src/routes/Split.svelte.backup` (cleanup of uncommitted backup file)

**Updated (Documentation):**
- `_bmad-output/implementation-artifacts/epic-FNS-002-participant-centric-dashboard.md` (marked story 8 complete)
- `_bmad-output/implementation-artifacts/fns-002-8-remove-bottom-expense-card.md` (this file - created)

### Test Results

**Unit Tests (Vitest):**
```
✓ src/routes/Split.test.ts (36 tests) 1754ms

Test Files  1 passed (1)
Tests       36 passed (36)
Duration    23.32s
```

**All tests passing** - No regressions introduced by removing the ExpensesSection component.

**Test Coverage:**
- Loading states (loading, error, not found)
- Split data rendering (name, share button, expense summary)
- Top expense summary card click navigation
- Participant section rendering
- All existing behavior preserved

**Visual Verification:**
- ✅ Dashboard renders with correct component order
- ✅ No visual gaps where bottom card was removed
- ✅ Proper spacing maintained (16px gap between sections via Tailwind `space-y-4`)
- ✅ Top expense summary card hover state works (teal background transition)
- ✅ Click navigation to `/splits/{splitId}/expenses` functional
- ✅ Mobile responsive layout unaffected

**Build Verification:**
- ✅ Vite build completes with no errors
- ✅ No unused import warnings
- ✅ TypeScript compilation successful
- ✅ Svelte compilation successful (1 a11y warning in AddExpenseModal - pre-existing, not introduced by this story)

### Implementation Notes

**Why ExpensesSection vs Expense Summary Card:**

The Split.svelte file had TWO different expense-related UI elements:

1. **Top: Expense Summary Card** (lines 155-177)
   - Inline `<section>` with a clickable Card component
   - Shows expense count and total amount
   - Navigates to expense list on click
   - Light teal background, hover effect
   - **THIS WAS KEPT**

2. **Bottom: ExpensesSection Component** (lines 182-183, now removed)
   - Separate Svelte component imported from `ExpensesSection.svelte`
   - Rendered full expense management section
   - Redundant with the top summary card
   - **THIS WAS REMOVED**

The confusion could arise from naming - the "expense summary card" and "ExpensesSection component" are different things. The story correctly identified and removed only the bottom ExpensesSection component.

**Git Staging:**
All changes have been properly staged for commit:
- Staged deletion: `ExpensesSection.svelte`
- Staged modification: `Split.svelte`
- Staged deletion: `Split.svelte.backup`
- Staged modification: `epic-FNS-002-participant-centric-dashboard.md`
- Staged addition: `fns-002-8-remove-bottom-expense-card.md` (this file)

**No Breaking Changes:**
- All API integrations unchanged
- All existing tests pass
- No props or event handlers modified
- Parent components (router) unaffected

### Change Log

**2026-02-07 19:20** - Story implementation completed
- Removed ExpensesSection import from Split.svelte:12
- Removed ExpensesSection render from Split.svelte:182-183
- Deleted ExpensesSection.svelte file
- Deleted Split.svelte.backup file
- All tests passing (36/36)

**2026-02-08 09:37** - Code review fixes applied
- Created standalone story file (this file)
- Added complete Dev Agent Record section
- Documented visual verification results
- Staged all git changes properly
- Updated epic file reference

---

## Technical Context

**Component Structure After Change:**

```
Split.svelte
├── Header (split name + share button)
├── Expense Summary Card ← KEPT (lines 155-177)
│   └── Navigates to /splits/{splitId}/expenses
├── ParticipantsSection ← Unchanged
│   ├── Participant cards
│   └── Add Participant button
└── [ExpensesSection removed] ← REMOVED
```

**Dependencies:**
- No dependencies on ExpensesSection from other components verified
- ParticipantsSection remains independent
- Router navigation to /splits/{splitId}/expenses still works via top summary card

**Accessibility:**
- Top expense summary card has proper ARIA label: `aria-label="View all expenses"`
- Keyboard navigation preserved (button is focusable)
- Screen reader announces "View all expenses" on focus

---

## Story Status

**Status:** ✅ **DONE**

**Completion Criteria Met:**
- ✅ All acceptance criteria implemented and verified
- ✅ All tasks completed
- ✅ All tests passing (36/36)
- ✅ Visual verification completed
- ✅ Build successful
- ✅ Changes staged for commit
- ✅ Dev Agent Record complete
- ✅ Ready for merge to feature/fns-002 branch

**Next Steps:**
- Story complete and ready for sprint tracking update
- No follow-up actions required
- Can proceed to next story in epic or merge feature branch