# Feature: Add numberOfPersons field to Home page

## What, Why and Constraints

**What**: Added the "Number of Persons" field to the Home page's create-split form, alongside the existing Nights field.

**Why**: The Participants page already supports `numberOfPersons` for family/group scenarios, but the Home page (which creates the first participant) was missing this field. Users had to navigate to the Participants page to edit the value after creation.

**Constraints**:
- Matched the same validation rules as the Participants page (0.5-50 range, step 0.5)
- Maintained the same side-by-side layout pattern used on the Participants page
- Label shortened from "Number of Nights" to "Nights" to fit the side-by-side layout

## How

### Modified files

1. **`fairnsquare-app/src/main/webui/src/routes/Home.svelte`**
   - Added `numberOfPersons` state variable (default: 1)
   - Added `numberOfPersonsTouched` state for validation display
   - Added `numberOfPersonsError` derived validation (0.5-50 range)
   - Included `numberOfPersons` in `isValid` derived check
   - Added `numberOfPersonsTouched = true` in `handleCreateSplit` to show errors on submit
   - Included `numberOfPersons` in the `addParticipant()` API call payload
   - Changed Nights field layout: now side-by-side with Persons in a `flex gap-3` container
   - Renamed label from "Number of Nights" to "Nights" for compact layout

2. **`fairnsquare-app/src/main/webui/src/routes/Home.test.ts`**
   - Updated field rendering test to include Persons field
   - Added test: defaults Persons to 1
   - Added test: validation error for persons < 0.5
   - Added test: validation error for persons > 50
   - Added test: sends numberOfPersons in API call with custom value
   - Updated mock return values to include `numberOfPersons: 1`
   - Updated `addParticipant` assertion to include `numberOfPersons`
   - Updated all label references from "Number of Nights" to "Nights"

## Tests

- **17 tests** in `Home.test.ts` — all pass
- **285 total frontend tests** — all pass
- All backend tests pass (pre-existing WebUITest discovery issue unrelated to this change)
