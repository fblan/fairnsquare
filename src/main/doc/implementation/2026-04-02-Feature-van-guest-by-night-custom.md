# Feature: BY_NIGHT_CUSTOM Expense Type (Van Guest Support)

## 1. What, Why and Constraints

### What
Added a new expense type `BY_NIGHT_CUSTOM` that allows selecting a specific subset of participants for a "by night" expense. This enables van guests (or any group of participants) who only participate in some nightly expenses (e.g., food and activities) but not others (e.g., house rent) to be excluded from specific expenses.

### Why
Issue #87: Van guests participate in all nightly expenses except the house rent. The previous model only supported BY_NIGHT (all participants) or FREE (manual parts), leaving no ergonomic way to distribute a nightly expense among a subset of participants proportionally by nights stayed.

### Constraints
- The number of nights per participant is **not editable** in the custom participant selection UI — it is read from the participant's existing configuration.
- BY_NIGHT_CUSTOM uses the same proportional calculation as BY_NIGHT (nights × share weight), but applied only to the selected subset.
- Backward-compatible persistence: new `BY_NIGHT_CUSTOM` type stored with `"type": "BY_NIGHT_CUSTOM"` in JSON.
- Frontend edit pattern: like FREE mode, BY_NIGHT_CUSTOM uses delete + create for all edits (the backend doesn't support updating participant lists in-place).
- The sealed `Expense` class and `ExpensePersistenceDTO` interface both had to be extended.

---

## 2. How

### Backend Domain

**Modified: `SplitMode.java`**
- Added `BY_NIGHT_CUSTOM("BY_NIGHT_CUSTOM")` enum value.

**Created: `ExpenseByNightCustom.java`**
- New `final` subclass of `Expense` (added to the `permits` clause of the sealed class).
- Stores a `List<Participant.Id> participantIds` — the selected subset of participants.
- `getShares(Split split)` filters the split's participants to the stored IDs, then applies the standard BY_NIGHT proportional calculation.
- Factory methods: `create(...)` (validates participant IDs non-null/non-empty) and `fromJson(...)` (for persistence roundtrip).

**Modified: `Expense.java`**
- Added `ExpenseByNightCustom` to the `permits` clause.
- Added `BY_NIGHT_CUSTOM` cases to the deprecated `create()` and `fromJson()` factory methods (both throw `UnsupportedOperationException`, directing callers to use `ExpenseByNightCustom` directly — same pattern as `FREE`).

### Backend Service

**Created: `AddByNightCustomExpenseRequest.java`**
- Request record with `amount`, `description`, `payerId`, and `participantIds: List<String>`.

**Modified: `SplitUseCases.java`**
- Added `addExpenseByNightCustom()` method: validates payer and all participant IDs exist in the split, then calls `ExpenseByNightCustom.create()`.
- Updated deprecated `addExpense()` to handle `BY_NIGHT_CUSTOM` case (throws `UnsupportedOperationException` directing to the new method).

### Backend API

**Created: `ExpenseByNightCustomDTO.java`**
- API response DTO for `BY_NIGHT_CUSTOM` type, same structure as `ExpenseByNightDTO`.

**Modified: `ExpenseDTO.java`**
- Added `ExpenseByNightCustomDTO` to `@JsonSubTypes` and `permits` clause.

**Modified: `ExpenseMapper.java`**
- Added `ExpenseByNightCustom` case in the `switch` expression, producing an `ExpenseByNightCustomDTO`.

**Modified: `SplitResource.java`**
- Added `POST /api/splits/{splitId}/expenses/by-night-custom` endpoint delegating to `addExpenseByNightCustom()`.

### Backend Persistence

**Created: `ExpenseByNightCustomPersistenceDTO.java`**
- Persistence record storing `participantIds: List<String>` alongside the standard expense fields.

**Modified: `ExpensePersistenceDTO.java`**
- Added `ExpenseByNightCustomPersistenceDTO` to `@JsonSubTypes` (with `"BY_NIGHT_CUSTOM"` discriminator) and `permits` clause.

**Modified: `ExpensePersistenceMapper.java`**
- `toPersistenceDTO()`: maps `ExpenseByNightCustom` → `ExpenseByNightCustomPersistenceDTO`, converting `Participant.Id` list to string list.
- `toDomain()`: maps `ExpenseByNightCustomPersistenceDTO` → `ExpenseByNightCustom`, converting string list back to `Participant.Id` list.

### Frontend

**Modified: `splits.ts`**
- Added `'BY_NIGHT_CUSTOM'` to the `SplitMode` union type.
- Added `AddByNightCustomExpenseRequest` interface.
- Added `addByNightCustomExpense()` API function calling `POST /splits/{splitId}/expenses/by-night-custom`.

**Created: `NightParticipantModal.svelte`**
- Sub-modal for selecting which participants participate in a BY_NIGHT_CUSTOM expense.
- Shows each participant with a checkbox and their night count (read-only) for reference.
- "Select all" shortcut button.
- Validates at least one participant is selected before allowing confirmation.

**Modified: `ExpenseEditModal.svelte`**
- Added import for `addByNightCustomExpense` and `NightParticipantModal`.
- Added `customParticipantIds: string[]` state and `showNightParticipantModal: boolean` state.
- Added `BY_NIGHT_CUSTOM` to `splitModeInfo` map.
- Added `BY_NIGHT_CUSTOM` radio button option in the split mode selector (with Moon + Edit3 icons and info button).
- Added "Participants" summary section when `BY_NIGHT_CUSTOM` is selected, with "Edit participants" button.
- Added `isCustomParticipantsValid` derived validation.
- Updated `handleSubmit()`: handles `BY_NIGHT_CUSTOM` with delete + create pattern (same as `FREE`).
- `resetForm()` now populates `customParticipantIds` from `expense.shares` when editing a `BY_NIGHT_CUSTOM` expense.
- Updated Escape key handler to include `showNightParticipantModal`.

**Modified: `ExpenseList.svelte`**
- Added `BY_NIGHT_CUSTOM` cases to `splitModeIcon()` (🌙✏️) and `splitModeText()` ("By Night (Custom)").

**Modified: `ExpenseCard.svelte`**
- Added `'BY_NIGHT_CUSTOM'` to `formatSplitMode()`.
- Updated `formatShareCalculation()`: for `BY_NIGHT_CUSTOM`, counts total nights only among the participating subset.

### Tests

**Created: `ExpenseByNightCustomTest.java`** (11 unit tests)
- Share calculation with subset of participants
- Single participant gets full amount
- Empty participants returns empty list
- Sum of shares equals expense amount (no rounding errors)
- Weighted by nights × share
- getSplitMode returns BY_NIGHT_CUSTOM
- getParticipantIds returns stored IDs
- fromJson roundtrip
- null participant IDs throws IllegalArgumentException
- Empty participant IDs throws IllegalArgumentException

**Modified: `AddExpenseModal.test.ts` and `EditExpenseModal.test.ts`**
- Updated two tests that used `getByRole('radio', { name: /by night/i })` to use exact string `'By Night'`, since the new "By Night (Custom)" radio option caused ambiguity.

---

## 3. Tests

### Backend Unit Tests
- `ExpenseByNightCustomTest.java`: 11 tests covering share calculation, validation, and persistence roundtrip.

### Frontend Tests
- All 447 existing tests continue to pass.
- Two existing tests were updated to use exact radio button names (`'By Night'` instead of `/by night/i`) to avoid ambiguity with the new "By Night (Custom)" option.
