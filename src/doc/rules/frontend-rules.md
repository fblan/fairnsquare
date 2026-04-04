# Frontend Development Rules

## Testing

- Every frontend change must be covered by automated tests, unless there is a documented reason justifying the exception.
- Exceptions must be recorded in the corresponding feature implementation document (`src/main/doc/implementation/YYYY-MM-DD-*.md`) under the **Tests** section, with a clear explanation of why automated testing was not feasible or applicable.
- Automated tests must be described in the feature implementation document under the **Tests** section, listing test files, test counts, and what each group of tests covers.

## Numeric Inputs

- For `<input type="number">` fields where the desired arrow-key step differs from what is acceptable as a manually typed value, use `step="any"` to allow free manual entry and implement the desired arrow-key step via an `onkeydown` handler. Do not use `step="N"` when it would silently restrict valid manually-typed values (e.g., `step="0.5"` rejects `12.30`), even if it produces the correct arrow-key increment.

## Form Validation

- Forms that create or update entities must include client-side validation that mirrors backend constraints (e.g. uniqueness, format, range). Do not rely solely on API error responses and toast notifications for user feedback — use inline form errors.
- Inline validation while typing must not show "required" errors on an empty field — that error is reserved for form submission. Other errors (format, range, uniqueness) must appear as soon as the invalid value is detected while typing.

## Testing Reactive Store Updates

- When a reactive store is mutated (e.g. `addToast()`, `clearToasts()`) after `render()` in a test, always wrap assertions in `waitFor()`. Svelte 5's DOM updates are batched and asynchronous from the test runner's perspective — synchronous assertions after store mutations will see stale DOM.

## Testing Number Inputs

- For `<input type="number">` with Svelte's `bind:value`, always use `fireEvent.input(el, { target: { value: '...' } })` instead of `userEvent.clear()` + `userEvent.type()`. The `userEvent` approach does not reliably trigger Svelte's `bind:value` reactivity for number inputs in jsdom. Use `fireEvent.blur()` to trigger `onblur` validation handlers.

## External Object Reactivity

- When using values from external objects (e.g., `route.search` from sv-router), use `$state` initialized from the external value rather than `$derived`. External objects are not tracked by Svelte 5's fine-grained reactivity — mutations to them will not trigger re-renders. Update the local `$state` directly in event handlers alongside any URL/external sync (e.g., `history.replaceState`).

## Dynamic Forms

- When a form is revealed dynamically (e.g. via `{#if}`), the first input must receive focus automatically. Use `bind:ref` + `await tick()` to focus programmatically after the DOM updates. Do not use the `use:` directive on Svelte components as it is not supported.

## Responsive Layout

- In flex rows that mix variable-width content (names, text) with fixed-width content (action buttons, badges), the variable-width element must have `flex-1 min-w-0` and `truncate` to prevent overflow on narrow screens. Fixed-width elements must have `flex-none` or `shrink-0`.
- When a row risks crowding (e.g. name + multiple badges + multiple buttons), split it into separate rows rather than stacking everything horizontally.

## Vite Dev Server Configuration

- Developer-specific hostnames (tunnel URLs, etc.) must not be hardcoded in `vite.config.ts`. Use the `VITE_ALLOWED_HOSTS` env var read from `process.env`, falling back to `undefined` for Vite's default behavior. Local values go in `src/main/webui/.env.local` (gitignored via `*.local`).

## Shared Components with Page-Specific Variants

- When a shared component needs to display differently depending on context (e.g. a summary card that shows a title on one page but not another), use a boolean prop with a sensible default rather than duplicating the component or adding conditional logic in each parent.
- Name the prop to describe what it controls (`showTitle`, `compact`, etc.) and default it to the richer/fuller display so existing usages are unaffected.

## localStorage Utilities

- localStorage access must be encapsulated in a dedicated utility module (e.g. `src/lib/stores/myStore.ts`) rather than called inline in components. The module must export named `save*`, `load*`, and `clear*` functions. The localStorage key must be a private constant inside that module.
- The module must be mocked in component tests (`vi.mock('$lib/stores/myStore')`). Tests for the utility itself must use `localStorage.clear()` in `beforeEach`.
- When loading a persisted value on component mount triggers an async operation (e.g. API verification), use `$effect` and handle the async result with `.then()/.catch()` rather than making `$effect` async.

## Action Button Placement

- When a page has a primary action button (e.g. Resolve, Add Participant, Add Expense), place it **after the header, before the content list or cards it acts upon**. This ensures the action is reachable without scrolling and is visually consistent across pages. Do not place primary action buttons below a list of items.

## Read-Only UI State for Locked Resources

- When a resource can be in a locked/read-only state (e.g. a settled split), derive the locked flag reactively using `$derived` from the resource data rather than a separate state variable.
- Gate action buttons (Add, Edit, Delete) with `{#if !isLocked}` — never just disable them. Hidden actions cannot be accidentally triggered and avoid misleading affordances.
- Show an amber banner that explains why editing is unavailable and provides a direct link/button to the action that would unlock the resource.
- Keep the locked-state check in a single `$derived` per page and reference it everywhere on that page — do not duplicate the condition inline.

## Capturing Transient State Before Reset

- When post-action feedback (e.g. a toast) needs to reference form state that is reset immediately after the API call, always capture those values into local `const` variables *before* the reset. Do not read from form state after it has been cleared — the values will reflect the reset defaults rather than the submitted data.
- Example: if `formNights` is reset to `1` before `addToast()` is called, capture it first: `const addedNights = formNights;`.

## Page Load Must Not Trigger Mutations

- `$effect` handlers that run on page load must only call safe read (GET) API methods. Never call a mutation-side-effect API (POST, PUT, DELETE) on page load, even if the operation appears idempotent — it can silently undo intentional prior user actions (e.g., calling a resolve POST on load immediately re-persists a settlement the user had just deleted via Unsettle). Derive UI state from already-loaded data, or use a dedicated GET endpoint.

## Error Message Lifecycle in Async Load Functions

- When a component function resets state and then awaits an async call (e.g., `loadChallenge` after a wrong answer), do not clear `errorMessage` at the top of the function. Clear it only in the success branch, after the await resolves. Clearing it synchronously at the top batches the reset with the other state changes before the first `await`, so the DOM never shows the error — breaking both UX (error flashes invisibly) and `waitFor` assertions in tests. Always pattern: set error → call `loadFn` → inside `loadFn`, clear error on success.

## Mutable Mock Objects in `vi.mock` Factories

- When a `vi.mock` factory needs to reference a variable that is mutated between tests (e.g. `route.pathname` set in `beforeEach`), define it with `vi.hoisted(() => ({ ... }))` rather than a top-level `const`. Top-level variables are not yet initialized when `vi.mock` is hoisted, causing a `ReferenceError` at runtime.

## Scoping Queries When Multiple Elements Share the Same Label

- When a component renders multiple buttons or elements with the same accessible name (e.g. two "Cancel" buttons — one in the modal footer, one inside a sub-banner), always scope `getByRole` queries using `within(container)` to avoid ambiguity errors. Use `within(screen.getByRole('alert'))`, `within(screen.getByRole('dialog'))`, or any distinct ARIA landmark to narrow the query to the relevant region.

## sessionStorage for Short-Lived Auth Tokens

- When storing a short-lived authentication credential (e.g. an admin token, a session token) in browser storage, prefer `sessionStorage` over `localStorage`. `sessionStorage` is cleared automatically when the browser tab is closed, limiting exposure on shared machines.
- The storage key must be a module-level constant (or defined in the component that owns it).
- In tests, call `sessionStorage.clear()` in `beforeEach` to prevent token bleed between tests.

## `getByText` Ambiguity from `<select>` Options

- When entity names appear both as rendered text nodes AND as `<option>` values in a `<select>`, `getByText('Name')` will throw due to multiple matches. Use `getAllByText` when only testing presence, or use more specific queries (`getByRole('heading', { name: '...' })`, `getByRole('combobox', { name: '...' })`).