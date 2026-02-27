# Feature: Numeric Input Select-All on Focus

**Date:** 2026-02-27

## What, Why and Constraints

**What:** When a user clicks (or tabs into) a numeric input field, the entire existing value is automatically selected.

**Why:** Without this behavior, users must manually clear the existing value before typing a new one, which is a common usability friction point for numeric entry forms (expense amounts, share parts).

**Constraints:**
- `input.select()` on `<input type="number">` is not reliably synchronous in Chrome — the selection must be deferred via `setTimeout(() => el.select(), 0)` to work cross-browser.
- The implementation must not break any parent-provided `onfocus` handlers — they must still be called.
- The `onfocus` prop must be destructured out of `restProps` before spreading to prevent the spread from overriding the internal handler.

## How

**Single file modified:** `fairnsquare-app/src/main/webui/src/lib/components/ui/input/input.svelte`

1. Destructured `onfocus` as `onFocusProp` from the component's props, so it is no longer part of `restProps`.
2. Added a `handleFocus` function that:
   - Calls `setTimeout(() => ref?.select(), 0)` when `type === 'number'` (deferred to ensure cross-browser compatibility).
   - Chains the caller's `onFocusProp` if provided.
3. Applied `onfocus={handleFocus}` on the non-file `<input>` element, placed **after** `{...restProps}` so it always wins.

This centralised the behavior in the shared input component — all numeric inputs across the application (expense amount, share parts) benefit automatically with zero changes to parent components.

**File created:** `fairnsquare-app/src/main/webui/src/lib/components/ui/input/input.test.ts`

## Tests

**New test file:** `src/lib/components/ui/input/input.test.ts` — 4 tests

| Test | What it covers |
|------|----------------|
| `calls select() on the input element when a number input is focused` | Core behavior: `select()` is deferred and called via `setTimeout` |
| `does not call select() when a text input is focused` | Non-regression: text inputs are unaffected |
| `still calls a parent-provided onfocus handler when focused` | Chaining: caller's `onfocus` is not lost |
| `calls both select() and a parent onfocus for number inputs` | Combined: both behaviors fire together |

All 339 tests pass (12 test files).