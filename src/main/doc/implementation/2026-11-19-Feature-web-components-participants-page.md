# Feature: web components step 5 — Participants page and its windows (#174)

Part of the migration to plain web components (#161).

## What, Why and Constraints

Moves the Participants page, the edit-participant window and the participant-expenses window to web components. Only the Settlement page is left in Svelte after this step.

Constraints: same URL (`/splits/<id>/participants`), behaviour and look; `ParticipantsPageTest` keeps checking the same things.

## How

New files under `fairnsquare-app/src/main/resources/META-INF/resources/`:
- `biz/participants/participants-page.js`: `<fns-participants-page split-id>`, replacing the body of `routes/Participants.svelte` (now only renders it): settled banner, participants summary, add form (single / family, nights remembered, inline validation, name focused), help window, empty state, participant cards (badges, spent / cost / balance, buttons), delete confirmation (a 409 closes it with the "has associated expenses" toast), the edited card highlighted for 2 s. It sets `require-participant` on the header while the split has no participant.
- `biz/participants/participant-dialog.js`: `<fns-participant-dialog>`, replacing `EditParticipantModal.svelte`; `openEdit(...)` and a `participantchange` event. Name has `autofocus` instead of a 50 ms focus timer — the same race as #168: in CI on #173, `editsAParticipant` saved "Alicia4", the nights typed into the name when the timer fired. A field the person empties says it is required when left, since Save stays disabled while invalid (same rule as #172).
- `biz/participants/participant-expenses-dialog.js`: `<fns-participant-expenses-dialog>`, replacing `ParticipantExpensesModal.svelte`.
- `components/info-dialog.js`: `<fns-info-dialog>`, the explanation bottom sheet, with the Nights / Members / Share texts shared by both participant forms.
- `infra/stores/nights-default.js`: same localStorage key as `nightsDefaultStore.ts`.
- `components/icons.js`: the icons of the cards, plus `solidIcon()` for the filled pencil and bin of the cards.
- Removed: `EditParticipantModal.svelte`, `ParticipantExpensesModal.svelte`, `ParticipantSummaryCard.svelte`, `confirm-dialog.svelte`, `nightsDefaultStore.ts`, and the whole `lib/icons` folder (no Svelte page uses an icon any more).

## Tests

- `E2eTestBase`: the page-level helpers (`button`, `field`, `text`) only match visible elements. A closed `<dialog>` stays in the page (hidden), so the edit window's "Name" field also matched the add form's; the Svelte windows were removed from the page when closed.
- `NavigationTest`: the two blocked-navigation tests wait for the page to load before clicking a tab (the block only exists once the page knows there is no participant; they passed by timing before).
- `ParticipantsPageTest`: 20 tests (10 existing + 10): family members of at least 0.5; nights remembered for the next participant; help and field-info windows; card badges, cost and balance; a participant's expenses window, and the empty one; edit window: discard confirmation, emptied name says it is required, Delete disabled with a reason when they paid an expense, delete from the window; the edited card is highlighted (and the share's arrow-key step).
- Checked visually with before/after screenshots at 390×844 and 1280×800: list, family form, field info, help, edit window, participant expenses.
