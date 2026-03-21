# Feature: Participant Computed Balances

**Date:** 2026-03-21

## What, Why and Constraints

### What
Added three computed fields to the `Participant` domain record: `totalPaid`, `totalCost`, and `balance` (= `totalPaid - totalCost`). These fields are recalculated by the `Split` aggregate root whenever participants or expenses change, and exposed in the REST API via `ParticipantDTO`.

### Why
Previously, per-participant financial data was only available after explicitly calling the settlement endpoint. Embedding these computed values directly on `Participant` allows consumers to see each participant's financial standing (how much they paid, how much they owe, and their net balance) on every split response, without requiring a separate settlement resolution step.

### Constraints
- The computed fields are **never persisted** — `ParticipantPersistenceDTO` is unchanged. They are recalculated on every mutation and naturally restored on deserialization, since `SplitPersistenceMapper.toDomain()` calls `addParticipant()` then `addExpense()` for each item, triggering recalculation after the last expense is loaded.
- `Participant` remains an immutable record. Recalculation replaces entries in the `Split.participants` list with new instances.
- The balance calculation in `Split.recalculateBalances()` is independent of `SettlementCalculator` (no reverse dependency introduced).

## How

### Files Modified

**`domain/participant/Participant.java`**
- Added `totalPaid: BigDecimal`, `totalCost: BigDecimal`, `balance: BigDecimal` as record components.
- Updated `create(name, nights)` and `create(name, nights, share)` factory methods to initialize these fields to `BigDecimal.ZERO`.
- Updated `toString()` to include `balance`.

**`domain/Split.java`**
- Added `recalculateBalances()` private method: iterates over expenses to accumulate `paid` and `cost` maps per participant, then replaces each participant in the list with a new instance carrying the computed values.
- Called `recalculateBalances()` in all six mutation methods: `addParticipant`, `addExpense`, `updateParticipant`, `removeParticipant`, `updateExpense`, `removeExpense`.
- Updated internal `new Participant(...)` instantiations (in `updateParticipant` and `removeParticipant`) to pass `BigDecimal.ZERO` for the three new fields (they are immediately overwritten by `recalculateBalances()`).
- Added imports: `java.math.BigDecimal`, `java.util.HashMap`, `java.util.Map`.

**`api/dto/ParticipantDTO.java`**
- Added `totalPaid`, `totalCost`, `balance` fields with `@JsonProperty` annotations.

**`api/mapper/ParticipantMapper.java`**
- Updated `toDTO()` to include `participant.totalPaid()`, `participant.totalCost()`, `participant.balance()`.

**`persistence/mapper/ParticipantPersistenceMapper.java`**
- Added `import java.math.BigDecimal`.
- Updated `toDomain()` to pass `BigDecimal.ZERO` for the three balance fields (they will be recalculated by `Split` after all expenses are loaded).

### Files Created

**`test/.../SplitParticipantBalanceTest.java`**
- New domain test class with 6 tests covering balance recalculation scenarios.

## Tests

### New tests added

**`SplitParticipantBalanceTest`** (6 tests):
- `newParticipant_hasZeroBalances` — Freshly added participant has all balances at ZERO.
- `addExpense_updatesPayerTotalPaidAndAllParticipantsCost` — Verifies correct totalPaid/totalCost/balance after adding an equal-split expense.
- `removeExpense_recalculatesBalances` — Removing the only expense resets balances to ZERO.
- `updateExpense_recalculatesBalances` — Updating expense amount correctly updates balances.
- `multipleExpenses_accumulateBalancesCorrectly` — Two expenses with different payers accumulate correctly.
- `removeParticipant_recalculatesRemainingParticipantBalances` — Removing a participant triggers recalculation for remaining participants.

### Updated tests

**`ParticipantTest`** (2 tests added):
- `create_initializesBalanceFieldsToZero` — Factory methods initialize balance fields to ZERO.
- `toString_containsBalance` — toString includes the balance field.

**`ParticipantMapperTest`** (assertions added):
- `shouldMapParticipantToDTO` — Now also asserts that `totalPaid`, `totalCost`, `balance` are ZERO in the DTO for a freshly created participant.

**`ParticipantPersistenceMapperTest`** (1 test added):
- `balanceFieldsAreNotPersisted_alwaysDeserializedAsZero` — Confirms that balance fields are always ZERO after deserialization from persistence.

### Full test run
All 290 tests pass (`mvn test -pl fairnsquare-app`).