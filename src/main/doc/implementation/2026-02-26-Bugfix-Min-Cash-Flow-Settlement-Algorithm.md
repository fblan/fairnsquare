# Bugfix: Better Settlement Algorithm (Min Cash Flow)

**Date:** 2026-02-26
**Issue:** [#46](https://github.com/fblan/fairnsquare/issues/46)
**Branch:** `bugfix/46-min-cash-flow-settlement`

---

## 1. What, Why and Constraints

**What:** Replaced the naive greedy settlement algorithm with a sorted greedy (min cash flow) algorithm in `SettlementCalculator.java`. The balance calculation (`calculateBalances`) is unchanged and correct.

**Why:** The previous algorithm matched debtors to creditors in participant insertion order. This meant:
- The same group of people with the same expenses could produce different reimbursement sequences depending on who was added to the split first — non-deterministic from the user's perspective.
- Smaller debts could be settled before larger ones, producing a less intuitive presentation (largest amounts should appear first).

The fix sorts debtors by absolute balance descending and creditors by balance descending before the two-pointer matching loop. This ensures:
- **Determinism**: the same net balances always produce the same reimbursements, regardless of insertion order.
- **Clarity**: largest transactions appear first, making the settlement easier to read and act on.
- **Minimal splits**: large amounts cancel each other cleanly before small remainders are distributed.

**Constraints:**
- Only the reimbursement calculation is changed. Balance calculation (totalPaid, totalCost, balance per participant) is correct and untouched.
- `BigDecimal` arithmetic is preserved throughout for financial precision.
- The sort is implemented as a simple in-place swap loop (no external sort utility needed) to keep the parallel `participants`/`amounts` lists in sync.

---

## 2. How

### Files modified

**`fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/settlement/SettlementCalculator.java`**

- Added `sortByAmountDescending(List<ParticipantBalance>, List<BigDecimal>)` private helper that co-sorts both parallel lists (participants and their remaining amounts) using a simple selection-sort swap.
- In `calculateReimbursements()`, called `sortByAmountDescending` on both `debtors`/`debtRemaining` and `creditors`/`creditRemaining` before the two-pointer matching loop.
- Updated Javadoc on the class and on `calculateReimbursements()` to reflect the new algorithm description.

**`fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/domain/settlement/SettlementCalculatorTest.java`**

- Added 2 new tests demonstrating the sorting behaviour (see Tests section).

---

## 3. Tests

**File:** `src/test/java/.../settlement/SettlementCalculatorTest.java`
**Total tests:** 12 (all passing, 10 pre-existing + 2 new)

| Test | What it covers |
|---|---|
| `calculate_largestDebtSettledFirst_regardlessOfInsertionOrder` | 4-participant scenario (Alice -10, Bob -2, Charlie +7, Dave +5): verifies sorted order produces Alice→Charlie €7, Alice→Dave €3, Bob→Dave €2 |
| `calculate_sortingIsDeterministic_regardlessOfInsertionOrder` | Same balances with reversed insertion order (Bob, Alice, Dave, Charlie): verifies the first reimbursement is still Alice→Charlie €7, proving results are independent of insertion order |