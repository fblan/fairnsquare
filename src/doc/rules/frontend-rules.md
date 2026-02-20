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

## Dynamic Forms

- When a form is revealed dynamically (e.g. via `{#if}`), the first input must receive focus automatically. Use `bind:ref` + `await tick()` to focus programmatically after the DOM updates. Do not use the `use:` directive on Svelte components as it is not supported.

## Responsive Layout

- In flex rows that mix variable-width content (names, text) with fixed-width content (action buttons, badges), the variable-width element must have `flex-1 min-w-0` and `truncate` to prevent overflow on narrow screens. Fixed-width elements must have `flex-none` or `shrink-0`.
- When a row risks crowding (e.g. name + multiple badges + multiple buttons), split it into separate rows rather than stacking everything horizontally.

## Shared Components with Page-Specific Variants

- When a shared component needs to display differently depending on context (e.g. a summary card that shows a title on one page but not another), use a boolean prop with a sensible default rather than duplicating the component or adding conditional logic in each parent.
- Name the prop to describe what it controls (`showTitle`, `compact`, etc.) and default it to the richer/fuller display so existing usages are unaffected.