# Bugfix: Participant Names in Logs (Issue #44)

## What, Why and Constraints

**What:** Removed participant names from application logs. Names are PII and must not appear in system logs.

**Why:** The `LogInterceptor` calls `toString()` on the result of every `@Log`-annotated method. Domain records (`Participant`, `ParticipantBalance`, `Reimbursement`) used auto-generated `toString()` which included all fields — including participant names. This caused names to appear in INFO-level logs on every `addParticipant`, `updateParticipant`, and `calculateSettlement` call.

**Constraints:**
- The API layer (REST DTOs) must still return participant names for the frontend — names were only removed from the **domain** and **persistence** layers.
- The API DTO mapper now resolves names at the boundary (from the participant list), following the principle that names are a presentation concern.
- Old persisted settlement JSON files that contain `participantName`/`fromName`/`toName` fields will be silently ignored on deserialization (Jackson ignores unknown properties by default in Quarkus).
- The backend rule mandates all domain entities implement `toString()` with "identity and key state (ID, counts, flags)" — this fix enforces that rule for the settlement records that were missing it.

**Residual:** Domain validation exception messages (e.g., "Missing share for participant 'Bob'") still include names as they are part of error semantics, not system logging. This is a separate concern.

## How

### Files modified

**`Participant.java`**
Added explicit `toString()` override showing `id`, `nights`, `numberOfPersons` — omitting `name`.

**`ParticipantBalance.java`**
Removed `participantName` field. The record now holds only financial data keyed by `participantId`. Added `toString()` showing id, paid, cost, balance.

**`Reimbursement.java`**
Removed `fromName` and `toName` fields. The record now holds only `fromId`, `toId`, `amount`. Added `toString()`.

**`Settlement.java`**
Added `toString()` showing counts only: `Settlement{balances=N, reimbursements=M}`.

**`SettlementCalculator.java`**
Updated `calculateBalances()` and `calculateReimbursements()` to construct `ParticipantBalance` and `Reimbursement` without names.

**`SettlementPersistenceDTO.java`**
Removed `participantName` from `ParticipantBalancePersistenceDTO` and `fromName`/`toName` from `ReimbursementPersistenceDTO`.

**`SplitPersistenceMapper.java`**
Updated both `settlementToPersistenceDTO()` and `settlementToDomain()` to no longer map name fields.

**`SettlementMapper.java`** (API mapper)
Changed `toDTO(Settlement)` to `toDTO(Settlement, List<Participant>)`. Builds a `Map<Participant.Id, String>` from the participant list to resolve names for the DTO at the API boundary.

**`SplitMapper.java`**
Updated call to `settlementMapper.toDTO()` to pass `split.getParticipants()`.

**`SplitResource.java`**
Updated to use `SettlementResult` returned by `calculateSettlement()`.

### Files created

**`SettlementResult.java`** (service layer)
New record bundling `Settlement` and `List<Participant>` so the resource can pass participants to the mapper without a second repository load.

### Files modified (tests)

**`SettlementCalculatorTest.java`**
Replaced all `fromName()`/`toName()`/`participantName()` assertions with `fromId()`/`toId()`/`participantId()`.

**`SplitPersistenceMapperTest.java`**
Same replacement for the settlement round-trip test.

**`ParticipantTest.java`** (created)
New test verifying `Participant.toString()` does not contain the participant name and does contain id, nights, and persons.

## Tests

- 250 unit tests pass with no failures.
- `ParticipantTest` (2 tests) — directly asserts the fix: `toString()` must not contain the name.
- `SettlementCalculatorTest` (10 tests) — reimbursement direction verified by participant ID instead of name.
- `SplitPersistenceMapperTest` — settlement round-trip verified by participant ID.
- Verified in test logs that `addParticipant` now logs `result=Participant{id=..., nights=2.0, persons=1.0}` with no name.