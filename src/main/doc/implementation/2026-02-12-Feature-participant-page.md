# Feature: Participant Edition Page

## What, Why and Constraints

### What
Redesigned the split main page to separate concerns into two distinct views:
- The **Split dashboard** now displays two compact summary cards: an expenses summary (count + total) and a participants summary (count + comma-separated list with nights).
- A new **Participants edition page** (`/splits/:splitId/participants`) provides full participant management: viewing participant cards with financial stats, editing, deleting, adding new participants, and quick-adding expenses per participant.

### Why
Previously, the split dashboard embedded all participant cards directly, making the page long and mixing navigation concerns with editing concerns. The new layout gives the dashboard a clean overview role, while dedicating a separate page to participant management, consistent with how expenses already have their own list page.

### Constraints
- Svelte 5 with runes (`$state`, `$derived`, `$effect`) for reactivity
- sv-router for client-side routing
- Bits UI + Tailwind CSS for styling, following existing teal theme and 44px min touch targets
- Must preserve all existing participant functionality: add, edit, delete, add expense per participant
- Must follow the same page patterns established by `ExpenseList.svelte` (self-loading via route params, back button header)

## How

### 1. Route registration (`src/lib/router.ts`)
Added a new route `/splits/:splitId/participants` pointing to the new `Participants.svelte` page component.

### 2. New Participants page (`src/routes/Participants.svelte`)
Created a standalone page that:
- Loads split data from the `splitId` route parameter (same pattern as `ExpenseList.svelte`)
- Displays a header with a back arrow button navigating to `/splits/:splitId` and a "Participants" title
- Renders participant cards with per-participant stats (spent, cost, balance) and action buttons (add expense, edit, delete)
- Provides an "Add Participant" button that toggles an inline form with name/nights fields and validation
- Includes all required modals: `EditParticipantModal`, `ExpenseEditModal`, `ConfirmDialog`

All logic was migrated from the former `ParticipantsSection.svelte` component.

### 3. Split dashboard update (`src/routes/Split.svelte`)
- Removed the embedded `ParticipantsSection` component
- Added two derived values: `participantCount` and `participantSummary` (comma-separated `Name (nights)` format)
- Added a clickable participants summary card (matching the existing expense summary card style) that navigates to the participants page

### 4. Cleanup
- Deleted `ParticipantsSection.svelte` as it was fully superseded by `Participants.svelte`
- Verified no remaining references to the deleted file

## Tests

### Automated Tests (45 tests total, all passing)

**`Split.test.ts`** (18 tests) — updated to reflect the new summary card layout:
- Loading / error / 404 states (5 tests)
- Dashboard header and share button (4 tests)
- Expense summary card: count, total, navigation (3 tests)
- Participant summary card: count, singular/plural, names with nights, empty state, navigation (6 tests)

**`Participants.test.ts`** (27 tests) — new test file for the participants edition page:
- Loading state (1 test)
- Page header and back button navigation (2 tests)
- Participant cards: name/nights display, stats (spent/cost/balance), balance colors (green/red/gray), empty state (5 tests)
- Action buttons: edit, delete, add expense per card, add expense modal opening (4 tests)
- Edit participant: modal integration (1 test)
- Add participant form: button display, form toggle, validation (name empty, nights range), API call, smart default nights, error toast, cancel (9 tests)
- Delete participant: confirmation dialog, cancel, API call, success toast, 409 conflict error (5 tests)

### Manual Testing

- Verified the Vite production build compiles without errors
- Created a test split via the API with two participants (Alice, Bob)
- Verified the split dashboard displays both summary cards with correct counts and participant list
- Verified clicking the participants card navigates to the participants edition page
- Verified the back button returns to the dashboard