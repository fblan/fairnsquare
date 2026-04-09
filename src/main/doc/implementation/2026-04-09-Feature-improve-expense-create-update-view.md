# Feature: Improve Expense Create/Update View (#114)

## What, Why and Constraints

**What:** Two UI improvements to the expense experience:
1. Renamed the "By Share" split mode to "By Participant members" (label, aria-label, and info modal title/description) in the expense create/update modal.
2. Added an always-visible participant amounts list on each expense card — showing each participant's owned amount (non-zero only), wrapping across lines when there are many participants.

**Why:** The label "By Share" was ambiguous. "By Participant members" better communicates that the cost is split based on how many people each participant represents (share weight). The participant amounts on the expense card give at-a-glance visibility into who owes what, without requiring any interaction.

**Constraints:**
- Frontend rules followed: responsive layout verified on mobile (390×844) and desktop (1280×800).
- No backend changes required — all data was already available via `expense.shares[].amount`.
- `ExpenseCard.svelte` exists as a component but is not used in the UI (only in tests); the actual expense cards are rendered inline in `ExpenseList.svelte`. Changes were applied to the correct file.

---

## How

### Files Modified

**`fairnsquare-app/src/main/webui/src/lib/components/expense/ExpenseEditModal.svelte`**
- Changed `<span>By Share</span>` → `<span>By Participant members</span>` in the radio group.
- Updated `aria-label="Info about By Share"` → `aria-label="Info about By Participant members"`.
- Updated `splitModeInfo.BY_SHARE.title` → `"By Participant members"`.
- Updated `splitModeInfo.BY_SHARE.description` to include a restaurant example: *"This is ideal for shared expenses where everyone was present, like a restaurant meal."*

**`fairnsquare-app/src/main/webui/src/lib/components/ui/expense-card/ExpenseCard.svelte`**
- Added `BY_SHARE` case to `formatSplitMode()` → returns `"By Members"` (component exists but is not rendered in the UI; fixed for correctness).

**`fairnsquare-app/src/main/webui/src/routes/ExpenseList.svelte`**
- Updated `splitModeText()` for `BY_SHARE` → `"By Members"`.
- Replaced `getParticipantNames()` (which returned names only, or "Everyone") with `getParticipantSharesText()` which returns `"Alice: €30.00 · Bob: €45.00"` format, filtered to shares with `amount > 0`.
- Added a new Row 4 in the expense card template to render the participant amounts paragraph, with `leading-relaxed` allowing natural line-wrapping for expenses with many participants.

**`fairnsquare-app/src/main/webui/src/lib/components/expense/EditExpenseModal.test.ts`**
- Updated all `{ name: /by share/i }` role queries → `{ name: /by participant members/i }`.
- Updated test description strings accordingly.

**`fairnsquare-app/src/main/webui/src/lib/components/expense/AddExpenseModal.test.ts`**
- Same updates as `EditExpenseModal.test.ts`.

**`fairnsquare-app/src/main/webui/src/routes/ExpenseList.test.ts`**
- Replaced `'shows participants as Everyone when all participate'` test with `'shows participant amounts on expense cards'`, asserting the new `"Alice: €60.00 · Bob: €30.00"` format.
- Added `'filters out zero-amount participants from card display'` — verifies that a participant with `amount: 0` does not appear in the shares text.
- Added `'shows By Members badge for BY_SHARE expenses'` — verifies the badge text for BY_SHARE mode.

---

## Tests

All automated tests pass:

| File | Tests |
|---|---|
| `EditExpenseModal.test.ts` | Updated 4 assertions for "By Participant members" label |
| `AddExpenseModal.test.ts` | Updated 4 assertions + test descriptions |
| `ExpenseList.test.ts` | Replaced 1 test, added 2 new tests |
| **Total** | **150 tests passing** |

Visual verification performed with Playwright on:
- Desktop: 1280×800 — expense cards and modal confirmed correct
- Mobile: 390×844 — layout wraps correctly, no overflow issues