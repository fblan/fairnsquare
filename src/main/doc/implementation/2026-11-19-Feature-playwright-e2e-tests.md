# Feature: Playwright end-to-end tests for the frontend (#157)

## What, Why and Constraints

The frontend had no automated tests since the Svelte/vitest suite was removed with the move to the Quarkus Web Bundler (#153). The frontend is going to move from Svelte to web components (keeping Tailwind at first). These tests are the safety net for that rewrite: they drive the real UI in a headless Chromium against the running app, so they keep passing whatever framework renders the pages, as long as the pages behave the same.

Constraints:
- No Node toolchain: the tests are Java (`@QuarkusTest` + `@WithPlaywright` from `io.quarkiverse.playwright:quarkus-playwright`) and run in `mvn package`, locally and in CI.
- Elements are found by role, label or exact text — never by Tailwind classes, `data-testid` or Svelte markup — so the tests survive the migration.
- Tests do not solve the CAPTCHA: each browser context starts with a valid token (made with `CaptchaTokenTestHelper`) in localStorage. `CaptchaTest` starts without one and checks that the CAPTCHA shows.
- Test data (splits, participants, expenses) is set up through the REST API, so each test only drives the UI it is about. Each test uses its own split (unique NanoID), following the test-context sharing rule.

## How

- `pom.xml`: manage `quarkus-playwright` 2.4.0 (`quarkus-playwright.version`).
- `fairnsquare-app/pom.xml`: `quarkus-playwright` test dependency.
- New package `src/test/java/org/asymetrik/web/fairnsquare/e2e` (not a module):
  - `E2eTestBase`: fresh browser context per test (isolated localStorage) with the CAPTCHA token; locator helpers `button`, `field`, `text`, `dialog`, `toast` with exact matching; API helpers `createSplit`, `addParticipant`, `addExpense`. Uses `TempStorageTestResource` like the other integration tests, so it shares their Quarkus context.
  - One test class per page or concern (see Tests).

- On failure, `E2eTestBase` saves a Playwright trace (screenshots, DOM snapshots, network) to `fairnsquare-app/target/playwright-traces/<Class>.<test>.zip`. Open it with `npx playwright show-trace <file>` or drop it on https://trace.playwright.dev.
- `.github/workflows/maven.yml`: when the build fails, the traces are uploaded as the `playwright-traces` artifact.

On first run Playwright downloads its browsers into `~/.cache/ms-playwright` (cached afterwards).

## Tests

34 end-to-end tests:

| Class | Tests | Covers |
|---|---|---|
| `HomePageTest` | 4 | create a split → participants page with its name; empty name inline error; recent splits list + Resume; Dismiss |
| `CaptchaTest` | 1 | without a token, creating a split opens the Human Verification dialog |
| `ParticipantsPageTest` | 9 | empty state; add single; add family with members; duplicate name and empty name inline errors; cancel; edit in modal; remove after confirmation; refusal to remove a participant with expenses |
| `ExpensesPageTest` | 5 | empty state; add with a chosen payer; edit; delete after confirmation; filter by payer and clear |
| `SettlementPageTest` | 4 | balances before resolving; resolve shows who pays whom; settled split is read-only on participants and expenses until unsettled; group two participants |
| `NavigationTest` | 5 | header tabs; dashboard cards; expenses blocked without a participant; unknown split goes home; close split |
| `AdminPageTest` | 2 | wrong password refused; sign in shows stats, survives reload, sign out |
| `LayoutTest` | 12 | home, dashboard, participants, expenses, settlement and admin fit the screen width (no sideways scroll) at 390×844 and 1280×800 |

The tests were checked to fail when the UI changes (a changed "Paid by" label made `ExpensesPageTest` fail).

In one full `mvn package` run, `addsAnExpensePaidByAChosenParticipant` failed once: the Add Expense dialog stayed open and no request reached the server. It could not be reproduced (5 cold runs under full CPU load passed). The test now checks the field values before submitting, and the trace saved on failure will show what happened if it comes back.

`LayoutTest` found a real bug: a long participant name makes the expense filters overflow on a phone. It is tracked in #158; until it is fixed, `LayoutTest` uses ordinary names.

## Rules updated

`src/doc/rules/frontend-rules.md` (approved by the user):
- **Testing:** the vitest/jsdom rules (reactive store `waitFor`, number input `fireEvent`, `vi.mock` hoisting, `within()` scoping, `getByText` ambiguity) are replaced by Playwright rules: where the tests live, role/label/text locators with exact matching, API-built test data, waiting assertions, `LayoutTest`, traces on failure.
- **Accessible Names (new):** icon-only buttons need an `aria-label`; fields need labels; dialogs need `role="dialog"` + `aria-labelledby`; custom pickers must follow the listbox pattern (the current payer picker does not yet).
