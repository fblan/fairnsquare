# Story 3.2: Edit Participant Inline

Status: done

## Story

As a **user managing a split**,
I want **to edit a participant's name or nights inline**,
So that **I can correct mistakes without navigating away**.

## Acceptance Criteria

1. **Given** I am viewing a split with participants **When** I tap/click on a participant card **Then** the card enters edit mode inline **And** the name becomes an editable text input **And** the nights becomes an editable number input **And** I see "Save" and "Cancel" buttons

2. **Given** I am in edit mode for a participant **When** I change the name to "Bob" and nights to 4 **And** I click "Save" **Then** the API is called PUT `/api/splits/{splitId}/participants/{participantId}` **And** a loading indicator appears **And** on success, the card shows updated values **And** edit mode closes **And** a success toast shows "Participant updated"

3. **Given** I am in edit mode **When** I click "Cancel" **Then** edit mode closes **And** original values are restored **And** no API call is made

4. **Given** I try to save with empty name **When** I click "Save" **Then** validation error "Name is required" is shown **And** the API is not called

5. **Given** I try to save with nights less than 1 **When** I click "Save" **Then** validation error "Nights must be at least 1" is shown **And** the API is not called

6. **Given** I try to save with nights greater than 365 **When** I click "Save" **Then** validation error "Nights cannot exceed 365" is shown **And** the API is not called

7. **Given** a PUT request to `/api/splits/{splitId}/participants/{participantId}` with body `{"name": "Bob", "nights": 4}` **When** the participant exists **Then** response status is 200 OK **And** response body contains the updated participant **And** the split's JSON file is updated

8. **Given** a PUT request with invalid data (empty name or nights < 1 or nights > 365) **When** sent to the API **Then** response status is 400 Bad Request **And** response follows Problem Details (RFC 9457) format

9. **Given** a PUT request to a non-existent participant **When** the participantId is invalid **Then** response status is 404 Not Found **And** response follows Problem Details format

10. **Given** a PUT request to a non-existent split **When** the splitId is invalid **Then** response status is 404 Not Found

11. **Given** I am viewing on mobile (< 768px) **When** editing a participant **Then** the edit inputs are full-width within the card **And** touch targets are at least 44px **And** the keyboard appears with appropriate input type (text/number)

## Tasks / Subtasks

- [x] Task 1: Create UpdateParticipantRequest DTO (AC: 7, 8)
  - [x] 1.1: Create `UpdateParticipantRequest` record in `split/domain/UpdateParticipantRequest.java`
  - [x] 1.2: Add `name` (String) and `nights` (int) fields
  - [x] 1.3: Add Bean Validation: `@NotBlank` on name, `@Size(max=50)` on name, `@Min(1)` and `@Max(365)` on nights

- [x] Task 2: Implement participant update on Split aggregate (AC: 7)
  - [x] 2.1: Add `Split.updateParticipant(Participant.Id participantId, String name, int nights)` method
  - [x] 2.2: Find participant by ID in list (throw if not found)
  - [x] 2.3: Create new Participant with updated values (same ID)
  - [x] 2.4: Replace participant in list

- [x] Task 3: Add updateParticipant to SplitService (AC: 7, 9, 10)
  - [x] 3.1: Add `updateParticipant(String splitId, String participantId, UpdateParticipantRequest request)` method
  - [x] 3.2: Load split from storage (return empty Optional if not found)
  - [x] 3.3: Call split.updateParticipant() (throws if participant not found)
  - [x] 3.4: Persist updated split
  - [x] 3.5: Return updated Participant

- [x] Task 4: Create PUT endpoint for updating participants (AC: 7, 8, 9, 10)
  - [x] 4.1: Add `PUT /api/splits/{splitId}/participants/{participantId}` in `SplitResource.java`
  - [x] 4.2: Validate splitId format (return 400 if invalid)
  - [x] 4.3: Validate participantId format (return 400 if invalid)
  - [x] 4.4: Handle split not found (404)
  - [x] 4.5: Handle participant not found (404)
  - [x] 4.6: Return 200 OK with updated participant

- [x] Task 5: Create ParticipantNotFoundError (AC: 9)
  - [x] 5.1: Create `ParticipantNotFoundError` class extending BaseError
  - [x] 5.2: Register exception mapper for 404 response (using existing mapper via BaseError inheritance)

- [x] Task 6: Write backend integration tests (AC: 7, 8, 9, 10)
  - [x] 6.1: Test PUT returns 200 with valid data
  - [x] 6.2: Test PUT updates participant in JSON file
  - [x] 6.3: Test PUT returns 400 for empty name
  - [x] 6.4: Test PUT returns 400 for nights < 1
  - [x] 6.5: Test PUT returns 400 for nights > 365
  - [x] 6.6: Test PUT returns 404 for non-existent split
  - [x] 6.7: Test PUT returns 404 for non-existent participant
  - [x] 6.8: Test PUT returns 400 for invalid splitId format
  - [x] 6.9: Test PUT returns 400 for invalid participantId format

- [x] Task 7: Add frontend API function for updating participant (AC: 2)
  - [x] 7.1: Add `updateParticipant(splitId, participantId, request)` function in `$lib/api/splits.ts`
  - [x] 7.2: Add `UpdateParticipantRequest` interface
  - [x] 7.3: Handle 200 success and error responses

- [x] Task 8: Implement inline edit mode in Split.svelte (AC: 1, 2, 3, 4, 5, 6, 11)
  - [x] 8.1: Add `editingParticipantId` state to track which participant is being edited
  - [x] 8.2: Add `editName` and `editNights` state for form values
  - [x] 8.3: Add `editValidationErrors` state for inline errors
  - [x] 8.4: Add `isEditSubmitting` state for loading indicator
  - [x] 8.5: Modify participant card to detect click and enter edit mode
  - [x] 8.6: Render edit form when participant.id === editingParticipantId
  - [x] 8.7: Add handleStartEdit(participant) to populate form with current values
  - [x] 8.8: Add handleCancelEdit() to restore original values and close
  - [x] 8.9: Add validateEditForm() with same rules as add form (1-365 nights, non-empty name)
  - [x] 8.10: Add handleSaveEdit() to call API and update list on success
  - [x] 8.11: Show loading spinner on Save button during submit
  - [x] 8.12: Show success toast and close edit mode on success
  - [x] 8.13: Show error toast on API failure, keep edit mode open

- [x] Task 9: Write frontend tests (AC: 1, 2, 3, 4, 5, 6, 11)
  - [x] 9.1: Test clicking participant card enters edit mode
  - [x] 9.2: Test edit form shows current values
  - [x] 9.3: Test Cancel button closes edit mode without API call
  - [x] 9.4: Test validation error for empty name
  - [x] 9.5: Test validation error for nights < 1
  - [x] 9.6: Test validation error for nights > 365
  - [x] 9.7: Test successful save updates participant and closes edit mode
  - [x] 9.8: Test API error shows toast and keeps edit mode open
  - [x] 9.9: Test loading state on Save button

## Dev Notes

### CRITICAL: Previous Story Intelligence

**From Story 3.1 (Add Participant):**
- `Participant.java` is a record with inner value objects: `Id`, `Name`, `Nights`
- `AddParticipantRequest.java` exists with Bean Validation (`@NotBlank`, `@Size(max=50)`, `@Min(1)`, `@Max(365)`)
- `SplitResource.java` has POST endpoint at `/{splitId}/participants`
- `SplitService.java` has `addParticipant()` method pattern
- Frontend `Split.svelte` renders participant list with name, nights, balance
- Frontend validation uses `MAX_NIGHTS = 365` constant
- Toast store at `$lib/stores/toastStore.svelte.ts`
- API client pattern in `$lib/api/splits.ts`

**Existing Code Patterns (MUST FOLLOW):**

```java
// Domain: Participant.java already has validation in value objects
public record Participant(Id id, Name name, Nights nights) {
    // Name validates non-blank, max 50 chars
    // Nights validates min 1, max 365
}

// For update, create new Participant with same ID, new values:
// Participant updated = new Participant(existingId, new Name(newName), new Nights(newNights));
```

```java
// Request DTO pattern (same as AddParticipantRequest)
public record UpdateParticipantRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 50, message = "Name cannot exceed 50 characters")
    String name,

    @Min(value = 1, message = "Nights must be at least 1")
    @Max(value = 365, message = "Nights cannot exceed 365")
    int nights
) {}
```

```java
// REST endpoint pattern
@PUT
@Path("/{splitId}/participants/{participantId}")
public Response updateParticipant(
    @PathParam("splitId") String splitId,
    @PathParam("participantId") String participantId,
    @Valid UpdateParticipantRequest request) {
    // Validate IDs, call service, return 200 OK
}
```

```typescript
// Frontend API pattern
export interface UpdateParticipantRequest {
  name: string;
  nights: number;
}

export async function updateParticipant(
  splitId: string,
  participantId: string,
  request: UpdateParticipantRequest
): Promise<Participant> {
  return apiRequest<Participant>(`/splits/${splitId}/participants/${participantId}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  });
}
```

```typescript
// Frontend edit state pattern (Svelte 5 runes)
let editingParticipantId = $state<string | null>(null);
let editName = $state('');
let editNights = $state(1);
let editValidationErrors = $state<{name?: string; nights?: string}>({});
let isEditSubmitting = $state(false);
```

### Architecture Patterns (MUST FOLLOW)

**Participant Update Logic:**
- Participant is immutable (record) - create new instance with updated values
- Split.updateParticipant() finds by ID, replaces with new instance
- If participant not found, throw ParticipantNotFoundError

**Error Handling:**
- ParticipantNotFoundError extends BaseError
- Exception mapper returns 404 with Problem Details
- Pattern matches SplitNotFoundError and InvalidSplitIdError

**Frontend Edit Mode:**
- Only one participant can be edited at a time
- Clicking a card (not in edit mode) starts edit mode
- Edit form replaces display view inline (same card, different content)
- Cancel restores original values without API call
- Save validates then calls API

### UI/UX Requirements

- **Touch targets:** min 44px height for Save/Cancel buttons
- **Edit form placement:** Inline within the participant card (transforms card content)
- **Inputs:** Use shadcn Input and Label components (same as add form)
- **Buttons:** "Save" (primary) and "Cancel" (outline) side by side
- **Validation:** Show errors inline below inputs
- **Success:** Toast notification + edit mode closes + list updates
- **Mobile:** Full-width inputs, appropriate keyboard types

### Testing Requirements

**Backend (Quarkus @QuarkusTest):**
- Tests in `SplitResourceIT.java` (add update participant tests)
- Use RestAssured for HTTP assertions
- Create split and participant first, then test update

**Frontend (Vitest + Testing Library):**
- Tests in `Split.test.ts` (add to existing file)
- Mock updateParticipant API call
- Test user interactions (click card, type, save/cancel)

### References

- [Source: architecture.md#REST-structure] - PUT `/api/splits/{splitId}/participants/{participantId}`
- [Source: architecture.md#Validation] - Bean Validation for request DTOs
- [Source: architecture.md#Error-format] - Problem Details (RFC 9457)
- [Source: architecture.md#Domain-Model-Patterns] - Immutable records, value objects
- [Source: ux-design-specification.md#Touch-targets] - Min 44px height
- [Source: ux-design-specification.md#Effortless-Interactions] - Inline editing
- [Source: epics.md#FR6] - Edit participant details
- [Source: story-3.1] - Participant domain model, API patterns, validation constants

### Project Structure Notes

**Files to modify:**
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/Split.java` - Add updateParticipant()
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/api/SplitResource.java` - Add PUT endpoint
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/service/SplitService.java` - Add updateParticipant()
- `fairnsquare-app/src/main/webui/src/lib/api/splits.ts` - Add updateParticipant function
- `fairnsquare-app/src/main/webui/src/routes/Split.svelte` - Add inline edit mode
- `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/api/SplitResourceTest.java` - Add tests
- `fairnsquare-app/src/main/webui/src/routes/Split.test.ts` - Add tests

**New files to create:**
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/UpdateParticipantRequest.java` - Request DTO
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/ParticipantNotFoundError.java` - Error class

## Dev Agent Record

### Agent Model Used

Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References

- Backend tests: 40 passing (9 new for update participant)
- Frontend tests: 33 passing (9 new for edit participant)

### Completion Notes List

- Implemented full inline edit functionality for participants
- Created UpdateParticipantRequest DTO with Bean Validation (@NotBlank, @Size, @Min, @Max)
- Added updateParticipant method to Split aggregate (finds participant by ID, replaces with new instance)
- Added updateParticipant to SplitService with proper error handling
- Created PUT endpoint at `/api/splits/{splitId}/participants/{participantId}`
- Created ParticipantNotFoundError and InvalidParticipantIdError for proper 404/400 responses
- Frontend edit mode allows one participant to be edited at a time (inline form replaces display)
- Validation matches add form: name required, max 50 chars; nights 1-365
- Used valid 21-char NanoID format for test participant IDs (e.g., `V1StGXR8_Z5jdHi6B-myT`)

### File List

**New files:**
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/UpdateParticipantRequest.java`
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/ParticipantNotFoundError.java`
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/InvalidParticipantIdError.java`

**Modified files:**
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/Split.java` - Added updateParticipant(), getParticipant() methods
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/service/SplitService.java` - Added updateParticipant() method
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/api/SplitResource.java` - Added PUT endpoint
- `fairnsquare-app/src/main/webui/src/lib/api/splits.ts` - Added UpdateParticipantRequest interface and updateParticipant function
- `fairnsquare-app/src/main/webui/src/routes/Split.svelte` - Added edit state, handlers, inline edit form
- `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/api/SplitResourceTest.java` - Added 9 update participant tests
- `fairnsquare-app/src/main/webui/src/routes/Split.test.ts` - Added 9 edit participant tests
- `pom.xml` - Refactored plugin versions to pluginManagement section
- `fairnsquare-app/pom.xml` - Removed redundant plugin version declarations (now inherited from parent)