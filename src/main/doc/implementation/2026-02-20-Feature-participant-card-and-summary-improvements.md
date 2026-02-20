# Feature: Participant card layout and summary card improvements

## What, Why and Constraints

**What**: Two visual improvements to the Participants page:
1. Participant cards now handle long names and narrow screens gracefully
2. The summary card no longer shows a redundant title on the Participants page; the page `<h1>` now shows the participant count. The Split dashboard keeps its existing title/count display.

**Why**: With names up to 50 characters and three action buttons on the right, the original single-row layout was too crowded on narrow screens. The "Participants" label and count inside the card was also redundant when displayed on the Participants page — the page header is the natural place for that information.

**Constraints**:
- `ParticipantSummaryCard` is a shared component (used by both Split dashboard and Participants page); the title visibility must be configurable via a prop
- No backend changes required

## How

### Modified files

1. **`fairnsquare-app/src/main/webui/src/routes/Participants.svelte`**
   - Participant card Row 1 split into two rows:
     - **Row 1**: name (with `truncate min-w-0 flex-1`) + action buttons (`flex-none`) — name now truncates cleanly instead of overflowing
     - **Row 2**: badges (nights / persons / expenses) with `flex-wrap` — they wrap gracefully on narrow screens instead of fighting for space
   - Page `<h1>` changed from `Participants` to `Participants ({split.participants.length})`
   - `ParticipantSummaryCard` now receives `showTitle={false}` to hide the redundant title/count

2. **`fairnsquare-app/src/main/webui/src/lib/components/participant/ParticipantSummaryCard.svelte`**
   - Added `showTitle` boolean prop (default `true`) to control title/count visibility
   - When `showTitle=false`: renders only the names summary line; hides the card entirely when there are no participants
   - When `showTitle=true` (default, Split dashboard): unchanged — shows label, count, and names

### Unmodified files
- `Split.svelte` — no changes needed; `showTitle` defaults to `true` preserving existing behaviour

## Tests

- **9 tests** updated or added across 3 files
- **`ParticipantSummaryCard.test.ts`**: Restored original 7 tests (default `showTitle=true` mode) + 2 new tests for `showTitle=false` mode
- **`Participants.test.ts`**: Updated header test to assert `Participants (N)` format; updated summary card tests to assert count is absent when `showTitle=false`
- **290 total frontend tests** — all pass
