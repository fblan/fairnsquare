# Feature: Split Header on All Pages

## What, Why and Constraints

**What:** The split name and Share button were only visible on the Split dashboard page. This feature makes them visible on all split-related pages (Expenses, Participants, Settlement) by extracting a shared `SplitPageHeader` component.

**Why:** Users navigating to sub-pages (expenses, participants, settlement) lost context of which split they were working on and had no way to share the current page URL. The header now provides consistent identity and sharing capability across all pages.

**Constraints:**
- Mobile-first layout: all pages use a max-width constraint; increased from 420px to 520px to give more breathing room
- Touch targets must be at least 44px (min-h-[44px], min-w-[44px])
- Layout rules: variable-width content (split name) uses `min-w-0` and `break-words`; fixed-width buttons use `shrink-0`
- Settlement page only calls `getSettlement()` and does not have the split name — an additional `getSplit()` call is made in parallel via `Promise.all`

## How

### Files created

- **`src/lib/components/ui/split-page-header/SplitPageHeader.svelte`**
  Shared header component. Props: `splitName`, `splitId`, `showBackButton` (default `false`), `children` (optional Svelte 5 snippet for extra right-side action buttons). Contains the Share logic (clipboard API with fallback toast) previously duplicated in `Split.svelte`. Back button navigates to `/splits/:splitId`.

- **`src/lib/components/ui/split-page-header/SplitPageHeader.test.ts`**
  7 unit tests covering: split name rendering, Share button presence, clipboard success/failure, back button visibility, back button navigation.

### Files modified

- **`Split.svelte`** — replaced inline header + removed `handleShare` function; now uses `<SplitPageHeader splitName={split.name} {splitId} />`
- **`ExpenseList.svelte`** — replaced inline header; uses `<SplitPageHeader ... showBackButton>` with the `+ Add` button passed as a `children` snippet; removed `ArrowLeft` import and dead `handleBack` function
- **`Participants.svelte`** — replaced inline header; uses `<SplitPageHeader ... showBackButton />`; removed `ArrowLeft` import and dead `handleBack` function
- **`Settlement.svelte`** — replaced inline header; added `getSplit` import and parallel fetch via `Promise.all([getSettlement, getSplit])` to obtain `splitName`; removed `ArrowLeft` import and dead `handleBack` function
- **`Home.svelte`** — updated `max-w-[420px]` → `max-w-[520px]`
- **`app.css`** — updated `#app { max-width: 420px }` → `max-width: 520px`

### Layout adjustments (requested during implementation)

- Split name title: `text-xl truncate` → `text-lg break-words` (smaller, wraps instead of truncating)
- Page max-width: 420px → 520px across all pages and the `#app` CSS rule

## Tests

**File:** `src/lib/components/ui/split-page-header/SplitPageHeader.test.ts` — 7 tests

| Test | Covers |
|------|--------|
| renders the split name | Name prop displayed |
| renders a Share button | Share button always present |
| copies URL to clipboard on Share click | Clipboard API success path |
| shows URL in toast when clipboard fails | Clipboard API failure fallback |
| no back button by default | `showBackButton` defaults to false |
| renders back button when showBackButton=true | Conditional back button |
| back button navigates to split dashboard | `navigate('/splits/:splitId')` called |