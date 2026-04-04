# Feature: Participant Addition UX Improvement

## What, Why and Constraints

**What:** Improved the participant addition flow on the Participants page (issue #88):

1. **Removed auto-open on split creation** — `Home.svelte` no longer passes `search: { addParticipant: 'true' }` when navigating to participants, and `Participants.svelte` no longer has a `$effect` that auto-opens the add form on arrival.
2. **Two add buttons** — replaced the single "Add Participant" button with "Single" and "Family" buttons, each opening a tailored form.
3. **Add Single form** — unchanged: name, nights, share (same as before).
4. **Add Family form** — same name and nights fields, but the share field is relabeled "Members" (step 0.5, min 0.5) with a detailed help text: children count for 0.5, concrete example (2 adults + 2 children = 3 members), and a note that a parent leaving early should be added as a separate single participant. Both forms call the same `addParticipant` API.

**Why:** Issue #88 — adding participants was not user-friendly. The auto-open on creation was surprising. There was no easy way to add a family group with an intuitive "members" label; users had to understand the "share" concept themselves.

**Constraints:**
- The backend API is unchanged — `addParticipant` accepts the same payload. "Members" is stored as `share`.
- `addMode: 'single' | 'family' | null` replaces the boolean `showAddForm`, driving both button visibility and form variant.
- `formMembers` (family) and `formShare` (single) are separate state fields to avoid contamination between modes.

## How

### Files modified

**`Home.svelte`**
- Removed `search: { addParticipant: 'true' }` from the `navigate()` call after split creation

**`Participants.svelte`**
- Removed `$effect` that auto-opened the add form when `route.search?.addParticipant` was set
- Replaced `showAddForm: boolean` with `addMode: 'single' | 'family' | null`
- Added `formMembers = $state(1)` for family mode
- Extended `validationErrors` type to include `members`
- Added `members` entry to `fieldInfo` with help text (children = 0.5)
- Extended `activeFieldInfo` type to `'nights' | 'share' | 'members' | null`
- Split `handleShowAddForm` into `handleShowSingleForm` and `handleShowFamilyForm`
- Updated `handleCancelAddForm` to reset `addMode = null` and `formMembers`
- Added `validateMembersOnInput` validation function
- Updated `validateAddForm` to validate `formMembers` in family mode, `formShare` in single mode
- Updated `handleAddParticipant` to use `formMembers` as share in family mode
- Updated template: two buttons ("Single" with `Plus` icon, "Family" with `Users` icon) → form shows "New Participant" or "New Family" title; Members field replaces Share field in family mode

### Files modified (tests)

**`Home.test.ts`**
- Removed `search: { addParticipant: 'true' }` from the navigation assertion after split creation

**`Participants.test.ts`**
- Removed `Auto-open from Home creation flow` describe (2 tests deleted)
- Renamed "Add Participant" describe tests to use `'Single'` button
- Updated button queries from `/Add Participant/i` → `'Single'`
- Updated "shows Add Participant button" → "shows Single and Family buttons" (asserts both)
- Updated "closes form" test to assert both `'Single'` and `'Family'` buttons after cancel
- Renamed "Number of Persons" describe → "Share field (single mode)"
- Added new "Add Family" describe with 7 tests: Family button visible, Members field shown (not Share), "New Family" title, Members default 1, API call submits members as share, Members validation (too low, too high)

## Tests

| File | Tests | Coverage |
|---|---|---|
| `Home.test.ts` | 18 (updated 1) | Navigation no longer passes addParticipant param |
| `Participants.test.ts` | 62 (removed 2, updated ~20, added 7) | Single/Family buttons, family form fields, Members validation, API submission |

**Total: 463 tests passing (full suite).**
