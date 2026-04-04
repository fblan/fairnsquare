# Feature: Mobile Navigation Menu Fix

## What, Why and Constraints

**What:** Fixed the per-split navigation tab bar in `SplitPageHeader` to be usable on mobile. On small screens, only icons are shown; labels appear at `sm:` breakpoint and above. The "Dashboard" tab was renamed to "Home" (with the `Home` icon). A close button (X icon only, no label) was added at the trailing end of the nav bar to navigate back to `/`.

**Why:** Issue #101 — the navigation menu was too wide on mobile because all tabs rendered their text labels at every viewport size, causing overflow or crowding.

**Constraints:**
- Labels must remain visible on desktop (`sm:` breakpoint and above)
- Each tab button must still have an accessible `aria-label` so screen readers announce the tab name even when the label text is hidden
- The close button is icon-only at all screen sizes (no label text, just `aria-label="Close split"`)

## How

### Files modified

**`SplitPageHeader.svelte`**
- Replaced `LayoutDashboard` icon import with `Home`; added `X` import for the close button
- Renamed the dashboard tab label from `"Dashboard"` to `"Home"` and swapped icon to `Home`
- Added `aria-label={tab.label}` to each tab `<button>` so accessibility is maintained when the label `<span>` is hidden
- Wrapped each tab label text in `<span class="hidden sm:inline">{tab.label}</span>` — hidden on mobile, visible on `sm+`
- Added a close button after the tab loop: icon-only (`X`), `aria-label="Close split"`, `ml-auto`, navigates to `'/'`

### Files modified (tests)

**`SplitPageHeader.test.ts`**
- Updated all `/dashboard/i` queries to `/^home$/i` to match the renamed tab label
- Added test: "renders a close button" — asserts `getByRole('button', { name: 'Close split' })`
- Added test: "renders a nav landmark with aria-label" — asserts `getByRole('navigation', { name: 'Split navigation' })`
- Added test: "navigates to home page when Close split button is clicked" — asserts `navigate('/')` called on click
- Updated "marks Participants tab as active" test: changed `/dashboard/i` reference to `/^home$/i`

**`ExpenseList.test.ts`, `Participants.test.ts`, `Settlement.test.ts`**
- Replaced remaining `/dashboard/i` button queries with `/^home$/i` (test descriptions updated from "Dashboard tab" to "Home tab")

## Tests

| File | Tests | Coverage |
|---|---|---|
| `SplitPageHeader.test.ts` | 16 | Tab rendering (AC1), navigate calls (AC2), active tab per pathname (AC3), share button (AC4), split name (AC5), close button render & navigation |
| `ExpenseList.test.ts` | updated | Home tab replaces Dashboard tab label |
| `Participants.test.ts` | updated | Home tab replaces Dashboard tab label |
| `Settlement.test.ts` | updated | Home tab replaces Dashboard tab label |

**Total: 458 tests passing (full suite).**
