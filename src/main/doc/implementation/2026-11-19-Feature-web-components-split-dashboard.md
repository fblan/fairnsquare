# Feature: web components step 3 — split header and split dashboard (#166)

Part of the migration to plain web components (#161).

## What, Why and Constraints

Moves the header shared by every split page and the split dashboard (`/splits/<id>`) to web components. The header is used both by the new dashboard and by the Expenses, Participants and Settlement pages that are still Svelte.

Constraints: same URLs, look and behaviour; `NavigationTest` passes unchanged. Svelte can only pass plain values to a custom element as attributes, so the header takes attributes, not functions.

## How

New files under `fairnsquare-app/src/main/resources/META-INF/resources/`:
- `components/split-header.js`: `<fns-split-header split-id split-name [require-participant]>`, replacing `SplitPageHeader.svelte`. Share copies the link (`Link copied!`), or shows it in a toast when the clipboard is unavailable. The active tab gets `aria-current="page"` and follows navigation (`popstate`). The Svelte `navigateGuard` function becomes the `require-participant` attribute: the Participants page sets it while the split has no participant, and leaving is then blocked by the "A participant is required" dialog (a native `<dialog>`, bottom sheet on phones; closes with Got it, Escape or a click outside).
- `components/participant-summary.js`: `<fns-participant-summary>`, the participants card, set through its `participants` property (a value set before the element is defined is replayed in `connectedCallback`). The Svelte `ParticipantSummaryCard` stays for the Participants page until step 5.
- `biz/split/split-page.js`: `<fns-split-page split-id>`, replacing the body of `routes/Split.svelte` (which now only renders it): expenses card (count, total), participants card, settlement card (Solve / Settled) once there are expenses and participants; loading state after 500 ms; error card with Retry; unknown split → home with an info toast; remembers the split for the home page. It loads once per split id: Svelte sets the attribute after the element is in the page, and loading on both the attribute and the connection showed the "Split not found" toast twice (caught by `NavigationTest`).
- `infra/stores/split-names.js`: the split name cache, so a page shows its title while loading, moves from a Svelte module's memory to sessionStorage, shared with `lib/stores/splitTitleCache.ts` (same key).
- `components/icons.js` now takes any SVG shapes (circles, lines), with the header icons and `chevron-right`.
- Svelte: the Expenses, Participants and Settlement pages render `<fns-split-header>`; `SplitPageHeader.svelte`, `lastSplitStore.ts` and the `Share2`, `Home` and `ArrowLeftRight` icons are deleted.

## Tests

- `NavigationTest`: 10 tests (5 existing, unchanged, + 5): the current page is marked in the tabs on each page; the tabs follow navigation without reload; Share copies the page address to the clipboard; the blocked-navigation dialog closes with Got it; adding a participant lifts the block.
- `SplitPageTest` (new, 5 tests): expenses total and participant summary; no settlement card without expenses; "Settled" once resolved; a loading error can be retried (API stubbed); an opened split is offered on the home page.
- Checked visually with before/after screenshots at 390×844 and 1280×800: dashboard, Expenses page header, blocked-navigation dialog.
