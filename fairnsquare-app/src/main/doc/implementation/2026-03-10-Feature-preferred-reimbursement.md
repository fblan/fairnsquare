# 2026-03-10 — Feature: Preferred Reimbursement Destination

## 1. What, Why and Constraints

**What:** A debtor (participant who owes money) can select a preferred creditor (participant they want to reimburse first) on the Settlement page. This preference is persisted per participant and honoured by the settlement algorithm before it runs the greedy optimisation pass.

**Why:** Allows participants to express real-world preferences (e.g. "I want to pay Alice back first"). This makes reimbursement proposals more practical.

**Constraints:**
- One preferred creditor per debtor (not multiple)
- If the preferred creditor is not owed money in the current settlement, the preference is silently ignored
- If a participant is removed from the split, all references to them as a preferred creditor are cleared automatically by the domain
- The preference is stored on the `Participant` domain object and persisted through the existing ZIP/JSON file store
- Backward compatible: existing splits without the field deserialise cleanly (Jackson returns null for missing field)

---

## 2. How

### Backend — Domain layer (`Participant`, `Split`)

- **`Participant.java`**: Added a 5th record component `Id preferredCreditorId` (nullable). Both `create()` factory methods updated to pass `null`.
- **`Split.java`**:
  - `updateParticipant()` signature extended with a 5th parameter `Participant.Id newPreferredCreditorId`.
  - `removeParticipant()` now iterates remaining participants after removal and nulls out any `preferredCreditorId` pointing to the removed participant.

### Backend — Settlement algorithm (`SettlementCalculator`)

- `calculateReimbursements()` now receives the participant list alongside balances.
- **Phase 1** (new): builds a `preferredCreditorMap` (debtorId → preferredCreditorId) and `creditorIndex` lookup. For each debtor with a preference, if the preferred creditor is in the creditors list, the minimum of the debtor's remaining debt and creditor's remaining credit is transferred first.
- **Phase 2** (unchanged): existing greedy two-pointer processes all remaining balances. Entries fully settled in Phase 1 are skipped naturally (remaining = 0 advances the pointer).

### Backend — Persistence (`ParticipantPersistenceDTO`, `ParticipantPersistenceMapper`)

- `ParticipantPersistenceDTO`: added 5th field `String preferredCreditorId` (nullable, no annotation needed — Jackson maps null cleanly).
- `ParticipantPersistenceMapper`:
  - `toPersistenceDTO()`: extracts `preferredCreditorId` value or null.
  - `toDomain()`: uses `Participant.Id.isValid()` guard before constructing the Id (null/invalid → null).

### Backend — API layer (`UpdateParticipantRequest`, `ParticipantDTO`, `ParticipantMapper`, `SplitUseCases`)

- `UpdateParticipantRequest`: added optional nullable `String preferredCreditorId`.
- `ParticipantDTO`: added `@JsonProperty("preferredCreditorId") String preferredCreditorId`.
- `ParticipantMapper.toDTO()`: extracts value or null.
- `SplitUseCases.updateParticipant()`: uses `Participant.Id.isValid()` guard before constructing the Id.

### Frontend — API client (`splits.ts`)

- `Participant` interface: added `preferredCreditorId?: string | null`.
- `UpdateParticipantRequest` interface: added `preferredCreditorId?: string | null`.

### Frontend — Settlement UI (`Settlement.svelte`)

- Imports `updateParticipant` and `Split` type.
- Stores the full `split` state (not just the name) to access participant data.
- Added `handlePreferredCreditorChange(participantId, creditorId)`: calls `updateParticipant`, updates local participant state via Svelte 5 deep reactivity, shows error toast on failure.
- For each debtor card (balance < −0.005): renders a labelled `<select>` with "No preference" and all other participants as options, pre-selected from `participant.preferredCreditorId`.

---

## 3. Tests

### Backend unit tests

- **`SettlementCalculatorTest`**: 9 new tests covering:
  - Preferred pairing honoured when both parties have remaining balances
  - Preferred pairing with partial debt (debtor owes less than creditor is owed)
  - Preferred pairing with partial credit (creditor is owed less than debtor owes — remainder resolved by greedy)
  - Multiple debtors preferring the same creditor
  - Preferred creditor not in creditors list → silently ignored
  - `removeParticipant()` clears preferred creditor reference from remaining participants
  - `removeParticipant()` on participant who is nobody's preferred → no crash
- **`SplitSettlementTest`**: Fixed `updateParticipant()` call (4-arg → 5-arg with null)
- **`ParticipantPersistenceMapperTest`**: Fixed DTO constructor calls (4-arg → 5-arg with null)
- **`SplitPersistenceMapperTest`**: Fixed DTO constructor call (4-arg → 5-arg with null)

### Frontend tests

- **`Settlement.test.ts`**: 5 new tests in `describe('Preferred Creditor', ...)`:
  - Preferred creditor select appears for debtors
  - No select rendered for creditors
  - Pre-selects existing `preferredCreditorId`
  - Calls `updateParticipant` with correct args on change
  - Sends `null` when "No preference" is selected
  - Shows error toast when `updateParticipant` rejects

All 360 frontend tests pass. All 41 backend unit tests pass.
