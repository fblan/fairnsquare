# Story 3.1: Add Participant with Smart Defaults

Status: done

## Story

As a **user managing a split**,
I want **to add participants with their name and number of nights**,
So that **expenses can be fairly calculated based on stay duration**.

## Acceptance Criteria

1. **Given** I am viewing a split overview **When** I look at the Participants section **Then** I see an "Add Participant" button with a plus icon **And** the button uses touch-friendly sizing (min 44px height)

2. **Given** I click "Add Participant" **When** the add form appears **Then** I see input fields for Name (text, required) and Nights (number, required, min 1) **And** the form appears inline within the Participants card **And** the Nights field is pre-filled with the smart default value

3. **Given** no participants have been added to this split yet **When** adding the first participant **Then** the Nights field defaults to 1

4. **Given** I previously added a participant with 3 nights **When** I add another participant **Then** the Nights field defaults to 3 (last entered value)

5. **Given** I enter a valid name "Alice" and nights "2" **When** I click "Add" or submit the form **Then** the API is called POST `/api/splits/{splitId}/participants` **And** a loading indicator appears on the submit button **And** on success, the new participant appears in the list **And** the form clears and closes **And** a success toast shows "Participant added"

6. **Given** I try to add a participant with empty name **When** I submit the form **Then** validation error "Name is required" is shown inline **And** the API is not called

7. **Given** I try to add a participant with nights less than 1 **When** I submit the form **Then** validation error "Nights must be at least 1" is shown inline **And** the API is not called

8. **Given** the API returns an error **When** adding a participant **Then** an error toast displays the error message **And** the form remains open for retry

9. **Given** a POST request to `/api/splits/{splitId}/participants` with body `{"name": "Alice", "nights": 2}` **When** the split exists **Then** response status is 201 Created **And** response body contains: `id` (21-char NanoID), `name`, `nights` **And** the participant is persisted in the split's JSON file

10. **Given** a POST request with invalid data (empty name or nights < 1) **When** sent to the API **Then** response status is 400 Bad Request **And** response follows Problem Details (RFC 9457) format

11. **Given** a POST request to a non-existent split **When** the splitId is invalid **Then** response status is 404 Not Found

## Tasks / Subtasks

- [x] Task 1: Implement Participant domain model (AC: 9, 10, 11)
  - [x] 1.1: Create `Participant` record with `Id`, `Name`, `Nights` value objects in `split/domain/Participant.java`
  - [x] 1.2: Add `Participant.Id` inner record (21-char NanoID, same pattern as Split.Id)
  - [x] 1.3: Add `Participant.Name` inner record (non-blank, max 50 chars)
  - [x] 1.4: Add `Participant.Nights` inner record (min 1, max 365)
  - [x] 1.5: Add Jackson annotations for JSON serialization
  - [x] 1.6: Add factory method `Participant.create(String name, int nights)`

- [x] Task 2: Create AddParticipantRequest DTO (AC: 9, 10)
  - [x] 2.1: Create `AddParticipantRequest` record with `name` (String) and `nights` (int)
  - [x] 2.2: Add Bean Validation: `@NotBlank` on name, `@Min(1)` on nights

- [x] Task 3: Implement participant addition on Split aggregate (AC: 9)
  - [x] 3.1: Update `Split.addParticipant()` to accept Participant with proper validation
  - [x] 3.2: Ensure participants list is mutable internally for adding

- [x] Task 4: Create POST endpoint for adding participants (AC: 9, 10, 11)
  - [x] 4.1: Add `POST /api/splits/{splitId}/participants` in `SplitResource.java`
  - [x] 4.2: Validate splitId format (return 400 if invalid)
  - [x] 4.3: Load split from storage (return 404 if not found)
  - [x] 4.4: Create Participant from request, add to split, persist
  - [x] 4.5: Return 201 Created with created participant

- [x] Task 5: Write backend integration tests (AC: 9, 10, 11)
  - [x] 5.1: Test POST returns 201 with valid participant data
  - [x] 5.2: Test POST returns 400 for empty name
  - [x] 5.3: Test POST returns 400 for nights < 1
  - [x] 5.4: Test POST returns 404 for non-existent split
  - [x] 5.5: Test participant is persisted in JSON file

- [x] Task 6: Add frontend API function for adding participant (AC: 5, 8)
  - [x] 6.1: Add `addParticipant(splitId, request)` function in `$lib/api/splits.ts`
  - [x] 6.2: Add `AddParticipantRequest` interface
  - [x] 6.3: Handle 201 success and error responses

- [x] Task 7: Implement Add Participant form in Split.svelte (AC: 1, 2, 5, 6, 7, 8)
  - [x] 7.1: Add "Add Participant" button in Participants section
  - [x] 7.2: Create inline form with Name and Nights inputs using shadcn components
  - [x] 7.3: Add form state management with Svelte 5 runes
  - [x] 7.4: Add client-side validation with inline error messages
  - [x] 7.5: Add loading state on submit button
  - [x] 7.6: Call API and handle success (refresh participant list, show toast, close form)
  - [x] 7.7: Handle API errors (show toast, keep form open)

- [x] Task 8: Implement smart default for nights (AC: 3, 4)
  - [x] 8.1: Track last entered nights value in component state
  - [x] 8.2: Default to 1 if no participants exist
  - [x] 8.3: Default to last entered value for subsequent additions
  - [x] 8.4: Persist smart default in localStorage for session continuity

- [x] Task 9: Write frontend tests (AC: 1, 2, 3, 4, 5, 6, 7, 8)
  - [x] 9.1: Test "Add Participant" button renders
  - [x] 9.2: Test form appears when button clicked
  - [x] 9.3: Test smart default: first participant gets nights=1
  - [x] 9.4: Test smart default: subsequent uses last value
  - [x] 9.5: Test validation errors for empty name
  - [x] 9.6: Test validation errors for nights < 1
  - [x] 9.7: Test successful submission adds participant to list
  - [x] 9.8: Test API error shows toast and keeps form open

## Dev Notes

### CRITICAL: Previous Story Intelligence

**From Story 2.3 (Split Overview):**
- `Split.svelte` already renders participants list with name, nights, balance
- Participant interface exists in `$lib/api/splits.ts`: `{ id, name, nights }`
- Empty state shows "No participants yet" - form should replace this
- Uses Card component from shadcn for sections
- Toast store at `$lib/stores/toastStore.svelte.ts` for notifications
- Route params via `route.params.splitId` from sv-router

**From Story 2.1 (Backend):**
- `Split.java` has `addParticipant(Participant)` method ready
- `Participant.java` is currently a placeholder - needs full implementation
- JSON persistence via SplitRepository (file-based storage)
- Problem Details (RFC 9457) pattern for errors
- NanoID generation via `NanoIdUtils.randomNanoId()`

### Architecture Patterns (MUST FOLLOW)

**Backend Domain Model Pattern:**
```java
// Follow Split.java pattern for Participant
public record Participant(Id id, Name name, Nights nights) {

    public record Id(@JsonValue String value) {
        // Same 21-char NanoID pattern as Split.Id
    }

    public record Name(@JsonValue String value) {
        public Name {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Name cannot be blank");
            }
        }
    }

    public record Nights(@JsonValue int value) {
        public Nights {
            if (value < 1) {
                throw new IllegalArgumentException("Nights must be at least 1");
            }
        }
    }

    public static Participant create(String name, int nights) {
        return new Participant(Id.generate(), new Name(name), new Nights(nights));
    }
}
```

**REST Endpoint Pattern:**
```java
@POST
@Path("/{splitId}/participants")
public Response addParticipant(
        @PathParam("splitId") String splitId,
        @Valid AddParticipantRequest request) {
    // Validate splitId, load split, create participant, persist, return 201
}
```

**Frontend State Pattern (Svelte 5 Runes):**
```typescript
let showAddForm = $state(false);
let formName = $state('');
let formNights = $state(1);
let isSubmitting = $state(false);
let validationErrors = $state<{name?: string; nights?: string}>({});

// Validation constants (must match backend)
const MAX_NIGHTS = 365;

// Smart default via helper functions (localStorage)
function getSmartDefaultNights(): number {
  const stored = localStorage.getItem('fairnsquare_lastParticipantNights');
  return stored ? parseInt(stored, 10) : 1;
}
function saveSmartDefaultNights(nights: number): void {
  localStorage.setItem('fairnsquare_lastParticipantNights', nights.toString());
}
```

**API Client Pattern:**
```typescript
export interface AddParticipantRequest {
  name: string;
  nights: number;
}

export async function addParticipant(
  splitId: string,
  request: AddParticipantRequest
): Promise<Participant> {
  return apiRequest<Participant>(`/splits/${splitId}/participants`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}
```

### UI/UX Requirements

- **Touch targets:** min 44px height for all interactive elements
- **Form placement:** Inline within Participants card (not modal)
- **Inputs:** Use shadcn Input and Label components
- **Button:** Use shadcn Button with loading spinner during submit
- **Validation:** Show errors inline below inputs (not toast)
- **Success:** Toast notification + form closes + list updates

### Testing Requirements

**Backend (Quarkus @QuarkusTest):**
- All tests in `SplitResourceIT.java` (add participant tests)
- Use RestAssured for HTTP assertions
- Verify JSON response structure
- Verify file persistence

**Frontend (Vitest + Testing Library):**
- Tests in `Split.test.ts` (add to existing file)
- Mock API calls via vi.mock
- Test user interactions (click, type, submit)
- Test state changes (form visibility, loading, validation)

### References

- [Source: architecture.md#REST-structure] - `/api/splits/{splitId}/participants` path pattern
- [Source: architecture.md#Validation] - Bean Validation for request validation
- [Source: architecture.md#Error-format] - Problem Details (RFC 9457)
- [Source: ux-design-specification.md#Touch-targets] - Min 44px height
- [Source: ux-design-specification.md#Effortless-Interactions] - Inline editing, minimal taps
- [Source: epics.md#FR5] - Add participant with name + nights
- [Source: epics.md#FR9] - Smart default for nights

### Project Structure Notes

**Files to create:**
- None needed - Participant.java already exists (expand it)

**Files to modify:**
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/Participant.java` - Full implementation
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/api/SplitResource.java` - Add POST endpoint
- `fairnsquare-app/src/main/webui/src/lib/api/splits.ts` - Add addParticipant function
- `fairnsquare-app/src/main/webui/src/routes/Split.svelte` - Add participant form
- `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/api/SplitResourceIT.java` - Add tests
- `fairnsquare-app/src/main/webui/src/routes/Split.test.ts` - Add tests

**New files to create:**
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/AddParticipantRequest.java` - Request DTO

## Dev Agent Record

### Agent Model Used

Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References

None - implementation completed successfully.

### Completion Notes List

- Implemented Participant as a record with inner value objects (Id, Name, Nights) following Split.java pattern
- Participant.Id uses 21-char NanoID for URL-safe identifiers
- Participant.Name validates non-blank, max 50 chars
- Participant.Nights validates min 1, max 365
- AddParticipantRequest DTO with Bean Validation annotations (@NotBlank, @Size, @Min, @Max)
- POST /api/splits/{splitId}/participants endpoint added to SplitResource
- SplitService.addParticipant() method handles persistence
- Frontend addParticipant() API function added to splits.ts
- Inline form in Split.svelte with shadcn Input/Label/Button components
- Smart default for nights stored in localStorage (fairnsquare_lastParticipantNights)
- Frontend validation matches backend constraints (nights 1-365)
- 9 backend integration tests added (20 total passing)
- 10 frontend tests added (18 total in Split.test.ts, 24 total passing)

### Code Review Fixes (2026-01-25)

- Added @Max(365) validation to AddParticipantRequest.nights (was missing, would cause 500 instead of 400)
- Added frontend validation for nights > 365 with MAX_NIGHTS constant
- Added backend test: addParticipant_withNightsGreaterThan365_returns400
- Added backend test: addParticipant_withNameExceeding50Chars_returns400
- Added frontend test: shows validation error for nights greater than 365
- Updated SplitResourceIT class comment to include Story 3.1
- Updated Dev Notes pattern to match actual implementation (helper functions vs direct $state)

### File List

**New Files:**
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/AddParticipantRequest.java`

**Modified Files:**
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/Participant.java`
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/api/SplitResource.java`
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/service/SplitService.java`
- `fairnsquare-app/src/main/webui/src/lib/api/splits.ts`
- `fairnsquare-app/src/main/webui/src/routes/Split.svelte`
- `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/api/SplitResourceIT.java`
- `fairnsquare-app/src/main/webui/src/routes/Split.test.ts`