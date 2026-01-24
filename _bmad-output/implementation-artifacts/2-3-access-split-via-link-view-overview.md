# Story 2.3: Access Split via Link & View Overview

Status: done

## Story

As a **user with a shared link**,
I want **to access the split directly and see an overview of the split state**,
So that **I can immediately understand who's participating and what expenses exist**.

## Acceptance Criteria

1. **Given** I have a valid split link `https://app.fairnsquare.com/splits/{splitId}` **When** I navigate to that URL **Then** the split overview page loads **And** I see the split name as the page title/header **And** I see a "Share" button to copy the link

2. **Given** I am viewing a split overview **When** the page loads **Then** I see the following sections:
   - **Header**: Split name + share button
   - **Participants**: List showing "No participants yet" or participant names with nights
   - **Expenses**: List showing "No expenses yet" or expense summaries
   - **Balance Summary**: Shows "Add participants and expenses to see balances"

3. **Given** the split has participants (from future epic) **When** viewing the overview **Then** each participant card shows: name, number of nights, running balance **And** participants are displayed in a vertical list

4. **Given** the split has expenses (from future epic) **When** viewing the overview **Then** each expense shows: description, amount, payer name, split mode badge **And** expenses are displayed in chronological order (newest first)

5. **Given** I navigate to a split that doesn't exist **When** the page attempts to load **Then** I see a 404 error page with message "Split not found" **And** I see a link to "Create a new split" that goes to the home page

6. **Given** the API call to fetch the split fails (network error) **When** loading the split page **Then** I see an error message "Failed to load split. Please try again." **And** I see a "Retry" button to attempt loading again

7. **Given** I am viewing the split on mobile (< 768px) **When** the page renders **Then** the layout is single-column **And** cards have 16px padding and 8px border radius **And** the max content width is 420px, centered

8. **Given** the split URL uses sv-router **When** I navigate to `/splits/{splitId}` **Then** the route parameter `splitId` is extracted **And** the split data is fetched from `GET /api/splits/{splitId}`

## Tasks / Subtasks

- [x] Task 1: Set up sv-router for client-side routing (AC: 8)
  - [x] 1.1: Install `sv-router` package
  - [x] 1.2: Configure router in `App.svelte` with routes for `/` and `/splits/:splitId`
  - [x] 1.3: Update `Home.svelte` to work with router (added `use:route` action)
  - [x] 1.4: Verify "Go to Split" button from Home.svelte navigates correctly

- [x] Task 2: Create Split.svelte route component (AC: 1, 2, 8)
  - [x] 2.1: Create `src/routes/Split.svelte` component
  - [x] 2.2: Extract `splitId` from route params via `route.params.splitId`
  - [x] 2.3: Use existing `getSplit()` from `$lib/api/splits.ts` to fetch data
  - [x] 2.4: Add loading state using Svelte 5 runes pattern
  - [x] 2.5: Display split name as page header

- [x] Task 3: Implement header with share functionality (AC: 1)
  - [x] 3.1: Create header section with split name
  - [x] 3.2: Add "Share" button that copies current URL to clipboard
  - [x] 3.3: Show toast confirmation on successful copy

- [x] Task 4: Create empty state sections (AC: 2)
  - [x] 4.1: Create Participants section with "No participants yet" empty state
  - [x] 4.2: Create Expenses section with "No expenses yet" empty state
  - [x] 4.3: Create Balance Summary section with placeholder message
  - [x] 4.4: Use Card components for each section

- [x] Task 5: Create participant list UI (AC: 3)
  - [x] 5.1: Implemented inline in Split.svelte (simpler than separate component)
  - [x] 5.2: Display name, nights, and running balance (balance shows €0.00 for now)
  - [x] 5.3: Render list of participants from split data

- [x] Task 6: Create expense list UI (AC: 4)
  - [x] 6.1: Implemented inline in Split.svelte (simpler than separate component)
  - [x] 6.2: Display description, amount, payer name, split mode badge
  - [x] 6.3: Render list of expenses from split data (newest first via sortedExpenses)
  - [x] 6.4: Create split mode badge (formatSplitMode helper function)

- [x] Task 7: Implement 404 error handling (AC: 5)
  - [x] 7.1: Implemented inline in Split.svelte (simpler than separate component)
  - [x] 7.2: Detect 404 response from API (status 404)
  - [x] 7.3: Display "Split not found" message with "Create a new split" link
  - [x] 7.4: Style consistently with app design

- [x] Task 8: Implement network error handling with retry (AC: 6)
  - [x] 8.1: Catch network errors from `getSplit()`
  - [x] 8.2: Display "Failed to load split. Please try again." message
  - [x] 8.3: Add "Retry" button that re-fetches the split
  - [x] 8.4: Show loading state during retry

- [x] Task 9: Mobile responsive styling (AC: 7)
  - [x] 9.1: Ensure single-column layout on mobile (flex-col)
  - [x] 9.2: Verify 16px card padding (p-4) and 8px border radius (rounded-lg)
  - [x] 9.3: Confirm max-width 420px centered layout

- [x] Task 10: Write backend integration test for GET split endpoint (AC: 5)
  - [x] 10.1: Test for GET /api/splits/{splitId} returns 200 already exists
  - [x] 10.2: Enhanced test to verify 404 returns Problem Details format

## Dev Notes

### CRITICAL: Previous Story Intelligence

**From Story 2.1 (Backend):**
- `GET /api/splits/{splitId}` endpoint already implemented in `SplitResource.java`
- Returns full split data: id, name, createdAt, participants[], expenses[]
- Returns 404 Problem Details if split not found
- SplitId validation (21 chars, URL-safe) prevents path traversal
- Tests already exist for basic GET functionality

**From Story 2.2 (Frontend):**
- `getSplit(splitId)` function already created in `$lib/api/splits.ts`
- `ApiError` type defined in `$lib/api/client.ts` (RFC 9457 format)
- Error handling pattern with network error fallback already in client.ts
- Toast store at `$lib/stores/toastStore.svelte.ts` for notifications
- Home.svelte "Go to Split" button uses `href={/splits/${createdSplit.id}}`

### Architecture Patterns

**Frontend State Pattern (MUST USE):**
```typescript
let split = $state<Split | null>(null);
let isLoading = $state(true);
let error = $state<string | null>(null);
let notFound = $state(false);
```

**API Client Pattern:**
```typescript
import { getSplit, type Split } from '$lib/api/splits';
import type { ApiError } from '$lib/api/client';

try {
  split = await getSplit(splitId);
} catch (err) {
  const apiError = err as ApiError;
  if (apiError.status === 404) {
    notFound = true;
  } else {
    error = apiError.detail || 'Failed to load split';
  }
}
```

**Svelte 5 Rules:**
- Use runes: `$state`, `$derived`, `$effect`
- Use `$props()` for component props, NOT `export let`
- Use `.svelte.ts` for shared state files

### Router Setup (sv-router)

**Package:** `sv-router` (per architecture.md)

**Router Configuration (`$lib/router.ts`):**
```typescript
import { createRouter } from 'sv-router';
import Home from '../routes/Home.svelte';
import Split from '../routes/Split.svelte';

export const { p, navigate, isActive, route } = createRouter({
  '/': Home,
  '/splits/:splitId': Split,
});
```

**App.svelte Pattern:**
```svelte
<script lang="ts">
  import { Router } from 'sv-router';
  import '$lib/router'; // Initialize routes
</script>

<Router />
```

**Accessing Route Params:**
```svelte
<script lang="ts">
  import { route } from '$lib/router';

  const splitId = $derived(route.params.splitId || '');
</script>
```

### Component Structure (per architecture.md)

**Architecture suggested separate components:**
```
src/lib/components/
├── participant/
│   └── ParticipantCard.svelte
├── expense/
│   └── ExpenseCard.svelte
└── NotFound.svelte
```

**Implementation Decision:** Rendered inline in Split.svelte for simplicity. These components are only used in one place and are small enough that separate files would add unnecessary indirection. Can be extracted later if reuse is needed.

**Use $props() pattern (when extracting components):**
```svelte
<script lang="ts">
  import type { Participant } from '$lib/api/splits';

  interface Props {
    participant: Participant;
  }

  let { participant }: Props = $props();
</script>
```

### TypeScript Interfaces (already in splits.ts)

```typescript
export interface Split {
  id: string;
  name: string;
  createdAt: string;
  participants: Participant[];
  expenses: Expense[];
}

// May need to extend for story 2.3:
export interface Participant {
  id: string;
  name: string;
  nights: number;
}

export interface Expense {
  id: string;
  description: string;
  amount: number;
  payerId: string;
  splitMode: 'BY_NIGHT' | 'EQUAL' | 'FREE';
  createdAt: string;
}
```

### UI Patterns

**Empty State Pattern:**
```svelte
{#if split.participants.length === 0}
  <p class="text-muted-foreground text-center py-4">No participants yet</p>
{:else}
  {#each split.participants as participant}
    <ParticipantCard {participant} />
  {/each}
{/if}
```

**Split Mode Badge:**
```svelte
<span class="text-xs px-2 py-1 rounded bg-secondary text-secondary-foreground">
  {splitMode === 'BY_NIGHT' ? 'By Night' : splitMode === 'EQUAL' ? 'Equal' : 'Free'}
</span>
```

### Mobile Styling (per UX spec)

- Max content width: 420px (already in #app from app.css)
- Card padding: 16px (p-4 in Tailwind)
- Border radius: 8px (rounded-lg in Tailwind)
- Touch targets: min 44px height (min-h-[44px])
- Single column layout on mobile

### Testing Notes

**Backend test already exists for:**
- POST /api/splits (create)
- GET /api/splits/{splitId} (retrieve)
- Validation errors (400)
- Path traversal protection

**May need to add:**
- Test for 404 when split doesn't exist
- Test response format matches frontend expectations

### References

- [Source: architecture.md#Frontend-Architecture] - sv-router, Svelte 5 runes
- [Source: architecture.md#Structure-Patterns] - Frontend component structure
- [Source: architecture.md#API-&-Communication-Patterns] - GET /api/splits/{splitId}
- [Source: epics.md#Story-2.3] - Acceptance criteria
- [Source: 2-1-create-split-backend-api.md] - Backend implementation details
- [Source: 2-2-create-split-frontend.md] - Frontend patterns, getSplit() exists
- [Source: project-context.md#Svelte-5-SPA-Mode-Rules] - Router requirements

### Project Structure Notes

**Originally planned to create (architecture.md suggestion):**
- `src/main/webui/src/routes/Split.svelte` ✅ Created
- `src/main/webui/src/lib/components/participant/ParticipantCard.svelte` ❌ Inline in Split.svelte
- `src/main/webui/src/lib/components/expense/ExpenseCard.svelte` ❌ Inline in Split.svelte
- `src/main/webui/src/lib/components/NotFound.svelte` ❌ Inline in Split.svelte

**Actually created:**
- `src/main/webui/src/routes/Split.svelte` - main route component with inline rendering
- `src/main/webui/src/lib/router.ts` - sv-router configuration
- `src/main/webui/src/routes/Split.test.ts` - frontend tests
- `src/main/webui/src/routes/Home.test.ts` - frontend tests
- `src/main/webui/src/test/setup.ts` - vitest setup

**Files to modify:**
- `src/main/webui/package.json` (add sv-router, testing deps)
- `src/main/webui/src/App.svelte` (add router)
- `src/main/webui/src/lib/api/splits.ts` (extend interfaces)
- `src/main/webui/vite.config.ts` (add test configuration)

## Dev Agent Record

### Agent Model Used

Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References

N/A

### Completion Notes List

- **Code Review Fixes Applied (2026-01-24):**
  - H1/H2: Updated "Create a new split" button to use `navigate('/')` for proper SPA navigation
  - M1: Fixed invalid locale `'en-EU'` → `'en-IE'` (valid BCP 47 locale for EUR with English)
- **Code Review #2 Fixes Applied (2026-01-24):**
  - H1/H2/H3: Updated File List with all created/modified/deleted files
  - M1/M2/M3/M4: Fixed Dev Notes to match actual implementation (sv-router package, route.params pattern, inline components)
- Installed `sv-router` for SPA routing
- Configured Router in App.svelte with createRouter pattern in `$lib/router.ts`
- Route params accessed via `route.params.splitId` (sv-router API)
- Created Split.svelte with full split overview functionality:
  - Loading state with SVG spinner
  - 404 error state with "Create a new split" link
  - Network error state with Retry button
  - Header with split name and Share button
  - Participants section with empty state and list rendering
  - Expenses section with empty state and list rendering (sorted newest first)
  - Balance Summary section with placeholder message
- Updated Home.svelte to use `use:route` action for SPA navigation
- Extended TypeScript interfaces in splits.ts: Participant, Expense
- Created SplitNotFoundError for proper Problem Details 404 responses
- Enhanced SplitResourceIT test to verify 404 Problem Details format
- All 11 backend tests passing
- Frontend builds successfully (124KB JS, 25KB CSS)

### File List

**New Files:**
- `fairnsquare-app/src/main/webui/src/routes/Split.svelte` - split overview page component
- `fairnsquare-app/src/main/webui/src/lib/router.ts` - sv-router configuration
- `fairnsquare-app/src/main/webui/src/routes/Split.test.ts` - Split component tests (8 tests)
- `fairnsquare-app/src/main/webui/src/routes/Home.test.ts` - Home component tests (6 tests)
- `fairnsquare-app/src/main/webui/src/test/setup.ts` - vitest test setup
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/SplitNotFoundError.java` - 404 error type

**Modified Files:**
- `fairnsquare-app/src/main/webui/package.json` (added sv-router, vitest, testing-library)
- `fairnsquare-app/src/main/webui/package-lock.json` (dependency lockfile)
- `fairnsquare-app/src/main/webui/vite.config.ts` (added test configuration)
- `fairnsquare-app/src/main/webui/src/App.svelte` (added Router)
- `fairnsquare-app/src/main/webui/src/routes/Home.svelte` (use navigate() for SPA navigation)
- `fairnsquare-app/src/main/webui/src/lib/api/splits.ts` (extended Participant, Expense interfaces)
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/Split.java` (moved Id as inner record)
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/api/SplitResource.java` (throw SplitNotFoundError)
- `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/split/api/SplitResourceIT.java` (enhanced 404 test)

**Deleted Files:**
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/sharedkernel/HelloRest.java` - removed unused starter code
- `fairnsquare-app/src/main/java/org/asymetrik/web/fairnsquare/split/domain/SplitId.java` - consolidated as Split.Id inner record