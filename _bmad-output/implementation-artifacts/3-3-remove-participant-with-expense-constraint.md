# Story 3.3: Remove Participant with Expense Constraint

Status: done

## Story

As a **user managing a split**,
I want **to remove a participant who has no expenses**,
So that **I can correct the participant list without breaking expense records**.

## Acceptance Criteria

1. **Given** I am viewing a split with participants **When** I look at a participant card **Then** I see a delete/remove button (trash icon) **And** the button is styled as a subtle secondary action

2. **Given** I click delete on a participant with NO associated expenses **When** the confirmation dialog appears **Then** I see "Remove [Name]?" with the participant's name **And** I see "This cannot be undone" warning **And** I see "Remove" (destructive) and "Cancel" buttons

3. **Given** I confirm deletion of a participant with no expenses **When** I click "Remove" **Then** the API is called DELETE `/api/splits/{splitId}/participants/{participantId}` **And** on success, the participant is removed from the list **And** a success toast shows "Participant removed"

4. **Given** I click "Cancel" on the confirmation dialog **When** the dialog closes **Then** the participant remains in the list **And** no API call is made

5. **Given** I click delete on a participant WITH associated expenses **When** the API returns 409 Conflict **Then** an error message is shown: "Cannot remove [Name] - they have associated expenses" **And** the message suggests: "Remove or reassign their expenses first" **And** the participant remains in the list

6. **Given** a DELETE request to `/api/splits/{splitId}/participants/{participantId}` **When** the participant has NO expenses (payerId or share allocations) **Then** response status is 204 No Content **And** the participant is removed from the split's JSON file

7. **Given** a DELETE request to `/api/splits/{splitId}/participants/{participantId}` **When** the participant HAS associated expenses **Then** response status is 409 Conflict **And** response follows Problem Details format with type "participant-has-expenses", title "Participant Has Expenses", detail "Cannot remove participant with associated expenses. Remove or reassign expenses first."

8. **Given** a DELETE request to a non-existent participant **When** the participantId is invalid **Then** response status is 404 Not Found

9. **Given** a DELETE request with invalid splitId format **When** sent to the API **Then** response status is 400 Bad Request

10. **Given** I am viewing on mobile **When** interacting with delete **Then** the confirmation dialog is a bottom sheet or centered modal **And** buttons are full-width and touch-friendly (min 44px)

## Tasks / Subtasks

- [x] Task 1: Create ParticipantHasExpensesError (AC: 7)
  - [x] 1.1: Create `ParticipantHasExpensesError` class in `split/domain/`
  - [x] 1.2: Extend `BaseError` with type "participant-has-expenses", title "Participant Has Expenses", status 409
  - [x] 1.3: Include participantId and splitId in error detail message

- [x] Task 2: Implement expense association check on Split aggregate (AC: 6, 7)
  - [x] 2.1: Add `Split.hasExpensesForParticipant(Participant.Id participantId)` method
  - [x] 2.2: Check if any expense has `payerId` matching the participant
  - [x] 2.3: Design method to support future share allocation checks (FREE mode) - currently checks payerId only
  - [x] 2.4: Return boolean (true if participant has expenses)

- [x] Task 3: Implement participant removal on Split aggregate (AC: 6)
  - [x] 3.1: Add `Split.removeParticipant(Participant.Id participantId)` method
  - [x] 3.2: Check hasExpensesForParticipant() first - throw ParticipantHasExpensesError if true
  - [x] 3.3: Find participant by ID (throw ParticipantNotFoundError if not found)
  - [x] 3.4: Remove participant from list

- [x] Task 4: Add removeParticipant to SplitService (AC: 6, 7, 8)
  - [x] 4.1: Add `removeParticipant(String splitId, String participantId)` method
  - [x] 4.2: Load split from storage (return empty Optional if not found)
  - [x] 4.3: Call split.removeParticipant() (throws if has expenses or not found)
  - [x] 4.4: Persist updated split
  - [x] 4.5: Return void (success is implied by no exception)

- [x] Task 5: Create DELETE endpoint (AC: 6, 7, 8, 9)
  - [x] 5.1: Add `DELETE /api/splits/{splitId}/participants/{participantId}` in `SplitResource.java`
  - [x] 5.2: Validate splitId format (return 400 if invalid)
  - [x] 5.3: Validate participantId format (return 400 if invalid)
  - [x] 5.4: Handle split not found (404)
  - [x] 5.5: Handle participant not found (404)
  - [x] 5.6: Handle participant has expenses (409)
  - [x] 5.7: Return 204 No Content on success

- [x] Task 6: Write backend integration tests (AC: 6, 7, 8, 9)
  - [x] 6.1: Test DELETE returns 204 when participant has no expenses
  - [x] 6.2: Test participant is removed from JSON file
  - [x] 6.3: Test DELETE returns 409 when participant is a payer on an expense
  - [x] 6.4: Test 409 response follows Problem Details format with correct type
  - [x] 6.5: Test DELETE returns 404 for non-existent participant
  - [x] 6.6: Test DELETE returns 404 for non-existent split
  - [x] 6.7: Test DELETE returns 400 for invalid splitId format
  - [x] 6.8: Test DELETE returns 400 for invalid participantId format

- [x] Task 7: Add frontend API function for deleting participant (AC: 3)
  - [x] 7.1: Add `deleteParticipant(splitId, participantId)` function in `$lib/api/splits.ts`
  - [x] 7.2: Handle 204 success (no response body)
  - [x] 7.3: Handle 409 Conflict error response

- [x] Task 8: Create confirmation dialog component (AC: 2, 10)
  - [x] 8.1: Create `ConfirmDialog.svelte` in `$lib/components/ui/` (or use shadcn AlertDialog)
  - [x] 8.2: Props: `open`, `title`, `description`, `confirmLabel`, `onConfirm`, `onCancel`
  - [x] 8.3: Style confirm button as destructive (red)
  - [x] 8.4: Style cancel button as outline
  - [x] 8.5: Ensure buttons have min 44px height for touch targets
  - [x] 8.6: On mobile: render as centered modal with full-width buttons

- [x] Task 9: Implement delete button and flow in Split.svelte (AC: 1, 2, 3, 4, 5, 10)
  - [x] 9.1: Add trash icon button to participant card (subtle secondary style)
  - [x] 9.2: Add `deletingParticipantId` state to track which participant deletion is pending
  - [x] 9.3: Add `isDeleting` state for loading indicator
  - [x] 9.4: Add `showDeleteConfirm` state to control dialog visibility
  - [x] 9.5: Add `handleDeleteClick(participant)` to open confirmation dialog
  - [x] 9.6: Add `handleConfirmDelete()` to call API and update list on success
  - [x] 9.7: Add `handleCancelDelete()` to close dialog without action
  - [x] 9.8: Show success toast "Participant removed" on 204
  - [x] 9.9: Show error toast with message on 409 Conflict
  - [x] 9.10: Prevent delete button click when in edit mode or during submission

- [x] Task 10: Write frontend tests (AC: 1, 2, 3, 4, 5, 10)
  - [x] 10.1: Test delete button is visible on participant card
  - [x] 10.2: Test clicking delete shows confirmation dialog with participant name
  - [x] 10.3: Test clicking Cancel closes dialog without API call
  - [x] 10.4: Test confirming delete calls API and removes participant from list
  - [x] 10.5: Test success toast is shown after deletion
  - [x] 10.6: Test 409 error shows appropriate message about expenses
  - [x] 10.7: Test delete button is disabled during edit mode or submission

## Dev Notes

### CRITICAL: Previous Story Intelligence

**From Story 3.2 (Edit Participant):**
- `Participant.java` is a record with inner value objects: `Id`, `Name`, `Nights`
- `ParticipantNotFoundError` and `InvalidParticipantIdError` already exist
- `SplitResource.java` has POST (add) and PUT (update) endpoints at `/{splitId}/participants`
- `SplitService.java` has `addParticipant()` and `updateParticipant()` methods
- Frontend `Split.svelte` has participant list with edit mode support
- Frontend uses `editingParticipantId` pattern for tracking active participant
- Toast store at `$lib/stores/toastStore.svelte.ts`
- API client pattern in `$lib/api/splits.ts` with `apiRequest<T>()` helper
- Tests in `SplitResourceTest.java` (backend) and `Split.test.ts` (frontend)
- Used valid 21-char NanoID format for test IDs (e.g., `V1StGXR8_Z5jdHi6B-myT`)

**From Story 3.1 (Add Participant):**
- Validation constants: `MAX_NIGHTS = 365`
- Participant.Id.isValid() for format validation

**Existing Code Patterns (MUST FOLLOW):**

```java
// Error class pattern (same as ParticipantNotFoundError)
public class ParticipantHasExpensesError extends BaseError {
    private static final String TYPE = "https://fairnsquare.app/errors/participant-has-expenses";
    private static final String TITLE = "Participant Has Expenses";
    private static final int STATUS = 409;

    public ParticipantHasExpensesError(String participantId, String splitId) {
        super(TYPE, TITLE, STATUS,
            "Cannot remove participant with associated expenses. Remove or reassign expenses first.");
    }
}
```

```java
// Split aggregate method pattern
public void removeParticipant(Participant.Id participantId) {
    if (hasExpensesForParticipant(participantId)) {
        throw new ParticipantHasExpensesError(participantId.value(), id.value());
    }
    boolean removed = participants.removeIf(p -> p.id().equals(participantId));
    if (!removed) {
        throw new ParticipantNotFoundError(participantId.value(), id.value());
    }
}

public boolean hasExpensesForParticipant(Participant.Id participantId) {
    return expenses.stream().anyMatch(e ->
        e.getPayerId().equals(participantId)
        // Future: || e.hasShareFor(participantId)
    );
}
```

```java
// REST endpoint pattern for DELETE
@DELETE
@Path("/{splitId}/participants/{participantId}")
public Response deleteParticipant(
    @PathParam("splitId") String splitId,
    @PathParam("participantId") String participantId) {
    // Validate IDs, call service, return 204 No Content
    return Response.noContent().build();
}
```

```typescript
// Frontend API pattern for DELETE
export async function deleteParticipant(
  splitId: string,
  participantId: string
): Promise<void> {
  await apiRequest<void>(`/splits/${splitId}/participants/${participantId}`, {
    method: 'DELETE',
  });
}
```

```typescript
// Frontend delete state pattern (Svelte 5 runes)
let deletingParticipantId = $state<string | null>(null);
let showDeleteConfirm = $state(false);
let isDeleting = $state(false);
```

### Architecture Patterns (MUST FOLLOW)

**Expense Association Check:**
- Check `expenses` list for any expense where `payerId` matches participant
- In future (FREE mode), also check share allocations
- Return 409 Conflict BEFORE attempting removal

**DELETE Response:**
- Success: 204 No Content (no response body)
- Not Found: 404 with Problem Details
- Has Expenses: 409 with Problem Details

**Confirmation Dialog UX:**
- Show participant name in dialog title
- Warning text about irreversibility
- "Remove" button styled destructive (red background)
- "Cancel" button styled outline
- On mobile: centered modal, full-width buttons

### UI/UX Requirements

- **Delete button placement:** Right side of participant card, trash icon
- **Delete button style:** Subtle/secondary, outline or ghost variant
- **Confirmation dialog:** Modal or bottom sheet on mobile
- **Touch targets:** min 44px height for all buttons
- **Destructive styling:** Red for confirm button per shadcn destructive variant
- **Loading state:** Disable buttons and show spinner during deletion

### Testing Requirements

**Backend (Quarkus @QuarkusTest):**
- Tests in `SplitResourceTest.java` (add delete participant tests)
- Create split with participant, then test delete
- Create split with participant + expense, test 409 response
- Use RestAssured for HTTP assertions

**Frontend (Vitest + Testing Library):**
- Tests in `Split.test.ts` (add delete participant tests)
- Mock deleteParticipant API call
- Test dialog interactions (open, confirm, cancel)
- Test error handling for 409 response

### References

- [Source: architecture.md#REST-structure] - DELETE `/api/splits/{splitId}/participants/{participantId}`
- [Source: architecture.md#Error-format] - Problem Details (RFC 9457)
- [Source: architecture.md#Domain-Model-Patterns] - Aggregate methods, no setters
- [Source: ux-design-specification.md#Touch-targets] - Min 44px height
- [Source: epics.md#FR7] - Remove participant (no expenses)
- [Source: epics.md#FR8] - Prevent removal with expenses
- [Source: story-3.2] - Participant error patterns, API patterns

### Project Structure Notes

**Files to modify:**
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/Split.java` - Add removeParticipant(), hasExpensesForParticipant()
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/api/SplitResource.java` - Add DELETE endpoint
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/service/SplitService.java` - Add removeParticipant()
- `fairnsquare-app/src/main/webui/src/lib/api/splits.ts` - Add deleteParticipant function
- `fairnsquare-app/src/main/webui/src/routes/Split.svelte` - Add delete button, confirmation dialog, handlers
- `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/api/SplitResourceTest.java` - Add delete tests
- `fairnsquare-app/src/main/webui/src/routes/Split.test.ts` - Add delete tests

**New files to create:**
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/ParticipantHasExpensesError.java` - 409 error class
- `fairnsquare-app/src/main/webui/src/lib/components/ui/confirm-dialog/` - shadcn AlertDialog (if not already available)

## Dev Agent Record

### Agent Model Used

Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References

- Backend tests: 37 tests, all pass (SplitResourceTest.java)
- Frontend tests: 40 tests, all pass (Split.test.ts + Home.test.ts)

### Completion Notes List

- Implemented full "Remove Participant with Expense Constraint" feature per story requirements
- Backend: Created ParticipantHasExpensesError (409 Conflict), added hasExpensesForParticipant() and removeParticipant() to Split aggregate, service layer, and REST endpoint
- Frontend: Created ConfirmDialog component, added delete button to participant cards, implemented full delete flow with confirmation dialog, proper error handling for 409 Conflict
- All acceptance criteria satisfied: delete button visible (AC 1), confirmation dialog with participant name (AC 2), API call and success toast (AC 3), cancel closes dialog (AC 4), 409 error handling (AC 5), DELETE 204 on success (AC 6), Problem Details format (AC 7), 404 for non-existent (AC 8), 400 for invalid IDs (AC 9), mobile-friendly touch targets (AC 10)

### File List

**New files:**
- fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/ParticipantHasExpensesError.java
- fairnsquare-app/src/main/webui/src/lib/components/ui/confirm-dialog/confirm-dialog.svelte
- fairnsquare-app/src/main/webui/src/lib/components/ui/confirm-dialog/index.ts

**Modified files:**
- fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/Split.java (added hasExpensesForParticipant, removeParticipant)
- fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/Expense.java (minimal expense entity for constraint check)
- fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/service/SplitService.java (added removeParticipant method)
- fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/api/SplitResource.java (added DELETE endpoint)
- fairnsquare-app/src/main/webui/src/lib/api/client.ts (fixed 204 No Content handling)
- fairnsquare-app/src/main/webui/src/lib/api/splits.ts (added deleteParticipant function)
- fairnsquare-app/src/main/webui/src/routes/Split.svelte (added delete button, confirmation dialog, handlers)
- fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/api/SplitResourceTest.java (added 8 delete participant tests)
- fairnsquare-app/src/main/webui/src/routes/Split.test.ts (added 11 delete participant tests)

### Change Log

- 2026-01-26: Implemented Story 3.3 - Remove Participant with Expense Constraint (all 10 ACs satisfied)
- 2026-01-26: Code review fixes applied:
  - Removed unused constructor parameters from ParticipantHasExpensesError
  - Fixed A11y accessibility issues in ConfirmDialog (added tabindex, onkeydown)
  - Simplified client.ts 204 handling (removed redundant content-length check)
  - Clarified Task 2.3 description (design for future FREE mode, currently payerId only)