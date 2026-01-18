# Story 1.4: Add shadcn-svelte Components

Status: done

## Story

As a **developer**,
I want **shadcn-svelte UI components installed and configured**,
So that **I can build accessible, consistent UI components with the design system**.

## Acceptance Criteria

1. **Given** the Svelte 5 frontend from Story 1.2 **When** shadcn-svelte is initialized **Then** the CLI completes without errors
2. **And** base components are available: Button, Input, Card, Label
3. **And** components use the project's CSS custom properties for theming
4. **And** components follow the UX spec color palette (Teal primary #0D9488)
5. **And** all components are accessible (keyboard navigation, focus states)
6. **And** components work with Svelte 5 runes syntax
7. **And** the project builds successfully with `npm run build`

## Tasks / Subtasks

- [x] Task 1: Initialize shadcn-svelte (AC: 1)
  - [x] 1.1: Install shadcn-svelte CLI dependencies
  - [x] 1.2: Run shadcn-svelte init command
  - [x] 1.3: Configure for Svelte 5 compatibility
- [x] Task 2: Configure theming (AC: 3, 4)
  - [x] 2.1: Update shadcn theme to use project CSS variables
  - [x] 2.2: Map shadcn colors to UX spec palette
  - [x] 2.3: Verify theme consistency with existing styles
- [x] Task 3: Add core components (AC: 2, 5, 6)
  - [x] 3.1: Add Button component
  - [x] 3.2: Add Input component
  - [x] 3.3: Add Card component
  - [x] 3.4: Add Label component
  - [x] 3.5: Verify accessibility (focus states, keyboard nav)
- [x] Task 4: Verify build and integration (AC: 7)
  - [x] 4.1: Run `npm run build` successfully
  - [x] 4.2: Run `mvn test` successfully (11 tests passing)
  - [x] 4.3: Update Home.svelte to use new components

## Dev Notes

### shadcn-svelte Overview

shadcn-svelte is NOT a component library - it's a collection of reusable components you copy into your project. Key benefits:
- Full ownership and control over the code
- Accessible by default (WAI-ARIA compliant)
- Built on Tailwind CSS (already configured)
- Works with Svelte 5 runes

### UX Spec Color Mapping

| UX Role | UX Color | shadcn Variable |
|---------|----------|-----------------|
| Primary | #0D9488 | --primary |
| Primary Light | #14B8A6 | --primary-foreground |
| Secondary | #475569 | --secondary |
| Danger | #DC2626 | --destructive |
| Background | #F8FAFC | --background |
| Surface | #FFFFFF | --card |
| Text | #1E293B | --foreground |
| Border | #E2E8F0 | --border |

### Component Requirements (from Architecture)

```
src/main/webui/src/lib/components/ui/
├── Button.svelte      (exists - replace with shadcn)
├── Input.svelte       (new)
├── Card.svelte        (new)
├── Label.svelte       (new)
└── ...
```

### References

- [shadcn-svelte docs](https://shadcn-svelte.com)
- [Source: ux-design-specification.md#Color-System] - Theme colors
- [Source: architecture.md#Frontend-Structure] - Component organization

## Dev Agent Record

### Agent Model Used

Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References

### Completion Notes List

- Initialized shadcn-svelte v1.1.1 with slate base color
- Added `$lib` path alias configuration in tsconfig.json and vite.config.ts for component imports
- Updated app.css with FairNSquare themed colors (Teal primary #0D9488, Slate secondary #475569)
- Colors mapped to oklch format for modern CSS color space support
- Added custom semantic colors (success, warning, danger) to theme
- Installed shadcn components: Button, Input, Card, Label using CLI
- All components use Svelte 5 runes syntax ($props(), $bindable())
- Removed old Button.svelte in favor of shadcn version
- Updated Home.svelte to demonstrate all new components
- Dependencies added: bits-ui, tailwind-variants, tailwind-merge, clsx, tw-animate-css, @lucide/svelte, @internationalized/date
- All 11 tests passing (3 HealthCheck, 4 OpenAPI, 4 OpenTelemetry)
- Frontend build successful (82.92 kB JS, 22.32 kB CSS gzipped)

**Code Review Fixes Applied (2026-01-18):**
- H1: Removed stale tailwind.config.js (Tailwind v4 uses @theme inline in app.css)
- H2: Added missing --destructive-foreground CSS variable in both light and dark themes
- M1: Fixed vite.config.ts to use import.meta.url for robust path resolution
- L1: Removed global min-height: 44px rule that conflicted with shadcn component sizing

### File List

- `fairnsquare-app/src/main/webui/components.json` - shadcn-svelte configuration
- `fairnsquare-app/src/main/webui/tsconfig.json` - Updated with $lib path alias
- `fairnsquare-app/src/main/webui/tsconfig.app.json` - Updated with $lib path alias and moduleResolution
- `fairnsquare-app/src/main/webui/vite.config.ts` - Updated with $lib resolve alias
- `fairnsquare-app/src/main/webui/package.json` - Updated with shadcn dependencies
- `fairnsquare-app/src/main/webui/src/app.css` - Updated with FairNSquare themed shadcn CSS variables
- `fairnsquare-app/src/main/webui/src/lib/utils.ts` - shadcn utility functions (cn, type helpers)
- `fairnsquare-app/src/main/webui/src/lib/components/ui/button/` - shadcn Button component
- `fairnsquare-app/src/main/webui/src/lib/components/ui/input/` - shadcn Input component
- `fairnsquare-app/src/main/webui/src/lib/components/ui/card/` - shadcn Card component (with subcomponents)
- `fairnsquare-app/src/main/webui/src/lib/components/ui/label/` - shadcn Label component
- `fairnsquare-app/src/main/webui/src/lib/hooks/` - shadcn hooks directory (empty)
- `fairnsquare-app/src/main/webui/src/routes/Home.svelte` - Updated to use shadcn components
