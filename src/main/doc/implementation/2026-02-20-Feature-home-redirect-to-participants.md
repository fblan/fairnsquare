# Feature: Redirect to Participants page after split creation

## What, Why and Constraints

**What**: After creating a split on the Home page, the user is now redirected directly to the Participants page with the "Add Participant" form already open, instead of the Split dashboard.

**Why**: The split dashboard alone is not useful right after creation — the natural next step is to add more participants. Pre-opening the form removes one click and makes the flow more intuitive.

**Constraints**:
- The add form must open automatically without requiring a user interaction
- The URL clean-up (removing the `?addParticipant=true` query param) must not trigger a full re-navigation
- Existing Participants page behavior must be unaffected when navigating there normally

## How

### Modified files

1. **`fairnsquare-app/src/main/webui/src/routes/Home.svelte`**
   - Changed `navigate` destination from `/splits/:splitId` to `/splits/:splitId/participants`
   - Added `search: { addParticipant: 'true' }` to the navigate options to signal the auto-open intent

2. **`fairnsquare-app/src/main/webui/src/routes/Participants.svelte`**
   - Added a `$effect` that checks `route.search?.addParticipant`
   - When the param is present, calls `handleShowAddForm()` (which already handles focus and form defaults)
   - Cleans up the URL param via `window.history.replaceState` without triggering re-navigation

3. **`fairnsquare-app/src/main/webui/src/routes/Home.test.ts`**
   - Updated `navigate` assertion to check for the new participants destination and search param

4. **`fairnsquare-app/src/main/webui/src/routes/Participants.test.ts`**
   - Added `search: {}` to the `route` mock declaration
   - Reset `route.search` to `{}` in `beforeEach` to prevent test leakage
   - Added describe block "Auto-open from Home creation flow" with 2 tests:
     - Auto-opens form when `addParticipant` param is present
     - Does not auto-open form when param is absent

## Tests

- **17 tests** in `Home.test.ts` — all pass
- **47 tests** in `Participants.test.ts` — all pass (2 new)
- **287 total frontend tests** — all pass
