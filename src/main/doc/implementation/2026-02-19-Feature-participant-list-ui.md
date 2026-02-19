# Feature: Participant List UI Improvements

## What, Why and Constraints

**What**: The participant list page (`Participants.svelte`) received a series of UI improvements:
1. **Stats row redesign** — replaced text labels ("Spent:", "Cost:") with lucide-svelte icons (`Wallet`, `Receipt`, `TrendingUp`, `TrendingDown`, `Minus`), distributed across the full card width
2. **Two-line card layout** — restructured each participant card into exactly 2 rows: row 1 for name + badges + action buttons, row 2 for financial stats at full width
3. **Summary card** — added a participant summary card (same as on the split dashboard) at the top of the page, showing count and alphabetically sorted names
4. **Alphabetical sort** — participant list and summary card always show participants sorted by name
5. **Add button at top** — moved the "Add Participant" button/form above the participant list
6. **Larger stats row** — bumped stats row from `text-sm` to `text-base` with `h-4 w-4` icons
7. **Expense count badge** — added a slate badge next to the nights badge showing the number of expenses paid by each participant
8. **Name formatting** — participant names displayed with first letter uppercase, rest lowercase (display-only, data unchanged)
9. **Auto-focus on add form** — name input receives focus automatically when the add form opens, using `bind:ref` + `tick()`
10. **Inline validation** — name and nights fields validate while typing (duplicate, length, out-of-range); "Name is required" is deferred to submit
11. **Toast message** — success toast on participant add now reads "{Name} successfully added" with a 4-second duration

**Why**: Improve usability and information density for a page developers and end users visit frequently. The stats row needed more visual clarity, the two-line layout prevents wrapping on realistic data, and inline validation gives faster feedback.

**Constraints**:
- Display-only changes: no backend modifications, no API changes
- `use:` directives cannot be applied to Svelte components — used `bind:ref` + `tick()` instead for auto-focus
- Sorting is purely presentational (`slice().sort()` on the derived list — original array not mutated)
- Name formatting is display-only (`formatName()` helper — stored data unchanged)
- All existing tests maintained; new tests added for each new behaviour

## How

### Step 1: Stats row icons (`Participants.svelte`)
- Imported `Wallet`, `Receipt`, `TrendingUp`, `TrendingDown`, `Minus` from `lucide-svelte`
- Replaced `"Spent: €X"` / `"Cost: €X"` text labels with icon + amount spans
- Balance: `TrendingUp` (green) for owed, `TrendingDown` (red) for owes, `Minus` (muted) for settled — no "Owed"/"Owes" text, only the amount
- Added `title="Spent"` / `title="Cost"` / `title="Balance"` for accessibility

### Step 2: Two-line card layout
- Replaced outer `flex items-start justify-between` with `flex flex-col gap-1`
- Row 1: `flex items-center justify-between` — name + badges on left, action buttons on right
- Row 2: `flex items-center justify-between` — 3 stat items spread full width (no wrapping)
- Stats row bumped to `text-base` / `h-4 w-4` icons

### Step 3: Summary card
- Added a non-clickable `Card.Root` with `border-teal-200 bg-teal-50/50` styling (matching dashboard) above the add form
- Shows participant count and comma-separated `Name (nights)` list in sorted order

### Step 4: Alphabetical sort
- Added `sortedParticipants` as a `$derived` that calls `.slice().sort((a, b) => a.name.localeCompare(b.name))`
- Used in both the `{#each}` loop and the summary card name list

### Step 5: Add button moved to top
- Moved the `{#if showAddForm} … {:else} <Button>Add Participant</Button> {/if}` block above the `{#each}` participant loop

### Step 6: Expense count badge
- Extended `getParticipantStats()` to return `expenseCount`: count of expenses where `e.payerId === participantId`
- Added a `bg-slate-100 text-slate-600` badge next to the nights badge in row 1

### Step 7: Name formatting
- Added `formatName(name)` helper: `name.charAt(0).toUpperCase() + name.slice(1).toLowerCase()`
- Applied to participant name display in cards and summary card (not to stored data or aria-labels)

### Step 8: Auto-focus on add form
- Imported `tick` from `svelte`
- Added `let nameInputEl = $state<HTMLInputElement | null>(null)`
- Made `handleShowAddForm` async; after `showAddForm = true`, calls `await tick()` then `nameInputEl?.focus()`
- Used `bind:ref={nameInputEl}` on the name `Input` component (shadcn Input exposes `ref` as a bindable prop)

### Step 9: Inline validation
- Added `validateNameOnInput()`: clears error when empty (defers "required"), shows duplicate/length errors immediately
- Added `validateNightsOnInput()`: shows out-of-range errors immediately
- Wired via `oninput={validateNameOnInput}` and `oninput={validateNightsOnInput}` on the respective inputs

### Step 10: Toast message + duration
- Captured `addedName` before clearing `formName` to avoid reading the already-reset value
- Changed toast message to `` `${addedName} successfully added` `` with `duration: 4000`

## Tests

**File**: `src/routes/Participants.test.ts` — grew from 29 to 38 tests (+9 new tests)

| Test | What it covers |
|------|---------------|
| `displays participant summary card with count and names` | Summary card shows count and name list |
| `displays summary card with 0 participants when empty` | Summary card handles empty state |
| `displays participants sorted alphabetically` | Summary name list is in alphabetical order |
| `displays expense count badge on each participant card` | Badge shows count of expenses paid |
| `shows duplicate name error while typing without submitting` | Inline validation on name input — duplicate |
| `shows length error while typing without submitting` | Inline validation on name input — max length |
| `does not show required error while field is empty during typing` | "Required" deferred to submit |
| `shows nights too low error while typing without submitting` | Inline validation on nights input — min |
| `shows nights too high error while typing without submitting` | Inline validation on nights input — max |

Updated existing tests:
- `displays Participants title` — changed `getByText` to `getByRole('heading')` to disambiguate from summary card label
- `displays participant stats: spent, cost, balance` — changed to `getAllByTitle('Spent'/'Cost')` since text labels were replaced by icons
- `shows positive balance in green and negative in red` — changed to `getAllByTitle('Balance')` + `textContent` assertions
- `calls API and refreshes list on successful submission` — updated toast assertion to new message + duration
