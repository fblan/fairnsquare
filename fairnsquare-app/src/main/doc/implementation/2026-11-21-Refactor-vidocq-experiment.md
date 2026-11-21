# Refactor (experiment): the backend on vidocq instead of Quarkus (#184)

## What, Why and Constraints

An experimental branch: the backend moves from Quarkus 3.37 to vidocq 0.4.0-RC2, the stack listit runs on, to
measure the real cost and risks before deciding. It isn't meant to be merged as is.

Constraints:

- The split files already on disk must still load: `PersistenceBackwardCompatibilityTest` and its six fixtures.
- The REST API, its JSON, its error responses (RFC 9457) and its headers stay the same: the frontend doesn't change.
- Every existing test keeps passing (backend and Playwright), except the OpenAPI ones: vidocq has no OpenAPI.

## How

### Build

- The root `pom.xml` is an aggregator only. `fairnsquare-app`'s parent is `vidocq-runtime-parent`.
- `module-info.java`: the application runs on the module path (`vidocq:package` builds `bin/`, `lib/`, `app/`).
- `vidocq.properties` replaces `application.properties`. REST is under `/api` (`vidocq.rest.context-path`), so
  the resources' `@Path` lose their `/api` prefix. The pages are served by a `frontend` mount (`FrontendMount`):
  vidocq's static files, with `index.html` for the frontend's own URLs. This replaces the Vert.x `WebRouter`.
- Dev mode: `mvn compile vidocq:dev` (hot reload, debugger port, sample split). Ravel reads the `%dev.` keys only
  under the MicroProfile profile, which the pom sets (`vidocq.dev.systemProperties`).
- `docker/Dockerfile.jvm` copies the vidocq distribution (3.7 MB, against 24 MB for `quarkus-app`).

### Code

- **Stored splits**: `SplitJsonCodec` reads and writes `data.bin` with JSON-P, by hand. Every legacy rule is in
  plain code: an expense without `type` is `BY_NIGHT`, `BY_PERSON` is `BY_SHARE`, `EQUAL` stays readable,
  `numberOfPersons` is `share`, unknown fields are ignored, and a number may be a JSON number or a string. The JSON
  written is the same as Jackson's (same fields and order, `null` written out). `ZipSerializer`'s metadata too.
- **REST bodies**: JSON-B (Champollion) instead of Jackson. The Jackson annotations are removed (the names were
  already the fields'). JSON-B leaves `null` fields out, where Jackson wrote them; the frontend only tests `!= null`
  and truthiness, which treat both alike.
- **Validation**: vidocq has no Bean Validation. `Violations` checks a request's fields and throws
  `ValidationError` (400, `Validation Error`, messages joined by `; ` as before). Each request type has a
  `validate()` method, which the resource calls with `Violations.valid(request)`.
- **Errors**: `ProblemDetailMapper` is a plain JAX-RS `ExceptionMapper<BaseError>`.
- **Nightly cleanup**: a single daemon `ScheduledExecutorService`, started with the application, replaces
  `@Scheduled`. A failure is logged and doesn't cancel the following nights.
- `StartupEvent` → CDI's `Startup`; the JBoss logger → `System.Logger`; OpenAPI annotations removed; the dev seeder
  runs when `fairnsquare.dev.seed` is true (true in the dev profile) instead of `@IfBuildProfile`.
- vidocq follows the CDI spec where Quarkus is lenient: no public field on a normal-scoped bean
  (`TenantPathResolver`), `@Inject` required on `@ConfigProperty` fields (`CaptchaService`), and an empty value is
  a missing one (`admin.password-hash` becomes an `Optional`).
- The resources are `@ApplicationScoped`: unscoped, their client proxies were generated at boot.

### vidocq issues met (to report)

1. **javac rejects nested records with compact constructors** (`Split.Name`, `Participant.Id`...): "invalid
   canonical constructor ... must not contain explicit constructor invocation", once a JAX-RS resource is compiled.
   Champollion's processor reads the resources' method bodies through `Trees` (`ResponseEntities`), which
   attributes classes during annotation processing. Workaround: `-Achampollion.jsonb.rest=false` (JSON-B binds by
   reflection at runtime).
2. **`@ConfigProperty` constructor parameters fail on an intercepted bean**: "@ConfigProperty metadata is required
   on injection point". Workaround: `FileSystemService` reads its limits from `Config`.
3. **vidocq's `dev` profile doesn't set MicroProfile's**: `%dev.` keys are ignored in dev mode. Workaround in the pom.

### Not carried over

- OpenAPI and Swagger UI (`/q/openapi`, `/q/swagger-ui`): vidocq has none (#139 called its exposure a risk anyway).
- CORS: the frontend is served by the application itself (same origin), so no browser request needs it.
  `FAIRNSQUARE_CORS_ORIGINS` is no longer read.
- JSON logs: vidocq writes plain lines.
- Health moves from `/q/health` to `/api/health`.

## Tests

- `@FairnsquareTest` (`testing/FairnsquareTestRuntime`) replaces `@QuarkusTest`: one vidocq application booted
  in-process for the run, on a free port and a temporary data directory. A class with its own `maxFileCount` gets
  its own application, as `restrictToAnnotatedClass` did.
- `TempStorageTestResource` is removed; tests read the data directory from the runtime.
- The end-to-end tests launch Chromium with plain Playwright (`testing/SharedBrowser`) instead of
  quarkus-playwright.
- `OpenApiTest` is removed; `HealthCheckTest` checks `/api/health`.
- Result: 431 tests pass (`mvn package`), the 99 Playwright ones included, and the formatter check passes.
- Manual: the packaged application, and dev mode with a hot reload, checked with curl.

Measured on the same machine, `-Xmx512m`, three runs each:

| | Quarkus 3.37 | vidocq 0.4.0-RC2 |
|---|---|---|
| First request answered after | ~5.7 s | ~3.9 s |
| Memory (RSS), idle | ~150 MB | ~126 MB |
| Memory (RSS), after a workload | ~177 MB | ~153 MB |
| Distribution | 24 MB | 3.7 MB |
