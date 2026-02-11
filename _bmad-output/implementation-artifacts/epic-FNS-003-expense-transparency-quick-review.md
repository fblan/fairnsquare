---
epic_id: 'FNS-003'
title: 'Expense Transparency & Quick Review'
status: 'ready_for_implementation'
created: '2026-02-08'
author: 'John (PM)'
priority: 'medium'
theme: 'User Experience - Expense Management'
---

# Epic FNS-003: Expense Transparency & Quick Review

## Executive Summary

Improve expense visibility and review workflow by showing beneficiary details directly on expense cards and providing a safe, read-only view for reviewing expense details without risk of accidental edits.

**User Value:** Users can quickly verify expense splits are correct at a glance and confidently review full expense details without entering edit mode.

**Core Jobs to be Done:**
- "I want to spot-check that expense splits make sense without clicking into every expense"
- "I want to review expense details safely without accidentally changing something"

---

## Problem Statement

**Current State:**
- Expense cards show basic info (description, amount, payer, split mode icon)
- To see WHO benefited and their share weights, users must click "Edit" on each expense
- Clicking "Edit" opens edit modal - high risk of accidental changes while just reviewing
- No safe way to view expense details in read-only mode

**Pain Points:**
1. **Can't verify split correctness at a glance** - "Did I split this 3-2-1 like I intended?"
2. **Edit mode anxiety** - "I just want to check details, not risk breaking something"
3. **Slow review workflow** - Must click → edit → mentally note it's correct → close → repeat

**Desired State:**
- Expense cards show beneficiaries with their share weights inline
- Click expense card → opens read-only detail view (no edit risk)
- Quick, confident expense verification workflow

**Gap:**
- Missing beneficiary/share display on expense cards
- Missing read-only expense detail view

---

## Scope

### In Scope

**Enhancements:**
1. **Expense Card Beneficiary Display** (Story 1)
   - Show comma-separated list of beneficiaries with share weights
   - Format: "Alice (3), Bob (2), Charlie (1)" for By Night / By Share
   - Format: "Alice, Bob, Charlie" for Equally mode (no weights needed)
   - Display on expense cards in Expense List screen

2. **Read-Only Expense View** (Story 2)
   - Click expense card → open read-only view of expense details
   - Same layout as edit modal but all fields disabled/non-editable
   - "Edit" button to transition to edit mode if needed
   - Close button to return to expense list

### Out of Scope

- Expense approval workflow (future epic)
- Expense comments/notes (future epic)
- Expense history/audit log (future epic)
- Inline editing on expense cards (future epic)
- Expense filtering/search (future epic)

---

## User Stories

### Story 1: Display Beneficiaries with Shares on Expense Cards

**As a** split user
**I want to** see who benefited from each expense and their share weights directly on the expense card
**So that** I can quickly verify the split is correct without clicking into edit mode

**Acceptance Criteria:**

- [ ] Expense cards display beneficiary information below split mode line
- [ ] Format varies by split mode:
  - [ ] **By Night mode:** "Alice (3 nights), Bob (2 nights), Charlie (1 night)"
  - [ ] **By Share mode:** "Alice (3), Bob (2), Charlie (1)"
  - [ ] **Equally mode:** "Alice, Bob, Charlie" (no weights - all equal)
- [ ] Beneficiaries listed in alphabetical order by name
- [ ] Payer name is **bolded** in the beneficiary list to highlight who paid
- [ ] Text style: 14px, muted color (text-muted-foreground), weight 400
- [ ] Line wraps if beneficiary list is long (no horizontal scroll)
- [ ] Icon prefix: "👥" (people icon) before beneficiary list
- [ ] Mobile responsive: maintains readability on small screens

**Technical Notes:**

**Data source:**
- Expense object has `beneficiaries` array with participant references
- Each beneficiary has `share` value (calculated or custom)
- Split mode determines display format

**Implementation approach:**
```typescript
// ExpenseList.svelte or expense card component
function formatBeneficiaries(expense: Expense): string {
  const { splitMode, beneficiaries, payer } = expense;

  // Sort alphabetically
  const sorted = [...beneficiaries].sort((a, b) =>
    a.participant.name.localeCompare(b.participant.name)
  );

  // Helper to bold payer name
  const formatName = (name: string, isPayer: boolean) =>
    isPayer ? `**${name}**` : name;

  if (splitMode === 'EQUALLY') {
    return sorted.map(b =>
      formatName(b.participant.name, b.participant.id === payer.id)
    ).join(', ');
  }

  if (splitMode === 'BY_NIGHT') {
    return sorted.map(b => {
      const name = formatName(b.participant.name, b.participant.id === payer.id);
      const nights = `${b.participant.nights} ${b.participant.nights === 1 ? 'night' : 'nights'}`;
      return `${name} (${nights})`;
    }).join(', ');
  }

  if (splitMode === 'BY_SHARE') {
    return sorted.map(b => {
      const name = formatName(b.participant.name, b.participant.id === payer.id);
      return `${name} (${b.share})`;
    }).join(', ');
  }
}
```

**Note:** The `**name**` markdown-style bold notation is for documentation. In actual implementation, use HTML `<strong>` tags or CSS `font-weight: 600` class on the payer's name element.

**Testing:**
- Unit tests for formatBeneficiaries() logic
- Test payer name is bolded in all split modes
- Visual regression tests for card layout
- Test with 2, 5, 10 beneficiaries (wrapping behavior)
- Test all three split modes

**UX Considerations:**
- Keep beneficiary line scannable - users should quickly spot names and identify payer
- Payer name bolded to make it obvious who paid
- Line wraps gracefully for long beneficiary lists

---

### Story 2: Read-Only Expense Detail View

**As a** split user
**I want to** view expense details in read-only mode by clicking the expense card
**So that** I can review all information safely without risk of accidental edits

**Acceptance Criteria:**

- [ ] Clicking expense card in Expense List opens read-only detail modal
- [ ] Modal displays all expense information:
  - [ ] Description (if present)
  - [ ] Amount (formatted as currency)
  - [ ] Payer name
  - [ ] Split mode (with icon)
  - [ ] Beneficiaries list (full names with shares/nights)
  - [ ] Date created/modified
- [ ] All fields are **read-only** (no input focus, no editing)
- [ ] Visual distinction: fields have disabled appearance (gray background or border)
- [ ] Modal header: "Expense Details" (not "Edit Expense")
- [ ] Footer buttons:
  - [ ] "Edit" button (primary) - opens edit modal for this expense
  - [ ] "Delete" button (danger, secondary) - same delete flow as edit modal
  - [ ] "Close" button (secondary) - returns to expense list
- [ ] Escape key closes modal
- [ ] Click outside modal overlay closes modal
- [ ] Mobile responsive layout (full screen on small devices)

**Technical Notes:**

**Implementation approach:**

**Option A: New ViewExpenseModal component**
- Create dedicated `ViewExpenseModal.svelte`
- Shares layout with `EditExpenseModal.svelte` but all inputs disabled
- Props: `expense`, `onEdit`, `onClose`, `onDelete`

**Option B: Reuse EditExpenseModal with view-only mode**
- Add `mode: 'edit' | 'view'` prop to `EditExpenseModal.svelte`
- Conditionally disable inputs when `mode === 'view'`
- Change footer buttons based on mode

**Recommendation: Option A** - cleaner separation of concerns, less conditional logic

**Component structure:**
```svelte
<!-- ViewExpenseModal.svelte -->
<script lang="ts">
  import type { Expense } from '$lib/api/expenses';

  let {
    expense,
    onEdit,
    onClose,
    onDelete
  }: {
    expense: Expense;
    onEdit: () => void;
    onClose: () => void;
    onDelete?: () => void;
  } = $props();

  function handleEdit() {
    onEdit(); // Parent opens EditExpenseModal
  }
</script>

<Modal title="Expense Details" onClose={onClose}>
  <div class="space-y-4">
    <ReadOnlyField label="Description" value={expense.description || '—'} />
    <ReadOnlyField label="Amount" value={formatCurrency(expense.amount)} />
    <ReadOnlyField label="Paid by" value={expense.payer.name} />
    <ReadOnlyField label="Split mode" value={formatSplitMode(expense.splitMode)} />
    <ReadOnlyField label="Beneficiaries" value={formatBeneficiaries(expense)} />
    <ReadOnlyField label="Date" value={formatDate(expense.createdAt)} />
  </div>

  <svelte:fragment slot="footer">
    <Button variant="secondary" onclick={onClose}>Close</Button>
    <Button variant="destructive-outline" onclick={onDelete}>Delete</Button>
    <Button variant="primary" onclick={handleEdit}>Edit</Button>
  </svelte:fragment>
</Modal>
```

**ExpenseList.svelte changes:**
- Change expense card click handler from "open edit modal" to "open view modal"
- Track `viewingExpense` and `editingExpense` separately
- Flow: Click card → View modal → Click "Edit" → Edit modal

**Testing:**
- All expense fields render correctly in read-only mode
- "Edit" button transitions to edit modal with same expense
- "Delete" button triggers confirmation (same as edit modal)
- Close button and escape key work
- Mobile responsive layout

**Accessibility:**
- Modal has proper ARIA labels (`role="dialog"`, `aria-labelledby`)
- Focus trap within modal
- Escape key closes modal
- Read-only fields use `aria-readonly="true"` or disabled attribute

---

## Navigation Flow (Updated)

```
Expense List Screen
├─→ [Click expense card] → View Expense Modal (read-only) ← NEW
│   ├─→ [Click Edit] → Edit Expense Modal → Save → back to Expense List
│   ├─→ [Click Delete] → Confirm → delete → back to Expense List
│   └─→ [Click Close / Esc] → back to Expense List
│
├─→ [Click + Add] → Add Expense Modal → back to Expense List (existing)
└─→ [Click Back] → Main Dashboard (existing)
```

---

## Technical Implementation

### Frontend Changes

**New Components:**
- `ViewExpenseModal.svelte` - read-only expense detail view
- `ReadOnlyField.svelte` - reusable read-only field component (label + value)

**Modified Components:**
- `ExpenseList.svelte`:
  - Add beneficiary display to expense cards (Story 1)
  - Change card click handler to open ViewExpenseModal (Story 2)
  - Add view modal state management
- Expense card rendering (inline or sub-component):
  - Add beneficiaries section with formatted list

**Utility Functions:**
- `formatBeneficiaries(expense: Expense): string` - format beneficiary list by split mode
- `formatSplitMode(mode: SplitMode): string` - human-readable split mode ("Split by night", "Split equally", etc.)

**No Backend Changes:**
- All data already exists in expense objects
- Beneficiaries array already includes share values
- No new API endpoints needed

### Styling (Tailwind + CSS Custom Properties)

**Expense Card - Beneficiaries Section:**
```css
.beneficiary-line {
  @apply text-sm text-muted-foreground flex items-center gap-2;
}

.beneficiary-line .icon {
  @apply text-base; /* emoji size */
}
```

**Read-Only Fields:**
```css
.readonly-field {
  @apply space-y-1;
}

.readonly-field-label {
  @apply text-sm font-medium text-foreground;
}

.readonly-field-value {
  @apply text-base text-muted-foreground bg-muted px-3 py-2 rounded-md;
}
```

---

## Design System Compliance

**Typography:**
- Beneficiary list: 14px, weight 400
- Read-only field labels: 14px, weight 500
- Read-only field values: 16px, weight 400

**Spacing:**
- Beneficiary line: 8px gap from split mode line
- Read-only fields: 16px vertical gap
- Modal padding: 24px

**Colors:**
- Beneficiaries text: `text-muted-foreground`
- Read-only field background: `bg-muted`
- Read-only field text: `text-muted-foreground`

**Accessibility:**
- Read-only fields must have proper ARIA attributes
- Modal focus trap and keyboard navigation
- Touch targets: 44px minimum

---

## Success Metrics

**Completion Criteria:**
- [ ] Story 1: Beneficiaries displayed on all expense cards
- [ ] Story 2: Read-only view modal functional
- [ ] All acceptance criteria met
- [ ] No console errors or warnings
- [ ] Accessibility audit passed
- [ ] Manual testing on mobile and desktop

**User Experience Validation:**
- [ ] Fred can verify expense splits at a glance without clicking (Story 1)
- [ ] Fred can view expense details safely without edit anxiety (Story 2)
- [ ] Expense review workflow feels faster (<5 seconds to verify an expense)

**Technical Quality:**
- [ ] TypeScript strict mode passes
- [ ] Svelte 5 best practices (runes, proper cleanup)
- [ ] Component tests for beneficiary formatting logic
- [ ] Visual regression tests for expense cards

---

## Implementation Checklist

### Story 1: Beneficiaries on Expense Cards
- [ ] Add `formatBeneficiaries()` utility function
- [ ] Implement payer name bolding logic
- [ ] Update expense card layout to include beneficiary line
- [ ] Test with all three split modes (By Night, By Share, Equally)
- [ ] Test payer name appears bolded correctly
- [ ] Test long beneficiary lists (wrapping)
- [ ] Mobile responsive testing

### Story 2: Read-Only Expense View
- [ ] Create `ViewExpenseModal.svelte` component
- [ ] Create `ReadOnlyField.svelte` component (optional, can inline)
- [ ] Update `ExpenseList.svelte` click handler
- [ ] Add state management for view vs edit modals
- [ ] Implement Edit button → Edit modal transition
- [ ] Implement Delete button with confirmation flow
- [ ] Test modal close behaviors (button, escape, click outside)
- [ ] Accessibility testing (focus trap, ARIA labels)
- [ ] Mobile responsive testing

---

## Architecture Impact

| Dimension | Impact Level | Notes |
|-----------|--------------|-------|
| Backend API | None | All data already exists in expense objects |
| Frontend Components | Medium | 1-2 new components, modify ExpenseList |
| State Management | Low | Add view modal state to ExpenseList |
| Design System | Low | New ReadOnlyField pattern (simple) |
| Build Process | None | No changes needed |

**Risk Assessment:**
- **Low Risk:** UI-only changes, no backend impact
- **Low Risk:** No breaking changes to existing components
- **Medium Risk:** Beneficiary formatting logic must handle edge cases (empty list, single beneficiary, all participants)

**Mitigation:**
- Unit test beneficiary formatting thoroughly
- Test with real expense data from existing splits
- Validate with Fred during implementation (quick feedback loop)

---

## Epic Ready for Implementation 🚀

**Next Steps:**
1. Review epic with Fred (confirm stories align with vision)
2. Create feature branch: `feature/fns-003-expense-transparency`
3. Implement Story 1 (beneficiaries display) first - foundational enhancement
4. Then Story 2 (read-only view) - builds on Story 1's formatting logic
5. Manual review with Fred before marking epic complete
6. Consider adding more stories to this epic if additional expense view improvements emerge

**Estimated Effort:** 4-6 hours (solo developer, Fred)

**Dependencies:**
- Epic FNS-002 complete (✅ Expense List screen exists)
- Expense data structure includes beneficiaries array (✅ confirmed in architecture)

**Confirmed Requirements (from Fred):**
- ✅ Never use "Everyone" shorthand - always show full beneficiary list
- ✅ Payer name must be bolded in beneficiary list
- ✅ Delete button included in read-only view
- ✅ Epic scope: just these two stories for now

---

**Epic FNS-003 finalized and ready for implementation! 🚀**

Both stories are scoped, requirements confirmed, ready for dev workflow.
