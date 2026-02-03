# Story TD-001.5: Split SplitResourceTest by Use Case

status: ready-for-dev  
epic_id: TD-001  
story_id: TD-001.5  
story_key: td-001-5-split-splitservicetest-by-use-case  
created: 2026-01-28  
priority: P2  
dependencies: TD-001-4 (ready-for-dev)  

---

## Story

**As a** developer maintaining the FairNSquare test suite,  
**I want** the monolithic `SplitResourceTest` split into separate use-case-focused test classes,  
**So that** tests are easier to navigate, maintain, and understand by behavioral context.

---

## Acceptance Criteria

### AC1: Create Split Use Case Tests
- [ ] Create `CreateSplitUseCaseTest` class
- [ ] Move all `createSplit_*` test methods to new class
- [ ] Move all `getSplit_*` test methods to new class (query part of create use case)
- [ ] All moved tests passing unchanged

### AC2: Create Participant Management Use Case Tests
- [ ] Create `ParticipantUseCaseTest` class
- [ ] Move all `addParticipant_*` test methods
- [ ] Move all `updateParticipant_*` test methods
- [ ] Move all `deleteParticipant_*` test methods
- [ ] All moved tests passing unchanged

### AC3: Create Expense Use Case Tests
- [ ] Create `ExpenseUseCaseTest` class
- [ ] Move all `addExpense_*` test methods (legacy endpoint)
- [ ] Move all `addExpenseByNight_*` test methods
- [ ] Move all `addExpenseEqual_*` test methods
- [ ] Move all expense JSON serialization tests
- [ ] All moved tests passing unchanged

### AC4: Create Configuration/Infrastructure Tests
- [ ] Create `SplitInfrastructureTest` class
- [ ] Move `configuredDataPath_isUsedByPathResolver` test
- [ ] Move any other infrastructure/configuration tests
- [ ] All moved tests passing unchanged

### AC5: Delete Original Monolithic Test
- [ ] Verify all 55 test methods migrated to new classes
- [ ] Delete `SplitResourceTest.java`
- [ ] Run `mvn clean test` - all 114 tests pass
- [ ] No test count regression

### AC6: Maintain Test Coverage
- [ ] Run `mvn verify` - JaCoCo passes (>57% threshold)
- [ ] Coverage maintained or improved
- [ ] No coverage regressions

---

## Tasks / Subtasks

### Task 1: Analyze Current Test Structure (Pre-work)
- [ ] 1.1 List all 55 test methods in `SplitResourceTest`
- [ ] 1.2 Categorize by use case (Create, Participant, Expense, Infrastructure)
- [ ] 1.3 Identify shared setup/teardown needs
- [ ] 1.4 Document test count per category

### Task 2: Create CreateSplitUseCaseTest (AC1)
- [ ] 2.1 Create new test class with `@QuarkusTest` annotation
- [ ] 2.2 Copy shared setup (`@BeforeEach setUp()`)
- [ ] 2.3 Move 8 create/get split tests:
  - `createSplit_withValidName_returns201WithSplitDetails`
  - `createSplit_persistsFileWithAllFields`
  - `createSplit_withEmptyName_returns400ProblemDetails`
  - `createSplit_withMissingName_returns400ProblemDetails`
  - `createSplit_createsDirectoryAutomatically`
  - `createSplit_generatesValid21CharNanoId`
  - `getSplit_nonExistent_returns404WithProblemDetails`
  - `getSplit_afterCreate_returnsTheSplit`
  - `getSplit_withPathTraversalChars_returns400`
  - `getSplit_withInvalidSplitIdFormat_returns400`
- [ ] 2.4 Run tests: `mvn test -Dtest=CreateSplitUseCaseTest`
- [ ] 2.5 Verify all pass

### Task 3: Create ParticipantUseCaseTest (AC2)
- [ ] 3.1 Create new test class with `@QuarkusTest`
- [ ] 3.2 Copy shared setup
- [ ] 3.3 Move 23 participant tests:
  - All `addParticipant_*` (10 tests)
  - All `updateParticipant_*` (9 tests)
  - All `deleteParticipant_*` (8 tests)
- [ ] 3.4 Run tests: `mvn test -Dtest=ParticipantUseCaseTest`
- [ ] 3.5 Verify all pass

### Task 4: Create ExpenseUseCaseTest (AC3)
- [ ] 4.1 Create new test class with `@QuarkusTest`
- [ ] 4.2 Copy shared setup
- [ ] 4.3 Move 23 expense tests:
  - All `addExpense_*` legacy endpoint tests (10 tests)
  - All `addExpenseByNight_*` (3 tests)
  - All `addExpenseEqual_*` (3 tests)
  - All expense JSON serialization tests (4 tests)
- [ ] 4.4 Run tests: `mvn test -Dtest=ExpenseUseCaseTest`
- [ ] 4.5 Verify all pass

### Task 5: Create SplitInfrastructureTest (AC4)
- [ ] 5.1 Create new test class with `@QuarkusTest`
- [ ] 5.2 Move infrastructure test:
  - `configuredDataPath_isUsedByPathResolver`
- [ ] 5.3 Run tests: `mvn test -Dtest=SplitInfrastructureTest`
- [ ] 5.4 Verify all pass

### Task 6: Delete Original and Verify (AC5, AC6)
- [ ] 6.1 Count total tests in new classes (should be 55)
- [ ] 6.2 Delete `SplitResourceTest.java`
- [ ] 6.3 Run `mvn clean test` - verify 114 tests pass
- [ ] 6.4 Run `mvn verify` - verify JaCoCo passes (>57%)
- [ ] 6.5 Verify no test count regression

---

## Dev Notes

### 🎯 Story Objective
Split the 1212-line, 55-test `SplitResourceTest` into **4 focused test classes** organized by behavioral use case. This improves maintainability, navigation, and clarity without changing any test logic.

### 📊 Current Test Distribution

**SplitResourceTest.java (55 tests, 1212 lines):**

| Use Case | Test Count | Test Methods |
|----------|-----------|--------------|
| **Create Split** | 10 | createSplit_*, getSplit_* |
| **Participant Management** | 23 | addParticipant_*, updateParticipant_*, deleteParticipant_* |
| **Expense Management** | 20 | addExpense_*, addExpenseByNight_*, addExpenseEqual_* |
| **Expense Serialization** | 4 | expenseByNight_jsonRoundtrip, expenseEqual_jsonRoundtrip, etc. |
| **Infrastructure** | 1 | configuredDataPath_isUsedByPathResolver |
| **TOTAL** | **55** | |

**Target Structure:**
```
├── CreateSplitUseCaseTest.java (10 tests) - split creation and retrieval
├── ParticipantUseCaseTest.java (23 tests) - add/update/delete participants
├── ExpenseUseCaseTest.java (24 tests) - expense management + serialization
└── SplitInfrastructureTest.java (1 test) - configuration validation
```

### 🏗️ Architecture Context

**Current State:**
- ✅ All tests are in `SplitResourceTest` (integration tests via `@QuarkusTest`)
- ✅ Tests use `@TestHTTPResource` for REST endpoints
- ✅ Tests verify persistence through public API (good pattern)
- ✅ 114 total tests in project (55 in this file)

**Why Split by Use Case?**
- **Cognitive Load:** 1212 lines too large to scan
- **Navigation:** Hard to find specific test scenarios
- **Parallel Execution:** Smaller test classes may run faster
- **Domain Clarity:** Use case names clarify behavioral intent

### 📂 Files to Create

**New Test Files:**
1. `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/api/CreateSplitUseCaseTest.java` (10 tests)
2. `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/api/ParticipantUseCaseTest.java` (23 tests)
3. `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/api/ExpenseUseCaseTest.java` (24 tests)
4. `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/api/SplitInfrastructureTest.java` (1 test)

**File to Delete:**
5. `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/api/SplitResourceTest.java` (after migration)

### 🔍 Shared Test Setup Pattern

All new test classes will share this structure:

```java
@QuarkusTest
class CreateSplitUseCaseTest {

    @TestHTTPResource
    URI baseUrl;

    @ConfigProperty(name = "application.data.path")
    String dataPath;

    @BeforeEach
    void setUp() throws IOException {
        Path dataDir = Path.of(dataPath);
        if (Files.exists(dataDir)) {
            try (Stream<Path> paths = Files.walk(dataDir)) {
                paths.sorted(Comparator.reverseOrder())
                     .map(Path::toFile)
                     .forEach(File::delete);
            }
        }
        Files.createDirectories(dataDir);
    }

    @Test
    void createSplit_withValidName_returns201WithSplitDetails() {
        // Test logic unchanged
    }
}
```

### ⚠️ Common Pitfalls to Avoid

**❌ Pitfall 1: Forgetting `@QuarkusTest` Annotation**
```java
// WRONG - Test won't run as integration test
class CreateSplitUseCaseTest {
    ...
}

// CORRECT
@QuarkusTest
class CreateSplitUseCaseTest {
    ...
}
```

**❌ Pitfall 2: Not Copying Setup Method**
- Each new test class needs its own `@BeforeEach setUp()` method
- Setup clears `data/` directory before each test
- Without it, tests will fail with stale data

**❌ Pitfall 3: Incomplete Test Migration**
```bash
# VERIFY all tests moved:
grep -c "@Test" SplitResourceTest.java  # Should be 0 before deletion
# Count in new files should total 55
```

**❌ Pitfall 4: Breaking Test Names**
- Test method names MUST remain unchanged
- Test logic MUST remain unchanged
- Only the class changes

**❌ Pitfall 5: Test Count Regression**
```bash
# Before split:
mvn test  # 114 tests

# After split:
mvn test  # Must still be 114 tests
```

### 🧪 Testing Strategy

**No New Tests Required:**
- This is pure reorganization - test logic unchanged
- Test count must remain 114
- Coverage must remain >57%

**Incremental Verification:**
1. Create new test class
2. Run ONLY that class: `mvn test -Dtest=CreateSplitUseCaseTest`
3. Verify all tests pass
4. Repeat for each new class
5. Delete original `SplitResourceTest`
6. Run full suite: `mvn clean test`

**Coverage Verification:**
```bash
mvn clean verify
# Check JaCoCo report: target/site/jacoco/index.html
# Line coverage must be >57%
```

### 🔗 Dependencies & Related Stories

**Depends On:**
- ✅ TD-001-4: Rename SplitService to SplitUseCases (ready-for-dev)
  - Reason: Do together - rename service class and split its tests in same PR

**Enables:**
- TD-001-6: Enforce Test Persistence Pattern (next story)
  - Reason: Easier to refactor test patterns when tests are split by use case

**Related:**
- ✅ TD-001-3: Refactor Expense to Sealed Abstract Class (done)
  - Added expense serialization tests that are now being moved

### 📚 Reference Documentation

**Source: Epic TD-001 (epic-TD-001-technical-debt-refactoring.md)**
- Story TD-001-5 definition
- Implementation Phase 3: Service Layer Refactoring

**Source: Project Context (project-context.md)**
- Testing strategy: Integration tests are primary
- `@QuarkusTest` pattern
- Coverage target >90% (but current threshold is 57%)

**Source: Previous Stories**
- TD-001-1: Migrated to AssertJ - all tests use AssertJ syntax
- TD-001-2: JaCoCo coverage verification at 57% threshold
- TD-001-3: Added expense serialization tests (4 new tests)

**Source: Current Codebase**
- `SplitResourceTest.java` - 1212 lines, 55 tests
- All tests are `@QuarkusTest` integration tests
- All tests use `@TestHTTPResource URI baseUrl`

### 📝 Test Method Categorization

**CreateSplitUseCaseTest (10 tests):**
- `createSplit_withValidName_returns201WithSplitDetails`
- `createSplit_persistsFileWithAllFields`
- `createSplit_withEmptyName_returns400ProblemDetails`
- `createSplit_withMissingName_returns400ProblemDetails`
- `createSplit_createsDirectoryAutomatically`
- `createSplit_generatesValid21CharNanoId`
- `getSplit_nonExistent_returns404WithProblemDetails`
- `getSplit_afterCreate_returnsTheSplit`
- `getSplit_withPathTraversalChars_returns400`
- `getSplit_withInvalidSplitIdFormat_returns400`

**ParticipantUseCaseTest (23 tests):**

*Add Participant (10):*
- `addParticipant_withValidData_returns201WithParticipantDetails`
- `addParticipant_persistsInJsonFile`
- `addParticipant_multipleParticipants_allPersisted`
- `addParticipant_withEmptyName_returns400`
- `addParticipant_withNightsLessThan1_returns400`
- `addParticipant_toNonExistentSplit_returns404`
- `addParticipant_withInvalidSplitId_returns400`
- `addParticipant_withNightsGreaterThan365_returns400`
- `addParticipant_withNameExceeding50Chars_returns400`

*Update Participant (9):*
- `updateParticipant_withValidData_returns200WithUpdatedParticipant`
- `updateParticipant_persistsChangesInJsonFile`
- `updateParticipant_nonExistentParticipant_returns404`
- `updateParticipant_toNonExistentSplit_returns404`
- `updateParticipant_withEmptyName_returns400`
- `updateParticipant_withNightsLessThan1_returns400`
- `updateParticipant_withInvalidSplitId_returns400`
- `updateParticipant_withInvalidParticipantId_returns400`
- `updateParticipant_withNightsGreaterThan365_returns400`

*Delete Participant (8):*
- `deleteParticipant_withNoExpenses_returns204`
- `deleteParticipant_removesFromJsonFile`
- `deleteParticipant_withExpenses_returns409`
- `deleteParticipant_withExpenses_returnsProblemDetailsFormat`
- `deleteParticipant_nonExistent_returns404`
- `deleteParticipant_toNonExistentSplit_returns404`
- `deleteParticipant_withInvalidSplitId_returns400`
- `deleteParticipant_withInvalidParticipantId_returns400`

**ExpenseUseCaseTest (24 tests):**

*Legacy Expense Endpoint (10):*
- `addExpense_withValidData_returns201AndExpense`
- `addExpense_persistsToSplitFile`
- `addExpense_byNightMode_calculatesSharesProportionally`
- `addExpense_equalMode_calculatesEqualShares`
- `addExpense_withEmptyDescription_returns400`
- `addExpense_withAmountBelowMinimum_returns400`
- `addExpense_withMissingAmount_returns400`
- `addExpense_withInvalidPayerId_returns400`
- `addExpense_toNonExistentSplit_returns404`
- `addExpense_withInvalidSplitIdFormat_returns400`

*Type-Specific Endpoints (6):*
- `addExpenseByNight_createsExpenseWithByNightShares`
- `addExpenseByNight_toNonExistentSplit_returns404`
- `addExpenseEqual_createsExpenseWithEqualShares`
- `addExpenseEqual_toNonExistentSplit_returns404`

*JSON Serialization (4):*
- `expenseByNight_jsonRoundtrip`
- `expenseEqual_jsonRoundtrip`
- `expense_deserializesMinimalJsonForBackwardCompatibility`
- `expense_legacyJsonWithSplitMode_deserializesToDefaultImpl`

**SplitInfrastructureTest (1 test):**
- `configuredDataPath_isUsedByPathResolver`

---

## 💡 Implementation Notes

### IDE Workflow (IntelliJ IDEA)

**Step-by-Step:**
1. Create `CreateSplitUseCaseTest.java` in same package
2. Copy class header: `@QuarkusTest` annotation
3. Copy field declarations: `@TestHTTPResource URI baseUrl`, `@ConfigProperty String dataPath`
4. Copy `@BeforeEach setUp()` method
5. In `SplitResourceTest`, select first test method group (e.g., all `createSplit_*`)
6. Use `Refactor → Move Members` or cut/paste to new class
7. Run `mvn test -Dtest=CreateSplitUseCaseTest` to verify
8. Repeat for each use case test class

### Manual Copy-Paste Workflow

**For each new test class:**
1. Create file with class skeleton
2. Copy `@QuarkusTest` annotation
3. Copy field injections (`@TestHTTPResource`, `@ConfigProperty`)
4. Copy `@BeforeEach setUp()` method
5. Copy relevant test methods from `SplitResourceTest`
6. Add necessary imports (IDE auto-import)
7. Run tests: `mvn test -Dtest={NewClassName}`
8. Fix any missing imports or dependencies

### Verification Checklist

**After Each New Test Class:**
- [ ] Class has `@QuarkusTest` annotation
- [ ] Class has `@TestHTTPResource URI baseUrl` field
- [ ] Class has `@ConfigProperty(name = "application.data.path") String dataPath` field
- [ ] Class has `@BeforeEach void setUp()` method
- [ ] All test methods have `@Test` annotation
- [ ] No compilation errors
- [ ] Run specific test class: `mvn test -Dtest={ClassName}`
- [ ] All tests in class pass

**Before Deleting SplitResourceTest:**
- [ ] Count `@Test` annotations in new classes = 55
- [ ] Count lines in new classes ≈ 1212 (plus some duplication for setup)
- [ ] Run `mvn clean test` - all 114 tests pass
- [ ] No test failures

**After Deleting SplitResourceTest:**
- [ ] `mvn clean test` - still 114 tests pass
- [ ] `mvn verify` - JaCoCo coverage >57% passes
- [ ] No test count regression

---

## 🔗 Related Stories

**Depends On:**
- ✅ TD-001-1: Migrate to AssertJ Assertions (done) - Foundation
- ✅ TD-001-2: Add JaCoCo Coverage Verification (done) - Foundation
- ✅ TD-001-3: Refactor Expense to Sealed Abstract Class (done) - Added tests to split
- TD-001-4: Rename SplitService to SplitUseCases (ready-for-dev) - Do together

**Enables:**
- TD-001-6: Enforce Test Persistence Pattern (next)
- TD-001-7: Synchronize Documentation (final)

---

**Story Status:** Ready for Dev 🚀

**Created:** 2026-01-28  
**Context Engine:** Full artifact analysis completed  
**Developer Readiness:** Comprehensive test reorganization guide provided

---

## 📝 Dev Agent Record

### Implementation Plan

**Phase 1: Create CreateSplitUseCaseTest (Task 2)**
- Create new test class
- Copy shared setup
- Move 10 create/get split tests
- Verify tests pass

**Phase 2: Create ParticipantUseCaseTest (Task 3)**
- Create new test class
- Copy shared setup
- Move 23 participant management tests (add/update/delete)
- Verify tests pass

**Phase 3: Create ExpenseUseCaseTest (Task 4)**
- Create new test class
- Copy shared setup
- Move 24 expense tests (legacy + type-specific + serialization)
- Verify tests pass

**Phase 4: Create SplitInfrastructureTest (Task 5)**
- Create new test class
- Copy shared setup
- Move 1 infrastructure test
- Verify tests pass

**Phase 5: Delete Original and Verify (Task 6)**
- Verify 55 tests migrated
- Delete `SplitResourceTest.java`
- Run full suite: `mvn clean verify`
- Verify 114 tests pass, coverage >57%

### Completion Notes

_To be filled by Dev Agent after implementation_

### File List

**Created Files:**
- `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/api/CreateSplitUseCaseTest.java` (10 tests, ~300 lines)
- `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/api/ParticipantUseCaseTest.java` (23 tests, ~700 lines)
- `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/api/ExpenseUseCaseTest.java` (24 tests, ~750 lines)
- `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/api/SplitInfrastructureTest.java` (1 test, ~50 lines)

**Deleted Files:**
- `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/api/SplitResourceTest.java` (1212 lines)

**Modified Files:**
- None (pure reorganization)

### Change Log

| Date | Change | Files |
|------|--------|-------|
| 2026-01-28 | Created CreateSplitUseCaseTest with 10 tests | CreateSplitUseCaseTest.java |
| 2026-01-28 | Created ParticipantUseCaseTest with 23 tests | ParticipantUseCaseTest.java |
| 2026-01-28 | Created ExpenseUseCaseTest with 24 tests | ExpenseUseCaseTest.java |
| 2026-01-28 | Created SplitInfrastructureTest with 1 test | SplitInfrastructureTest.java |
| 2026-01-28 | Deleted monolithic test class | SplitResourceTest.java |
