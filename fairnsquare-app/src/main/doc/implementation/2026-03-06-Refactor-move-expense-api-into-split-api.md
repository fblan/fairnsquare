# Refactor: Move expense API into split API

## What, Why and Constraints

**What:** Moved the `expense/api/` package (DTOs and mapper) into `split/api/expense/` — consolidating all API-layer code under the `split` domain.

**Why:** The `expense` top-level package was a stray artifact. Expenses only exist within splits; their API representation belongs under `split/api/`, consistent with how persistence DTOs already live under `split/persistence/`.

**Constraints:**
- No behavior changes — pure rename/move
- All existing tests must continue to pass

## How

### Files created
- `split/api/expense/dto/ExpenseDTO.java` — moved from `expense/api/dto/`, package updated to `split.api.expense.dto`
- `split/api/expense/dto/ExpenseByNightDTO.java` — same
- `split/api/expense/dto/ExpenseByShareDTO.java` — same
- `split/api/expense/dto/ExpenseEqualDTO.java` — same
- `split/api/expense/dto/ExpenseFreeDTO.java` — same
- `split/api/expense/dto/ShareDTO.java` — same
- `split/api/expense/mapper/ExpenseMapper.java` — moved from `expense/api/mapper/`, package and imports updated

### Files modified (import updates only)
- `split/api/dto/SplitResponseDTO.java` — updated `ExpenseDTO` import
- `split/api/SplitResource.java` — updated `ExpenseMapper` import
- `split/api/mapper/SplitMapper.java` — updated `ExpenseMapper` import

### Test files
- `test/.../split/api/expense/mapper/ExpenseMapperTest.java` — moved from `test/.../expense/api/mapper/`, package and imports updated

### Files deleted
- `expense/api/dto/` (6 files) — replaced by new location
- `expense/api/mapper/ExpenseMapper.java` — replaced by new location
- `test/.../expense/api/mapper/ExpenseMapperTest.java` — replaced by new location

## Tests

All existing `ExpenseMapperTest` tests pass unchanged:
- `shouldMapExpenseByNightToDTO`
- `shouldMapExpenseEqualToDTO`
- `shouldHandleNullInput`
- `shouldReturnEmptySharesWhenSplitIsNull`

Full Maven test suite passed (`mvn test`, exit code 0).