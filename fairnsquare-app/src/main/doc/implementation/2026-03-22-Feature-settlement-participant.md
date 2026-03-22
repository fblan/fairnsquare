# Feature: Settlement Participant (Shared Account)

## What, Why and Constraints

**What:** Replaced the "preferred creditor" concept with a "shared account" concept. Participants who share a bank account can be grouped so the settlement algorithm treats them as a single economic entity (one debtor or creditor). The grouping is expressed by a new `sharedAccountId` field on `Participant`; participants with the same `sharedAccountId` form a shared account group.

**Why:** The preferred-creditor feature was a workaround for couples/roommates who share finances — they should be treated as one payer/debtor in the settlement, not two separate individuals with a preference ordering. The shared account model captures this intent directly and produces cleaner reimbursement proposals.

**Constraints:**
- `SettlementParticipant` is NOT persisted — it is computed at runtime from `Participant` records.
- Already-persisted splits (with no `sharedAccountId` field, or with the old `preferredCreditorId` field) must continue to load and function correctly (backward compatibility).
- Already-persisted settlements (with old `Reimbursement` records referencing `Participant.Id` pairs) must also load correctly.
- The two-phase preferred-creditor settlement algorithm is removed; only the greedy min-cash-flow algorithm remains.

## How

### Step 1 — `SettlementParticipant` sealed interface

**Created** `SettlementParticipant.java`: sealed interface with three implementations:
- `Standard(Participant)` — a normal individual participant
- `SharedAccount(SharedAccountId, List<Participant>)` — a group treated as one entity; `name()` returns member names sorted alphabetically joined by " & "; `balance()` returns sum of member balances
- `SharedAccountMember(Participant)` — kept only so `SettlementCalculator` can skip it (only `Standard` and `SharedAccount` participate in reimbursement computation)

**Created** `SettlementPartyId.java`: sealed type used in `Reimbursement` to reference either an individual (`Individual(Participant.Id)`) or a group (`Group(Participant.SharedAccountId)`). This avoids conflating the two ID namespaces in the persistence layer.

**Created** `SettlementParticipantMapper.java`: converts `List<Participant>` → `List<SettlementParticipant>`. Groups participants by `sharedAccountId`; singletons (groups of 1) → `Standard`; groups of 2+ → one `SharedAccount` node followed by one `SharedAccountMember` per member.

### Step 2 — `Participant` model

**Modified** `Participant.java`:
- Replaced `preferredCreditorId: Participant.Id` with `sharedAccountId: Participant.SharedAccountId`
- Added nested `SharedAccountId` value object (NanoId format, same pattern as `Participant.Id`) with `generate()`, `of()`, and `isValid()` factory methods
- Removed `create(String, int, int)` overload that accepted `preferredCreditorId`; `create` factory now always sets `sharedAccountId = null`

**Modified** `Split.java`:
- `updateParticipant`: last parameter changed from `Participant.Id newPreferredCreditorId` → `Participant.SharedAccountId newSharedAccountId`
- `removeParticipant`: removed the for-loop that cleared `preferredCreditorId` references on other participants (no longer needed)
- `recalculateBalances`: updated field reference

**Modified** `UpdateParticipantRequest.java`: `preferredCreditorId` → `sharedAccountId`

### Step 3 — `SettlementCalculator`

**Rewritten** `SettlementCalculator.java`:
- Uses `SettlementParticipantMapper.from(split.getParticipants())` to build the participant list
- Skips `SharedAccountMember` when computing reimbursements
- Removed two-phase preferred-creditor logic; only greedy min-cash-flow algorithm remains
- Helper `toPartyId(SettlementParticipant)` converts `Standard` → `Individual`, `SharedAccount` → `Group`

**Modified** `Reimbursement.java`: `fromId`/`toId` fields changed from `Participant.Id` to `SettlementPartyId`

### Step 4 — Persistence layer (backward compatible)

**Modified** `ParticipantPersistenceDTO.java`: `preferredCreditorId` → `sharedAccountId` (Jackson silently ignores the old `preferredCreditorId` field in files saved before this change; `sharedAccountId` defaults to `null`)

**Modified** `SettlementPersistenceDTO.java**: added optional `fromType`/`toType` fields to `ReimbursementPersistenceDTO` (null or absent = `Individual` for backward compatibility; `"group"` = `Group`)

**Modified** `ParticipantPersistenceMapper.java`: maps `sharedAccountId` instead of `preferredCreditorId`

**Modified** `SplitPersistenceMapper.java`:
- Saves reimbursements with `partyType()` helper (null for `Individual`, `"group"` for `Group`)
- Loads reimbursements with `toPartyId(id, type)` helper (null/absent type → `Individual`, backward compat)

### Step 5 — API layer

**Modified** `ParticipantDTO.java`: `preferredCreditorId` → `sharedAccountId`

**Modified** `ParticipantMapper.java`: maps `sharedAccountId`

**Rewritten** `SettlementMapper.java`: builds a combined `Map<String, String> nameById` covering both individual participant IDs and `SharedAccount` group IDs, so `fromName`/`toName` in the API response correctly resolves to "Alice & Bob" for group reimbursements

**Modified** `SplitUseCases.java`: `updateParticipant` uses `Participant.SharedAccountId.isValid()` and `.of()` instead of `Participant.Id`

### Step 6 — Backend tests

**Rewritten** `SettlementCalculatorTest.java`:
- Removed all 5 preferred-creditor tests and 2 `removeParticipant_clearsPreferredCreditorReferences` tests
- Updated all `r.fromId()`/`r.toId()` → `r.from()`/`r.to()` using `SettlementPartyId.Individual`
- Added 3 new shared-account tests: `calculate_sharedAccount_treatedAsSingleDebtor`, `calculate_sharedAccount_treatedAsSingleCreditor`, `calculate_singletonSharedAccount_treatedAsStandard`

**Created** `SettlementParticipantMapperTest.java`: 7 tests covering all-Standard, singleton group → Standard, 2-member group → SharedAccount+Members, alphabetical name, balance sum, empty list, two independent groups

**Modified** `SplitPersistenceMapperTest.java`: updated `r.fromId()`/`r.toId()` to `r.from()`/`r.to()` with `SettlementPartyId.Individual` assertions

**Modified** `PersistenceBackwardCompatibilityTest.java`: renamed test; `preferredCreditorId()` → `sharedAccountId()`

### Step 7 — Frontend

**Modified** `splits.ts`: `preferredCreditorId` → `sharedAccountId` in `UpdateParticipantRequest` and `Participant` interfaces

**Modified** `Settlement.svelte`:
- Removed `handlePreferredCreditorChange` function and "Reimburse first" select
- Re-added `updateParticipant` import for shared account editing
- Added `generateSharedAccountId()` — generates a valid 21-char NanoId without external dependencies using `crypto.getRandomValues` and the 64-char NanoId alphabet
- Added `DisplayEntry` interface and `displayBalances` `$derived` — collapses shared-account members into one group entry (combined name "Alice & Bob", combined totals); members are never rendered as separate cards
- Added `handleSharedAccountChange(participantId, partnerIdOrEmpty)` — 1 PUT when joining an existing group; 2 PUTs when creating a new group; 1 PUT with `null` when ungrouping; reloads `split` via `getSplit` and recomputes `settlement.balances` from fresh participant data
- Added `handleUngroup(memberIds[])` — PUTs all members with `sharedAccountId: null`, then reloads
- Each balance card shows a "Group with..." button (standard) or "Ungroup" button (group), hidden after Resolve
- "Group with..." opens an inline modal: participant name in title, `<select>` of other participants, Cancel/OK buttons; OK calls `handleSharedAccountChange` then closes modal

**Modified** `Settlement.test.ts`:
- Replaced all `preferredCreditorId` fixture values with `sharedAccountId`
- Removed entire "Preferred Creditor" describe block (8 tests)
- Added "Shared Account Grouping" describe block (16 tests): collapsed display, group card combined financials, "Group with..." button visibility, "Ungroup" button on group cards, modal open/close/cancel, new group (2 PUTs), join existing group (1 PUT), Ungroup action (null for all members), error handling

## Tests

**Backend (294 tests, all passing):**
- `SettlementCalculatorTest` — 3 new shared-account scenarios; 7 preferred-creditor tests removed
- `SettlementParticipantMapperTest` — 7 new unit tests for mapper logic
- `SplitPersistenceMapperTest` — updated for new `Reimbursement` field names
- `PersistenceBackwardCompatibilityTest` — existing backward-compat fixture tests still pass

**Frontend (410 tests, all passing):**
- `Settlement.test.ts` — 46 tests; preferred-creditor UI tests removed; shared-account grouping tests added (16 tests covering collapsed display, modal flow, Ungroup, error handling); all settlement display, resolve, unsettle, and export tests pass
