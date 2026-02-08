# Story FNS-002.7: Share Button (Placeholder)

Status: done

## Story

As a **split creator**,
I want **to easily share the split link with others**,
so that **participants can access the split and add their expenses**.

## Acceptance Criteria

1. **Given** I'm viewing the dashboard
   **When** I look at the header area
   **Then** I see a Share button with a share icon (teal) in the top-right corner

2. **Given** I see the Share button
   **When** I observe its appearance
   **Then** the button has a minimum 44px touch target and a share icon

3. **Given** I click the Share button
   **When** `navigator.clipboard` is available
   **Then** the current page URL is copied to the clipboard
   **And** I see a toast notification: "Link copied!"

4. **Given** I click the Share button
   **When** `navigator.clipboard` is NOT available (older browsers)
   **Then** I see a toast with the URL text: "Share link: [URL]" so I can manually copy it

5. **Given** I successfully copy the link
   **When** the toast appears
   **Then** it auto-dismisses after the standard toast duration

## Tasks / Subtasks

- [x] Task 1: Verify existing Share button implementation (AC: 1, 2)
  - [x] 1.1: Check current Share button in Split.svelte header
  - [x] 1.2: Verify 44px touch target (`min-h-[44px]`)
  - [x] 1.3: Verify share icon renders correctly

- [x] Task 2: Verify clipboard copy behavior (AC: 3, 4)
  - [x] 2.1: Verify `handleShare()` in Split.svelte copies URL via `navigator.clipboard.writeText`
  - [x] 2.2: Update success toast: "Link copied to clipboard" → "Link copied!" (per AC3)
  - [x] 2.3: Add fallback for browsers without clipboard API (show URL in info toast)
  - [x] 2.4: Verify error/fallback toast on clipboard failure

- [x] Task 3: Write/update tests (AC: 3, 4, 5)
  - [x] 3.1: Test Share button renders in header
  - [x] 3.2: Test clipboard copy on click (mock navigator.clipboard)
  - [x] 3.3: Test success toast message ("Link copied!")
  - [x] 3.4: Test fallback behavior (clipboard denied → shows URL in toast)
  - [x] 3.5: Test error handling

## Dev Notes

### Current State Analysis

**Share button ALREADY EXISTS in Split.svelte:**
```svelte
<Button
  onclick={handleShare}
  variant="outline"
  size="sm"
  class="min-h-[44px]"
  aria-label="Share"
>
  <svg ...share icon... />
  Share
</Button>
```

**handleShare() already implemented:**
```typescript
async function handleShare() {
  try {
    await navigator.clipboard.writeText(window.location.href);
    addToast({ type: 'success', message: 'Link copied to clipboard' });
  } catch {
    addToast({ type: 'error', message: 'Failed to copy link' });
  }
}
```

### What's Already Done vs What's Needed

| Requirement | Status | Notes |
|-------------|--------|-------|
| Share button in header | ✅ Done | Split.svelte already has it |
| Share icon (teal) | ✅ Done | SVG share icon with teal styling |
| 44px touch target | ✅ Done | `min-h-[44px]` class applied |
| Clipboard copy | ✅ Done | `navigator.clipboard.writeText` |
| Success toast | ✅ Done | "Link copied to clipboard" |
| Error toast | ✅ Done | "Failed to copy link" |
| Fallback (no clipboard API) | ⚠️ Partial | Current error path shows generic error, could show URL instead |

### Minimal Changes Needed

This story is **mostly already implemented** from Story FNS-002-2. Only minor enhancements:

1. **Clipboard fallback:** Update error handler to show URL as text instead of generic error
2. **Toast message alignment:** Optionally update "Link copied to clipboard" → "Link copied!" (per epic spec)
3. **Test coverage:** Ensure Share button behavior has test coverage in Split.test.ts

### Implementation

Update `handleShare()` in Split.svelte for better fallback:
```typescript
async function handleShare() {
  const url = window.location.href;
  try {
    await navigator.clipboard.writeText(url);
    addToast({ type: 'success', message: 'Link copied!' });
  } catch {
    // Fallback: show URL in toast for manual copy
    addToast({ type: 'info', message: `Share link: ${url}` });
  }
}
```

### Svelte 5 Patterns

No new patterns needed — existing code already follows all conventions.

### Accessibility

- ✅ `aria-label="Share"` already set
- ✅ 44px touch target
- ✅ Keyboard accessible (Button component)

### Previous Story Learnings

- Share button was implemented in FNS-002-2 alongside the dashboard
- Minimal work remaining — primarily test verification and fallback enhancement

### Project Structure Notes

- Modified: `fairnsquare-app/src/main/webui/src/routes/Split.svelte` (minor handleShare update)
- Modified: `fairnsquare-app/src/main/webui/src/routes/Split.test.ts` (add/verify share button tests)

### References

- [Source: _bmad-output/implementation-artifacts/epic-FNS-002-participant-centric-dashboard.md#Story 7]
- [Source: fairnsquare-app/src/main/webui/src/routes/Split.svelte] — Share button already implemented
- [Source: _bmad-output/project-context.md] — Accessibility requirements

## Dev Agent Record

### Agent Model Used
- Claude Sonnet 4.5 (claude-sonnet-4-5-20250929)

### Debug Log References
- N/A

### Completion Notes List
- **Mostly pre-implemented:** Share button was already implemented in FNS-002-2 (dashboard story). Only minor updates needed.
- **Toast message update:** Changed success toast from "Link copied to clipboard" → "Link copied!" per AC3
- **Clipboard fallback:** Updated error handler from generic error toast to info toast showing the URL: "Share link: [URL]" per AC4
- **Clipboard API check:** Added explicit `navigator.clipboard` existence check before calling `writeText()` for browsers without clipboard API (AC4)
- **Guard clause:** Added `if (typeof window === 'undefined') return;` for SSR safety
- **Tests updated:** Updated existing clipboard test expectation and added new test for fallback behavior
- **Total Test Coverage:** 351 tests passing (196 frontend + 155 backend)
- **Code Review Fix:** Added `!navigator.clipboard` guard for truly missing API (not just denied access)

### File List

**Modified Files:**
- fairnsquare-app/src/main/webui/src/routes/Split.svelte — Updated handleShare() toast message and fallback behavior
- fairnsquare-app/src/main/webui/src/routes/Split.test.ts — Updated clipboard test expectation, added fallback test
- _bmad-output/implementation-artifacts/sprint-status.yaml
