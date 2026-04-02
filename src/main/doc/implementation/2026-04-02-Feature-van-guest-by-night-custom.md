# Feature: Add BY_NIGHT_CUSTOM Expense Type for Van Guest Management

**Date:** 2026-04-02
**Issue:** #87

---

## 1. What, Why and Constraints

### What
Added a new `BY_NIGHT_CUSTOM` expense split mode that allows a subset of participants to be included in a "by night" expense. A button "Edit participants" is exposed on each BY_NIGHT (and BY_NIGHT_CUSTOM) expense in the expense list, opening a modal where the user can check/uncheck participants. The expense then becomes `BY_NIGHT_CUSTOM` (or reverts to `BY_NIGHT` if all participants are selected). Within the Expense Edit Modal, `BY_NIGHT_CUSTOM` is also a directly selectable split mode, consistent with the existing FREE mode pattern.

### Why
Van guests participate in all "by night" shared expenses (groceries, activities, etc.) except the house rent. There was no way to exclude specific participants from a single BY_NIGHT expense without switching to the manual FREE mode (which requires entering share parts manually). `BY_NIGHT_CUSTOM` solves this cleanly: nights × share weighting is preserved, just filtered to the selected participants.

### Constraints
- **Nights per participant are NOT editable** in the participants modal (read-only display only), as specified in the issue.
- Backward compatibility is preserved: existing `BY_NIGHT`, `EQUAL`, `BY_SHARE`, `FREE` expenses are unaffected.
- Sealed class hierarchy (`Expense`, `ExpensePersistenceDTO`, `ExpenseDTO`) is extended with `ExpenseByNightCustom` following the same pattern as the other subtypes.
- The `Expense.fromJson(...)` and `Expense.create(...)` factory methods throw `UnsupportedOperationException` for `BY_NIGHT_CUSTOM` (same as `FREE`), since a separate `participantIds` list is required.

---

## 2. How

### Backend

#### `SplitMode.java` (modified)
Added `BY_NIGHT_CUSTOM("BY_NIGHT_CUSTOM")` enum value.

#### `ExpenseByNightCustom.java` (new)
New sealed subclass of `Expense`. Stores `List<Participant.Id> participantIds`. `getShares(Split split)` filters split participants to only those in the list and delegates to `ExpenseByNight.calculateShares()` (the existing package-private method) for identical proportional calculation. Factory methods: `create(...)` and `fromJson(...)`.

#### `Expense.java` (modified)
- Added `ExpenseByNightCustom` to `sealed ... permits` clause.
- Added `BY_NIGHT_CUSTOM` cases in `create()` and `fromJson()` throwing `UnsupportedOperationException`.

#### `ExpenseByNightCustomPersistenceDTO.java` (new)
Persistence record with the standard 5 fields plus `List<String> participantIds`.

#### `ExpensePersistenceDTO.java` (modified)
Added `@JsonSubTypes.Type` for `BY_NIGHT_CUSTOM` and `ExpenseByNightCustomPersistenceDTO` to `sealed ... permits`.

#### `ExpensePersistenceMapper.java` (modified)
Added bidirectional mapping for `ExpenseByNightCustom` ↔ `ExpenseByNightCustomPersistenceDTO`.

#### `ExpenseByNightCustomDTO.java` (new)
API response record identical to `ExpenseByNightDTO` plus `List<String> participantIds`.

#### `ExpenseDTO.java` (modified)
Added `@JsonSubTypes.Type` for `BY_NIGHT_CUSTOM` and `ExpenseByNightCustomDTO` to `sealed ... permits`.

#### `ExpenseMapper.java` (modified)
Added `ExpenseByNightCustom` case mapping to `ExpenseByNightCustomDTO`.

#### `UpdateExpenseRequest.java` (modified)
Added `@Nullable List<String> participantIds` field (no bean validation — validated in service layer). Used when updating an expense to `BY_NIGHT_CUSTOM`.

#### `AddByNightCustomExpenseRequest.java` (new)
Request record with `amount`, `description`, `payerId`, `participantIds` (all validated).

#### `Split.java` (modified)
`updateExpense` refactored into two overloads: the original (without `participantIds`) delegates to a new overload. The new overload routes to `ExpenseByNightCustom.fromJson()` when `splitMode == BY_NIGHT_CUSTOM`, otherwise uses the existing `Expense.fromJson()`.

#### `SplitUseCases.java` (modified)
- Added `addExpenseByNightCustom()` (validates payer + all participantIds exist, creates and saves).
- Updated `updateExpense()` for `BY_NIGHT_CUSTOM`: validates participantIds provided and exist, converts to domain objects.

#### `SplitResource.java` (modified)
Added `POST /{splitId}/expenses/by-night-custom` endpoint following the pattern of `addExpenseFree`.

### Frontend

#### `splits.ts` (modified)
- Added `'BY_NIGHT_CUSTOM'` to the `SplitMode` union type.
- Added `participantIds?: string[]` to `UpdateExpenseRequest`.
- Added `AddByNightCustomExpenseRequest` interface.
- Added `addByNightCustomExpense()` function posting to `/splits/${splitId}/expenses/by-night-custom`.

#### `ByNightParticipantsModal.svelte` (new)
Sub-modal (z-[60]) matching the `ShareEditModal` pattern. Shows each participant with a checkbox, their name, nights (read-only), and share count (read-only). Defaults all participants to checked when `initialParticipantIds` is empty (new BY_NIGHT → BY_NIGHT_CUSTOM flow). Confirm is disabled until at least one participant is selected.

#### `ExpenseEditModal.svelte` (modified)
- Imports `MoonStar` (lucide) and `ByNightParticipantsModal`.
- Added `byNightCustomParticipantIds` state and `showByNightParticipantsModal` state.
- Extended `splitModeInfo` with `BY_NIGHT_CUSTOM`.
- `resetForm()` populates `byNightCustomParticipantIds` from shares when editing a BY_NIGHT_CUSTOM expense.
- `isPartsValid` updated to require ≥ 1 participant for BY_NIGHT_CUSTOM.
- `handleSubmit()` routes BY_NIGHT_CUSTOM to `addByNightCustomExpense` (add) or `updateExpense` with `participantIds` (edit).
- Added BY_NIGHT_CUSTOM radio button (after BY_NIGHT) with MoonStar icon and info button.
- Added BY_NIGHT_CUSTOM participants summary section with "Edit participants" button.
- Added `<ByNightParticipantsModal>` component at bottom.

#### `ExpenseList.svelte` (modified)
- Imports `updateExpense`, `MoonStar`, `UserMinus`, and `ByNightParticipantsModal`.
- `splitModeIcon()` and `splitModeText()` handle `BY_NIGHT_CUSTOM` (`🌟` / "By Night (Custom)").
- Added `showParticipantsModal` and `expenseForParticipants` state.
- `handleEditParticipantsClick()` sets state and opens the modal.
- `handleParticipantsConfirm()` calls `updateExpense` with the selected IDs (auto-downgrades to `BY_NIGHT` if all participants selected).
- "Edit participants" button (`UserMinus` icon) appears in the action row for BY_NIGHT and BY_NIGHT_CUSTOM expenses.
- `<ByNightParticipantsModal>` added at bottom, seeded from existing shares for BY_NIGHT_CUSTOM, empty for BY_NIGHT.

---

## 3. Tests

### Automated Tests Added

- **`ExpenseByNightCustomTest.java`** (new): Unit tests covering:
  - `getSplitMode()` returns `BY_NIGHT_CUSTOM`
  - `getShares()` excludes non-selected participants
  - `getShares()` calculates proportionally by nights for selected participants
  - `getShares()` with single participant gets full amount
  - `getShares()` with share weight (multiple persons) weighs correctly
  - `create()` with null/empty participantIds throws
  - `getParticipantIds()` returns stored IDs
  - `fromJson()` round-trip

### Existing Tests Fixed

- **`AddExpenseModal.test.ts`**: Updated `/by night/i` regex to `/^by night$/i` to avoid ambiguity with the new "By Night (Custom)" radio button.
- **`EditExpenseModal.test.ts`**: Same fix.

### Test Results
- 447 frontend tests pass (18 test files).
- Backend main compilation succeeds (Java version mismatch in CI prevents Surefire execution from this sandbox).
