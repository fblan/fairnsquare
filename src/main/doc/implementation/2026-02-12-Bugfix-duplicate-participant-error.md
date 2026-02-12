# Bugfix: Duplicate Participant Name — No Error Message

## What, Why and Constraints

### What
When adding a participant with a name that already exists in the split, the operation failed silently — no error message was displayed to the user.

### Why
The backend correctly returned HTTP 400 with a detailed error message, and the frontend caught it and dispatched a toast notification. However, toast notifications are transient and easy to miss. More importantly, the add participant form lacked client-side duplicate name validation, unlike the `EditParticipantModal` which already had this check.

### Constraints
- Follow the existing validation pattern used in `EditParticipantModal` (case-insensitive name comparison)
- Show the error as an inline form validation message on the name field, consistent with other validation errors ("Name is required", "Name cannot exceed 50 characters")
- Svelte 5 runes for reactivity

## How

### 1. Added duplicate name validation (`routes/Participants.svelte`)
Added an `else if` clause in `validateAddForm()` that checks if `formName` (trimmed, lowercased) matches any existing participant name (lowercased). If a match is found, sets the inline validation error: "A participant with this name already exists".

### 2. Added automated tests (`routes/Participants.test.ts`)
Added 2 test cases in the "Add Participant" describe block:
- **Duplicate name**: enters an existing participant name ("Alice"), submits, verifies inline error is shown and API is not called
- **Case-insensitive**: enters "aLiCe" when "Alice" exists, verifies the same inline error is shown

## Tests

### Automated Tests (29 tests total, all passing)

**`Participants.test.ts`** — 2 new tests added:
- "shows validation error when name duplicates an existing participant" — enters exact match, verifies inline error and no API call
- "duplicate name validation is case-insensitive" — enters mixed-case match, verifies inline error and no API call