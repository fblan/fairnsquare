# Feature: BY_PERSON Split Mode

**Date:** 2026-02-20
**Branch:** `feature/by-person-split-mode`

---

## 1. What, Why and Constraints

**What:** Added a new `BY_PERSON` expense split mode that distributes costs proportionally to each participant's `numberOfPersons` field. A participant with 2 persons pays twice as much as one with 1 person, regardless of nights stayed.

**Why:** The existing `BY_NIGHT` mode weighs both nights and persons (`nights × numberOfPersons`). Some expenses (e.g., groceries, park entry) are better allocated purely by headcount rather than duration of stay.

**Formula:** `share = amount × participant.numberOfPersons / total_numberOfPersons`

**Constraints:**
- Follow the exact same domain patterns as `BY_NIGHT` and `EQUAL` (sealed class hierarchy, `calculateShares`, `fromJson`, persistence DTO)
- The REST API uses a dedicated endpoint `/expenses/by-person` consistent with the existing pattern
- Frontend routing in `addExpense()` must handle the new value without breaking existing modes
- All existing tests must continue to pass

---

## 2. How

### Backend – Domain layer

**`SplitMode.java`** — Added `BY_PERSON` enum value.

**`ExpenseByPerson.java`** — New class, modeled on `ExpenseByNight`. Key difference: weight = `numberOfPersons` only (nights are ignored). Same rounding strategy: 2dp HALF_UP, last participant gets remainder.

**`Expense.java`** — Added `ExpenseByPerson` to `permits` clause; added `BY_PERSON` cases to the deprecated `create()` and `fromJson()` switch expressions.

### Backend – Persistence layer

**`ExpenseByPersonPersistenceDTO.java`** — New persistence record, identical structure to `ExpenseByNightPersistenceDTO` (no shares stored; recalculated on load).

**`ExpensePersistenceDTO.java`** — Added `BY_PERSON` to `@JsonSubTypes` and `permits`.

**`ExpensePersistenceMapper.java`** — Added `ExpenseByPerson` case in both `toPersistenceDTO()` and `toDomain()` switches.

### Backend – API layer

**`ExpenseByPersonDTO.java`** — New API response record implementing `ExpenseDTO`.

**`ExpenseDTO.java`** — Added `BY_PERSON` to `@JsonSubTypes` and `permits`.

**`ExpenseMapper.java`** — Added `ExpenseByPerson` case in `toDTO()` switch.

### Backend – Service & REST

**`SplitUseCases.java`** — Added `addExpenseByPerson()` method following the same pattern as `addExpenseByNight()`; updated deprecated `addExpense()` switch.

**`SplitResource.java`** — Added `POST /api/splits/{splitId}/expenses/by-person` endpoint.

### Frontend

**`splits.ts`** — Added `'BY_PERSON'` to `SplitMode` union type; updated `addExpense()` routing to map `BY_PERSON` → `/expenses/by-person`.

**`ExpenseEditModal.svelte`** — Added `Users` icon import; added "By Person" radio button between Equal and Manual in the split mode group. No changes to submit logic needed — `BY_PERSON` correctly falls into the existing `else` branch that calls `addExpense()`.

### Frontend — Bugfixes (included in this branch)

**`ExpenseEditModal.svelte` — Amount input step constraint:**
- Changed `step="0.5"` → `step="any"` to allow manual entry of arbitrary decimal amounts (e.g., 12.30)
- Removed `min="0.5"` (custom validation already enforces min €0.01)
- Added `handleAmountKeydown()` function that intercepts ArrowUp/Down and manually applies ±0.5 increment with floor at 0

**`ExpenseEditModal.svelte` — Description made mandatory:**
- Changed label from `"Description (optional)"` to `"Description"`
- Updated `isDescriptionValid` to require `description.trim().length > 0`
- Updated `validateDescription()` to show `"Description is required"` error for blank input
- Aligns frontend validation with existing backend `@NotBlank` constraint

### Frontend — Pre-existing test fixes

**`EditParticipantModal.test.ts`** — Fixed 4 tests that used `userEvent.clear()` + `userEvent.type()` on `<input type="number">`, which does not reliably trigger Svelte's `bind:value` reactivity in jsdom. Replaced with `fireEvent.input()` + `fireEvent.blur()`, consistent with the pattern used in all expense modal tests.

---

## 3. Tests

### Backend — `ExpenseByPersonTest.java` (10 new tests)

| Test | What it verifies |
|------|-----------------|
| Equal persons → equal split | 3 × 1 person, €90 → €30 each |
| Different persons → proportional | 2 persons vs 1 person, €90 → €60 / €30 |
| Nights are ignored | Same persons, different nights → same result |
| Half-person (child) weighting | 2.5 vs 1.0 persons, €70 → €50 / €20 |
| Indivisible amount → remainder to last | €100 / 3 → €33.33 / €33.33 / €33.34 |
| Single participant gets full amount | 1 participant → full amount |
| Empty participants → empty list | Edge case |
| Null participants → empty list | Edge case |
| `getSplitMode()` → `BY_PERSON` | Type identity |
| `fromJson()` → valid expense | Persistence round-trip |

### Frontend — `AddExpenseModal.test.ts` (4 new BY_PERSON tests + 1 updated test)
- Renders By Person radio button
- Allows selecting By Person
- Calls `addExpense` with `BY_PERSON` splitMode
- Does not show FREE mode share inputs when By Person is selected
- **Updated:** `"allows empty description (optional field)"` → `"shows error when description is empty on submit"` (description now mandatory)
- **Updated:** FREE mode API call test now includes a description in the request

### Frontend — `EditExpenseModal.test.ts` (4 new tests)
- Renders By Person radio button
- Pre-selects BY_PERSON when expense has that mode
- Enables Save Changes when mode changed to BY_PERSON
- Calls `updateExpense` with `BY_PERSON` splitMode

### Frontend — `EditParticipantModal.test.ts` (4 tests fixed)
- Replaced `userEvent` with `fireEvent.input`/`fireEvent.blur` for number inputs (nights validation, nights dirty tracking, persons validation, persons dirty tracking)

**Totals:** 10 backend tests + 8 new frontend tests + 6 updated/fixed frontend tests = 24 test changes. All 298 frontend tests and all 43 backend domain tests pass.