# Bugfix: Rounding Remainder Always Goes to a Beneficiary

## What, Why and Constraints

**What:** In `ExpenseFree` expenses, a participant with `parts=0` (a non-beneficiary) could end up owing a small negative or positive amount due to rounding when they happened to be the last entry in the shares list.

**Why:** Issue #118 — the "last participant absorbs the remainder" pattern ensures `sum(shares) == expense.amount` exactly. But the loop previously used positional last (`i == shares.size() - 1`), not "last with positive parts". When HALF_UP rounding caused all preceding non-zero participants to round up, the final balance (e.g. -€0.01) landed on a zero-parts participant, making them incorrectly owe money.

**Constraints:** Fix is confined to `ExpenseFree.getShares()`. `ExpenseByNight` and `ExpenseByShare` are not affected — their participant weights derive from `Participant.Nights` (min 0.5) and `Participant.Share` (min 0.5), so weight is always > 0 and all participants are always beneficiaries.

## How

### Files modified

- **`ExpenseFree.java`** — Added a reverse scan before the main loop to find `lastPositiveIndex` (the last index with `parts > 0`). In the main loop:
  - `parts == 0` → always assigns `BigDecimal.ZERO`
  - `i == lastPositiveIndex` → absorbs the rounding remainder (`amount - totalAssigned`)
  - Otherwise → standard proportional calculation

  Original (bug): `if (i == shares.size() - 1)` — blindly gave remainder to last list entry.
  Fixed: remainder goes to last entry with `parts > 0`.

- **`ExpenseFreeTest.java`** — Added two regression tests:
  - `getShares_zeroPartsParticipantLast_getsExactlyZeroNotRoundingRemainder`: €1.01 / [Alice(1), Bob(1), Charlie(0)] — Charlie must get €0.00, not -€0.01
  - `getShares_zeroPartsParticipantInMiddle_getsExactlyZero`: €100 / [Alice(2), Charlie(0), Bob(1)] — Charlie in middle also gets €0.00, Bob (last positive) gets the remainder

## Tests

- **Automated:** 2 new tests added in `ExpenseFreeTest.java`, all 35 domain expense tests pass.
- The first test directly reproduces the bug: without the fix, Charlie would receive -€0.01.
