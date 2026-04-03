# Feature: Copy Nights Shares in Custom Expense

## What, Why and Constraints

**What:** Added a "Copy nights shares" button in the `ShareEditModal` (the edit shares view for FREE/custom expenses). When clicked, it shows an amber warning banner explaining that values will be overwritten and are a static snapshot. On confirmation, each participant's share parts are overwritten with `nights × share`, and all participants are enabled (checked).

**Why:** Issue #87 — the BY_NIGHT split mode was the only way to use nights-weighted splits, but it's automatic and not customizable. Users wanted a shortcut to seed a FREE expense with nights-weighted values as a starting point, which they can then fine-tune manually.

**Constraints:**
- Frontend-only change. The backend already stores FREE expense parts as-is; no new API or calculation logic was needed.
- The `Participant` type already exposes `nights` and `share` fields, so no data fetching was added.
- Values are a static snapshot: future changes to a participant's nights or share are not reflected in the custom expense automatically (by design — this is a FREE expense).

## How

### Files modified

**`fairnsquare-app/src/main/webui/src/lib/components/expense/ShareEditModal.svelte`**

1. Added `Moon` and `AlertTriangle` to the lucide-svelte import.
2. Added `showNightsWarning` reactive state (`$state(false)`).
3. Added three functions:
   - `copyNightsShares()` — sets `showNightsWarning = true`.
   - `applyNightsShares()` — iterates participants, sets `localParts[id] = nights * share`, `localChecked[id] = true`, `localPreviousValues[id] = value`, then hides the warning.
   - `cancelNightsWarning()` — sets `showNightsWarning = false`.
4. Added the "Copy nights shares" button (outline, full-width, with Moon icon) below the description/total header row.
5. Added an `{#if showNightsWarning}` amber banner (`role="alert"`) with an `AlertTriangle` icon, a warning message, and Apply/Cancel buttons scoped inside the banner.

### Files created

**`fairnsquare-app/src/main/webui/src/lib/components/expense/ShareEditModal.test.ts`**

New test file for `ShareEditModal`. Previously no test file existed for this component.

## Tests

**File:** `src/lib/components/expense/ShareEditModal.test.ts`
**Count:** 8 tests across 5 describe blocks

| Describe (AC) | Tests |
|---|---|
| AC1 — Button rendered | "Copy nights shares" button is present in the modal |
| AC2 — Warning shown | Clicking shows the alert banner; banner contains Apply and Cancel buttons |
| AC3 — Warning content | Banner text matches /overwrite/ and /static snapshot/ |
| AC4 — Cancel dismisses | Cancel hides banner, Alice's input value unchanged (still 5) |
| AC5 & AC6 — Apply | Overwrites Alice→3, Bob→4, Charlie→4 (nights×share); banner disappears; Bob's input is enabled even if he was unchecked |

All 8 tests pass.
