# Feature: Persist Settlement

## What, Why and Constraints

**What**: The settlement (balances and reimbursement proposals) is now persisted in the Split aggregate. When the settlement endpoint is called, the backend calculates the settlement, stores it in the Split, and saves to disk. The Split API response now includes the settlement if present. Any modification to participants or expenses automatically clears the persisted settlement.

**Why**: Users can leave the application and come back later to see their settlement results without recalculating. The dashboard also indicates whether a split has been settled.

**Constraints**:
- Rich domain model pattern preserved — `settle()` and `clearSettlement()` are behavior methods on the Split aggregate
- `validate()` remains the last call in all mutation methods
- Backward compatible with existing JSON files (settlement field is nullable, missing field deserializes as `null`)
- No new API endpoints — existing `GET /settlement` now also persists; `GET /splits/{splitId}` now includes settlement in response

## How

### Step 1: Domain model (`Split.java`)
- Added `Settlement settlement` field (nullable)
- Added `settle(Settlement)` method to store a calculated settlement
- Added `clearSettlement()` method to clear it
- Added `getSettlement()` getter
- All 6 mutation methods (`addParticipant`, `updateParticipant`, `removeParticipant`, `addExpense`, `updateExpense`, `removeExpense`) call `clearSettlement()` before `validate()`

### Step 2: Persistence DTO (`SettlementPersistenceDTO.java`, `SplitPersistenceDTO.java`)
- Created `SettlementPersistenceDTO` with nested `ParticipantBalancePersistenceDTO` and `ReimbursementPersistenceDTO`
- Added nullable `settlement` field to `SplitPersistenceDTO`

### Step 3: Persistence mapper (`SplitPersistenceMapper.java`)
- Added `settlementToPersistenceDTO()` and `settlementToDomain()` private helpers
- `toPersistenceDTO()` maps settlement (null-safe)
- `toDomain()` restores settlement via `split.settle()` after participants/expenses are loaded

### Step 4: Service layer (`SplitUseCases.java`)
- `calculateSettlement()` now: loads split, calculates, calls `split.settle()`, saves, returns settlement

### Step 5: API layer (`SplitResponseDTO.java`, `SplitMapper.java`)
- Added nullable `settlement` field (reuses `SettlementResponseDTO`) to `SplitResponseDTO`
- `SplitMapper` injects `SettlementMapper` and maps settlement to DTO

### Step 6: Frontend
- `splits.ts`: Added `settlement: Settlement | null` to `Split` interface
- `Split.svelte`: Added `isSettled` derived flag; Solve card shows "Settled" vs "Solve"; sets `sessionStorage` flag when navigating to settlement page while settled
- `Settlement.svelte`: Reads `sessionStorage` flag on init to auto-show reimbursements (resolved view) when coming from a settled split

## Tests

### Backend (13 new tests)

**`SplitSettlementTest.java`** — 9 tests:
- `settle_storesSettlement`, `getSettlement_returnsNullByDefault`, `clearSettlement_removesSettlement`
- Settlement cleared on: `addParticipant`, `updateParticipant`, `removeParticipant`, `addExpense`, `updateExpense`, `removeExpense`

**`SplitPersistenceMapperTest.java`** — 2 new tests:
- `shouldMapSettlementInRoundTrip` (with settlement data)
- `shouldMapNullSettlementInRoundTrip`

**`SplitMapperTest.java`** — 2 new tests:
- `shouldMapSettlementWhenPresent`
- `shouldMapNullSettlementWhenNotPresent`

### Frontend (4 new tests)

**`Split.test.ts`** — 3 new tests:
- Shows "Settled" when settlement is persisted
- Sets sessionStorage flag and navigates when settlement is persisted
- Shows "Solve" when settlement is null

**`Settlement.test.ts`** — 1 new test:
- Shows reimbursements directly when settlement-resolved flag is set in sessionStorage