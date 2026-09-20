# Refactor: Migrate from Quinoa to Quarkus Web Bundler

## What, Why and Constraints

Replaced the `quarkus-quinoa` extension (which proxies to an external Vite/npm process) with `quarkus-web-bundler` (which bundles JS/CSS natively inside the Quarkus Maven build using esbuild). The Svelte extension `quarkus-web-bundler-svelte` handles `.svelte` compilation, and the separate `quarkus-web-bundler-tailwindcss` extension handles Tailwind CSS 4 processing before esbuild runs.

**Motivation:** Eliminate the separate Node.js build process. Web Bundler integrates the frontend build directly into `mvn package`, with npm packages managed as Maven dependencies via [mvnpm](https://mvnpm.org).

**Constraints:**
- `lucide-svelte` and `bits-ui` are not published on mvnpm and could not be used as Maven dependencies. `lucide-svelte` was replaced with 24 local Svelte icon components derived from its installed dist. `bits-ui` was replaced with a plain HTML `<label>` element (it was only used in one file for its Label primitive).
- Web Bundler does not support running JavaScript tests (vitest). `WebUITest.java` was removed. Frontend unit/component tests remain in the old test files but are no longer executed by the Maven build. E2E testing (e.g. Playwright) is the recommended path forward.
- Web Bundler has no built-in SPA routing config; a Vert.x `WebRouter` catch-all was added to the Java layer.
- `quarkus.web-bundler.svelte.custom-element=false` is required to use Svelte in SPA/mount mode rather than Web Components mode.
- TailwindCSS 4 requires a **separate** extension `quarkus-web-bundler-tailwindcss` — it is not included in the base `quarkus-web-bundler` extension. Without it, esbuild cannot resolve `@import "tailwindcss"` (the package only exports via the `"style"` condition which esbuild doesn't activate for CSS). The tailwindcss extension pre-processes the CSS through Tailwind's engine before esbuild runs, and also handles `@import "tw-animate-css"` correctly via the `"style"` condition.

## How

### Files deleted
- `fairnsquare-app/src/main/webui/` — entire old Vite frontend directory (Vite config, svelte.config.js, postcss.config.js, tsconfig files, package.json, node_modules, source)
- `src/test/java/.../WebUITest.java` — Quinoa test profile runner (not compatible with Web Bundler)

### Files created
- `src/main/resources/web/index.html` — Qute template replacing the Vite index.html; uses `{#bundle /}` to inject bundled script/style tags
- `src/main/resources/web/tsconfig.json` — TypeScript config for esbuild; defines the `$lib` path alias pointing to `app/lib/`
- `src/main/resources/web/app/**` — all frontend source files migrated from `src/main/webui/src/`
- `src/main/resources/web/public/favicon.svg` — static asset
- `src/main/resources/web/app/lib/icons/Icon.svelte` — base SVG icon component (port of lucide-svelte's Icon.svelte)
- `src/main/resources/web/app/lib/icons/{AlertTriangle,ArrowLeftRight,...,X}.svelte` — 24 local icon components with SVG paths extracted from the lucide-svelte dist
- `src/main/resources/web/app/lib/icons/index.ts` — re-exports all 24 named icons for drop-in import compatibility
- `src/main/java/.../infrastructure/web/WebRouter.java` — Vert.x catch-all handler for HTML5 history routing (reroutes non-API paths to `/`)

### Files modified
- `pom.xml` (root) — replaced `quarkus-quinoa.version` property and dependencyManagement entries with `quarkus-web-bundler` equivalents
- `fairnsquare-app/pom.xml` — replaced quinoa runtime/test deps with `quarkus-web-bundler`, `quarkus-web-bundler-svelte`, and six mvnpm runtime deps (`svelte`, `sv-router`, `tw-animate-css`, `clsx`, `tailwind-merge`, `tailwind-variants`); added mvnpm Maven repository
- `src/main/resources/application.properties` — replaced `quarkus.quinoa.*` properties with `quarkus.web-bundler.svelte.custom-element=false` and `quarkus.web-bundler.tailwindcss.pattern`; removed `localhost:5173` from dev CORS origins (no separate dev server)
- `src/main/resources/web/app/lib/components/ui/label/label.svelte` — removed `bits-ui` dependency; replaced `LabelPrimitive.Root` with a plain `<label>` and `LabelPrimitive.RootProps` type with `HTMLLabelAttributes`
- 9 Svelte files with `lucide-svelte` imports — import path changed from `'lucide-svelte'` to `'$lib/icons'`

## Tests

No automated tests added. Manual verification required:
- `mvn package` in `fairnsquare-app/` builds without errors (Web Bundler downloads mvnpm packages and runs esbuild)
- Application starts and serves the SPA at `http://localhost:8080`
- Client-side routing (sv-router) works across all routes (Home, Split, ExpenseList, Participants, Settlement, Admin)
- Icons render correctly in all components
- Form labels are functional (bits-ui replacement)
- Tailwind CSS styles apply correctly (theme, dark mode variables)
