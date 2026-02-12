# Feature: Solve / Reimbursement

## What, Why and Constraints

### What
Added a "Solve" button on the split dashboard that navigates to a settlement page. The settlement page displays per-participant financial balances (paid, cost, net balance) and a "Resolve" button that reveals reimbursement proposals calculated by a naive greedy algorithm.

### Why
After entering participants and expenses, users need to know who owes whom and how much. This feature provides a clear settlement view with actionable reimbursement suggestions, closing the core expense-splitting loop.

### Constraints
- Svelte 5 with runes for frontend reactivity
- Backend API for settlement calculation (not frontend-only)
- Naive greedy algorithm (a future story will implement an optimized algorithm)
- Follow existing patterns: rich domain model, DTO records, `@ApplicationScoped` mappers, `Optional`-based service methods, JAX-RS endpoints

## How

### 1. Settlement domain logic (`domain/settlement/`)
Created 4 domain classes:
- `ParticipantBalance` record: participantId, name, totalPaid, totalCost, balance
- `Reimbursement` record: fromId, fromName, toId, toName, amount
- `Settlement` record: aggregates balances + reimbursements
- `SettlementCalculator`: static `calculate(Split)` method that iterates all expenses, sums paid/cost per participant via `expense.getShares(split)`, then runs a naive greedy matching algorithm (debtors vs creditors, transfer min of each pair)

### 2. Settlement DTO and mapper (`api/dto/`, `api/mapper/`)
- `SettlementResponseDTO` record with nested `ParticipantBalanceDTO` and `ReimbursementDTO` records, Jackson-annotated
- `SettlementMapper` (`@ApplicationScoped`): converts domain Settlement to DTO

### 3. Service method (`service/SplitUseCases.java`)
Added `calculateSettlement(splitId)` returning `Optional<Settlement>` using the `repository.load().map(SettlementCalculator::calculate)` pattern. Read-only — no persistence needed.

### 4. REST endpoint (`api/SplitResource.java`)
Added `GET /api/splits/{splitId}/settlement` endpoint. Validates split ID, calls service, maps to DTO, returns 200 OK (or 400/404). Injected `SettlementMapper` into the resource.

### 5. API client types and function (`lib/api/splits.ts`)
Added `ParticipantBalance`, `Reimbursement`, `Settlement` TypeScript interfaces and `getSettlement(splitId)` API function.

### 6. Route registration (`lib/router.ts`)
Added `/splits/:splitId/settlement` route pointing to `Settlement.svelte`.

### 7. Solve card on dashboard (`routes/Split.svelte`)
Added a conditional "Solve" card (visible only when `expenseCount > 0 && participantCount > 0`) that navigates to the settlement page. Uses same teal card styling with a chevron-right icon.

### 8. Settlement page (`routes/Settlement.svelte`)
Created a standalone page that:
- Loads settlement data from the `splitId` route parameter
- Displays a header with back arrow + "Settlement" title
- Renders balance cards per participant (name, paid, cost, balance with green/red/gray coloring)
- Shows a "Resolve" button that toggles visibility of reimbursement details inline on each card ("Pay X to Y" in red, "Receive X from Y" in green)

## Tests

### Automated Tests

**`SettlementCalculatorTest.java`** (10 tests) — backend unit tests:
- Empty expenses: all balances zero, no reimbursements
- No participants: empty settlement
- Single payer equal expense: correct balance computation
- Multiple expenses: accumulates paid/cost correctly
- BY_NIGHT expense: proportional cost allocation
- Simple debt: single reimbursement (Bob pays Alice)
- Three participants: multiple reimbursements
- All settled: no reimbursements generated
- Greedy algorithm: splits debt across creditors correctly
- Preserves participant order in output

**`Settlement.test.ts`** (15 tests) — frontend tests:
- Loading state
- Header and back button navigation
- API error toast handling
- Balance cards: names, paid/cost amounts, balance colors (green/red/gray), empty state
- Resolve button: visible before click, hidden after click
- Reimbursement details: hidden before Resolve, shown after Resolve (pay/receive lines)
- All-settled scenario

**`Split.test.ts`** (5 new tests, 23 total) — Solve card tests:
- Solve card shown when both participants and expenses exist
- Solve card hidden with no expenses
- Solve card hidden with no participants
- Solve card hidden when empty split
- Navigation to settlement page on click

**Totals:** 186 backend tests (all passing), 244 frontend tests passing (3 pre-existing failures unrelated)
