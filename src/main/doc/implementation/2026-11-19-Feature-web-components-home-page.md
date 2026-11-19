# Feature: web components step 2 — Home page and CAPTCHA dialog (#164)

Part of the migration to plain web components (#161).

## What, Why and Constraints

Moves the home page and the CAPTCHA dialog to web components, and brings the shared pieces the remaining pages need: navigation, toasts, the local stores, icons and a shared stylesheet.

Constraints: same URL (`/`), behaviour and look; `HomePageTest` and `CaptchaTest` pass unchanged. While the Svelte shell remains, Svelte pages and web components must share navigation and toasts.

## How

New files under `fairnsquare-app/src/main/resources/META-INF/resources/`:
- `infra/navigation.js`: `navigate(path)` pushes the address to the history and fires `popstate`. sv-router listens to `popstate` (checked in its sources), so the Svelte pages follow; the plain router of step 7 will listen to the same event.
- `infra/toasts.js` + `components/toaster.js`: `showToast()` sends a `fns-toast` event on `window`; `<fns-toaster>` shows it (same look, `role="alert"`, Dismiss, 5 s). The stack is a popover, so a toast shown while a dialog is open appears above it. The Svelte `addToast()` (`toastStore.svelte.ts`) now only sends the same event; `Toaster.svelte` is gone and `App.svelte` renders `<fns-toaster>`.
- `infra/stores/last-splits.js`, `infra/stores/captcha-token.js`: same localStorage keys as the TypeScript stores. `captchaStore.ts` is deleted (no Svelte user left); `lastSplitStore.ts` stays for `Split.svelte` until step 3.
- `components/base-styles.js`: a constructable stylesheet with the shared card, button, field and spinner styles, adopted by each component's Shadow DOM (`adoptedStyleSheets`). The Admin page now uses it too. Inputs use 16 px text on phones, as the Svelte inputs did (iOS zooms into smaller text on focus).
- `components/icons.js`: inline Lucide SVGs (only `refresh-cw` so far).
- `components/captcha-dialog.js`: `<fns-captcha-dialog>`, replacing `CaptchaModal.svelte` and `lib/api/captcha.ts`. A native modal `<dialog>` (focus kept inside, background inert); Escape is blocked because the challenge must be solved; `open()` resolves once solved. After a wrong answer, "Wrong answer — try again!" now stays visible with the new challenge (the Svelte dialog cleared it as soon as the new challenge loaded, so the person could not tell why the picture changed).
- `biz/home/home-page.js`: `<fns-home-page>`, replacing `routes/Home.svelte` (now only renders the element). Creating retries after a refused token (401) as a loop: verify again, then create. The dev-only "Clear CAPTCHA token" button shows when the page is served from `localhost` (dev mode), since plain JS has no build-time dev flag.
- `web/index.html` loads the toaster and the home page; `RefreshCw.svelte` is deleted.

## Tests

- `HomePageTest`: 7 tests (4 existing, unchanged, + 3): a split that no longer exists drops out of the recent splits (and out of localStorage); a server error on create shows an error toast and re-enables the button; a refused token (401) asks for verification again, then creates the split.
- `CaptchaTest`: 5 tests (1 existing + 4), with the challenge API stubbed by `page.route` (the real image is a random sum the test cannot read): solving creates the split; a wrong answer shows the message with a new challenge; "New challenge" loads another; Escape does not close the dialog.
- `AdminPageTest`: 7 tests, unchanged, pass with the shared stylesheet.
- Checked visually with before/after screenshots at 390×844 and 1280×800: home (empty with error, recent splits, error toast), CAPTCHA dialog, admin.
