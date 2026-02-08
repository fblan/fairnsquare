# Story FNS-002-2: Main Dashboard with Participant Cards

Status: done

## Story

As a **split participant**,
I want **to see all participants, their nights, balances, and quick actions on one screen**,
So that **I understand the split state at a glance without navigating multiple pages**.

## Acceptance Criteria

1. **Given** I navigate to `/splits/{splitId}`
   **When** the page loads
   **Then** I see the split name as a header

2. **Given** the dashboard is loaded
   **When** I look at the top-right area
   **Then** I see a share button (placeholder: copies URL to clipboard + toast)

3. **Given** the split has expenses
   **When** I look at the expense summary card
   **Then** I see expense count and total amount
   **And** the card is visually clickable (navigation to expense list deferred to story 5)

4. **Given** the split has participants
   **When** I look at the dashboard
   **Then** each participant is displayed as an individual card with:
   - Name (bold)
   - Nights badge (light teal background, "X nights")
   - Stats row: "Spent: €X | Cost: €X | Balance: ±€X"
   - Edit button (uses existing inline edit from ParticipantsSection)
   - Delete icon (existing behavior from ParticipantsSection)

5. **Given** participant stats are visible
   **When** expenses exist
   **Then** Spent = sum of expenses paid by participant
   **And** Cost = sum of participant's shares across all expenses
   **And** Balance = Spent - Cost (positive = green, negative = red, zero = gray)

6. **Given** I want to add a new participant
   **When** I click "Add Participant" at the bottom
   **Then** the existing add participant form appears

7. **Given** the dashboard is loaded on mobile
   **When** I view the layout
   **Then** cards stack vertically with 16px gap
   **And** all touch targets are minimum 44px height

## Tasks / Subtasks

- [x] Task 1: Rewrite Split.svelte as participant-centric dashboard (AC: 1, 2)
  - [x] 1.1: Add dashboard header with split name and share button
  - [x] 1.2: Replace shareable URL section with share icon button
  - [x] 1.3: Keep existing loading/error/404 states

- [x] Task 2: Add expense summary card (AC: 3)
  - [x] 2.1: Create expense summary card showing count and total
  - [x] 2.2: Style with teal accents (clickable appearance, navigation deferred)

- [x] Task 3: Create participant cards with stats (AC: 4, 5)
  - [x] 3.1: Calculate per-participant stats (spent, cost, balance) using $derived
  - [x] 3.2: Display participant cards with name, nights badge, stats row
  - [x] 3.3: Color-code balance (green positive, red negative, gray zero)
  - [x] 3.4: Include edit and delete buttons on each card
  - [x] 3.5: Keep existing inline edit and delete confirmation behavior

- [x] Task 4: Integrate add participant flow (AC: 6)
  - [x] 4.1: Move "Add Participant" button to bottom of participant list
  - [x] 4.2: Keep existing add form behavior from ParticipantsSection

- [x] Task 5: Responsive layout and accessibility (AC: 7)
  - [x] 5.1: Cards stack vertically with 16px gap
  - [x] 5.2: All touch targets minimum 44px
  - [x] 5.3: Proper ARIA labels on all interactive elements

- [x] Task 6: Write tests (all ACs)
  - [x] 6.1: Test dashboard renders split name in header
  - [x] 6.2: Test share button copies URL and shows toast
  - [x] 6.3: Test expense summary card shows count and total
  - [x] 6.4: Test participant cards display name, nights, stats
  - [x] 6.5: Test balance color coding (positive/negative/zero)
  - [x] 6.6: Test add participant button is present

- [x] Task 7: Run tests and verify no regressions
  - [x] 7.1: Run full test suite (61/61 passing across 3 test files)

## Dev Notes

### Current State Analysis

**Split.svelte** currently:
- Shows split name in a Card header with shareable URL + copy button
- Renders ParticipantsSection and ExpensesSection as separate card sections
- Has Balance Summary placeholder section
- Loading/error/404 states already implemented

**ParticipantsSection.svelte** currently:
- Lists participants as simple rows (name + nights)
- Inline add form with smart defaults (localStorage)
- Inline edit mode (click to edit name/nights)
- Delete with confirmation dialog (409 handling for expenses)

**ExpensesSection.svelte** currently:
- Add expense form (amount, description, payer, split mode)
- Expense list with expandable ExpenseCard components
- Already handles all expense CRUD display

### Implementation Approach

Transform Split.svelte into a dashboard layout:
1. Replace the header card (remove inline URL, add share icon button)
2. Add expense summary card below header
3. Replace ParticipantsSection with participant cards that show stats
4. **Keep ExpensesSection visible** (better UX - users can manage expenses immediately; original plan to hide deferred)
5. Keep all existing add/edit/delete participant logic, just restyle the display

### Stats Calculation

```typescript
// Per-participant stats from Split data
function getParticipantStats(participantId: string, split: Split) {
  const spent = split.expenses
    .filter(e => e.payerId === participantId)
    .reduce((sum, e) => sum + e.amount, 0);

  const cost = split.expenses
    .reduce((sum, e) => {
      const share = e.shares.find(s => s.participantId === participantId);
      return sum + (share?.amount || 0);
    }, 0);

  const balance = spent - cost;
  return { spent, cost, balance };
}
```

### Completion Notes

- Rewrote Split.svelte as participant-centric dashboard with header (split name + share button), expense summary card, and participant cards section
- Rewrote ParticipantsSection.svelte to render individual cards per participant with stats (spent, cost, balance), nights badge, edit/delete buttons
- Fixed ConfirmDialog prop names (was using wrong prop names: isOpen→open, message→description, confirmText→confirmLabel, etc.)
- Per-participant stats calculated from expense shares data: Spent (sum paid), Cost (sum of shares), Balance (Spent - Cost)
- Balance color-coded: green (owed), red (owes), gray (settled)
- Expense summary card shows count and total with teal styling
- Share button copies URL to clipboard with toast feedback
- All existing add/edit/delete participant functionality preserved
- **ExpensesSection kept visible** (deviated from plan to hide it - better UX for immediate expense management)
- 37 tests covering dashboard layout, participant cards, stats, balance colors, add/edit/delete participant flows
- Full test suite: 61/61 passing (Home: 13, ExpenseCard: 11, Split: 37)

### File List

- `fairnsquare-app/src/main/webui/src/routes/Split.svelte` — Rewritten as participant-centric dashboard
- `fairnsquare-app/src/main/webui/src/routes/ParticipantsSection.svelte` — Rewritten with participant cards and stats
- `fairnsquare-app/src/main/webui/src/routes/Split.test.ts` — Rewritten with 37 tests for dashboard layout

## Dev Agent Record

### Agent Model Used

Claude Opus 4.5 (claude-opus-4-5-20251101)

## Change Log

- 2026-02-04: Implementation complete — all tasks done, 61/61 tests passing, story ready for review