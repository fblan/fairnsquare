# Feature: Split Navigation Menu

## What, Why and Constraints

**What:** Added a tab navigation bar to `SplitPageHeader` allowing users to switch between the four per-split views — Dashboard, Participants, Expenses, Settlement — without going back to the dashboard first. The active tab is highlighted based on the current route pathname. The back button (`showBackButton` prop) was removed since the Dashboard tab replaces it.

**Why:** Issue #93 — there was no way to switch between views without navigating back to the dashboard first. This added unnecessary friction for users managing a split.

**Constraints:**
- Navigation only applies to per-split pages (not the home page)
- `SplitPageHeader` already had `splitId` as a prop, so building the tab paths required no new data
- Active tab derived from `route.pathname` suffix (no dependency on `isActive` from the router, which is not reliably testable with the current mock setup)
- `<svelte:component>` deprecated in Svelte 5 runes mode — used `{@const Icon = tab.icon}` pattern instead

## How

### Files modified

**`SplitPageHeader.svelte`**
- Added `route` import from `$lib/router`
- Added `activeTab` derived from `route.pathname` (suffix matching: `/participants`, `/expenses`, `/settlement`, else `dashboard`)
- Added `tabs` array with id, label, icon, and path factory for each view
- Added tab bar rendered below the title row using `aria-current="page"` for the active tab
- Removed `showBackButton` prop and back button rendering

**`ExpenseList.svelte`, `Participants.svelte`, `Settlement.svelte`**
- Removed `showBackButton` from `<SplitPageHeader>` usage (3 files)

### Files modified (tests)

**`SplitPageHeader.test.ts`**
- Used `vi.hoisted()` to define `mockRoute` (needed because `vi.mock` factories are hoisted above variable declarations)
- Removed back-button tests (prop removed)
- Added 14 tests covering: all 4 tabs rendered, nav landmark aria-label, correct `navigate` calls per tab, `aria-current="page"` on active tab per pathname, share button behaviour

**`ExpenseList.test.ts`, `Participants.test.ts`, `Settlement.test.ts`**
- Replaced "back button" tests with equivalent "Dashboard tab navigation" tests (4 tests updated)

## Tests

| File | Tests | Coverage |
|---|---|---|
| `SplitPageHeader.test.ts` | 14 | Tab rendering (AC1), navigate calls (AC2), active tab per pathname (AC3), share button (AC4), split name (AC5) |
| `ExpenseList.test.ts` | updated | Dashboard tab replaces back button navigation test |
| `Participants.test.ts` | updated | Dashboard tab replaces back button navigation test |
| `Settlement.test.ts` | updated | 2 tests updated (header assertion + dashboard navigation) |

**Total: 456 tests passing (full suite).**
