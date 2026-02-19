# Frontend Development Rules

## Testing

- Every frontend change must be covered by automated tests, unless there is a documented reason justifying the exception.
- Exceptions must be recorded in the corresponding feature implementation document (`src/main/doc/implementation/YYYY-MM-DD-*.md`) under the **Tests** section, with a clear explanation of why automated testing was not feasible or applicable.
- Automated tests must be described in the feature implementation document under the **Tests** section, listing test files, test counts, and what each group of tests covers.

## Form Validation

- Forms that create or update entities must include client-side validation that mirrors backend constraints (e.g. uniqueness, format, range). Do not rely solely on API error responses and toast notifications for user feedback — use inline form errors.
- Inline validation while typing must not show "required" errors on an empty field — that error is reserved for form submission. Other errors (format, range, uniqueness) must appear as soon as the invalid value is detected while typing.

## Dynamic Forms

- When a form is revealed dynamically (e.g. via `{#if}`), the first input must receive focus automatically. Use `bind:ref` + `await tick()` to focus programmatically after the DOM updates. Do not use the `use:` directive on Svelte components as it is not supported.