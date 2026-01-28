---
epic_id: TD-001
epic_name: "Technical Debt: Code Quality & Maintainability Enhancement"
status: planned
created: 2026-01-28
priority: high
estimated_stories: 6
dependencies: []
---

# Epic TD-001: Technical Debt - Code Quality & Maintainability Enhancement

## Epic Overview

**Theme:** Refactoring for extensibility, test quality, and documentation alignment

**Business Value:** Improved maintainability, easier onboarding, reduced cognitive load for AI agents implementing future features.

**Architectural Impact:** Medium - touches core domain model and test infrastructure but preserves external APIs.

---

## Stories Breakdown

### Story TD-001-1: Migrate to AssertJ Assertions
**Priority:** P0 (Foundation)  
**Description:** Replace all JUnit 5 assertions with AssertJ fluent assertions across the entire test suite.

**Acceptance Criteria:**
- [ ] Add AssertJ dependency (`assertj-core:3.25.1`)
- [ ] Replace all `assertEquals` → `assertThat().isEqualTo()`
- [ ] Replace all `assertTrue/False` → `assertThat().isTrue/isFalse()`
- [ ] Replace all `assertNotNull` → `assertThat().isNotNull()`
- [ ] All tests passing with AssertJ assertions
- [ ] No JUnit 5 assertions remaining in codebase

**Technical Notes:**
```java
// Before: assertEquals(expected, actual);
// After: assertThat(actual).isEqualTo(expected);
```

**Dependencies:** None

---

### Story TD-001-2: Add JaCoCo Coverage Verification
**Priority:** P0 (Foundation)  
**Description:** Implement automated code coverage verification with 80% threshold using JaCoCo Maven plugin.

**Acceptance Criteria:**
- [ ] Add JaCoCo Maven plugin to pom.xml
- [ ] Configure 80% line coverage threshold
- [ ] Build fails if coverage drops below 80%
- [ ] Coverage report generated in `target/site/jacoco`
- [ ] Baseline coverage measured and documented

**Technical Notes:**
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <configuration>
        <rules>
            <rule>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

**Dependencies:** None

---

### Story TD-001-3: Refactor Expense to Sealed Abstract Class
**Priority:** P1 (Domain Refactoring)  
**Description:** Transform Expense into a sealed abstract class with concrete implementations ExpenseByNight and ExpenseEqual, each implementing their own calculateShare() logic.

**Acceptance Criteria:**
- [ ] Create sealed abstract class `Expense`
- [ ] Create `ExpenseByNight extends Expense` with BY_NIGHT calculation logic
- [ ] Create `ExpenseEqual extends Expense` with EQUAL calculation logic
- [ ] Configure Jackson polymorphic deserialization
- [ ] All existing Expense tests updated to use concrete types
- [ ] All tests passing
- [ ] JSON serialization/deserialization working correctly
- [ ] API contracts unchanged (REST endpoints accept same JSON)

**Technical Notes:**
```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ExpenseByNight.class, name = "BY_NIGHT"),
    @JsonSubTypes.Type(value = ExpenseEqual.class, name = "EQUAL")
})
public sealed abstract class Expense permits ExpenseByNight, ExpenseEqual {
    public abstract BigDecimal calculateShare(Participant participant);
}
```

**Dependencies:** TD-001-1 (easier with AssertJ in place)

---

### Story TD-001-4: Rename SplitService to SplitUseCases
**Priority:** P2 (Service Layer Refactoring)  
**Description:** Rename SplitService to SplitUseCases to better reflect Domain-Driven Design application service pattern.

**Acceptance Criteria:**
- [ ] Rename class `SplitService` → `SplitUseCases`
- [ ] Update all injection points and references
- [ ] Update all test class names referencing the service
- [ ] All tests passing
- [ ] Method signatures unchanged

**Technical Notes:**
- Use IDE refactoring tool (Rename) for safe refactoring
- Verify all REST resource injections updated

**Dependencies:** TD-001-3 (do after Expense refactoring settles)

---

### Story TD-001-5: Split SplitServiceTest by Use Case
**Priority:** P2 (Service Layer Refactoring)  
**Description:** Split monolithic SplitServiceTest into separate test classes, one per use case, for better organization and maintainability.

**Acceptance Criteria:**
- [ ] Create `CreateSplitUseCaseTest` (extract create split tests)
- [ ] Create `AddParticipantUseCaseTest` (extract participant tests)
- [ ] Create `AddExpenseUseCaseTest` (extract expense tests)
- [ ] Create `CalculateBalancesUseCaseTest` (extract balance calculation tests)
- [ ] Create additional use case test classes as needed
- [ ] Delete original `SplitServiceTest` after migration
- [ ] All tests passing
- [ ] Test coverage maintained or improved

**Technical Notes:**
- Each test class focuses on one behavioral scenario
- Follow naming pattern: `{UseCaseName}UseCaseTest`

**Dependencies:** TD-001-4 (do together with rename)

---

### Story TD-001-6: Enforce Test Persistence Pattern
**Priority:** P3 (Test Pattern Enforcement)  
**Description:** Refactor tests to verify persistence through public API only, eliminating direct file system access in test code.

**Acceptance Criteria:**
- [ ] Identify all tests directly accessing `data/` folder
- [ ] Refactor tests to use integration pattern: Create → Save → Load → Verify
- [ ] Remove all `new File()`, `Path.of()`, `Files.readString()` from test code
- [ ] All tests passing
- [ ] Test persistence pattern documented

**Test Pattern Example:**
```java
@Test
void shouldPersistAndReloadSplitWithIdentity() {
    Split split1 = createSplit("Split A");
    String split1Id = splitUseCases.saveSplit(split1);
    
    Split split2 = createSplit("Split B");
    splitUseCases.saveSplit(split2);
    
    Split reloaded = splitUseCases.loadSplit(split1Id);
    assertThat(reloaded).isEqualTo(split1);
}
```

**Dependencies:** TD-001-5 (do after test split to avoid massive refactor)

---

### Story TD-001-7: Synchronize Documentation
**Priority:** P4 (Documentation Sync)  
**Description:** Update all project documentation to reflect the refactoring changes made in previous stories.

**Acceptance Criteria:**
- [ ] Update architecture.md Implementation Patterns section (Expense example)
- [ ] Update architecture.md Project Structure section (test class names)
- [ ] Update project-context.md (if references service naming)
- [ ] Update README.md (if contains code examples)
- [ ] Update inline code comments referencing old patterns
- [ ] All documentation consistent with codebase

**Files to Update:**
- `architecture.md`
- `project-context.md` 
- `README.md`
- Inline code comments

**Dependencies:** TD-001-6 (final step after all code stabilizes)

---

## Implementation Phases

**Phase 1: Foundation (Stories 1-2)** - Independent, can run in parallel  
**Phase 2: Domain Refactoring (Story 3)** - Depends on Phase 1  
**Phase 3: Service Layer (Stories 4-5)** - Depends on Phase 2  
**Phase 4: Test Patterns (Story 6)** - Depends on Phase 3  
**Phase 5: Documentation (Story 7)** - Depends on all previous phases  

---

## Epic Acceptance Criteria

- [ ] All 7 stories completed
- [ ] Code coverage ≥ 80% enforced by build
- [ ] No direct file access in test code
- [ ] All documentation synchronized
- [ ] All tests passing
- [ ] No regression in existing functionality

---

## Risk Assessment

**Low Risk:** Stories 1, 2, 7 (additive or isolated)  
**Medium Risk:** Stories 3, 4, 5 (refactoring but well-understood patterns)  
**Medium Risk:** Story 6 (may reveal hidden filesystem assumptions)

**Mitigation:**
- Comprehensive test coverage before refactoring (Story 2 first)
- Incremental phasing (prevents big-bang failures)
- Use IDE refactoring tools (reduces manual errors)

---

## Architecture Impact

| Dimension | Impact Level | Notes |
|-----------|--------------|-------|
| API Contracts | None | External REST APIs unchanged |
| Domain Model | Medium | Expense becomes abstract, JSON schema identical |
| Service Layer | Low | Rename only, signatures unchanged |
| Testing | High | All tests updated (assertions, organization, patterns) |
| Documentation | Medium | Multiple files require updates |
| Build Process | Low | JaCoCo plugin added, coverage enforced |

---

**Epic Status:** Ready for implementation 🚀
