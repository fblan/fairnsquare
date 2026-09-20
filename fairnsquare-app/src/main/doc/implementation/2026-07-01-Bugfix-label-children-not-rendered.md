# Bugfix: Label component text content not rendered

## What, Why and Constraints

Fixed `label.svelte` to properly render its children (the label text).

**Root cause:** During the bits-ui removal, `LabelPrimitive.Root` was replaced with a plain `<label>` element but the template used the self-closing form `<label {...restProps} />` without a `{@render children?.()}` call. Svelte 5 compiles this to `from_html('<label></label>')` — an empty label. The `children` snippet passed by callers (e.g. `<Label>Split Name</Label>`) was silently discarded, producing `<label></label>` in the DOM with no text content. Every form field label in the app was affected: "Split Name", "Name", "Nights", "Members", "Password" were all invisible.

**Constraint:** In Svelte 5, native HTML elements do not automatically render a `children` snippet from props — that must be done explicitly with `{@render children?.()}` inside the element.

## How

### Files modified

- **`src/main/resources/web/app/lib/components/ui/label/label.svelte`**
  - Added `import type { Snippet } from 'svelte'`
  - Extracted `children` from `$props()` (typed as `Snippet`) so it is excluded from `restProps` (and therefore not incorrectly applied as a DOM attribute)
  - Changed `<label ... />` to `<label ...>{@render children?.()}</label>`

The compiled output changed from `from_html('<label></label>')` to `from_html('<label><!></label>')`, confirming the slot anchor is now present and the children snippet renders correctly.

## Tests

No automated tests added. Manual verification:
- After hot-reload, the bundle shows `root8 = from_html('<label><!></label>')` and the `Label` function calls `snippet(node, () => $$props.children ?? noop)` — confirming children render.
- Form field labels ("Split Name", "Name", "Nights", "Password") are now visible on all affected pages (Home, Participants, Admin).
