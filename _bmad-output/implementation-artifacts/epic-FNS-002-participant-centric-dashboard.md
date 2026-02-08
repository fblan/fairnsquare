---
epic_id: 'FNS-002'
title: 'Participant-Centric Dashboard & Expense Management UI'
status: 'ready_for_implementation'
created: '2026-02-04'
author: 'Fred'
ux_designer: 'Sally'
priority: 'high'
theme: 'Core User Experience'
---

# Epic FNS-002: Participant-Centric Dashboard & Expense Management UI

## Executive Summary

Implement the participant-centric main dashboard and associated modals/screens designed by Sally, transforming the split management experience from entity-focused (lists of participants, lists of expenses) to **participant-first** (each participant is the center of their own card with inline actions).

**User Value:** Users can view at-a-glance who's involved, what's been spent, and take quick actions (add expense for a specific participant, edit nights, view balances) without navigating through multiple screens.

**Design Philosophy:** "That was easy!" - minimize taps, maximize clarity, make the by-night calculation instantly visible.

---

## Problem Statement

**Current State (Assumed):**
- Separate screens for participants, expenses, and settlement
- Users navigate between screens to add participants, add expenses, view balances
- Cognitive load: "Where do I add an expense? Who paid for what?"

**Desired State:**
- Single main dashboard shows everything in context
- Each participant card displays their nights, spending, balance, and quick actions
- Modals for focused tasks (add expense, edit participant)
- Expense list accessible from summary card for detailed expense management

**Gap:**
The UX spec defines screens/components that don't exist yet:
- Main Dashboard with participant cards (participant-centric view)
- Add Expense Modal (participant-scoped entry)
- Edit Participant Modal (inline editing)
- Expense List Screen (detailed expense view with CRUD)
- Edit Expense Modal (expense editing)

---

## Scope

### In Scope

**New Screens:**
1. **Create Split Screen** (`routes/CreateSplit.svelte`) - Initial split creation with first participant, direct redirect to dashboard (no intermediate share screen)
2. **Main Dashboard** (`routes/Dashboard.svelte`) - Replaces or extends current Split.svelte
3. **Expense List Screen** (`routes/ExpenseList.svelte`) - Dedicated expense management

**New Modals:**
4. **Add Expense Modal** (`components/expense/AddExpenseModal.svelte`)
5. **Edit Expense Modal** (`components/expense/EditExpenseModal.svelte`)
6. **Edit Participant Modal** (`components/participant/EditParticipantModal.svelte`)

**New Components:**
7. **Expense Summary Card** (`components/dashboard/ExpenseSummaryCard.svelte`)
8. **Participant Card** (`components/participant/ParticipantCard.svelte`)
9. **Share Button** (`components/split/ShareButton.svelte`)
10. **Create Split Form** (`components/split/CreateSplitForm.svelte`)

**API Integrations (already exist per Architecture):**
- `GET /api/splits/{splitId}` - Get split with participants + expenses
- `POST /api/splits/{splitId}/expenses` - Add expense
- `PUT /api/expenses/{id}` - Edit expense
- `DELETE /api/expenses/{id}` - Delete expense
- `PUT /api/participants/{id}` - Edit participant
- `DELETE /api/participants/{id}` - Delete participant

### Out of Scope

- Settlement screen redesign (future epic)
- Share link generation UI (placeholder button for now)
- Empty state onboarding (explicitly deferred per Sally's UX session)
- Real-time updates / WebSockets (manual refresh acceptable)

---

## User Stories

### Story 1: Create Split Flow & Direct Dashboard Redirect

**As a** new split creator
**I want to** create a split with basic info and the first participant, then immediately land on the dashboard
**So that** I can start managing the split without an extra "share link" screen

**Acceptance Criteria:**
- [ ] Create Split screen displays:
  - [ ] Header with "FairNSquare" branding (teal, 20px)
  - [ ] Form card with "Create a New Split" title
  - [ ] Split Name input (required, placeholder: "e.g., Weekend Trip")
  - [ ] First Participant section (light teal background):
    - [ ] Name input (required)
    - [ ] Number of Nights input (required, default to 1, stepper)
  - [ ] "Create Split" button (primary teal, full width, 44px height)
- [ ] Validation:
  - [ ] Split name: required, max 100 characters
  - [ ] Participant name: required, max 50 characters
  - [ ] Nights: minimum 1, maximum 365
- [ ] Submit button disabled until all fields valid
- [ ] On successful creation:
  - [ ] API call: `POST /api/splits` with split name + first participant
  - [ ] Receive split ID in response
  - [ ] **Immediately redirect to Dashboard** (`/splits/{splitId}/dashboard`)
  - [ ] No intermediate "share link" screen
  - [ ] Dashboard loads with:
    - [ ] One participant (the creator added during split creation)
    - [ ] Zero expenses
    - [ ] Share button available for later sharing
- [ ] Error handling:
  - [ ] Show validation errors inline
  - [ ] API error: show toast "Failed to create split"

**Technical Notes:**
- Router path: `/` (home) → Create Split screen
- API endpoint: `POST /api/splits`
- Request body:
  ```json
  {
    "name": "Weekend Trip",
    "firstParticipant": {
      "name": "Alice",
      "nights": 3
    }
  }
  ```
- Response: `{ "id": "V1StGXR8_Z5jdHi6B-myT" }`
- Redirect using sv-router: `goto(/splits/${splitId}/dashboard)`
- UX rationale: Skip the old "share link" screen - users can share from dashboard when ready

**UX Reference:** Screen 1: Create Split in wireframe

---

### Story 2: Main Dashboard with Participant Cards

**As a** split participant
**I want to** see all participants, their nights, balances, and quick actions on one screen
**So that** I understand the split state at a glance without navigating multiple pages

**Acceptance Criteria:**
- [ ] Dashboard displays split name in header
- [ ] Share button visible in top-right corner (placeholder - just shows alert)
- [ ] Expense summary card shows count and total amount
- [ ] Clicking expense summary navigates to Expense List screen
- [ ] Each participant displayed as individual card with:
  - [ ] Name (bold, 18px)
  - [ ] Nights badge (light teal background, "X nights")
  - [ ] Stats row: "Spent: €X | Cost: €X | Owed/Owes: ±€X"
  - [ ] Edit button (opens Edit Participant modal)
  - [ ] Add Expense button (opens Add Expense modal pre-filled with this participant as payer)
  - [ ] Delete icon (red X disabled if has expenses, green checkmark enabled if no expenses)
- [ ] "Add Participant" button at bottom (secondary style)
- [ ] "Split" button at bottom (primary CTA, navigates to settlement)
- [ ] Mobile responsive (cards stack vertically, 16px gap)
- [ ] Touch targets minimum 44px height

**Technical Notes:**
- Use `$derived` rune to calculate stats from expense data
- Color coding: positive balance = green, negative = red, zero = gray
- Stats update reactively when expenses change
- Delete icon shows tooltip on hover if disabled

**UX Reference:** Main Dashboard section in UX spec from Sally's session

---

### Story 3: Add Expense Modal

**As a** split participant
**I want to** quickly add an expense from a participant's card
**So that** I don't have to navigate away or manually select the payer

**Acceptance Criteria:**
- [ ] Modal triggered by clicking "Add Expense" button on participant card
- [ ] Modal displays centered with semi-transparent overlay
- [ ] Form fields:
  - [ ] Description (optional, placeholder: "e.g., Groceries", max 100 chars)
  - [ ] Amount (required, number input, auto-focus, placeholder: "€0.00")
  - [ ] Payer (required, dropdown, pre-selected to participant from card)
  - [ ] Split Mode (required, radio buttons: By Night [default], Equally, By Share)
  - [ ] Participants (checkboxes, all checked by default)
  - [ ] Custom Shares (conditional, only visible if "By Share" selected)
- [ ] Footer buttons: Cancel (gray) + Add Expense (teal, disabled until valid)
- [ ] Save success:
  - [ ] Close modal
  - [ ] Show toast: "Expense added"
  - [ ] Dashboard stats update immediately (optimistic UI)
- [ ] Validation errors shown inline below fields
- [ ] Close button (X) in header
- [ ] Dirty form confirmation on close: "Discard changes?"

**Technical Notes:**
- Use `$state` rune for form values
- Use `$derived` for validation state
- Split mode default from split settings (or BY_NIGHT if not set)
- API call: `POST /api/splits/{splitId}/expenses`
- Optimistic update: add expense to local state, rollback on error

**UX Reference:** Modal A: Add Expense in UX spec

---

### Story 4: Edit Participant Modal

**As a** split participant
**I want to** edit a participant's name or nights
**So that** I can correct mistakes or adjust for late changes

**Acceptance Criteria:**
- [ ] Modal triggered by clicking "Edit" button on participant card
- [ ] Form fields:
  - [ ] Name (required, max 50 chars, no duplicates)
  - [ ] Number of Nights (required, number stepper, min: 1, max: 365)
- [ ] Danger zone section (below horizontal line):
  - [ ] "Delete Participant" button (red border, secondary)
  - [ ] Disabled if participant has expenses
  - [ ] Tooltip if disabled: "Remove expenses first"
- [ ] Footer buttons: Cancel + Save Changes (disabled until dirty and valid)
- [ ] Save success:
  - [ ] Close modal
  - [ ] Recalculate all balances (nights changed affects everyone)
  - [ ] Show toast: "Participant updated"
  - [ ] Briefly highlight updated card (subtle background flash)
- [ ] Delete flow:
  - [ ] Confirmation dialog: "Delete [Name]? This cannot be undone."
  - [ ] On confirm: remove participant, recalculate, close modal
  - [ ] On cancel: stay in modal

**Technical Notes:**
- Validation: no duplicate names (case-insensitive check)
- API call: `PUT /api/participants/{id}`
- Recalculation triggers on nights change (affects all by-night expenses)
- Highlight effect: 2s light teal background fade

**UX Reference:** Modal B: Edit Participant in UX spec

---

### Story 5: Expense List Screen

**As a** split participant
**I want to** see all expenses in detail and edit/delete them
**So that** I can manage expenses individually when needed

**Acceptance Criteria:**
- [ ] Screen accessible by clicking expense summary card on dashboard
- [ ] Header:
  - [ ] Back button (left arrow, returns to dashboard)
  - [ ] Title: "Expenses"
  - [ ] Add button (+ icon, opens Add Expense modal with no payer pre-selected)
- [ ] Summary bar (light teal background):
  - [ ] "X total expenses"
  - [ ] "€XXX.XX total"
- [ ] Empty state (if no expenses):
  - [ ] Receipt icon (gray)
  - [ ] Text: "No expenses yet"
  - [ ] Subtext: "Tap + to add your first expense"
- [ ] Expense cards (if has expenses):
  - [ ] Description (bold) + Amount (right-aligned, teal)
  - [ ] "Paid by [Name]" + Date (small, right-aligned)
  - [ ] Split info icon + text (e.g., "🌙 Split by night")
  - [ ] Participants: "Everyone" or name list
  - [ ] Edit button (opens Edit Expense modal)
  - [ ] Delete button (shows confirmation dialog)
- [ ] List ordered reverse chronological (most recent first)
- [ ] Delete confirmation: "Delete this expense? This will recalculate balances."
- [ ] Delete success: Remove expense, update totals, show toast

**Technical Notes:**
- Use `$state` for expense list loaded from API
- Date formatting: relative ("2 hours ago") or absolute ("Jan 15")
- Split mode icons: ⊜ Equal, 🌙 By Night, 📊 By Share
- API calls:
  - `GET /api/splits/{splitId}/expenses`
  - `DELETE /api/expenses/{id}`
- Optimistic delete: remove from list, rollback on error

**UX Reference:** Screen: Expense List in UX spec

---

### Story 6: Edit Expense Modal

**As a** split participant
**I want to** edit an existing expense
**So that** I can fix mistakes or update details

**Acceptance Criteria:**
- [ ] Modal triggered by clicking "Edit" on expense card in Expense List
- [ ] Form fields identical to Add Expense modal, but pre-filled with expense data
- [ ] Title: "Edit Expense"
- [ ] Footer button: "Save Changes" (instead of "Add Expense")
- [ ] Additional action: "Delete Expense" button (red, below form)
- [ ] Delete triggers same confirmation as Delete button on expense card
- [ ] Save success:
  - [ ] Close modal
  - [ ] Update expense in list
  - [ ] Recalculate balances
  - [ ] Show toast: "Expense updated"
- [ ] All validation and behavior same as Add Expense modal

**Technical Notes:**
- API call: `PUT /api/expenses/{id}`
- Recalculation needed if amount, payer, or split mode changed
- Delete from modal: `DELETE /api/expenses/{id}`, then close modal + navigate back

**UX Reference:** Modal C: Edit Expense (Bonus) in UX spec

---

### Story 7: Share Button (Placeholder)

**As a** split creator
**I want to** easily share the split link
**So that** others can access and add expenses

**Acceptance Criteria:**
- [ ] Share button in top-right of dashboard header
- [ ] Icon: share icon (teal)
- [ ] Tap target: 44px minimum
- [ ] Click behavior (placeholder for now):
  - [ ] Show alert/toast: "Share link: [copy current URL]"
  - [ ] OR copy URL to clipboard + toast: "Link copied!"
- [ ] Future enhancement: proper share modal with QR code, social links

**Technical Notes:**
- Use `navigator.clipboard.writeText()` for copy
- Fallback for older browsers: manual text selection
- URL format: `https://fairnsquare.app/splits/{splitId}`

**UX Reference:** Share Button in Main Dashboard header

---

### Story 8: Remove Bottom Expense Summary Card from Dashboard

**As a** split user
**I want** the bottom expense summary card removed from the main dashboard (below "Add Participant" button)
**So that** the interface is cleaner without duplicate expense information

**Acceptance Criteria:**
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

**Technical Notes:**
- ✅ Removed ExpensesSection component import from Split.svelte:12
- ✅ Removed ExpensesSection component render from Split.svelte:182-183
- ✅ Deleted backup file Split.svelte.backup
- ✅ All tests passing (36/36 in Split.test.ts)
- ✅ Build successful with no errors
- Top expense summary card (lines 155-177) remains functional

**Rationale:**
Two expense cards on the same page is redundant. Keep the top one for quick access to expense list, remove the bottom one to reduce clutter and scrolling.

---

## Navigation Flow

```
Create Split Screen (routes/CreateSplit.svelte)
└─→ [Submit form] → Main Dashboard (routes/Dashboard.svelte)
    │
Main Dashboard (routes/Dashboard.svelte)
├─→ [Click Expense Summary] → Expense List Screen (routes/ExpenseList.svelte)
│   ├─→ [Click +] → Add Expense Modal → back to Expense List
│   ├─→ [Click Edit] → Edit Expense Modal → back to Expense List
│   ├─→ [Click Delete] → Confirm → back to Expense List
│   └─→ [Click Back] → Main Dashboard
│
├─→ [Click Add Expense on participant] → Add Expense Modal → back to Dashboard
│
├─→ [Click Edit on participant] → Edit Participant Modal → back to Dashboard
│
├─→ [Click Share] → Share Modal/Toast (placeholder)
│
└─→ [Click Split button] → Settlement Screen (existing, not in this epic)
```

---

## Technical Implementation

### Frontend Architecture

**Router Updates (sv-router):**
- Add route: `/` (home) → `CreateSplit.svelte`
- Add route: `/splits/:splitId/dashboard` → `Dashboard.svelte`
- Add route: `/splits/:splitId/expenses` → `ExpenseList.svelte`
- Default split route redirects to dashboard
- After split creation: redirect to `/splits/{splitId}/dashboard` (no intermediate share screen)

**State Management:**
- `splitStore.svelte.ts` - holds current split data (participants, expenses, balances)
- Components read from store, modals update store on save
- Optimistic updates: UI reflects changes before API confirms

**Shared Components:**
- `Modal.svelte` - base modal component (overlay, header, footer)
- `Button.svelte` - consistent button styling (primary, secondary, danger)
- `Input.svelte` - form input with validation state
- `Toast.svelte` - toast notification system

**API Client:**
- `lib/api/expenses.ts` - expense CRUD operations
- `lib/api/participants.ts` - participant CRUD operations
- Error handling: show toast on failure, rollback optimistic updates

### Styling (Tailwind + CSS Custom Properties)

**Color System (from UX spec):**
- Primary: `#0D9488` (teal)
- Success: `#16A34A` (green)
- Danger: `#DC2626` (red)
- Neutrals: Slate scale

**Component Patterns:**
- Cards: `bg-white border-2 border-slate-200 rounded-lg p-4`
- Buttons: 44px height, 8px radius
- Touch targets: min 44px for mobile
- Spacing: 16px gap between cards

**Responsive:**
- Mobile-first (< 768px): stacked cards, full-width buttons
- Desktop (≥ 768px): max 420px centered, same layout

### API Endpoints (Already Exist)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/splits` | Create new split with first participant |
| GET | `/api/splits/{splitId}` | Load split with participants + expenses |
| POST | `/api/splits/{splitId}/expenses` | Create expense |
| PUT | `/api/expenses/{id}` | Update expense |
| DELETE | `/api/expenses/{id}` | Delete expense |
| PUT | `/api/participants/{id}` | Update participant |
| DELETE | `/api/participants/{id}` | Delete participant (if no expenses) |

**Error Handling:**
- 400 Bad Request: Show validation errors inline
- 404 Not Found: Show toast "Not found"
- 409 Conflict: Show toast (e.g., "Cannot delete - has expenses")
- 500 Server Error: Show toast "Something went wrong"

---

## Design System Compliance

All components must follow the UX Design Specification:

**Typography:**
- h1: 24px, weight 600
- Body: 16px, weight 400
- Small: 14px, 12px for captions

**Spacing:**
- Base unit: 4px
- Card padding: 16px
- Component gap: 16px
- Border radius: 8px

**Accessibility:**
- WCAG 2.1 Level A minimum
- Keyboard navigation: all interactive elements focusable
- Screen reader labels on all icons
- Focus visible: teal ring on focus
- Touch targets: 44px minimum

---

## Implementation Checklist

### Phase 1: Split Creation & Core Dashboard
- [ ] Story 1: Create Split Flow & Direct Dashboard Redirect
- [ ] Story 2: Main Dashboard with Participant Cards
- [ ] Story 7: Share Button (placeholder)

### Phase 2: Modals
- [ ] Story 3: Add Expense Modal
- [ ] Story 4: Edit Participant Modal

### Phase 3: Expense Management
- [ ] Story 5: Expense List Screen
- [ ] Story 6: Edit Expense Modal

### Phase 4: Polish
- [ ] Responsive testing (mobile, tablet, desktop)
- [ ] Accessibility audit (keyboard nav, screen reader)
- [ ] Error state testing (API failures, validation)
- [ ] Loading state refinement (skeletons, spinners)

---

## Success Metrics

**Completion Criteria:**
- [ ] All 7 stories implemented and tested
- [ ] All acceptance criteria met
- [ ] No console errors or warnings
- [ ] Lighthouse accessibility score > 80
- [ ] Manual test pass on mobile (Chrome Android)
- [ ] Manual test pass on desktop (Chrome, Firefox, Safari)

**User Experience Validation:**
- [ ] Add expense flow: < 15 seconds (timed)
- [ ] Edit participant: < 10 seconds
- [ ] Dashboard comprehension: instant (informal user test)

**Technical Quality:**
- [ ] TypeScript strict mode passes
- [ ] No unused imports/variables
- [ ] Svelte component best practices (reactive declarations, proper cleanup)
- [ ] API error handling tested (mock 4xx/5xx responses)

---

## Architecture Impact

| Dimension | Impact Level | Notes |
|-----------|--------------|-------|
| Backend API | None | All endpoints already exist per architecture |
| Frontend Routes | Medium | 2 new routes, update default route |
| Frontend Components | High | 8 new components + 1 base modal |
| State Management | Low | Extend existing splitStore |
| Design System | Low | Follow existing Tailwind + CSS vars |
| Build Process | None | No changes needed |

**Risk Assessment:**
- **Low Risk:** Backend unchanged, UI only
- **Medium Risk:** State management complexity (optimistic updates, rollback)
- **Low Risk:** UX patterns are standard (modals, forms, cards)

**Mitigation:**
- Test optimistic updates thoroughly (success + failure paths)
- Use TypeScript for API response typing
- Follow existing component patterns from codebase

---

## Epic Ready for Implementation 🚀

**Next Steps:**
1. Review epic with team (or Fred solo)
2. Confirm story priority (can implement stories 1-4 first, defer 5-6)
3. Create feature branch: `feature/fns-002-participant-dashboard`
4. Start with Story 1 (Create Split Flow) - entry point
5. Then Story 2 (Main Dashboard) - foundational screen
6. Iterate through stories 3-7 in order
7. Create PR when all stories complete

**Estimated Effort:** 10-14 hours (solo developer, Fred)

**Dependencies:**
- UX Design Spec (✅ Complete - Sally's session)
- Architecture Doc (✅ Complete - file-based persistence, REST API)
- Backend API (✅ Assumed complete based on architecture)

**Questions for Fred:**
- Do you want to implement stories 1-4 first (create split + dashboard + modals) and defer expense list (stories 5-6) to a future epic?
- Should Share Button be a real modal or just clipboard copy for MVP?
- Any specific browser versions you want to prioritize for testing?
- Confirm: After split creation, go directly to dashboard (no "share link" intermediate screen) ✅ - per UX session decision