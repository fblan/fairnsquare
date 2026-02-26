# Bugfix: Redirect to Split Creation Page When Split Not Found

**Date:** 2026-02-26
**Issue:** [#50](https://github.com/fblan/fairnsquare/issues/50)
**Branch:** `bugfix/50-redirect-split-not-found`

---

## 1. What, Why and Constraints

**What:** When navigating to `/splits/:splitId` and the split cannot be loaded — either because the ID format is invalid (API returns 400) or the split does not exist (API returns 404) — the user is now automatically redirected to the home page (`/`) — the split creation page — instead of being shown a static "Split not found" card requiring a manual click.

Split IDs are NanoIDs (exactly 21 URL-safe characters). An ID of the wrong length or format produces a 400; a valid but unknown ID produces a 404. Both cases are treated identically from the user's perspective.

**Why:** The previous UX presented a dead-end card with a single button. An automatic redirect is faster and less disruptive for users who arrive via a stale or invalid link.

**Constraints:**
- The redirect must happen in the frontend only (no backend change needed).
- An info toast must be shown so the user understands why they were redirected.
- All existing frontend rules must be followed, including full automated test coverage.

---

## 2. How

### Files modified

**`fairnsquare-app/src/main/webui/src/routes/Split.svelte`**

- Removed the `notFound` state variable (`$state<boolean>`).
- Removed the `handleGoHome()` function.
- Removed the `{:else if notFound}` UI block (the "Split not found" card with the "Create a new split" button).
- In `loadSplit()`, the condition `apiError.status === 404 || apiError.status === 400` now calls `addToast({ type: 'info', message: 'Split not found — create a new one.' })` followed by `navigate('/')`. Both 400 (invalid NanoID format) and 404 (valid format, not found) are treated identically.

**`fairnsquare-app/src/main/webui/src/routes/Split.test.ts`**

- Removed tests:
  - `"shows 404 state when split not found"` (card no longer rendered)
  - `"navigates home when clicking Create a new split on 404"` (button no longer exists)
- Added tests:
  - `"automatically redirects to home when split not found (404)"` — asserts `navigate('/')` is called
  - `"shows info toast when split not found (404)"` — asserts `addToast` is called with the correct payload

---

## 3. Tests

**File:** `src/routes/Split.test.ts`
**Total tests:** 27 (all passing)

| Group | Tests added | What they cover |
|---|---|---|
| Loading / Error / 404 States | 4 | Auto-redirect to `/` on 404; info toast on 404; auto-redirect to `/` on 400 (invalid format); info toast on 400 |