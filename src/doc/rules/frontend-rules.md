# Frontend Development Rules

## Testing

- Every frontend change must be covered by automated tests, unless there is a documented reason justifying the exception.
- Exceptions must be recorded in the corresponding feature implementation document (`src/main/doc/implementation/YYYY-MM-DD-*.md`) under the **Tests** section, with a clear explanation of why automated testing was not feasible or applicable.
- Automated tests must be described in the feature implementation document under the **Tests** section, listing test files, test counts, and what each group of tests covers.
- Frontend tests are Playwright end-to-end tests in `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/e2e/`, extending `E2eTestBase`. They run in `mvn package`; no Node toolchain is needed.
- Locate elements only by role, label or exact text (`button(...)`, `field(...)`, `text(...)`, `dialog(...)`, `toast(...)` helpers) — never by CSS classes, `data-testid` or framework markup. The tests must keep passing when the rendering framework changes.
- Use exact matching: `getByLabel("Nights")` also matches an "Info about Nights" button, and `getByText("Groceries")` also matches a toast repeating it. When two elements share a name (two "Cancel" buttons), scope the locator to the dialog or region (`button(dialog("…"), "Cancel")`).
- Set up test data (splits, participants, expenses) through the REST API helpers, and drive through the UI only what the test is about. Each test creates its own split; never rely on data from another test.
- Assert with Playwright's `assertThat(locator)`, which waits for the UI to update. Never assert right after an action with a one-shot read (`isVisible()`, `textContent()`).
- A page that changes layout must keep `LayoutTest` passing: no sideways scrolling at 390×844 and 1280×800. Add realistic long values (names, descriptions) when a page shows user text.
- When a test fails, its Playwright trace is in `fairnsquare-app/target/playwright-traces/` (CI: the `playwright-traces` artifact). Open it before guessing at the cause.

## Accessible Names

- Every button that shows only an icon must have an `aria-label` describing its action, including the item it acts on when there are several (e.g. `Edit Alice`, `Delete expense: Groceries`). Tests and screen readers both rely on it.
- Every form field must have a `<label for>` (or `aria-label`), and every modal must be `role="dialog"` with `aria-labelledby` pointing at its title.
- Custom pickers (dropdowns that are not a native `<select>`) must expose the listbox pattern: the trigger has `aria-haspopup="listbox"` and `aria-expanded`, the list has `role="listbox"` and each choice `role="option"` with `aria-selected`, and the arrow keys move between choices.

## Numeric Inputs

- For `<input type="number">` fields where the desired arrow-key step differs from what is acceptable as a manually typed value, use `step="any"` to allow free manual entry and implement the desired arrow-key step via an `onkeydown` handler. Do not use `step="N"` when it would silently restrict valid manually-typed values (e.g., `step="0.5"` rejects `12.30`), even if it produces the correct arrow-key increment.

## Form Validation

- Forms that create or update entities must include client-side validation that mirrors backend constraints (e.g. uniqueness, format, range). Do not rely solely on API error responses and toast notifications for user feedback — use inline form errors.
- Inline validation while typing must not show "required" errors on an empty field — that error is reserved for form submission. Other errors (format, range, uniqueness) must appear as soon as the invalid value is detected while typing.

## Dynamic Forms

- When a form is revealed dynamically (e.g. an add form shown by a button), the first input must receive focus automatically: call `focus()` once it is shown; in a `<dialog>`, put `autofocus` on it so `showModal()` focuses it.

## Responsive Layout

- In flex rows that mix variable-width content (names, text) with fixed-width content (action buttons, badges), the variable-width element must have `flex: 1; min-width: 0` and be truncated (`overflow: hidden; text-overflow: ellipsis; white-space: nowrap`) to prevent overflow on narrow screens. Fixed-width elements must have `flex: none`.
- When a row risks crowding (e.g. name + multiple badges + multiple buttons), split it into separate rows rather than stacking everything horizontally.

## Shared Components with Page-Specific Variants

- When a shared component needs to display differently depending on context (e.g. a summary card that shows a title on one page but not another), use a boolean attribute rather than duplicating the component or adding conditional logic in each parent.
- Name the attribute after what it changes (`no-title`, `compact`, etc.): without it, the component shows its richer/fuller display, so existing usages are unaffected.

## localStorage Utilities

- localStorage access must be encapsulated in a dedicated module in `infra/stores/` (e.g. `infra/stores/nights-default.js`) rather than called inline in components. The module must export named `save*`, `load*`, and `clear*` functions. The localStorage key must be a private constant inside that module.
- End-to-end tests run in a fresh browser context, so localStorage starts empty in every test; set needed values with `context.addInitScript` (as `E2eTestBase` does for the CAPTCHA token).

## Action Button Placement

- When a page has a primary action button (e.g. Resolve, Add Participant, Add Expense), place it **after the header, before the content list or cards it acts upon**. This ensures the action is reachable without scrolling and is visually consistent across pages. Do not place primary action buttons below a list of items.

## Read-Only UI State for Locked Resources

- When a resource can be in a locked/read-only state (e.g. a settled split), derive the locked flag from the loaded resource data (e.g. a `get settled()` getter) rather than a separate state variable.
- Hide action buttons (Add, Edit, Delete) while locked — never just disable them. Hidden actions cannot be accidentally triggered and avoid misleading affordances.
- Show an amber banner that explains why editing is unavailable and provides a direct link/button to the action that would unlock the resource.
- Keep the locked-state check in a single getter per page and reference it everywhere on that page — do not duplicate the condition inline.

## Capturing Transient State Before Reset

- When post-action feedback (e.g. a toast) needs to reference form state that is reset immediately after the API call, always capture those values into local `const` variables *before* the reset. Do not read from form state after it has been cleared — the values will reflect the reset defaults rather than the submitted data.
- Example: if the nights field is reset to `1` before `showToast()` is called, capture it first: `const addedNights = nights;`.

## Page Load Must Not Trigger Mutations

- Code that runs when a page loads must only call safe read (GET) API methods. Never call a mutation-side-effect API (POST, PUT, DELETE) on page load, even if the operation appears idempotent — it can silently undo intentional prior user actions (e.g., calling a resolve POST on load immediately re-persists a settlement the user had just deleted via Unsettle). Derive UI state from already-loaded data, or use a dedicated GET endpoint.

## Error Message Lifecycle in Async Load Functions

- When a component function resets state and then awaits an async call (e.g., `loadChallenge` after a wrong answer), do not clear `errorMessage` at the top of the function. Clear it only in the success branch, after the await resolves. Clearing it at the top hides the error before the person has had a chance to read it — the user never sees it and tests cannot assert it. Always pattern: set error → call `loadFn` → inside `loadFn`, clear error on success.

## sessionStorage for Short-Lived Auth Tokens

- When storing a short-lived authentication credential (e.g. an admin token, a session token) in browser storage, prefer `sessionStorage` over `localStorage`. `sessionStorage` is cleared automatically when the browser tab is closed, limiting exposure on shared machines.
- The storage key must be a module-level constant (or defined in the component that owns it).

## Browser Testing Before Proposing

- Before marking any UI feature as complete, verify it visually in the browser on **both mobile and desktop** viewports (390×844 and 1280×800), e.g. with Playwright screenshots. `LayoutTest` catches sideways overflow, but clipping, modal sizing and truncation are only caught by looking.