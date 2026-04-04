# Bugfix: Remove Share Field from Single Participant Form (#88 follow-up)

## What, Why and Constraints

**What:** Removed the Share input field from the "Single" participant add form. Single participants now always submit `share: 1` implicitly. The Share/Members concept is only exposed in the "Family" form.

**Why:** Issue #88 comment — "The change is ok, but I would remove the shares from single participant." A solo traveller always represents 1 person; the Share concept is only meaningful for family groups with children (0.5 per child). Exposing Share in the Single form added unnecessary complexity for the common case.

**Constraints:**
- The backend API is unchanged — `addParticipant` still accepts `share`. Single mode simply always sends `share: 1`.
- The Family form retains the Members field (min 0.5, step 0.5) with its help text.

## How

### Files modified

**`Participants.svelte`**
- Removed `let formShare = $state(1)` (state field)
- Removed `activeFieldInfo` type variant `'share'`
- Removed `fieldInfo.share` entry
- Removed `validationErrors` type field `share`
- Removed `validateShareOnInput()` function
- Removed `else` branch (share validation for single mode) from `validateAddForm()`
- Changed `addMode === 'family' ? formMembers : formShare` → `addMode === 'family' ? formMembers : 1` in `handleAddParticipant`
- Removed `formShare = 1` reset in `handleAddParticipant`
- Removed the `{:else}` Share field block from the Nights/Share/Members row in the template

### Files modified (tests)

**`Participants.test.ts`**
- Renamed "Share field (single mode)" describe → "Single form"
- Removed test "shows Share input field in single add form"
- Removed test "defaults Share to 1 in single add form"
- Updated test "sends share in API call" → "sends share=1 in API call for single participant": removed Share field interaction, asserts `share: 1` without filling the field
- Removed test "shows share too low error while typing"
- Removed test "shows share too high error while typing"

## Tests

| File | Tests | Coverage |
|---|---|---|
| `Participants.test.ts` | 460 passing (removed 4, updated 1) | Share field absent in single form, API call submits share=1 |

**Total: 460 tests passing (full suite).**
