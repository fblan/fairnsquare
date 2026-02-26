# Refactor: Rename BY_PERSON → BY_SHARE

**Date:** 2026-02-26
**Branch:** `refactor/participant-share`

---

## 1. What, Why and Constraints

**What:** Renamed the `BY_PERSON` expense split mode to `BY_SHARE` across the full stack (backend domain, API layer, persistence layer, frontend types, components, and tests). Renamed the `ExpenseByPerson` class (and related DTOs) to `ExpenseByShare`.

**Why:** This rename follows naturally from the earlier `numberOfPersons → share` refactor (same session). The split mode "BY_PERSON" implies a count of people, while the actual semantic is a weight multiplier (`share`). "BY_SHARE" accurately describes what the mode does: it distributes an expense proportionally based on each participant's `share` value.

**Constraints:**
- Same calculation logic: `participant_share / total_share × amount`.
- Backward compatibility: existing JSON storage files written with `"type": "BY_PERSON"` must still load correctly. Handled via a second `@JsonSubTypes.Type` entry on `ExpensePersistenceDTO` pointing the old name `"BY_PERSON"` to `ExpenseBySharePersistenceDTO`.
- The REST API JSON `splitMode` field was renamed from `"BY_PERSON"` → `"BY_SHARE"` (the frontend was updated in the same commit).
- The REST endpoint path changed from `/expenses/by-person` → `/expenses/by-share`.

---

## 2. How

### Backend — domain

**`SplitMode.java`**
- Enum constant `BY_PERSON("BY_PERSON")` → `BY_SHARE("BY_SHARE")`. Javadoc updated.

**`ExpenseByShare.java`** (renamed from `ExpenseByPerson.java` via `git mv`)
- Class name `ExpenseByPerson` → `ExpenseByShare`.
- Factory methods `create()` and `fromJson()` return type updated.
- `getSplitMode()` returns `SplitMode.BY_SHARE`.
- Javadoc and comments updated.

**`Expense.java`**
- `permits` clause: `ExpenseByPerson` → `ExpenseByShare`.
- Both switch cases in `create()` and `fromJson()`: `case BY_PERSON -> new ExpenseByPerson(...)` → `case BY_SHARE -> new ExpenseByShare(...)`.

### Backend — API/service

**`ExpenseByShareDTO.java`** (renamed from `ExpenseByPersonDTO.java` via `git mv`)
- Record name `ExpenseByPersonDTO` → `ExpenseByShareDTO`. Javadoc updated.

**`ExpenseDTO.java`**
- `@JsonSubTypes` entry: `ExpenseByPersonDTO` / `"BY_PERSON"` → `ExpenseByShareDTO` / `"BY_SHARE"`.
- `permits` clause updated.

**`ExpenseBySharePersistenceDTO.java`** (renamed from `ExpenseByPersonPersistenceDTO.java` via `git mv`)
- Record name `ExpenseByPersonPersistenceDTO` → `ExpenseBySharePersistenceDTO`. Javadoc updated.

**`ExpensePersistenceDTO.java`**
- Replaced `@JsonSubTypes.Type(value = ExpenseByPersonPersistenceDTO.class, name = "BY_PERSON")` with two entries:
  - `@JsonSubTypes.Type(value = ExpenseBySharePersistenceDTO.class, name = "BY_SHARE")` (new)
  - `@JsonSubTypes.Type(value = ExpenseBySharePersistenceDTO.class, name = "BY_PERSON")` (backward compat)
- `permits` clause: `ExpenseByPersonPersistenceDTO` → `ExpenseBySharePersistenceDTO`.

**`ExpenseMapper.java`**
- Import `ExpenseByPersonDTO` → `ExpenseByShareDTO`; `ExpenseByPerson` → `ExpenseByShare`.
- Switch case updated: `case ExpenseByPerson byPerson → new ExpenseByPersonDTO(..., "BY_PERSON", ...)` → `case ExpenseByShare byShare → new ExpenseByShareDTO(..., "BY_SHARE", ...)`.

**`ExpensePersistenceMapper.java`**
- Imports updated for `ExpenseByShare` and `ExpenseBySharePersistenceDTO`.
- `toPersistenceDTO`: `case ExpenseByPerson _ -> new ExpenseByPersonPersistenceDTO(...)` → `case ExpenseByShare _ -> new ExpenseBySharePersistenceDTO(...)`.
- `toDomain`: `case ExpenseBySharePersistenceDTO _ -> ExpenseByShare.fromJson(...)`.

**`SplitUseCases.java`**
- Import `ExpenseByPerson` → `ExpenseByShare`.
- Method `addExpenseByPerson()` → `addExpenseByShare()`: return type, Javadoc, internal calls.
- Deprecated `addExpense()` switch: `case BY_PERSON -> addExpenseByPerson(...)` → `case BY_SHARE -> addExpenseByShare(...)`.

**`SplitResource.java`**
- Method `addExpenseByPerson()` → `addExpenseByShare()`.
- `@Path("/{splitId}/expenses/by-person")` → `@Path("/{splitId}/expenses/by-share")`.
- `@Operation` summary/description and Javadoc updated.
- `splitService.addExpenseByPerson(...)` → `splitService.addExpenseByShare(...)`.

### Frontend

**`splits.ts`**
- `SplitMode` type: `'BY_PERSON'` → `'BY_SHARE'`.
- `addExpense()` routing: `request.splitMode === 'BY_PERSON'` → `'BY_SHARE'`; endpoint `by-person` → `by-share`.

**`ExpenseEditModal.svelte`**
- Radio button: `value="BY_PERSON"`, `id="modal-mode-by-person"`, label "By Person" → `value="BY_SHARE"`, `id="modal-mode-by-share"`, label "By Share".

---

## 3. Tests

**Backend (updated):**

| File | Changes |
|---|---|
| `ExpenseByShareTest.java` | Class name `ExpenseByPersonTest` → `ExpenseByShareTest`; all `ExpenseByPerson` references → `ExpenseByShare`; `BY_PERSON` → `BY_SHARE`; test method names updated (`getSplitMode_returnsByShare`, etc.) |

**Frontend (updated):**

| File | Changes |
|---|---|
| `AddExpenseModal.test.ts` | `BY_PERSON` → `BY_SHARE`; "By Person" → "By Share"; `byPersonRadio` → `byShareRadio`; describe block renamed |
| `EditExpenseModal.test.ts` | Same changes as `AddExpenseModal.test.ts` |