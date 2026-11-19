# Plain web component shell, Svelte and the Web Bundler removed (#180, step 7 of #161)

## What, Why and Constraints

This is the last step of the move from Svelte to plain web components (#161). Steps 1–6 had already turned every page
into a web component. The Svelte shell was the only part left: `App.svelte`, the sv-router routes, the footer and
`main.ts`, built by the Quarkus Web Bundler with Svelte and Tailwind. It is replaced by a plain `index.html` and a
small `main.js` router, served as they are.

The frontend now has no build step and no npm packages. Node.js also leaves the Docker image build.

Constraints:

- **Same addresses.** `/`, `/splits/<id>`, `/splits/<id>/expenses|participants|settlement` and `/admin` stay the
  same. People share these links, and a reload must open the same page.
- **Same look.** The page width (520 px), the padding and the footer are unchanged; before/after screenshots are
  identical.
- **No caching problem.** The no-cache rule from #178 still applies to every file, now that no file has a hashed
  name.

## How

### New files in `META-INF/resources/`

- **`index.html`**
  - The theme and a few base styles, taken from the old `app.css`: font, colours, focus ring, the page container.
  - A `<main class="page">` where the router puts the current page.
  - A `<footer>` for the build commit, placed outside `<main>` so it has the page-footer (`contentinfo`) role.
  - `<fns-toaster>`.
- **`main.js`**: the router.
  - A table of address patterns, each with its page tag and module. A page module is imported the first time its
    page is shown.
  - On load and on `popstate`, it shows the matching page. `popstate` comes from the back button and from
    `navigate()`. Another page, or another split, gets a new element, and the window scrolls back to the top.
  - A trailing slash is removed from the address. An unknown address is replaced by `/` and shows the home page.
  - When navigations overlap, a page whose module finishes loading late does not replace the newer one.
  - It also fills the footer from `/api/info`.
- **`favicon.svg`**: moved from `web/public/`.

### Server

- **`WebRouter`**: still reroutes every GET that no static file answers (outside `/api` and `/q`) to `/`, which now
  serves this `index.html`. The `/static` exception is gone with the bundle.
- **`application.properties`**:
  - the Web Bundler settings are removed;
  - the no-cache filter loses its `/static/bundle/` exception.

### Removed

- **Svelte code:** the whole `web/` folder, i.e. `index.html`, `App.svelte`, `main.ts`, `app.css`, `router.ts`, the
  routes, the footer, the TypeScript API client, `tsconfig.json`.
- **Maven dependencies:**
  - `quarkus-web-bundler`, `-svelte` and `-tailwindcss` (from both poms);
  - the mvnpm packages `svelte`, `sv-router`, `tw-animate-css`, `clsx`, `tailwind-merge` and `tailwind-variants`.
- **Docker:** the Node.js stage of `docker/Dockerfile.jvm`.
- **Ignore files:** the Node/Svelte/Quinoa entries of `.gitignore` and `.dockerignore`.

### Updated

- **Comments in the web components** that still mentioned Svelte (navigation, toasts, theme, split names store).
- **README:**
  - no Node.js;
  - dev mode serves everything on 8080;
  - where the frontend lives;
  - the full build runs the Playwright tests;
  - the Vite variable is gone.
- **Makefile:**
  - `test-backend` / `test-frontend` select the tests with Surefire (`!**/e2e/**` / `**/e2e/**`) instead of vitest and
    Quinoa;
  - `test-parallel` is gone.
- **`src/doc/rules/frontend-rules.md`:** the Svelte, Tailwind and Vite parts are translated to plain web components
  (CSS instead of utility classes, getters instead of `$derived`, `autofocus` / `focus()` instead of `tick()`, stores
  in `infra/stores/`) or removed where nothing replaces them (sv-router reactivity, `import.meta.env`, Vite dev server,
  Tailwind compilation). No new rule was added.

## Tests

- **`NavigationTest`**: 4 new tests.
  - An unknown address shows the home page at `/`.
  - A trailing slash still opens the page, with its tab marked.
  - Back then forward show their pages.
  - The footer shows the build commit.
- **`StaticCacheTest`**:
  - the no-cache check now covers `main.js`;
  - a new test checks that `/`, a split's address, its expenses and `/admin` all serve the app page.
- **The rest of the Playwright suite** (every page) passes unchanged on the new shell.
- **Before/after screenshots** of the home, split, expenses and admin pages at 390 and 1280 px are identical. The only
  difference is the page height: the old shell was 32 px taller than the window, so it always scrolled a little.
- **The Docker image without Node.js** was not built locally: the server's disk filled up during the build. Dokploy builds `docker/Dockerfile.jvm` when it deploys staging, so the staging deploy checks it.
