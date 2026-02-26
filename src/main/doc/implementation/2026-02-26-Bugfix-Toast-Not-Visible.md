# Bugfix: Toast Notifications Not Visible

**Date:** 2026-02-26
**Issue:** [#49](https://github.com/fblan/fairnsquare/issues/49)
**Branch:** `bugfix/49-toast-not-visible`

---

## 1. What, Why and Constraints

**What:** Toast notifications were never displayed in the UI despite `addToast()` being called throughout the application (on errors, success actions, redirects, etc.).

**Why:** The `toastStore.svelte.ts` store correctly manages toast state using Svelte 5 `$state` runes, and `addToast` / `removeToast` were already wired up across all route components. However, no component ever read from the store and rendered the toasts into the DOM. `App.svelte` only mounted the `<Router />` — there was no `<Toaster />` component anywhere in the component tree.

**Constraints:**
- The `Toaster` must be mounted outside the `<main>` content container so it is not clipped by `overflow` or constrained by `max-width`.
- It must use `aria-live="polite"` for accessibility.
- Styling must follow the existing Tailwind/shadcn design system (destructive, primary, green-600, yellow-500).
- Every frontend change must be covered by automated tests.

---

## 2. How

### Files created

**`fairnsquare-app/src/main/webui/src/lib/components/ui/toast/Toaster.svelte`**

- New component that calls `getToasts()` from the store reactively inside an `{#each}` block.
- Renders a fixed bottom-right container (`z-50`) with one `role="alert"` div per toast.
- Each toast is colour-coded by type: success (green-600), error (destructive), warning (yellow-500), info (primary).
- Each toast has a dismiss button that calls `removeToast(toast.id)`.
- The container has `aria-live="polite"` and `aria-atomic="false"` for screen reader support.

**`fairnsquare-app/src/main/webui/src/lib/components/ui/toast/Toaster.test.ts`**

- 10 automated tests covering all behaviour of the `Toaster` component (see Tests section).

### Files modified

**`fairnsquare-app/src/main/webui/src/App.svelte`**

- Imported `Toaster` and added `<Toaster />` after the closing `</main>` tag, so it sits at the root of the document, unrestricted by the content container's max-width or padding.

---

## 3. Tests

**File:** `src/lib/components/ui/toast/Toaster.test.ts`
**Total tests:** 10 (all passing)

| Test | What it covers |
|---|---|
| renders nothing when there are no toasts | Empty state — container present but no alerts |
| renders a toast message when addToast is called | Single toast rendered after store update |
| renders multiple toasts | Multiple toasts from multiple `addToast` calls |
| shows a dismiss button on each toast | Dismiss button accessibility |
| removes toast when dismiss button is clicked | `removeToast` wired to button click |
| applies success styles for type success | `bg-green-600` class present |
| applies error styles for type error | `bg-destructive` class present |
| applies warning styles for type warning | `bg-yellow-500` class present |
| applies info styles for type info | `bg-primary` class present |
| has aria-live polite on the container | Accessibility attribute on container |