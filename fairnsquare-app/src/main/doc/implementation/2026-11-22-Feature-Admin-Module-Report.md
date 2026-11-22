# Feature: the module report in the admin page (#191)

## What, Why and Constraints

The admin page gets a **Module report ↗** button that opens modular's module report of the running build in a new
tab: the modules, their sizes, the dependencies between them, a diagram and the findings (as in listit).

Constraints:

- The report describes the code: admin only, never among the public files.
- The admin API is authenticated by the `X-Admin-Token` header (the admin password), which a plain link cannot send,
  and the token is kept in the admin page's `sessionStorage`.
- The report is a single page that runs inline scripts (its diagram, its filter): they must not be able to read
  that token.
- Builds on modular 0.1.5 (#189, PR #190), whose `report` artifact writes the page.

## How

- `fairnsquare-app/pom.xml`: `exec-maven-plugin` runs `org.asymetrik.modular.report.ModularReport` at
  `process-classes`, on the compiled classes, into `target/classes/admin-report/index.html`. Every build writes it,
  the Docker one included, so the jar carries it, outside `META-INF/resources`. `report` is a plugin dependency
  only: its ArchUnit, JUnit and AssertJ stay out of the application.
- `admin/service/ModuleReportService`: reads the page from the class path (empty when the build has none, e.g. run
  from an IDE without Maven).
- `AdminResource`: `GET /api/admin/module-report` (behind `@AdminProtected`), `text/html`, 404 without a report.
  Its `Content-Security-Policy` sandboxes the page, in case it is ever opened directly.
- `biz/admin/admin-page.js`: the button, next to Sign out. It opens the tab on the click (before any `await`, or
  browsers block it), fetches the report with the token, and shows it in the tab inside
  `<iframe sandbox="allow-scripts" srcdoc>`: the report's scripts run, with an origin of their own, so they cannot
  reach the application's storage. On failure the tab closes and a toast says so.

## Tests

- `admin/ModuleReportResourceTest` (2): the admin gets the report of this build (its title and modules, the sandbox
  policy); 401 without or with a wrong token.
- `e2e/AdminPageTest` (+2, Playwright): the button opens a tab with the report in a sandboxed frame, whose scripts
  get a `SecurityError` on `sessionStorage`; a failing report closes its tab and shows a toast.
- Checked visually at 390×844 and 1280×800 (admin toolbar and report tab).
