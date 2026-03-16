# 2026-03-16 — Feature: Export Settlement

## What, Why and Constraints

**What:** Added an "Export Settlement" button on the Settlement page that copies a formatted summary of the settlement to the clipboard.

**Why:** Once a split is resolved, users need a convenient way to share the settlement details with other participants (via messaging apps, email, etc.) without having to manually copy numbers from the screen.

**Constraints:**
- The button is only shown after the user clicks "Resolve" (i.e., when `showReimbursements` is true), replacing the Resolve button slot.
- Clipboard access follows the same defensive pattern used in `SplitPageHeader`: check for `navigator.clipboard`, fall back to an info toast with the raw text if unavailable or if the write fails.
- The formatted text uses the existing `formatCurrency` helper for consistent number formatting.

**Clipboard text format:**
```
=== <split name> ===
<N> expense(s) — Total: €<total>
<N> participant(s)

Settlements:
<FromName> → <ToName>: €<amount>
...
```
If no reimbursements are needed, the Settlements block is replaced with `All settled — no transfers needed!`.

Reimbursements are sorted alphabetically by payer name (`fromName`).

## How

### Files modified

**`fairnsquare-app/src/main/webui/src/routes/Settlement.svelte`**

1. Added `formatSettlementText(): string` — builds the clipboard text from `split` (name, expenses, participants) and `settlement` (reimbursements). Reimbursements are sorted alphabetically by `fromName` using `localeCompare`.
2. Added `handleExportSettlement(): Promise<void>` — calls `formatSettlementText()`, attempts `navigator.clipboard.writeText()`, shows a success toast on success, or an info toast with the raw text as fallback (same pattern as `SplitPageHeader.handleShare`).
3. Updated the Resolve/Export button slot in the template: the `{:else}` branch (when `showReimbursements` is true) now renders the "Export Settlement" button instead of nothing.

### Files modified (tests)

**`fairnsquare-app/src/main/webui/src/routes/Settlement.test.ts`**

Added a new `describe('Export Settlement')` block with 7 tests (see Tests section).

## Tests

**File:** `src/routes/Settlement.test.ts` — `describe('Export Settlement')` block — 7 tests

| Test | What it covers |
|------|----------------|
| does not show Export Settlement button before clicking Resolve | Button is hidden until the resolved view is active |
| shows Export Settlement button after clicking Resolve | Button appears once reimbursements are shown |
| copies correct formatted text to clipboard when Export Settlement is clicked | Full end-to-end: text format, correct split name, expense count, total, participant count, reimbursement line |
| shows info toast with text when clipboard is unavailable | Fallback path when `navigator.clipboard` is undefined |
| formats text with plural expense/participant labels correctly | "expenses" / "participants" pluralisation and multi-reimbursement layout |
| formats text with "All settled" message when no reimbursements needed | Empty reimbursements list produces the correct alternative message |
| sorts reimbursements alphabetically by payer name | Ensures Charlie appears before Zoe in the output |

All 32 tests in the file pass (25 pre-existing + 7 new).
