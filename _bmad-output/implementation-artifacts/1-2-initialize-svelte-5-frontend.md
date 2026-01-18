# Story 1.2: Initialize Svelte 5 Frontend

Status: done

## Story

As a **developer**,
I want **a Svelte 5 frontend with TypeScript and Tailwind CSS configured**,
So that **I can build the mobile-first UI with the design system tokens**.

## Acceptance Criteria

1. **Given** the Quarkus project from Story 1.1 **When** the Svelte 5 frontend is initialized in `src/main/webui/` **Then** `npm install` completes without errors
2. **And** Svelte 5 with runes is configured (not legacy mode)
3. **And** TypeScript is enabled with strict mode
4. **And** Tailwind CSS is installed and configured
5. **And** CSS custom properties are defined for theming (primary: #0D9488, etc. per UX spec)
6. **And** the frontend directory structure matches Architecture: `lib/`, `routes/`, `App.svelte`, `main.ts`
7. **And** running `mvn quarkus:dev` serves the frontend at the root path
8. **And** hot reload works for both Java and Svelte changes

## Tasks / Subtasks

- [x] Task 1: Initialize Svelte 5 project with Vite (AC: 1, 2, 3)
  - [x] 1.1: Create `src/main/webui/` directory structure
  - [x] 1.2: Initialize Vite + Svelte 5 + TypeScript project
  - [x] 1.3: Verify Svelte 5 runes mode is enabled (not legacy)
  - [x] 1.4: Configure TypeScript with strict mode
  - [x] 1.5: Run `npm install` and confirm no errors
- [x] Task 2: Configure Tailwind CSS (AC: 4)
  - [x] 2.1: Install Tailwind CSS, PostCSS, and Autoprefixer
  - [x] 2.2: Create tailwind.config.js with UX design tokens
  - [x] 2.3: Create postcss.config.js
  - [x] 2.4: Import Tailwind directives in app.css
- [x] Task 3: Define CSS custom properties for theming (AC: 5)
  - [x] 3.1: Create theme variables file with primary/secondary/semantic colors
  - [x] 3.2: Integrate theme with Tailwind config
  - [x] 3.3: Verify white-label readiness (CSS variables not hardcoded values)
- [x] Task 4: Create frontend directory structure (AC: 6)
  - [x] 4.1: Create `lib/` directory with subdirectories: `components/`, `stores/`, `api/`, `utils/`
  - [x] 4.2: Create `routes/` directory
  - [x] 4.3: Update App.svelte with basic structure
  - [x] 4.4: Verify main.ts entry point is correct
- [x] Task 5: Verify Quinoa integration (AC: 7, 8)
  - [x] 5.1: Run `mvn quarkus:dev` and verify frontend serves at root
  - [x] 5.2: Test hot reload for Svelte changes
  - [x] 5.3: Test hot reload for Java changes
  - [x] 5.4: Verified Quinoa integration works (Quinoa disabled in test mode by design)

## Dev Notes

### Critical Architecture Requirements

**From Architecture Document:**
- Frontend source location: `src/main/webui/` (Quinoa default)
- This is **vanilla Svelte 5**, NOT SvelteKit - no `+page.svelte` or file-based routing
- Use Svelte 5 runes (`$state`, `$derived`, `$effect`)
- Use `$props()` for component props, never `export let` (Svelte 4 syntax)
- Configure Quinoa SPA fallback: `quarkus.quinoa.enable-spa-routing=true`

**Frontend Structure (from Architecture):**
```
src/main/webui/src/
├── lib/
│   ├── components/        # Reusable UI components
│   ├── stores/            # Svelte 5 state (.svelte.ts)
│   ├── api/               # API client functions
│   └── utils/             # Helper functions
├── routes/                # Page components (sv-router)
├── App.svelte             # Root component
└── main.ts                # Entry point
```

### Color System (from UX Specification)

**Primary Palette:**
| Role | Hex |
|------|-----|
| Primary | `#0D9488` |
| Primary Light | `#14B8A6` |
| Primary Dark | `#0F766E` |
| Secondary | `#475569` |
| Secondary Light | `#64748B` |

**Semantic Colors:**
| Role | Hex |
|------|-----|
| Success | `#16A34A` |
| Success Light | `#22C55E` |
| Warning | `#F59E0B` |
| Warning Light | `#FBBF24` |
| Danger | `#DC2626` |
| Danger Light | `#EF4444` |

**Neutral Colors:**
| Role | Hex |
|------|-----|
| Background | `#F8FAFC` |
| Surface | `#FFFFFF` |
| Text | `#1E293B` |
| Text Muted | `#64748B` |
| Border | `#E2E8F0` |

### Project Context Rules (MUST FOLLOW)

- Use Svelte 5 runes syntax, never legacy `let` reactivity
- White-label: Use CSS custom properties for all themeable values
- Never hardcode colors or brand-specific values in component styles
- `mvn quarkus:dev` proxies frontend requests to Vite dev server
- Do NOT start Vite separately - Quinoa manages it

### Vite Command

```bash
cd fairnsquare-app/src/main/webui
npm create vite@latest . -- --template svelte-ts
```

**Note:** Use `.` to create in current directory, use `svelte-ts` template for TypeScript support.

### References

- [Source: architecture.md#Frontend-Structure] - Directory organization
- [Source: ux-design-specification.md#Color-System] - Theme colors
- [Source: project-context.md#Quinoa-Rules] - Integration rules
- [Source: epics.md#Story-1.2] - Acceptance criteria

## Dev Agent Record

### Agent Model Used

Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References

### Completion Notes List

- Initialized Svelte 5.43.8 with Vite 7.3.1 using `svelte-ts` template
- Configured TypeScript with strict mode (`strict`, `noImplicitAny`, `strictNullChecks`, `strictFunctionTypes`)
- Installed Tailwind CSS v4 with `@tailwindcss/postcss` plugin (new in v4)
- Created CSS custom properties for full UX color palette in app.css
- Tailwind config references CSS variables for white-label support
- Created directory structure: lib/components/ui, lib/stores, lib/api, lib/utils, routes
- Created placeholder files: Button.svelte, toastStore.svelte.ts, client.ts, formatCurrency.ts
- Created Home.svelte route with Svelte 5 runes syntax (`$state`)
- Verified Quinoa integration: `mvn quarkus:dev` starts Vite dev server on port 5173
- Frontend served at http://localhost:8080 with hot reload working
- Note: Quinoa is disabled in test mode by design (Quarkus behavior)

### File List

- `fairnsquare-app/src/main/webui/package.json` - NPM package configuration
- `fairnsquare-app/src/main/webui/package-lock.json` - NPM lock file
- `fairnsquare-app/src/main/webui/vite.config.ts` - Vite configuration
- `fairnsquare-app/src/main/webui/svelte.config.js` - Svelte preprocessor config
- `fairnsquare-app/src/main/webui/tsconfig.json` - TypeScript root config
- `fairnsquare-app/src/main/webui/tsconfig.app.json` - TypeScript app config with strict mode
- `fairnsquare-app/src/main/webui/tsconfig.node.json` - TypeScript node config
- `fairnsquare-app/src/main/webui/tailwind.config.js` - Tailwind CSS config with theme tokens
- `fairnsquare-app/src/main/webui/postcss.config.js` - PostCSS config with Tailwind plugin
- `fairnsquare-app/src/main/webui/index.html` - HTML entry point
- `fairnsquare-app/src/main/webui/src/main.ts` - App entry point
- `fairnsquare-app/src/main/webui/src/App.svelte` - Root Svelte component
- `fairnsquare-app/src/main/webui/src/app.css` - Global styles with CSS custom properties
- `fairnsquare-app/src/main/webui/src/routes/Home.svelte` - Home page component
- `fairnsquare-app/src/main/webui/src/lib/components/ui/Button.svelte` - Reusable button component
- `fairnsquare-app/src/main/webui/src/lib/stores/toastStore.svelte.ts` - Toast notification store
- `fairnsquare-app/src/main/webui/src/lib/api/client.ts` - API client wrapper
- `fairnsquare-app/src/main/webui/src/lib/utils/formatCurrency.ts` - Currency formatting utility