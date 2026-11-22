# Feature: copy the persons shares into a Manual expense (#185)

## What, Why and Constraints

In a Manual (`FREE`) expense, the Edit Shares window could only fill the parts with each participant's
nights × share ("Copy nights shares"). An expense shared by members whatever their nights (a meal with only some
participants, then adjusted by hand) had to be typed participant by participant. "Copy persons shares" fills the
parts with each participant's share (number of persons) alone.

Constraints: frontend only (the parts are sent as before); same behaviour as the existing copy: a warning to
confirm first, every participant included, and the values are a snapshot.

## How

- `components/share-dialog.js`:
  - a second button, **Copy persons shares** (users icon), next to **Copy nights shares**; the two wrap side by
    side or stacked depending on the width;
  - a `COPIES` table says, per button, what the warning names and how the parts are computed (`nights × share`,
    or `share`); the single warning shows the chosen one's text, and **Apply** applies it (`askCopy` / `applyCopy`
    replace `copyNights`).
- `components/expense-dialog.js`: choosing **Manual** moves the focus to **Edit shares**. The shares block appears
  under the split modes, often below the screen on a phone; the focus scrolls the button into view and lets the
  keyboard go straight to it. Only on the person's choice, not when an existing Manual expense is opened.
- `E2eTestBase.addParticipant(splitId, name, nights, share)`: an overload to create a participant of several
  persons (the existing one keeps share 1).

## Tests

`ExpensesPageTest` (Playwright):

- `manualModeCopiesThePersonsShares`: Alice (3 nights, 2 persons) and Bob (2 nights, 1 person); the copy gives
  parts 2 and 1 (not 6 and 2), includes Bob, and the €90 expense is shared €60 / €30.
- `cancellingACopyKeepsTheParts`: cancelling the warning hides it and leaves the parts unchanged.
- `choosingManualBringsTheEditSharesButtonIntoView`: at 390×844, choosing Manual focuses **Edit shares** and it is
  in the viewport (fails without the change).

`ExpensesPageTest` (20) and `LayoutTest` (13) pass. Checked visually at 390×844 and 1280×800 with a long
participant name.
