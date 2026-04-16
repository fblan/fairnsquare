# Refactor: Remove EQUAL Expense Type

## What, Why and Constraints

**What:** Removed the `EQUAL` expense type from the domain model, API layer, and persistence layer. Legacy ZIP files that stored `EQUAL` expenses are transparently converted to `FREE` (with equal `parts = 1` per participant) on deserialization, ensuring zero data loss.

**Why:** `EQUAL` was functionally redundant — it split an expense equally among all participants by computing shares at runtime from whoever was in the split at load time. `FREE` with `parts = 1` for each participant achieves the same result but with explicit, immutable shares. Keeping both types added accidental complexity: two classes to maintain, two API endpoints, duplicate test coverage, and a subtle correctness hazard (removing a participant after adding an EQUAL expense would silently change historical cost distribution). Removing EQUAL reduces the type surface while preserving all user-visible behavior.

**Constraints followed:**
- `ExpenseEqualPersistenceDTO` was intentionally **kept** so Jackson can still deserialize `"type": "EQUAL"` from old ZIP archives.
- `SplitPersistenceMapper` intercepts `ExpenseEqualPersistenceDTO` before delegating to `ExpensePersistenceMapper`, converting it to `ExpenseFree` with equal parts (1 per participant in the split).
- `ExpensePersistenceMapper` now throws `UnsupportedOperationException` if it receives an `ExpenseEqualPersistenceDTO` directly, enforcing that conversion must happen upstream.
- No migration of stored files is required.

---

## How

### Step 1 — Domain layer: remove `ExpenseEqual`
- **Deleted** `ExpenseEqual.java`: the domain class for EQUAL expenses.
- **Modified** `Expense.java`: removed `ExpenseEqual` from the `permits` clause; removed the `EQUAL` case from `create()` and `fromJson()` factory methods.
- **Modified** `SplitMode.java`: removed the `EQUAL("EQUAL")` enum value.

### Step 2 — Persistence layer: backward compat conversion
- **Kept** `ExpenseEqualPersistenceDTO.java` (Jackson needs it to deserialize legacy archives).
- **Modified** `SplitPersistenceMapper.java`: added a `case ExpenseEqualPersistenceDTO e ->` branch in `toExpenseDomain()` that converts the DTO to an `ExpenseFree` by assigning `parts = 1` to every participant currently in the split. This replicates the original EQUAL semantics exactly.
- **Modified** `ExpensePersistenceMapper.java`: removed the `ExpenseEqual` import and its mapping case; added a guard that throws `UnsupportedOperationException` if an `ExpenseEqualPersistenceDTO` arrives without having been pre-converted.

### Step 3 — API layer: remove `ExpenseEqualDTO` and endpoint
- **Deleted** `ExpenseEqualDTO.java`: the API response DTO.
- **Modified** `ExpenseDTO.java`: removed `EQUAL` from `@JsonSubTypes` and from the `permits` clause.
- **Modified** `ExpenseMapper.java`: removed the `ExpenseEqual → ExpenseEqualDTO` mapping case.
- **Modified** `SplitResource.java`: removed the `POST /{splitId}/expenses/equal` endpoint.
- **Modified** `SplitUseCases.java`: removed the `addExpenseEqual()` method and its `EQUAL` case from the deprecated `addExpense()` dispatcher.

### Step 4 — Dev data seeder
- **Modified** `DevDataSeeder.java`: replaced all 7 `ExpenseEqual.create()` calls with `ExpenseFree.create()` calls passing equal parts (1 each) for all 10 participants. Participant ID variables were extracted at the top of the activities section to avoid duplicate declarations.

### Step 5 — Test cleanup
All tests referencing `ExpenseEqual` or `SplitMode.EQUAL` were updated:

| File | Change |
|---|---|
| `ExpenseTest.java` | Removed `create_withEqualMode_returnsExpenseEqual` and `fromJson_withEqualMode_createsExpenseEqual` |
| `ExpensePersistenceMapperTest.java` | Removed EQUAL mapping tests; removed unused imports |
| `ExpenseMapperTest.java` | Removed `shouldMapExpenseEqualToDTO` |
| `SplitMapperTest.java` | Replaced `ExpenseEqual.create()` with `ExpenseByNight.create()` |
| `SplitParticipantBalanceTest.java` | All EQUAL expenses → `ExpenseFree` with equal parts; `updateExpense` test uses `BY_NIGHT` (FREE cannot be updated via `split.updateExpense()`) |
| `SplitSettlementTest.java` | All EQUAL expenses → `ExpenseFree` with equal parts; `SplitMode.EQUAL` → `BY_NIGHT` for the update test |
| `SettlementCalculatorTest.java` | All EQUAL expenses → `ExpenseFree` via a new private helper `equalFreeExpense()` that creates `parts = 1` for each participant passed in |
| `ExpenseUseCaseTest.java` | Removed three EQUAL endpoint tests; all `"EQUAL"` mode references → `"BY_NIGHT"`; `/expenses/equal` → `/expenses/by-night`; updated expected share amounts for BY_NIGHT proportional distribution |
| `SplitPersistenceMapperTest.java` | Replaced EQUAL assertions with FREE assertions; added `shouldConvertLegacyEqualPersistenceDTOToFreeOnLoad()` test that directly constructs an `ExpenseEqualPersistenceDTO` and verifies backward compat conversion |
| `PersistenceRoundTripTest.java` | `shouldPersistAndLoadSplitWithEqualExpense` → `shouldPersistAndLoadSplitWithFreeExpense` using `ExpenseFree.create()` |

---

## Tests

### Automated tests added
- **`SplitPersistenceMapperTest#shouldConvertLegacyEqualPersistenceDTOToFreeOnLoad`**: directly constructs a `SplitPersistenceDTO` containing an `ExpenseEqualPersistenceDTO` (simulating a legacy archive) and asserts that the loaded domain object is an `ExpenseFree` with equal parts for all participants.

### Automated tests modified
All tests listed in Step 5 above were updated to use `ExpenseFree` (or `ExpenseByNight` where `updateExpense` requires a mode that supports `fromJson`) instead of the removed `ExpenseEqual`. No test logic was removed — the same coverage scenarios are exercised, just with the replacement type.

### Key correctness note
`split.updateExpense(id, amount, desc, payer, SplitMode)` calls `Expense.fromJson()` internally, which cannot create a `FREE` expense (FREE requires explicit shares that are not part of the update request signature). Tests that exercise `updateExpense` were therefore changed to `BY_NIGHT` with equal nights (1 per participant) so the expected share amounts remain equal.
