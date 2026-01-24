# Story 2.2: Create Split Frontend

Status: done

## Story

As a **user**,
I want **to create a new split by entering a name and instantly receive a shareable link**,
So that **I can start tracking expenses for my group with zero friction**.

## Acceptance Criteria

1. **Given** I am on the home page **When** the page loads **Then** I see a form with:
   - A text input for split name with placeholder "e.g., Bordeaux Weekend 2026"
   - A "Create Split" button
   - The button uses the primary teal color (#0D9488)

2. **Given** I am on the home page **When** I enter a split name "Beach Trip 2026" and click "Create Split" **Then** a loading state is shown on the button **And** the API is called to create the split **And** on success, I am shown the shareable link prominently

3. **Given** a split was just created successfully **When** the success state is displayed **Then** I see the full shareable URL (e.g., `https://app.fairnsquare.com/splits/{splitId}`) **And** I see a "Copy Link" button next to the URL **And** clicking "Copy Link" copies the URL to clipboard and shows confirmation **And** I see a "Go to Split" button to navigate to the split overview

4. **Given** I try to create a split with an empty name **When** I click "Create Split" **Then** the form shows a validation error "Split name is required" **And** the API is not called

5. **Given** the API returns an error **When** creating a split **Then** an error toast is displayed with the error message **And** the form remains editable for retry

6. **Given** I am on a mobile device (< 768px) **When** viewing the create split form **Then** the form is centered with max-width 420px **And** touch targets are at least 44px in height **And** the input and button are full-width

## Tasks / Subtasks

- [x] Task 1: Create splits API client function (AC: 2)
  - [x] 1.1: Create `src/lib/api/splits.ts` with `createSplit()` function
  - [x] 1.2: Define `CreateSplitRequest` and `Split` TypeScript interfaces
  - [x] 1.3: Use existing `apiRequest` from `client.ts`

- [x] Task 2: Update Home.svelte with loading state (AC: 1, 2)
  - [x] 2.1: Add `isLoading` state using Svelte 5 runes
  - [x] 2.2: Show loading spinner/text on button during API call
  - [x] 2.3: Disable form inputs while loading

- [x] Task 3: Implement success state with shareable link (AC: 3)
  - [x] 3.1: Add `createdSplit` state to track successful creation
  - [x] 3.2: Create success UI showing full shareable URL
  - [x] 3.3: Implement "Copy Link" button with clipboard API
  - [x] 3.4: Show copy confirmation (toast or inline feedback)
  - [x] 3.5: Add "Go to Split" button (link to /splits/{splitId})
  - [x] 3.6: Add "Create Another" button to reset form

- [x] Task 4: Add client-side validation (AC: 4)
  - [x] 4.1: Add `validationError` state
  - [x] 4.2: Validate name is not empty on submit
  - [x] 4.3: Show inline error message below input
  - [x] 4.4: Clear error when user starts typing

- [x] Task 5: Add error handling with toast (AC: 5)
  - [x] 5.1: Integrate toastStore for error notifications
  - [x] 5.2: Catch API errors and display in toast
  - [x] 5.3: Ensure form remains editable after error

- [x] Task 6: Mobile responsive styling (AC: 6)
  - [x] 6.1: Set max-width 420px on form container
  - [x] 6.2: Ensure touch targets are min 44px height
  - [x] 6.3: Verify full-width inputs/buttons on mobile

## Dev Notes

### Architecture Patterns (from architecture.md)

**Frontend State Pattern:**
```typescript
let isLoading = $state(false);
let error = $state<string | null>(null);
let data = $state<Split | null>(null);
```

**API Client Pattern:**
- Use `apiRequest<T>()` from `$lib/api/client.ts`
- Errors are typed as `ApiError` (RFC 9457 Problem Details)

**Svelte 5 Rules:**
- Use runes: `$state`, `$derived`, `$effect`
- Use `$props()` for component props, not `export let`
- Use `.svelte.ts` for shared state files

### Shareable URL Format

Production: `https://app.fairnsquare.com/splits/{splitId}`
Development: `{window.location.origin}/splits/{splitId}`

### shadcn Components Available

- Button (with loading state via disabled + spinner)
- Input
- Card (Root, Header, Title, Content)
- Label

### Toast Store

Located at `$lib/stores/toastStore.svelte.ts` - verify API before use.

## Dev Agent Record

### Agent Model Used

Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References

N/A

### Completion Notes List

- Created `splits.ts` API client with `createSplit()` and `getSplit()` functions
- Defined TypeScript interfaces: `CreateSplitRequest`, `Split`
- Updated `Home.svelte` with full implementation:
  - Loading state with SVG spinner and "Creating..." text
  - Success state showing shareable URL with Copy Link button
  - Go to Split and Create Another buttons
  - Client-side validation with inline error message
  - Error handling via toastStore
  - Mobile responsive: max-width 420px, min-h-[44px] touch targets
- Uses Svelte 5 runes: `$state`, `$derived`
- Uses existing `apiRequest` and `addToast` utilities
- Frontend builds successfully (86KB gzipped)
- All 22 backend tests passing
- `getSplit()` function pre-created for story 2.3 (split overview page)

### File List

**New Files:**
- `fairnsquare-app/src/main/webui/src/lib/api/splits.ts`

**Modified Files:**
- `fairnsquare-app/src/main/webui/src/routes/Home.svelte`
- `fairnsquare-app/src/main/webui/src/lib/api/client.ts`
- `.gitignore`

---

## Senior Developer Review (AI)

**Reviewer:** Amelia (Dev Agent) | **Date:** 2026-01-24 | **Model:** Claude Opus 4.5

### Review Summary

| Category | Finding Count |
|----------|---------------|
| HIGH | 1 (deferred - see notes) |
| MEDIUM | 4 (all fixed) |
| LOW | 2 (all fixed) |

### Issues Fixed

1. **Hardcoded color violation** - Changed `text-green-600` to `text-success` in Home.svelte:98 to comply with white-label theming rules
2. **data/ folder not gitignored** - Added `fairnsquare-app/data/` to .gitignore
3. **client.ts fragile error handling** - Added robust handling for network failures and non-JSON error responses
4. **Emoji spinner** - Replaced `⏳` emoji with proper SVG spinner for cross-platform consistency
5. **File List incomplete** - Updated to include all modified files
6. **getSplit() undocumented** - Added note that it's pre-created for story 2.3

### Deferred Issue

**Frontend tests not configured** - No test framework (vitest/jest) is installed in package.json. Per project-context.md, the testing strategy relies on backend integration tests (>90% coverage target). Frontend unit tests would require:
- Adding vitest + @testing-library/svelte to devDependencies
- Creating vitest.config.ts
- Writing component tests

Recommendation: Create a separate story for frontend test infrastructure if desired.

### AC Validation

All 6 Acceptance Criteria verified as fully implemented.

### Verdict: ✅ APPROVED