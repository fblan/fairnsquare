# Bugfix: Nights must be integer-only

## What, Why and Constraints

**What**: Restricted the nights field to integer values only across all participant forms (create on Home, add on Participants, and Edit modal). Previously the field accepted steps of 0.5, allowing values like 0.5, 1.5, 2.5, etc.

**Why**: Half-night values were considered redundant and confusing. Nights represent full overnight stays, so fractional values don't make practical sense for trip expense splitting.

**Constraints**:
- Existing participants with fractional nights stored in the backend are preserved (no migration). Their values continue to display as-is.
- When editing an existing participant with a legacy fractional value, the form pre-fills with `Math.round(participant.nights)` — enforcing integers without requiring a separate migration step.
- The `nightsDefaultStore` (localStorage) may contain an old fractional value from a previous session; on load it is rounded to the nearest integer and clamped to a minimum of 1.

## How

### Files modified

**`nightsDefaultStore.ts`**
- Changed `parseFloat(stored)` → `Math.max(1, Math.round(parseFloat(stored)))` in `loadNightsDefault` to silently migrate old 0.5-step stored values to valid integers.

**`Home.svelte`**
- Nights input: `step={0.5}` → `step={1}`, `min={0.5}` → `min={1}`.
- Validation: `nights < 0.5` → `nights < 1`, error message updated to `'Nights must be at least 1'`.
- `isValid` derived: `nights >= 0.5` → `nights >= 1`.

**`Participants.svelte`** (add participant form)
- Nights input: `step="0.5"` → `step="1"`, added `min="1"`.
- `validateNightsOnInput`: `< 0.5` → `< 1`, error message updated.
- `validateAddForm`: same.
- Removed the sentence *"Half-nights (0.5) are supported for arrivals or departures during the day."* from the Nights field info tooltip.

**`EditParticipantModal.svelte`**
- Nights input: `step="0.5"` → `step="1"`, `min="0.5"` → `min="1"`.
- `isNightsValid` derived: `>= 0.5` → `>= 1`.
- `validateNights`: `< 0.5` → `< 1`, error message updated.
- Pre-fill on modal open: `editNights = participant.nights` → `editNights = Math.max(1, Math.round(participant.nights))`.
- Removed the half-nights sentence from the Nights field info tooltip.

## Tests

**`nightsDefaultStore.test.ts`**
- Removed test: *"saves and reloads a half-night value (0.5 step)"* — half-night saving is no longer a supported use case.
- Removed test: *"preserves 0.5 minimum value"* — replaced by the rounding/clamping behaviour.
- Added test: *"rounds a fractional value stored from an old session to the nearest integer"* — verifies 3.5 → 4.
- Added test: *"clamps a sub-1 value stored from an old session to 1"* — verifies 0.5 → 1.

**`Home.test.ts`**
- Updated test name and assertion: `'Nights must be at least 0.5'` → `'Nights must be at least 1'`.

**`Participants.test.ts`**
- Updated three test names and assertions referencing `0.5` minimum to use `1`.

**`EditParticipantModal.test.ts`**
- Updated one test name and assertion: `'nights less than 0.5'` / `/at least 0\.5/i` → `'nights less than 1'` / `/at least 1/i`.
