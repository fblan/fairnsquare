# Settlement page as a web component (#176, step 6 of #161)

## What, Why and Constraints

The Settlement page (`/splits/<id>/settlement`) moves from Svelte to a plain web component, like the Home, Split,
Expenses, Participants and Admin pages before it. After this step the Svelte code is only the shell (App, router,
footer, API client), removed in step 7.

Same behaviour as before:

- one balance card per participant (paid, cost, "Owed …" / "Owes …" / "Settled"); members of a shared account are merged
  into one card ("Alice & Bob");
- before resolving: **Resolve**, and "Group with..." / "Ungroup" on the cards;
- after resolving: who pays whom on each card, **Export Settlement** and **Unsettle**;
- opening the page never resolves (no POST on load: that would save a settlement and undo an intentional unsettle); an
  already resolved split shows its saved reimbursements.

Small changes:

- the "Group with" window is a native `<dialog>` (Escape closes it, focus stays inside, the Partner list has focus), and
  OK stays disabled until a partner is chosen (OK with "(none)" used to send a useless update);
- an unknown split sends the person home with "Split not found — create a new one.", like the other pages;
- Resolve shows a spinner, and the group buttons are disabled while a grouping is saved;
- when the clipboard is not available, the export shows the summary in a toast: toasts now keep line breaks.

Constraints: rules in `src/doc/rules/frontend-rules.md`; values from the server go in with `textContent`.

## How

- `META-INF/resources/biz/settlement/settlement-page.js` (new): `<fns-settlement-page split-id>`.
  - Loads the split, and the saved settlement when `split.settlement` is set; balances come from the participants'
    computed fields.
  - `entries()` merges shared-account members; an entry's id is the shared account id, which is what the
    reimbursements use for a group.
  - Grouping: joining a partner already in a group reuses their account id, otherwise a new 21-character id is made
    for both. Ungroup clears it for each member. Then the page reloads.
  - `settlementText()` builds the export text (same format as before).
- `web/app/routes/Settlement.svelte`: now only hosts `<fns-settlement-page>`.
- `web/index.html`: loads the new script.
- `components/toaster.js`: `.message` keeps line breaks (`white-space: pre-line`).
- Deleted Svelte code no longer used by anything: `lib/api/splits.ts`, `lib/components/ui/{button,card,input,label}`,
  `lib/utils.ts`, `lib/utils/formatCurrency.ts`, `lib/stores/` (toast store, split title cache).

## Tests

`SettlementPageTest` (Playwright), 11 tests:

- kept: balances before resolving, resolving shows who pays whom, a settled split is read-only until unsettled, grouping
  two participants (now checks the merged "Alice & Bob" card and the disabled OK);
- new: an already resolved split shows its transfers, opening the page does not resolve, export copies the summary
  (clipboard stubbed), export without a clipboard shows it in a toast, joining an existing group, ungroup, unknown split
  goes home.

Before/after screenshots compared at 390 and 1280 px wide (unresolved, group window, resolved).
