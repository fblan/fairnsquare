# Feature: BY_NIGHT_CUSTOM — Van Guest Participant Management

**Date:** 2026-04-02  
**Issue:** #87 — Add a way to manage van guest

---

## What, Why and Constraints

### What
Added a new `BY_NIGHT_CUSTOM` split mode that allows editing which participants are included in a by-night expense. Users can click an "Edit Participants" button on any `BY_NIGHT` or `BY_NIGHT_CUSTOM` expense card to open a participant selection modal. The expense is then converted to (or stays as) `BY_NIGHT_CUSTOM`, and the cost is split proportionally by nights among only the selected participants.

### Why
Van guests participate in all by-night expenses except the house rent. Previously there was no way to exclude them from specific expenses. This feature allows fine-grained participant control per expense.

### Constraints
- Nights per participant are **not editable** from this flow — only participation (in/out) is toggled.
- The `BY_NIGHT_CUSTOM` mode uses the same proportional nights × share formula as `BY_NIGHT`, applied only to the selected subset.
- Updating participants on an existing expense uses the delete + recreate pattern (consistent with how FREE mode updates work), since the backend update endpoint does not support share data.
- The new mode is fully backward-compatible: existing `BY_NIGHT` data is unaffected.

---

## How

### Backend

**New files:**
- `split/domain/expenses/ExpenseByNightCustom.java` — New sealed class permitted by `Expense`. Stores `List<Participant.Id> includedParticipantIds`. `getShares()` filters the split's participants to the included subset and delegates to `ExpenseByNight.calculateShares()`.
- `split/persistence/dto/ExpenseByNightCustomPersistenceDTO.java` — Stores `participantIds: List<String>` in addition to standard expense fields.
- `split/api/expense/dto/ExpenseByNightCustomDTO.java` — API response DTO (same shape as other expense DTOs).
- `split/service/AddByNightCustomExpenseRequest.java` — Request record with `amount`, `description`, `payerId`, `participantIds`.

**Modified files:**
- `SplitMode.java` — Added `BY_NIGHT_CUSTOM` enum value.
- `Expense.java` — Added `ExpenseByNightCustom` to `permits`; added `BY_NIGHT_CUSTOM` case in `fromJson()` and deprecated `create()` (throws UnsupportedOperationException like FREE mode).
- `ExpensePersistenceDTO.java` — Added `BY_NIGHT_CUSTOM` to `@JsonSubTypes` and sealed interface `permits`.
- `ExpensePersistenceMapper.java` — Added `ExpenseByNightCustom ↔ ExpenseByNightCustomPersistenceDTO` branches in both directions.
- `ExpenseMapper.java` — Added `ExpenseByNightCustom` branch producing `ExpenseByNightCustomDTO`.
- `SplitUseCases.java` — Added `addExpenseByNightCustom()` method validating payer and all participant IDs exist in the split.
- `SplitResource.java` — Added `POST /splits/{splitId}/expenses/by-night-custom` endpoint.

### Frontend

**New files:**
- `lib/components/expense/ParticipantSelectModal.svelte` — Modal with a checkbox list of participants. Shows nights next to each name (read-only). Validates at least one participant selected. Calls `onConfirm(selectedIds[])` on confirm.
- `lib/components/expense/ParticipantSelectModal.test.ts` — 15 tests covering render, pre-selection, toggle, confirm/cancel, keyboard dismiss.

**Modified files:**
- `lib/api/splits.ts` — Added `'BY_NIGHT_CUSTOM'` to `SplitMode` union; added `AddByNightCustomExpenseRequest` interface and `addByNightCustomExpense()` function calling `POST /expenses/by-night-custom`.
- `routes/ExpenseList.svelte`:
  - Imported `addByNightCustomExpense`, `ParticipantSelectModal`, `UserCog` icon.
  - `splitModeIcon()`: `BY_NIGHT_CUSTOM` → `🌙★`
  - `splitModeText()`: `BY_NIGHT_CUSTOM` → `"By Night (Custom)"`
  - `getParticipantNames()`: unchanged — `BY_NIGHT_CUSTOM` shares already contain only included participants.
  - Added `showEditParticipants`, `expenseToEditParticipants`, `isEditingParticipants` state.
  - Added `UserCog` ("Edit Participants") button on `BY_NIGHT` and `BY_NIGHT_CUSTOM` expense cards.
  - `handleConfirmEditParticipants()`: calls `deleteExpense` + `addByNightCustomExpense` + reload.
  - Wired up `ParticipantSelectModal`.
- `lib/components/expense/ExpenseEditModal.svelte`:
  - Added `addByNightCustomExpense` import and `MoonStar` icon.
  - Added `BY_NIGHT_CUSTOM` entry in `splitModeInfo`.
  - Added `byNightCustomParticipantIds` derived (reads participant IDs from existing shares).
  - Handles `BY_NIGHT_CUSTOM` in submit: uses delete + recreate preserving participant IDs.
  - Shows `BY_NIGHT_CUSTOM` as a conditional radio option (visible only when editing a custom expense).

---

## Tests

### Backend (unit tests)
File: `src/test/java/.../split/domain/expenses/ExpenseByNightCustomTest.java` — 8 tests:
- `getShares_withAllParticipantsIncluded_behavesLikeByNight`
- `getShares_withSubsetOfParticipants_excludesNonIncluded`
- `getShares_withSingleIncludedParticipant_getsFullAmount`
- `getShares_sumsToExactExpenseAmount`
- `getSplitMode_returnsByNightCustom`
- `create_withEmptyParticipantIds_throwsIllegalArgumentException`
- `create_withNullParticipantIds_throwsIllegalArgumentException`
- `fromJson_recreatesExpenseWithSameParticipantIds`
- `getShares_withPersonsWeighting_appliesNightTimesPersonsFormula`

> Note: Backend tests could not be executed in this environment due to Maven BOM resolution failure (no network access to `repo.maven.apache.org`). The test logic mirrors the existing `ExpenseByNightTest` patterns and was verified by code review.

### Frontend (vitest)
File: `src/lib/components/expense/ParticipantSelectModal.test.ts` — **15 tests**, all passing:
- Render/visibility, participant list, nights display
- Pre-selection from `initialSelectedIds`
- Enable/disable confirm button
- Validation message when empty
- `onConfirm` called with correct IDs
- Unchecked participants excluded
- `onCancel` from cancel button, close button, Escape key, backdrop click
- Count in confirm button updates on toggle

**Full suite:** 462 tests passed (19 test files), no regressions.
