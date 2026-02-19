# Feature: Dev Sample Data

## What, Why and Constraints

**What**: When the application starts in dev mode, a realistic sample split is automatically created with a fixed, predictable ID. If the split already exists it is deleted and recreated, so every restart produces a clean, deterministic state. The split represents one week of vacation in Provence with 10 participants having different stay durations, and 50 realistic expenses.

**Why**: Developers need a rich, realistic dataset to work with immediately after starting the application without having to manually create participants and expenses. The fixed split ID (`devSampleSplit2026001`) allows bookmarking the split URL and sharing it across the team.

**Constraints**:
- Only active in dev mode — `@IfBuildProfile("dev")` ensures the bean is never registered in production or test builds
- The split ID is a valid 21-character URL-safe NanoID (`devSampleSplit2026001`)
- Domain model used directly (no service layer bypass): `Split` public constructor, `Participant.create()`, `ExpenseByNight.create()`, `ExpenseEqual.create()`, `ExpenseFree.create()` — all existing factory methods and validation rules apply
- No new API endpoints, no frontend changes
- Groceries, meals, and daily supplies are `BY_NIGHT` expenses (proportional to stay duration); one-off activities are `EQUAL`; travel is `FREE` (only the participants who traveled together)

## How

### Step 1: Add `delete()` to the persistence layer

- **`JsonFileRepository.java`**: added package-private `delete(String splitId)` method that calls `Files.deleteIfExists(pathResolver.resolve(splitId))` and wraps `IOException` in `PersistenceException`
- **`SplitRepository.java`**: added public `delete(String splitId)` method delegating to `jsonFileRepository.delete(splitId)`

These changes are minimal and follow the existing pattern of the repository (same `pathResolver` usage, same exception wrapping).

### Step 2: Create `DevDataSeeder`

- **New file**: `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/dev/DevDataSeeder.java`
- New `split/dev` package isolates dev-only infrastructure from the main domain and service packages
- The class is annotated `@ApplicationScoped @IfBuildProfile("dev")` and observes `StartupEvent`
- On startup: checks if the dev split exists, deletes it if so, then recreates it from scratch

**Vacation scenario — 10 participants (total 49 nights):**

| Participant | Nights | Arrival |
|-------------|--------|---------|
| Alice, Bob, Charlie, Diana | 7 | Saturday (full week) |
| Eve, Frank | 5 | Sunday |
| Isabelle, Jack | 4 | Isabelle: Sunday–Thursday / Jack: Saturday–Wednesday |
| Grace, Henry | 3 | Wednesday (weekend) |

**50 expenses breakdown:**

| Type | Count | Examples |
|------|-------|---------|
| `BY_NIGHT` | 35 | House rental, pool service, all groceries, home-cooked meals, daily supplies, take-away dinners |
| `EQUAL` | 7 | Kayak rental, wine tasting, boat trip, escape game, museum visit, jazz concert, bike rental |
| `FREE` | 8 | Travel expenses (car fuel, train tickets) split only among the sub-group who traveled together |

**Travel groups (FREE expenses):**
- Alice, Bob, Charlie, Diana — drove from Paris (aller + retour)
- Eve & Frank — shared a ride from Lyon (aller + retour)
- Grace & Henry — drove from Bordeaux for the weekend (aller + retour)
- Isabelle & Jack — took the TGV together (aller + retour)

Each `FREE` share uses `Expense.Share.withParts(id, BigDecimal.ONE)` (equal parts within the group) via a private `equalParts(Participant.Id...)` helper method.

## Tests

No automated tests added for this feature. The seeder is dev-infrastructure and the behavior is verified manually:

1. Start the application with `mvn quarkus:dev -pl fairnsquare-app`
2. Open `http://localhost:5173/splits/devSampleSplit2026001`
3. Verify 10 participants, 50 expenses are visible
4. Restart the application and verify the split is reset to its initial state
