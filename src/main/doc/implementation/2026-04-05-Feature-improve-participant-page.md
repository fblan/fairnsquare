# Feature: Improve Participant Page (Issue #109)

## What, Why and Constraints

### What was done
Four improvements to the Participants page:

1. **Button labels** — Renamed "Single" → "Add Single Participant" and "Family" → "Add Family Participant" for clarity.
2. **Help button** — Added a help button (question-mark icon) next to the add buttons that opens a modal describing use cases for Single vs Family participants, with concrete examples.
3. **Decimal input fix** — The Members field (add form) and Share field (edit modal) previously used `step="0.5"`, which silently rejected manually-typed values like `2.30`. Changed to `step="any"` with an `onkeydown` handler for 0.5 arrow-key steps per the frontend rule on numeric inputs.
4. **Navigation guard** — When a split has no participants, navigating away via the header tabs now shows a confirmation dialog ("Leave without adding a participant?"). The user must either add a participant or explicitly confirm they want to leave.

### Why
- The old button labels "Single" and "Family" were ambiguous — not obviously "add" actions.
- Users had no guidance on when to use Single vs Family mode; use case examples reduce confusion.
- `step="0.5"` on number inputs prevents users from freely entering values with two decimal places (e.g. `2.30`), which is a valid member count.
- On split creation, the user lands on the Participants page and should be encouraged to add at least one participant before proceeding.

### Constraints
- Frontend rule for numeric inputs: use `step="any"` + `onkeydown` handler for arrow-key step behavior; never use `step="N"` when it would silently restrict valid manual values.
- The navigation guard must not block navigation when participants exist — only on first creation (empty list).
- The guard is implemented in `SplitPageHeader` via an optional `navigateGuard` prop, so it can be reused or extended in other pages if needed.

---

## How

### Files modified

**`fairnsquare-app/src/main/webui/src/routes/Participants.svelte`**
- Added `HelpCircle` to lucide-svelte imports.
- Added `showHelp` state variable.
- Renamed button labels to "Add Single Participant" and "Add Family Participant".
- Added Help button (ghost icon button) next to the add buttons.
- Added Help modal at the bottom of the file with Single vs Family use case descriptions and examples.
- Changed Members input from `step="0.5"` to `step="any"` with an `onkeydown` handler that increments/decrements by 0.5 on ArrowUp/ArrowDown.
- Passed `navigateGuard` prop to `SplitPageHeader` — returns a non-null string when `split.participants.length === 0`.

**`fairnsquare-app/src/main/webui/src/lib/components/participant/EditParticipantModal.svelte`**
- Changed Share input from `step="0.5"` to `step="any"` with an `onkeydown` handler for 0.5 arrow-key steps.

**`fairnsquare-app/src/main/webui/src/lib/components/ui/split-page-header/SplitPageHeader.svelte`**
- Added `ConfirmDialog` import.
- Added optional `navigateGuard?: () => string | null` prop to the `Props` interface.
- Added `pendingNavTarget` state, `handleNavigate()`, `confirmNavigation()`, and `cancelNavigation()` functions.
- Changed all tab `onclick` handlers from `navigate(...)` to `handleNavigate(...)`.
- Added `ConfirmDialog` at the bottom of the template, bound to `pendingNavTarget`.

### Files added
- `src/main/doc/implementation/2026-04-05-Feature-improve-participant-page.md` (this file)

---

## Tests

### `src/routes/Participants.test.ts`
- Updated all existing tests that referenced `{ name: 'Single' }` and `{ name: 'Family' }` buttons (34 occurrences) to use the new names.
- Updated `navigates to dashboard when Home tab is clicked` to use `mockSplitWithData` (split with participants) so the navigation guard is not triggered.
- Added **Help Modal** describe block (4 tests):
  - Shows Help button when no participants and add form is not open
  - Opens help modal when Help button is clicked
  - Closes help modal when "Got it" button is clicked
  - Closes help modal when close button is clicked
- Added **Navigation Guard** describe block (4 tests):
  - Shows navigation guard dialog when clicking Home tab with no participants
  - Navigates when user confirms leaving with no participants
  - Dismisses guard dialog when user clicks Stay
  - Does not show navigation guard when split has participants
- Added **Members input arrow-key step** describe block (4 tests):
  - Increments Members by 0.5 on ArrowUp
  - Decrements Members by 0.5 on ArrowDown
  - Does not go below 0.5 on ArrowDown
  - Allows typing values with two decimal places

### `src/lib/components/participant/EditParticipantModal.test.ts`
- Added **Share input arrow-key step** describe block (4 tests):
  - Increments Share by 0.5 on ArrowUp
  - Decrements Share by 0.5 on ArrowDown
  - Does not go below 0.5 on ArrowDown
  - Allows typing values with two decimal places

### `src/lib/components/ui/split-page-header/SplitPageHeader.test.ts`
- Added **Navigation Guard** describe block (5 tests):
  - Navigates immediately when no guard is provided
  - Shows confirmation dialog when guard returns a warning string
  - Navigates after user confirms in guard dialog
  - Dismisses dialog without navigating when user clicks Stay
  - Navigates immediately when guard returns null

All 494 tests pass.
