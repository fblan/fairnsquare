# Refactor: Rename numberOfPersons → share

**Date:** 2026-02-26
**Branch:** `refactor/participant-share`

---

## 1. What, Why and Constraints

**What:** Renamed the `numberOfPersons` concept to `share` across the full stack (backend domain, API layer, persistence layer, frontend types, components, and tests).

**Why:** The label "Number of Persons" was unclear to users. "Persons" implies an integer count of people, while the actual meaning is a weight multiplier that can be fractional (e.g. 0.5 for a child, 2 for a couple sharing a room). The new label **Share** better communicates this — a participant with a share of 2 contributes twice as much to per-person expenses as a participant with a share of 1.

**Constraints:**
- Same validation rules: min 0.5, max 50, step 0.5.
- Same business logic: the `share` value is used as a weight in `ExpenseByPerson` and `ExpenseByNight` calculations.
- Backward compatibility: existing JSON storage files (written with the old `numberOfPersons` field key) must still load correctly. Handled via `@JsonAlias("numberOfPersons")` on the `ParticipantPersistenceDTO.share` field.
- The REST API JSON key was also renamed from `numberOfPersons` → `share` (the frontend was updated in the same commit, so no external consumers are broken).

---

## 2. How

### Backend — domain

**`Participant.java`**
- Renamed inner record `NumberOfPersons` → `Share`.
- Renamed the record component `numberOfPersons` → `share`.
- Updated both `create()` factory methods, the class-level Javadoc, `toString()`, and all validation messages.

**`ExpenseByPerson.java`**
- `p.numberOfPersons().value()` → `p.share().value()`
- Local variable `totalPersons` → `totalShares`.
- Updated class-level Javadoc.

**`ExpenseByNight.java`**
- `p.numberOfPersons().value()` → `p.share().value()` (×2).

### Backend — service/API

**`AddParticipantRequest.java`**
- Field `numberOfPersons` → `share`; method `numberOfPersonsOrDefault()` → `shareOrDefault()`; validation messages updated.

**`UpdateParticipantRequest.java`**
- Same changes as `AddParticipantRequest`.

**`ParticipantDTO.java`**
- `@JsonProperty("numberOfPersons")` → `@JsonProperty("share")` (changes the REST API JSON key).

**`ParticipantMapper.java`**
- `participant.numberOfPersons()` → `participant.share()`.

**`ParticipantPersistenceDTO.java`**
- Field renamed to `share`; added `@JsonAlias("numberOfPersons")` for backward compatibility with existing stored data.

**`ParticipantPersistenceMapper.java`**
- `toPersistenceDTO`: `participant.numberOfPersons()` → `participant.share()`.
- `toDomain`: variable and default-fallback updated; `Participant.NumberOfPersons` → `Participant.Share`.

**`SplitUseCases.java`**
- Both calls to `request.numberOfPersonsOrDefault()` → `request.shareOrDefault()`.

**`ExpenseByPersonPersistenceDTO.java`**
- Updated Javadoc comment only.

### Frontend

**`splits.ts`**
- Renamed `numberOfPersons: number` → `share: number` in `AddParticipantRequest`, `UpdateParticipantRequest`, and `Participant` interfaces.

**`Home.svelte`**
- State variable `numberOfPersons` → `share`; touched flag `numberOfPersonsTouched` → `shareTouched`; derived error `numberOfPersonsError` → `shareError`.
- Label "Persons" → "Share"; input id and `aria-describedby` updated accordingly.

**`Participants.svelte`**
- State `formNumberOfPersons` → `formShare`; validation errors key `numberOfPersons` → `share`; validator function `validateNumberOfPersonsOnInput` → `validateShareOnInput`.
- Label "Persons" → "Share"; badge label "person/persons" → "share/shares".

**`EditParticipantModal.svelte`**
- State `editNumberOfPersons` → `editShare`; touched flag `numberOfPersonsTouched` → `shareTouched`; derived `isNumberOfPersonsValid` → `isShareValid`; validator `validateNumberOfPersons` → `validateShare`; blur handler `handleNumberOfPersonsBlur` → `handleShareBlur`.
- Label "Persons" → "Share"; `bind:value`, `aria-invalid`, `aria-describedby` updated.

**`ParticipantSummaryCard.svelte`**
- `p.numberOfPersons > 1 ? `, ${p.numberOfPersons}p`` → `p.share > 1 ? `, ×${p.share}`` (clearer multiplier notation).

---

## 3. Tests

**Backend (all pre-existing, updated):**

| File | Changes |
|---|---|
| `ParticipantPersistenceMapperTest.java` | Updated field assertions: `dto.share()`, `participant.share()`; renamed test `shouldDefaultShareToOneWhenZeroInDTO` |
| `ParticipantMapperTest.java` | Updated assertion: `dto.share()` |
| `ExpenseByPersonTest.java` | Renamed test `calculateShares_withDifferentShares_proportionalToShare`; renamed helper parameter |

**Frontend (all pre-existing, updated):**

| File | Changes |
|---|---|
| `Home.test.ts` | Renamed test; updated fixture field and `getByLabelText('Share')` |
| `Participants.test.ts` | All fixture fields; `getByLabelText('Share')`; 4 test renames; badge text; summary card text |
| `Split.test.ts` | All fixture fields; 1 test rename; summary card text |
| `ParticipantSummaryCard.test.ts` | All fixture fields; 2 test renames; expected text updated for `×N` format |
| `EditParticipantModal.test.ts` | All fixture fields; describe/test renames; all `getByLabelText(/share/i)` queries; API expectation |