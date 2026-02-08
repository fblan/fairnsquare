# Story 4.3: Free Mode Manual Share Specification

Status: ready-for-dev

## Story

As a **user**,
I want **to manually specify each participant's share for an expense**,
so that **I can handle custom splitting scenarios that don't fit by-night or equal**.

## Acceptance Criteria

1. **Given** I am adding an expense and select "Free" split mode
   **When** the form updates
   **Then** I see a list of all participants with individual amount inputs
   **And** each input is pre-filled with €0.00
   **And** I see a running total showing "Total: €X / €Y" (entered vs expense amount)

2. **Given** I am in Free mode for a €100.00 expense with 3 participants
   **When** I enter: Alice €50, Bob €30, Charlie €20
   **Then** the running total shows "Total: €100.00 / €100.00" (green, valid)
   **And** the submit button is enabled

3. **Given** I am in Free mode and enter shares that don't sum to the expense amount
   **When** the total is €80.00 but expense is €100.00
   **Then** the running total shows "Total: €80.00 / €100.00" (red, invalid)
   **And** validation error "Shares must equal the expense amount" is shown
   **And** the form submission is prevented (shows inline errors on submit attempt)

4. **Given** I am in Free mode and enter shares that exceed the expense amount
   **When** the total is €120.00 but expense is €100.00
   **Then** the running total shows "Total: €120.00 / €100.00" (red, invalid)
   **And** validation error "Shares must equal the expense amount" is shown
   **And** the form submission is prevented (shows inline errors on submit attempt)

5. **Given** I am in Free mode and change the expense amount after entering shares
   **When** I change amount from €100 to €150
   **Then** the validation updates to reflect the new target
   **And** I see "Total: €100.00 / €150.00" (red, invalid)

6. **Given** I submit a valid Free mode expense
   **When** the API request is made
   **Then** the request body includes shares array:
   ```json
   {
     "amount": 100.00,
     "description": "Custom split",
     "payerId": "abc123",
     "splitMode": "FREE",
     "shares": [
       {"participantId": "p1", "amount": 50.00},
       {"participantId": "p2", "amount": 30.00},
       {"participantId": "p3", "amount": 20.00}
     ]
   }
   ```

7. **Given** a POST request with Free mode and invalid shares total
   **When** shares don't sum to expense amount
   **Then** response status is 400 Bad Request
   **And** response follows Problem Details format with detail "Shares must sum to expense amount"

8. **Given** a POST request with Free mode and missing participant shares
   **When** not all participants have a share specified
   **Then** response status is 400 Bad Request
   **And** response follows Problem Details format with detail "All participants must have a share specified"

9. **Given** I am viewing on mobile (< 768px)
   **When** in Free mode
   **Then** participant share inputs are stacked vertically
   **And** each row shows: participant name, amount input
   **And** touch targets are at least 44px
   **And** the keyboard shows numeric input type

10. **Given** I am viewing a FREE mode expense in the expense list
    **When** I tap on the expense card to expand details
    **Then** I see the manually-specified share breakdown for each participant
    **And** display shows "Manual split" (not nights fraction or "Split equally")

## Tasks / Subtasks

- [ ] Task 1: Create ExpenseFree domain subclass (AC: 6, 7, 8)
  - [ ] 1.1: Create `ExpenseFree.java` extending `Expense` in `split/domain/`
  - [ ] 1.2: Update `Expense` sealed class: `permits ExpenseByNight, ExpenseEqual, ExpenseFree`
  - [ ] 1.3: Constructor accepts pre-calculated shares list (`List<Share>`)
  - [ ] 1.4: Implement `getSplitMode()` returning `SplitMode.FREE`
  - [ ] 1.5: Implement `getShares(Split split)` — returns stored shares directly (no calculation)
  - [ ] 1.6: Add static factory `ExpenseFree.create(amount, description, payerId, shares)` with validation:
    - All participants must have a share specified
    - Shares must sum exactly to the expense amount (BigDecimal `compareTo`)
    - No negative share amounts
  - [ ] 1.7: Update `Expense.create()` deprecated factory: replace `throw` with `ExpenseFree` creation (or keep deprecated with new signature)
  - [ ] 1.8: Update `Expense.fromJson()` to handle `FREE` mode reconstitution

- [ ] Task 2: Update persistence layer for ExpenseFree (AC: 6)
  - [ ] 2.1: Create `ExpenseFreePersistenceDTO` implementing `ExpensePersistenceDTO` sealed interface
  - [ ] 2.2: Include `shares` list in DTO (Jackson serialization with `@JsonTypeName("FREE")`)
  - [ ] 2.3: Update `ExpensePersistenceDTO` permits clause to include `ExpenseFreePersistenceDTO`
  - [ ] 2.4: Update `ExpensePersistenceMapper.toPersistenceDTO()` to handle `ExpenseFree`
  - [ ] 2.5: Update `ExpensePersistenceMapper.toDomain()` to handle `ExpenseFreePersistenceDTO`

- [ ] Task 3: Update API DTO layer for ExpenseFree (AC: 6)
  - [ ] 3.1: Create `ExpenseFreeDTO` implementing `ExpenseDTO` sealed interface
  - [ ] 3.2: Include `shares` array in DTO (same as other expense DTOs)
  - [ ] 3.3: Update `ExpenseDTO` permits clause to include `ExpenseFreeDTO`
  - [ ] 3.4: Update `ExpenseMapper.toDTO()` to handle `ExpenseFree`

- [ ] Task 4: Create REST endpoint and use case for FREE expenses (AC: 6, 7, 8)
  - [ ] 4.1: Create `AddFreeExpenseRequest` record in `split/domain/` with fields: `amount`, `description`, `payerId`, `shares[]` (each: `participantId`, `amount`)
  - [ ] 4.2: Add Bean Validation annotations: `@NotNull`, `@NotEmpty` for shares list
  - [ ] 4.3: Add `addExpenseFree(splitId, request)` method to `SplitUseCases.java`
    - Validate all participants have a share
    - Validate shares sum equals expense amount
    - Create `ExpenseFree` and persist
  - [ ] 4.4: Add `POST /api/splits/{splitId}/expenses/free` endpoint to `SplitResource.java`
  - [ ] 4.5: Update deprecated `addExpense()` in `SplitUseCases` to handle FREE mode (or keep throwing)

- [ ] Task 5: Write backend tests (AC: 7, 8)
  - [ ] 5.1: Create `ExpenseFreeTest.java` — unit tests for share validation:
    - Valid shares summing to amount
    - Shares not summing to amount (throws)
    - Missing participant share (throws)
    - Negative share amount (throws)
    - Zero share amount (allowed)
  - [ ] 5.2: Add integration tests in `SplitResourceTest.java` or relevant use case test:
    - POST /expenses/free with valid shares → 201 + correct response
    - POST /expenses/free with invalid sum → 400 Problem Details
    - POST /expenses/free with missing participants → 400 Problem Details
    - POST /expenses/free and verify shares persisted correctly
    - GET split after adding free expense → shares match

- [ ] Task 6: Update frontend API client (AC: 6)
  - [ ] 6.1: Add `AddFreeExpenseRequest` interface to `splits.ts` with `shares` array
  - [ ] 6.2: Add `addFreeExpense(splitId, request)` function calling `POST /expenses/free`
  - [ ] 6.3: Keep existing `AddExpenseRequest` unchanged (used by BY_NIGHT/EQUAL)

- [ ] Task 7: Add FREE mode share inputs to AddExpenseModal (AC: 1, 2, 3, 4, 5, 9)
  - [ ] 7.1: Add `FREE` option to split mode RadioGroup (with Edit3 icon from lucide-svelte)
  - [ ] 7.2: When FREE mode selected, render participant share inputs below the radio group
  - [ ] 7.3: Each row: participant name (label) + amount input (number, step 0.01, min 0, placeholder "€0.00")
  - [ ] 7.4: Add `$state` for shares: `shareAmounts = $state<Record<string, number | ''>>({})` initialized with all participant IDs → `''`
  - [ ] 7.5: Add `$derived` for running total: sum of all non-empty share amounts
  - [ ] 7.6: Add `$derived` for shares validation: `sharesTotal === amountValue` (BigDecimal-safe comparison)
  - [ ] 7.7: Display running total: "Total: €X.XX / €Y.YY" with green text when valid, red when invalid
  - [ ] 7.8: Update `isValid` derived to include shares validation when FREE mode is selected
  - [ ] 7.9: On submit with FREE mode: call `addFreeExpense()` instead of `addExpense()`, include shares array
  - [ ] 7.10: Update `isDirty` to track share input changes
  - [ ] 7.11: Reset share inputs in `resetForm()`

- [ ] Task 8: Add FREE mode share inputs to ExpensesSection inline form (AC: 1, 2, 3, 4, 5, 9)
  - [ ] 8.1: Uncomment FREE radio button option in ExpensesSection.svelte
  - [ ] 8.2: Add participant share inputs (same logic as Task 7)
  - [ ] 8.3: Update validation and submission to handle FREE mode
  - [ ] 8.4: Call `addFreeExpense()` when FREE mode is selected

- [ ] Task 9: Update ExpenseCard display for FREE mode (AC: 10)
  - [ ] 9.1: Update `formatShareCalculation()` in ExpenseCard.svelte for FREE mode
  - [ ] 9.2: Display "Manual split" for FREE mode share breakdown
  - [ ] 9.3: Ensure all shares display correctly with participant names and amounts

- [ ] Task 10: Write frontend tests (AC: 1-5, 9, 10)
  - [ ] 10.1: AddExpenseModal tests:
    - FREE radio button renders
    - Selecting FREE shows participant share inputs
    - Share inputs pre-filled with empty/0
    - Running total displays correctly
    - Valid shares (sum matches) enables submission
    - Invalid shares (under/over) shows validation error
    - Changing amount updates running total target
    - Successful FREE expense submission calls correct API
    - Form reset clears share inputs
  - [ ] 10.2: ExpensesSection tests (if applicable):
    - FREE radio button visible and selectable
    - Share inputs appear when FREE selected
  - [ ] 10.3: ExpenseCard tests:
    - FREE mode expense shows "Manual split" in breakdown
    - Shares display with correct participant names and amounts

## Dev Notes

### Current State Analysis

**Backend — Sealed Expense Hierarchy:**
- `Expense.java` (line 15): `sealed abstract class Expense permits ExpenseByNight, ExpenseEqual`
- `Expense.create()` (line 37): `case FREE -> throw new UnsupportedOperationException("FREE mode not yet implemented");`
- `Expense.fromJson()` (line 55): Same throw for FREE mode
- `SplitMode.java`: `FREE("FREE")` already defined in enum
- **ACTION:** Add `ExpenseFree` subclass, update permits clause, implement factory methods

**Backend — Type-Specific Endpoint Pattern (MUST FOLLOW):**
- `POST /splits/{splitId}/expenses/by-night` → `SplitUseCases.addExpenseByNight()`
- `POST /splits/{splitId}/expenses/equal` → `SplitUseCases.addExpenseEqual()`
- **ACTION:** Add `POST /splits/{splitId}/expenses/free` → `SplitUseCases.addExpenseFree()`
- The deprecated `POST /expenses` generic endpoint can remain as-is

**Backend — Persistence DTO Pattern:**
- `ExpensePersistenceDTO` sealed interface permits `ExpenseByNightPersistenceDTO`, `ExpenseEqualPersistenceDTO`
- Uses Jackson `@JsonTypeInfo(property = "type")` discriminator
- Mapper: `ExpensePersistenceMapper` handles bidirectional conversion
- **ACTION:** Add `ExpenseFreePersistenceDTO` with shares, update mapper

**Backend — API DTO Pattern:**
- `ExpenseDTO` sealed interface permits `ExpenseByNightDTO`, `ExpenseEqualDTO`
- Mapper: `ExpenseMapper` handles domain → DTO conversion
- **ACTION:** Add `ExpenseFreeDTO`, update mapper

**Frontend — AddExpenseModal.svelte (integration point):**
- Location: `$lib/components/expense/AddExpenseModal.svelte`
- Currently has BY_NIGHT and EQUAL radio buttons (lines 328-342)
- Uses `addExpense()` from `$lib/api/splits.ts`
- **ACTION:** Add FREE radio option, conditional share inputs, new API call

**Frontend — ExpensesSection.svelte (inline form):**
- Location: `routes/ExpensesSection.svelte`
- FREE radio button is commented out (lines 248-257) with note: "Uncomment when Story 4.3 is implemented"
- **ACTION:** Uncomment FREE option, add share inputs

**Frontend — ExpenseCard.svelte (display):**
- Location: `$lib/components/ui/expense-card/ExpenseCard.svelte`
- `formatShareCalculation()` returns empty string for FREE mode (line 54)
- **ACTION:** Return "Manual split" for FREE mode

**Frontend — API Client (splits.ts):**
- `SplitMode = 'BY_NIGHT' | 'EQUAL' | 'FREE'` — already defined
- `Share` interface already exists: `{ participantId: string; amount: number }`
- `AddExpenseRequest` lacks shares field — use separate request type
- **ACTION:** Add `AddFreeExpenseRequest` with shares, add `addFreeExpense()` function

### Key Architecture Decisions

**ExpenseFree.getShares() — No Calculation, Just Return:**
Unlike ExpenseByNight (calculates proportionally) and ExpenseEqual (calculates equally), ExpenseFree stores shares directly. The `getShares(Split split)` method simply returns the stored shares without any calculation. Validation happens at creation time, not at retrieval time.

**Validation Location — Domain Layer:**
Share validation (sum equals amount, all participants present) is domain logic and belongs in `ExpenseFree.create()` factory method, not in the REST resource or service layer. This follows the rich domain model pattern established by `ExpenseByNight` and `ExpenseEqual`.

**BigDecimal Comparison for Share Sum Validation:**
Use `compareTo() == 0` (not `equals()`) to compare share sum with expense amount, because `BigDecimal("100.00").equals(BigDecimal("100.0"))` returns false due to scale differences.

**Frontend Decimal Precision:**
JavaScript floating-point arithmetic can cause issues (e.g., `0.1 + 0.2 = 0.30000000000000004`). Use `Math.round(value * 100) / 100` or `parseFloat(value.toFixed(2))` for share total comparison in the frontend.

### Project Structure Notes

**New Backend Files:**
```
src/main/java/org/asymetrik/web/fairnsquare/split/domain/
├── ExpenseFree.java                      ← NEW: Free mode subclass
├── AddFreeExpenseRequest.java            ← NEW: Request DTO with shares
src/main/java/org/asymetrik/web/fairnsquare/split/persistence/dto/
├── ExpenseFreePersistenceDTO.java        ← NEW: Persistence DTO
src/main/java/org/asymetrik/web/fairnsquare/expense/api/dto/
├── ExpenseFreeDTO.java                   ← NEW: API response DTO
src/test/java/org/asymetrik/web/fairnsquare/split/domain/
├── ExpenseFreeTest.java                  ← NEW: Unit tests
```

**Modified Backend Files:**
```
Expense.java                              ← UPDATE: permits + factory methods
ExpensePersistenceDTO.java                ← UPDATE: permits clause
ExpensePersistenceMapper.java             ← UPDATE: FREE case
ExpenseDTO.java                           ← UPDATE: permits clause
ExpenseMapper.java                        ← UPDATE: FREE case
SplitUseCases.java                        ← UPDATE: addExpenseFree()
SplitResource.java                        ← UPDATE: POST /expenses/free
```

**Modified Frontend Files:**
```
src/main/webui/src/lib/api/splits.ts                              ← UPDATE: new request type + function
src/main/webui/src/lib/components/expense/AddExpenseModal.svelte   ← UPDATE: FREE mode UI
src/main/webui/src/routes/ExpensesSection.svelte                   ← UPDATE: uncomment FREE + add inputs
src/main/webui/src/lib/components/ui/expense-card/ExpenseCard.svelte ← UPDATE: FREE display
```

### Testing Requirements

**Backend Unit Tests (`ExpenseFreeTest.java`):**
- Valid creation with shares summing to amount
- Invalid: shares don't sum to amount → IllegalArgumentException
- Invalid: missing participant → IllegalArgumentException
- Invalid: negative share → IllegalArgumentException
- Valid: zero share amount allowed (participant doesn't owe)
- getShares() returns stored shares unchanged
- getSplitMode() returns FREE

**Backend Integration Tests:**
- POST /expenses/free → 201 with correct shares in response
- POST /expenses/free with bad sum → 400 Problem Details
- POST /expenses/free with missing participants → 400 Problem Details
- Round-trip: create free expense → GET split → shares match

**Frontend Component Tests:**
- AddExpenseModal: FREE mode selection, share inputs, running total, validation, submission
- ExpenseCard: FREE mode display shows "Manual split"

### Previous Story Intelligence (Story 4.2)

**Key Learnings from 4-2:**
1. Backend share calculation was already complete from TD-001-3 refactoring
2. ExpenseCard component uses controlled expansion state with Map
3. Currency formatting: en-IE locale with 2 decimal places
4. Accessibility: button with aria-expanded, keyboard navigable
5. 117 backend tests, 11 frontend tests all passing

**From FNS-002-3 (Add Expense Modal):**
1. Submit button uses `disabled={isLoading}` (NOT `disabled={!isValid}`)
2. Validation errors shown on submit attempt via touching all fields
3. Focus trap implemented in modal
4. Amount upper bound: €999,999.99
5. 37 tests covering all acceptance criteria

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Story 4.3]
- [Source: _bmad-output/planning-artifacts/architecture.md#Domain Model Patterns]
- [Source: _bmad-output/implementation-artifacts/4-2-calculate-shares-by-split-mode.md]
- [Source: _bmad-output/implementation-artifacts/fns-002-3-add-expense-modal.md]
- [Source: fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/Expense.java]
- [Source: fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/ExpenseByNight.java]
- [Source: fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/ExpenseEqual.java]
- [Source: fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/AddExpenseRequest.java]
- [Source: fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/service/SplitUseCases.java]
- [Source: fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/api/SplitResource.java]
- [Source: fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/persistence/dto/ExpensePersistenceDTO.java]
- [Source: fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/persistence/mapper/ExpensePersistenceMapper.java]
- [Source: fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/expense/api/dto/ExpenseDTO.java]
- [Source: fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/expense/api/mapper/ExpenseMapper.java]
- [Source: fairnsquare-app/src/main/webui/src/lib/api/splits.ts]
- [Source: fairnsquare-app/src/main/webui/src/lib/components/expense/AddExpenseModal.svelte]
- [Source: fairnsquare-app/src/main/webui/src/routes/ExpensesSection.svelte]
- [Source: fairnsquare-app/src/main/webui/src/lib/components/ui/expense-card/ExpenseCard.svelte]
- [Source: _bmad-output/project-context.md]

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

## Change Log

- 2026-02-06: Story created — ready for development