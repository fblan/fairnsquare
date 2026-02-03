# Story TD-001.4: Rename SplitService to SplitUseCases

status: ready-for-dev  
epic_id: TD-001  
story_id: TD-001.4  
story_key: td-001-4-rename-splitservice-to-splitusecases  
created: 2026-01-28  
priority: P2  
dependencies: TD-001-3 (done)  

---

## Story

**As a** developer maintaining the FairNSquare codebase,  
**I want** `SplitService` renamed to `SplitUseCases`,  
**So that** the application layer naming follows Domain-Driven Design conventions and improves code clarity.

---

## Acceptance Criteria

### AC1: Rename Core Class
- [ ] Rename class `SplitService` → `SplitUseCases`
- [ ] File renamed: `SplitService.java` → `SplitUseCases.java`
- [ ] Package remains `org.asymetrik.web.fairnsquare.split.service`
- [ ] All method signatures unchanged

### AC2: Update All Injection Points
- [ ] Update `SplitResource` to inject `SplitUseCases` instead of `SplitService`
- [ ] Update any other classes that inject `SplitService`
- [ ] Verify `@Inject` annotations work correctly with new name
- [ ] No compilation errors remain

### AC3: Update Test Class Names
- [ ] Rename `SplitServiceTest` → `SplitUseCasesTest`
- [ ] Update all `@InjectMock SplitService` → `@InjectMock SplitUseCases`
- [ ] Update all variable names `splitService` → `splitUseCases` in tests
- [ ] All test methods unchanged

### AC4: Verify All Tests Pass
- [ ] Run `mvn clean verify`
- [ ] All 114 tests passing (same count as TD-001.3)
- [ ] JaCoCo coverage maintained (>57% threshold)
- [ ] No failing tests, no compilation errors

### AC5: No API Contract Changes
- [ ] REST endpoints unchanged (`/api/splits/**`)
- [ ] Request/response formats unchanged
- [ ] HTTP status codes unchanged
- [ ] External consumers unaffected (internal rename only)

---

## Tasks / Subtasks

### Task 1: Perform Class Rename (AC1)
- [ ] 1.1 Use IDE refactor tool: `Refactor → Rename` on `SplitService` class
- [ ] 1.2 Verify file renamed: `SplitService.java` → `SplitUseCases.java`
- [ ] 1.3 Run `mvn clean compile` to check for immediate errors
- [ ] 1.4 Verify all imports updated automatically by IDE

### Task 2: Update Injection Points (AC2)
- [ ] 2.1 Update `SplitResource.java` injection
- [ ] 2.2 Search codebase for any other `@Inject SplitService` references
- [ ] 2.3 Update variable names from `splitService` to `splitUseCases`
- [ ] 2.4 Run `mvn clean compile` to verify no compile errors

### Task 3: Rename Test Classes (AC3)
- [ ] 3.1 Rename `SplitServiceTest.java` → `SplitUseCasesTest.java`
- [ ] 3.2 Update `@InjectMock SplitService` → `@InjectMock SplitUseCases`
- [ ] 3.3 Update all variable names in test methods
- [ ] 3.4 Verify test class name matches production class convention

### Task 4: Verify and Test (AC4, AC5)
- [ ] 4.1 Run `mvn clean test` - verify all 114 tests pass
- [ ] 4.2 Run `mvn verify` - verify JaCoCo coverage passes (>57%)
- [ ] 4.3 Manually test a REST endpoint (create split, add expense)
- [ ] 4.4 Verify API contracts unchanged

---

## Dev Notes

### 🎯 Story Objective
This is a **pure refactoring story** - zero functional changes, only rename for DDD alignment. The class currently named `SplitService` is actually an **Application Service** (Use Cases layer), not a Domain Service. The rename clarifies architectural intent.

### 🏗️ Architecture Context

**Domain-Driven Design Layers:**
```
├── Domain Layer (Expense, Split, Participant)
├── Application Layer (SplitUseCases ← YOU ARE HERE)
└── Infrastructure Layer (JsonFileRepository)
```

**Current State (TD-001.3):**
- ✅ `SplitService` exists with 11 public methods
- ✅ Injects `JsonFileRepository` only (SplitCalculator removed in TD-001.3)
- ✅ Located in `org.asymetrik.web.fairnsquare.split.service` package
- ✅ Injected by `SplitResource` REST controller

**Target State (TD-001.4):**
- Identical behavior, different name
- Better alignment with DDD terminology

### 📂 Files to Modify

**Production Code:**
1. `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/service/SplitService.java`  
   → Rename to `SplitUseCases.java`

2. `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/api/SplitResource.java`  
   → Update injection: `@Inject SplitUseCases splitUseCases;`

**Test Code:**
3. `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/service/SplitServiceTest.java`  
   → Rename to `SplitUseCasesTest.java`

4. `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/api/SplitResourceTest.java`  
   → Update `@InjectMock SplitService splitService;` → `SplitUseCases splitUseCases;`

### 🔍 Search Patterns to Find All References

```bash
# Find all references to SplitService
grep -r "SplitService" fairnsquare-app/src/ --include="*.java"

# Find all variable names
grep -r "splitService" fairnsquare-app/src/ --include="*.java"
```

Expected matches:
- `SplitService.java` (class definition)
- `SplitServiceTest.java` (test class)
- `SplitResource.java` (injection point)
- `SplitResourceTest.java` (mock injection)

### ⚠️ Common Pitfalls to Avoid

**❌ Pitfall 1: Manual Find-Replace Instead of IDE Refactor**
- **Problem:** Miss references, break imports, typos
- **Solution:** Use IDE's `Refactor → Rename` (IntelliJ: Shift+F6, VS Code: F2)
- **Verify:** IDE shows preview of all changes before applying

**❌ Pitfall 2: Forgetting Variable Names**
```java
// WRONG - inconsistent naming
@Inject
SplitUseCases splitService; // ← old variable name

// CORRECT
@Inject
SplitUseCases splitUseCases; // ← matches class name
```

**❌ Pitfall 3: Not Running Clean Build**
- **Problem:** Stale `.class` files cause mysterious errors
- **Solution:** Always run `mvn clean` before `mvn compile`

**❌ Pitfall 4: Breaking Javadoc References**
```java
/**
 * Uses {@link SplitService} to fetch split data. ← WRONG
 */
// Should be:
/**
 * Uses {@link SplitUseCases} to fetch split data.
 */
```

### 🧪 Testing Strategy

**No New Tests Required:**
- This is a pure rename - all existing tests must pass unchanged
- Test count must remain 114 (from TD-001.3)
- Coverage must remain >57%

**Manual Verification:**
1. Start app: `mvn quarkus:dev`
2. Create a split: `POST /api/splits`
3. Add expense: `POST /api/splits/{id}/expenses/by-night`
4. Verify: Response identical to before rename

### 🔗 Dependencies & Related Stories

**Depends On:**
- ✅ TD-001-3: Refactor Expense to Sealed Abstract Class (done)
  - Reason: Let Expense refactoring settle before service layer changes

**Enables:**
- TD-001-5: Split SplitServiceTest by Use Case (next story)
  - Reason: Easier to split tests when class name clearly indicates purpose

**Unrelated To:**
- TD-001-1, TD-001-2 (foundation stories already complete)
- TD-001-6, TD-001-7 (test patterns and docs, come later)

### 📚 Reference Documentation

**Source: Epic TD-001 (epic-TD-001-technical-debt-refactoring.md)**
- Story TD-001-4 definition
- Implementation phases (this is Phase 3)

**Source: Architecture Patterns**
- DDD layers described in `project-context.md`
- Service layer conventions

**Source: Previous Story (TD-001-3)**
- Completed 2026-01-28
- SplitCalculator injection removed from SplitService constructor
- 114 tests passing baseline established
- Coverage >57% threshold maintained

### 🎓 Domain-Driven Design Context

**Why "Use Cases" instead of "Service"?**

DDD distinguishes between:
1. **Domain Services** - Business logic that doesn't belong in entities (e.g., `ExpenseShareCalculator`)
2. **Application Services** - Use case orchestration, transaction boundaries (e.g., `SplitUseCases`)

Current `SplitService` orchestrates use cases like:
- `createSplit()`
- `addParticipant()`
- `addExpenseByNight()`
- `calculateBalances()`

These are **application-layer concerns**, not domain logic. The rename clarifies this distinction.

---

## 💡 Implementation Notes

### IDE Refactoring Workflow (IntelliJ IDEA)

**Step-by-Step:**
1. Open `SplitService.java`
2. Right-click class name → `Refactor → Rename` (or Shift+F6)
3. Type new name: `SplitUseCases`
4. Check options:
   - ✅ Search in comments and strings
   - ✅ Search for text occurrences
   - ✅ Search in non-Java files (for Javadoc)
5. Click "Preview" to see all changes
6. Verify list includes:
   - Class definition
   - Constructor
   - All injection points
   - Test class references
7. Click "Do Refactor"
8. Run `mvn clean compile` to verify

### IDE Refactoring Workflow (VS Code)

**Step-by-Step:**
1. Open `SplitService.java`
2. Place cursor on class name
3. Press F2 (Rename Symbol)
4. Type new name: `SplitUseCases`
5. Press Enter
6. VS Code updates all references automatically
7. Run `mvn clean compile` to verify

### Manual Verification Checklist

After IDE refactor:
- [ ] `SplitService.java` no longer exists
- [ ] `SplitUseCases.java` exists with identical methods
- [ ] `SplitResource` imports `SplitUseCases`
- [ ] `SplitResourceTest` mocks `SplitUseCases`
- [ ] No compilation errors: `mvn clean compile`
- [ ] All tests pass: `mvn clean test`
- [ ] Coverage passes: `mvn verify`

---

## 🔗 Related Stories

**Depends On:**
- ✅ TD-001-1: Migrate to AssertJ Assertions (done) - Foundation
- ✅ TD-001-2: Add JaCoCo Coverage Verification (done) - Foundation
- ✅ TD-001-3: Refactor Expense to Sealed Abstract Class (done) - Domain stabilization

**Enables:**
- TD-001-5: Split SplitServiceTest by Use Case (next)
- TD-001-6: Enforce Test Persistence Pattern (after TD-001-5)
- TD-001-7: Synchronize Documentation (final step)

---

**Story Status:** Ready for Dev 🚀

**Created:** 2026-01-28  
**Context Engine:** Full artifact analysis completed  
**Developer Readiness:** Comprehensive refactoring guide provided

---

## 📝 Dev Agent Record

### Implementation Plan

**Phase 1: IDE Refactor (AC1)**
- Use IDE's `Refactor → Rename` on `SplitService` class
- Verify class file renamed
- Verify imports updated automatically

**Phase 2: Update Injections (AC2)**
- Update `SplitResource` injection
- Update any other injection points
- Update variable names for consistency

**Phase 3: Update Tests (AC3)**
- Rename `SplitServiceTest` → `SplitUseCasesTest`
- Update test mocks and variable names
- Verify test structure unchanged

**Phase 4: Verification (AC4, AC5)**
- Run full test suite: `mvn clean verify`
- Verify 114 tests pass
- Verify coverage >57%
- Manual API smoke test

### Completion Notes

_To be filled by Dev Agent after implementation_

### File List

**Modified Files:**
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/service/SplitUseCases.java` (renamed from SplitService.java)
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/api/SplitResource.java` (updated injection)
- `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/service/SplitUseCasesTest.java` (renamed from SplitServiceTest.java)
- `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/api/SplitResourceTest.java` (updated mock)

**Deleted Files:**
- None (renamed only)

**Created Files:**
- None (renamed only)

### Change Log

| Date | Change | Files |
|------|--------|-------|
| 2026-01-28 | Class renamed: SplitService → SplitUseCases | SplitService.java → SplitUseCases.java |
| 2026-01-28 | Updated injection in REST resource | SplitResource.java |
| 2026-01-28 | Test class renamed | SplitServiceTest.java → SplitUseCasesTest.java |
| 2026-01-28 | Updated test mocks | SplitResourceTest.java |
