# Feature: web components step 1 — foundations and the Admin page (#162)

Part of the migration to plain web components (#161).

## What, Why and Constraints

The frontend moves from Svelte + Tailwind (built by the Quarkus Web Bundler) to plain web components, as in listit: native ES modules in plain JavaScript, served as static files with no build step, each component with its own Shadow DOM and CSS. The move goes page by page, each step merged and checked on staging, so for a while the Svelte shell displays pages that are already web components.

This first step lays the foundations and moves the smallest standalone page, Admin, to prove the pipeline end to end.

Constraints: same URL (`/admin`), same behaviour — the existing `AdminPageTest` passes unchanged — and the same look.

## How

- **New static root** `fairnsquare-app/src/main/resources/META-INF/resources/`, which Quarkus serves as plain files next to the Svelte bundle. Laid out like listit: `infra/` (no UI), `biz/<page>/` (one folder per page), and later `components/` (shared components).
- `infra/theme.css`: the colour tokens (`:root` and `.dark` variables), moved out of `web/app/app.css`. CSS variables inherit into every Shadow DOM, so components use `var(--primary)` etc.; the Tailwind side still maps them in `app.css` (`@theme inline`).
- `infra/api.js`: the fetch wrapper, same contract as `lib/api/client.ts` (problem details `{type, title, status, detail}`, network error with status 0, non-JSON errors, 204 → undefined).
- `biz/admin/admin-page.js`: `<fns-admin-page>`, replacing `routes/Admin.svelte` (sign in with the admin password kept in sessionStorage, stats, the three split tables, sign out, messages for 401 / 503 / other errors). Rows are built with `textContent`, never `innerHTML`, so server data cannot inject markup.
- `web/index.html` loads `theme.css` and `admin-page.js`; `routes/Admin.svelte` now only renders `<fns-admin-page>` so the Svelte router can show it. `lib/api/admin.ts` is deleted.
- Improvements over the Svelte page:
  - the split tables no longer push the page sideways on a phone (they did: three columns of dates did not fit). On narrow screens the cell padding is tighter, and a table that still does not fit scrolls inside its card;
  - the error message is announced to screen readers (`role="alert"`), and each table section is named by its heading.

## Tests

`AdminPageTest`: 7 tests (2 existing, unchanged, + 5 new):
- `theSignInButtonWaitsForAPassword`: disabled until a password is typed.
- `saysWhenAdminAccessIsNotConfigured`, `showsTheServerMessageOnOtherErrors`: 503 and other errors, with the API answer stubbed by Playwright (`page.route`).
- `theTablesSortTheSplitsByAge`: stubbed stats; checks the totals and the order of the three tables.
- `theStatsFitAPhoneScreen`: signed in at 390×844, no sideways scroll (fails without the fix above).
- The existing sign-out test now also checks that sign-out survives a reload (the token is gone from sessionStorage).

Checked visually with before/after screenshots at 390×844 and 1280×800, signed out and signed in: same look, and the phone tables now fit.
