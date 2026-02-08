# Story FNS-002.6: Edit Expense Modal

Status: done

## Story

As a **split participant**,
I want **to edit an existing expense's details**,
so that **I can fix mistakes or update amounts, payers, or split modes**.

## Acceptance Criteria

1. **Given** I'm on the Expense List screen
   **When** I click the Edit button (pencil icon) on an expense card
   **Then** the Edit Expense Modal opens centered with overlay
   **And** all form fields are pre-filled with the expense's current data

2. **Given** the Edit Expense Modal is open
   **When** the modal loads
   **Then** I see:
   - Title: "Edit Expense"
   - Description input (optional, pre-filled, max 100 chars)
   - Amount input (required, pre-filled, min €0.01, max €999,999.99)
   - Payer dropdown (required, pre-filled with current payer)
   - Split Mode radio (pre-filled with current mode: By Night, Equal)
   - Footer: Cancel (outline) + Save Changes (primary teal, disabled until dirty and valid)
   - Close button (X) in header
   - "Delete Expense" button (red, destructive variant, below form)

3. **Given** I have NOT changed any field values
   **When** I look at the Save Changes button
   **Then** it is disabled (form is not dirty)

4. **Given** I change any field to a valid value
   **When** I click "Save Changes"
   **Then** the system calls `PUT /api/splits/{splitId}/expenses/{expenseId}`
   **And** on success: modal closes, expense list updates, balances recalculate, toast "Expense updated"
   **And** on error: toast with error detail, modal stays open, form data preserved

5. **Given** I enter an invalid amount (< €0.01 or > €999,999.99) or description > 100 chars
   **When** validation runs
   **Then** I see inline validation errors below the affected fields

6. **Given** I click "Delete Expense" in the modal
   **When** the confirmation dialog appears: "Delete this expense? This will recalculate balances."
   **Then** on confirm: expense deleted via API, modal closes, list updates, toast "Expense deleted"
   **And** on cancel: dialog closes, stay in modal

7. **Given** I have made changes (dirty form)
   **When** I click Close (X), Cancel, or press Escape
   **Then** I see "Discard changes?" confirmation
   **And** confirm: modal closes | cancel: stay in modal

8. **Given** I have NOT made changes (pristine)
   **When** I click Close (X), Cancel, or Escape
   **Then** modal closes immediately

## Tasks / Subtasks

- [x] Task 1: Create EditExpenseModal.svelte component (AC: 1, 2)
  - [x] 1.1: Create `$lib/components/expense/EditExpenseModal.svelte`
  - [x] 1.2: Props: `open`, `splitId`, `expense`, `participants`, `onClose`, `onSuccess`
  - [x] 1.3: Modal structure identical to AddExpenseModal (overlay, header, form, footer)
  - [x] 1.4: Pre-fill all fields from `expense` prop
  - [x] 1.5: Focus trap and Escape key handling (same as AddExpenseModal)

- [x] Task 2: Implement form with dirty tracking (AC: 2, 3, 5)
  - [x] 2.1: `$state` for form values initialized from expense prop
  - [x] 2.2: `$derived` for `isDirty` comparing each field against original
  - [x] 2.3: `$derived` for `isValid` (amount, description length, payer selected)
  - [x] 2.4: Save Changes disabled when `!isDirty || !isValid || isLoading`
  - [x] 2.5: Inline validation on blur and submit attempt

- [x] Task 3: Implement update expense API (AC: 4)
  - [x] 3.1: Add `updateExpense(splitId, expenseId, request)` to `$lib/api/splits.ts`
  - [x] 3.2: **⚠️ Backend: Add `PUT /api/splits/{splitId}/expenses/{expenseId}` endpoint**
  - [x] 3.3: **⚠️ Backend: Add `updateExpense()` to Split domain + SplitUseCases**
  - [x] 3.4: On success: `onSuccess()`, toast "Expense updated", close modal
  - [x] 3.5: On error: toast with error, keep modal open

- [x] Task 4: Implement delete from modal (AC: 6)
  - [x] 4.1: "Delete Expense" button below form (destructive variant)
  - [x] 4.2: Reuse ConfirmDialog: "Delete this expense? This will recalculate balances."
  - [x] 4.3: On confirm: call `deleteExpense(splitId, expenseId)` (from Story FNS-002-5)
  - [x] 4.4: On success: `onSuccess()`, toast "Expense deleted", close modal
  - [x] 4.5: On error: toast with error

- [x] Task 5: Implement dirty form confirmation (AC: 7, 8)
  - [x] 5.1: Same pattern as AddExpenseModal / EditParticipantModal

- [x] Task 6: Integrate into ExpenseList screen (AC: 1)
  - [x] 6.1: Import EditExpenseModal into ExpenseList.svelte
  - [x] 6.2: Wire Edit button on expense cards → open modal with selected expense
  - [x] 6.3: On success: reload split data

- [x] Task 7: Write comprehensive tests (all ACs)
  - [x] 7.1: Test modal opens with pre-filled data
  - [x] 7.2: Test Save button disabled when pristine
  - [x] 7.3: Test successful update flow
  - [x] 7.4: Test API error handling
  - [x] 7.5: Test validation errors
  - [x] 7.6: Test delete from modal
  - [x] 7.7: Test dirty form confirmation
  - [x] 7.8: Test Escape and close behaviors
  - [x] 7.9: Backend integration tests for PUT expense endpoint

## Dev Notes

### ⚠️ CRITICAL: Backend Work Required

**Missing backend endpoints (must be created in this story):**
- `PUT /api/splits/{splitId}/expenses/{expenseId}` ❌

**Backend implementation needed:**
1. Add `UpdateExpenseRequest` record to domain (amount, description, payerId, splitMode)
2. Add `updateExpense(Expense.Id, ...)` to `Split.java` domain:
   - Find expense by ID, replace with updated version
   - Recalculate shares based on new split mode / amount / participants
   - Throw `ExpenseNotFoundError` if not found (created in Story FNS-002-5)
3. Add `updateExpense(String splitId, String expenseId, UpdateExpenseRequest)` to `SplitUseCases.java`
4. Add `PUT /{splitId}/expenses/{expenseId}` to `SplitResource.java` returning 200 with updated expense DTO
5. Handle split mode change: when mode changes, recalculate all shares for the expense
6. Add integration tests

**Expense subclass complexity:**
- The codebase uses sealed abstract `Expense` with subclasses: `ExpenseByNight`, `ExpenseEqual`
- Updating split mode means creating a NEW subclass instance (not mutating)
- `Split.updateExpense()` must remove old expense and add new one (correct subclass)
- Share recalculation happens in the subclass constructor / factory

**NOTE:** If Story FNS-002-5 is implemented first, `deleteExpense` API + `ExpenseNotFoundError` will already exist. Coordinate with FNS-002-5.

### Current State Analysis

**AddExpenseModal.svelte (pattern to follow):**
- Identical form structure: description, amount, payer, split mode
- Same validation rules, dirty tracking, confirmation dialogs
- **Difference:** Pre-fill from existing expense instead of empty form
- **Difference:** "Save Changes" instead of "Add Expense"
- **Addition:** "Delete Expense" button below form

**Expense domain model (sealed hierarchy):**
```
Expense (sealed abstract)
├── ExpenseByNight — shares calculated by participant nights
└── ExpenseEqual — shares calculated equally
```
- Updating split mode = creating new subclass instance
- Shares auto-calculated in subclass constructors

**API client needs:**
```typescript
export interface UpdateExpenseRequest {
  amount: number;
  description: string;
  payerId: string;
  splitMode: SplitMode;
}

export async function updateExpense(
  splitId: string,
  expenseId: string,
  request: UpdateExpenseRequest
): Promise<Expense> {
  return apiRequest<Expense>(`/splits/${splitId}/expenses/${expenseId}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  });
}
```

### Svelte 5 Patterns (MUST FOLLOW)

```typescript
let {
  open, splitId, expense, participants, onClose, onSuccess,
}: {
  open: boolean;
  splitId: string;
  expense: Expense | null;
  participants: Participant[];
  onClose: () => void;
  onSuccess: () => Promise<void>;
} = $props();

let amount = $state<number | ''>('');
let description = $state('');
let payerId = $state('');
let splitMode = $state<SplitMode>('BY_NIGHT');

// Initialize from expense on open
$effect(() => {
  if (open && expense) {
    amount = expense.amount;
    description = expense.description;
    payerId = expense.payerId;
    splitMode = expense.splitMode;
    // Reset payer select display
    const payer = participants.find(p => p.id === expense.payerId);
    if (payer) payerSelected = { value: payer.id, label: payer.name };
  }
});

// Dirty tracking against original expense values
const isDirty = $derived(
  expense != null && (
    (typeof amount === 'number' ? amount : 0) !== expense.amount ||
    description !== expense.description ||
    payerId !== expense.payerId ||
    splitMode !== expense.splitMode
  )
);
```

### Code Reuse Strategy

This modal is nearly identical to AddExpenseModal. Consider:
1. **Option A (recommended):** Copy AddExpenseModal, modify for edit mode
   - Simpler, avoids complex conditional logic
   - Each modal is self-contained and easy to maintain
2. **Option B:** Refactor AddExpenseModal to support both add/edit modes via props
   - More DRY but adds complexity
   - Could be done as future refactoring

Choose Option A for this story — keep it simple and working.

### Accessibility Requirements

- Same as AddExpenseModal: focus trap, Escape key, ARIA attributes
- Delete button: `aria-label="Delete this expense"`
- Touch targets: 44px minimum

### Previous Story Learnings

1. ✅ AddExpenseModal pattern is proven — clone and adapt
2. ✅ ConfirmDialog works for delete confirmations
3. ✅ Focus trap via manual Tab key handling
4. ✅ Dirty form tracking with `$derived`
5. ⚠️ Expense sealed hierarchy: split mode change = new subclass (backend complexity)
6. ⚠️ Share recalculation must happen server-side (don't calculate on frontend)

### Project Structure Notes

**New files:**
- `fairnsquare-app/src/main/webui/src/lib/components/expense/EditExpenseModal.svelte`
- `fairnsquare-app/src/main/webui/src/lib/components/expense/EditExpenseModal.test.ts`
- `fairnsquare-app/src/main/java/.../split/domain/UpdateExpenseRequest.java` (if not already)

**Modified files:**
- `fairnsquare-app/src/main/webui/src/lib/api/splits.ts` — Add updateExpense function
- `fairnsquare-app/src/main/webui/src/routes/ExpenseList.svelte` — Wire edit button to modal
- `fairnsquare-app/src/main/java/.../split/domain/Split.java` — Add updateExpense method
- `fairnsquare-app/src/main/java/.../split/service/SplitUseCases.java` — Add updateExpense
- `fairnsquare-app/src/main/java/.../split/api/SplitResource.java` — Add PUT expense endpoint
- `fairnsquare-app/src/main/java/.../expense/api/mapper/ExpenseMapper.java` — May need update for response

### Dependencies

- **Story FNS-002-5 should be completed first** — it creates:
  - ExpenseList.svelte (where edit button lives)
  - `deleteExpense` API function
  - `DELETE /api/splits/{splitId}/expenses/{expenseId}` backend endpoint
  - `ExpenseNotFoundError`
  - `removeExpense` in Split domain

### References

- [Source: _bmad-output/implementation-artifacts/epic-FNS-002-participant-centric-dashboard.md#Story 6]
- [Source: fairnsquare-app/src/main/webui/src/lib/components/expense/AddExpenseModal.svelte] — Clone and adapt
- [Source: fairnsquare-app/src/main/webui/src/lib/api/splits.ts] — API client to extend
- [Source: fairnsquare-app/src/main/java/.../split/domain/Split.java] — Domain model
- [Source: fairnsquare-app/src/main/java/.../split/domain/Expense.java] — Sealed expense hierarchy
- [Source: fairnsquare-app/src/main/java/.../split/domain/ExpenseByNight.java] — Subclass pattern
- [Source: fairnsquare-app/src/main/java/.../split/domain/ExpenseEqual.java] — Subclass pattern
- [Source: _bmad-output/project-context.md] — Testing strategy, architecture rules

## Dev Agent Record

### Agent Model Used
- Claude Sonnet 4.5 (claude-sonnet-4-5-20250929)

### Debug Log References
- N/A

### Completion Notes List
- **Frontend Implementation:** Created EditExpenseModal.svelte following AddExpenseModal pattern with pre-filled fields, dirty tracking, validation, and delete capability
- **Dirty Tracking:** Implemented using $derived comparing all form fields (amount, description, payerId, splitMode) against original expense values
- **Backend Implementation:** Added PUT /api/splits/{splitId}/expenses/{expenseId} endpoint with full CRUD support
- **Domain Logic:** Implemented updateExpense in Split.java using Expense.fromJson to create correct subclass (ExpenseByNight/ExpenseEqual) based on split mode
- **Split Mode Changes:** Handled sealed expense hierarchy - updating split mode creates new subclass instance with recalculated shares
- **Delete from Modal:** Reused existing deleteExpense API with confirmation dialog
- **Tests:** Added 43 frontend tests (all passing) and 9 backend integration tests (all passing)
- **Total Test Coverage:** 350 tests passing (195 frontend + 155 backend)
- **Code Review Fixes Applied:**
  - Fixed description validation: Backend changed from 200 to 100 char limit (aligned with frontend and AC5)
  - Made description optional: Removed @NotBlank from UpdateExpenseRequest (aligned with AC2 and frontend)
  - Documented cross-story file changes: Added section clarifying files modified by FNS-002-1 and FNS-002-2

### File List

**New Files:**
- fairnsquare-app/src/main/webui/src/lib/components/expense/EditExpenseModal.svelte
- fairnsquare-app/src/main/webui/src/lib/components/expense/EditExpenseModal.test.ts
- fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/UpdateExpenseRequest.java

**Modified Files:**
- fairnsquare-app/src/main/webui/src/lib/api/splits.ts
- fairnsquare-app/src/main/webui/src/routes/ExpenseList.svelte
- fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/Split.java
- fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/service/SplitUseCases.java
- fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/api/SplitResource.java
- fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/api/ExpenseUseCaseTest.java
- _bmad-output/implementation-artifacts/sprint-status.yaml

**Files Modified by Other Stories (not part of this story):**
- fairnsquare-app/src/main/webui/package.json (FNS-002-1 dependency changes)
- fairnsquare-app/src/main/webui/package-lock.json (FNS-002-1 dependency changes)
- fairnsquare-app/src/main/webui/src/lib/router.ts (FNS-002-1 routing changes)
- fairnsquare-app/src/main/webui/src/routes/Home.svelte (FNS-002-1 create flow refactor)
- fairnsquare-app/src/main/webui/src/routes/Home.test.ts (FNS-002-1 test updates)
- fairnsquare-app/src/main/webui/src/routes/ParticipantsSection.svelte (FNS-002-2 dashboard)
- fairnsquare-app/src/main/webui/src/routes/Split.svelte (FNS-002-2 dashboard changes)
- fairnsquare-app/src/main/webui/src/routes/Split.test.ts (FNS-002-2 test updates)
