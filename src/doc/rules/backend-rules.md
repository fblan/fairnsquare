# Backend Development Rules

## Domain Model

- In mutation methods on aggregate roots, `validate()` must always be the last method call. Any side effects (e.g. `clearSettlement()`) must happen before validation.

## Dev Infrastructure

- Dev-only beans (seeders, data fixtures, etc.) must be placed in a dedicated `*.dev` sub-package and annotated with `@IfBuildProfile("dev")`. They must never be referenced from production code.

## Scheduled Jobs

- Scheduled jobs (classes annotated with `@Scheduled`) must be placed in the same package as the service they delegate to, and must be thin wrappers — they must contain no business logic themselves. All logic belongs in the injected service.

## Infrastructure Errors

- Errors related to infrastructure constraints (storage, rate limits, etc.) must extend `BaseError` and be placed in the relevant infrastructure package (e.g. `persistence/`), not in the domain layer. They must use appropriate HTTP status codes (e.g. 507 for storage limits) rather than generic 4xx codes.

## CDI Interceptors

- Cross-cutting concern interceptors (`@InterceptorBinding` and their `@Interceptor` implementations) must be placed in the `sharedkernel` package (e.g. `sharedkernel/logging/`). Interceptors must be purely observational — they must not alter the return value or suppress exceptions.

## Domain Model toString()

- All domain entities (aggregate roots, entities, value objects) must implement `toString()` with a concise, bounded-length summary of their identity and key state (ID, counts, flags). Avoid dumping full collections or large fields. This ensures meaningful and readable log output when entities appear in interceptor or debug logs.

## Application Configuration — Environment-Specific URLs

- URLs that vary per developer or environment (tunnel URLs, external service endpoints, etc.) must not be hardcoded in `application.properties`. Use SmallRye Config's `${ENV_VAR:default}` substitution syntax. Local overrides go in `fairnsquare-app/.env` (gitignored).

## Logging Conventions

- Use `org.jboss.logging.Logger` with format methods (`infof`, `errorf`, etc.). Log entries for service calls must use structured key=value format (e.g. `method=X splitId=Y result=Z duration=Nms`) to enable log parsing and filtering.

## Module Boundaries

- Modules are declared at leaf-domain level using `@Module` on `package-info.java` (via `org.asymetrik.modular:api`). Parent and child packages cannot both be annotated as modules.
- Non-exported implementation details must be placed in a `<module>.internal` sub-package. Only classes that are intentionally part of the module's public API should remain in the root module package.
- `ModularArchitectureTest` (plain JUnit, no `@QuarkusTest`) must scan `org.asymetrik.web.fairnsquare` and fail the build on any export violation or nested module violation.
- When moving a class to an `internal` sub-package, Java package-private access is broken. Any fields or constants that test code or sibling classes need must be explicitly made `public`.

## Test-Only Code Must Not Live in Production Sources

- Methods, utilities, or helpers that exist solely to support tests must not be added to `src/main/java`. They belong in `src/test/java`.
- Before each commit, verify that no production class has been polluted with test-only methods (e.g. `cleanAll`, `reset`, `seed`, fixture helpers). A method is test-only if it is called exclusively from test code.
- If a test needs access to internal state, prefer injecting config properties (e.g. `@ConfigProperty`) or using the public API rather than adding a backdoor to the production service.

## API Layer Placement

- DTOs and mappers for a domain resource must be co-located under the owning domain's `api/` package (e.g. `split/api/expense/dto/`, `split/api/expense/mapper/`). They must not live in a top-level domain package unrelated to the resource that exposes them.