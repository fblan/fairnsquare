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

## Logging Conventions

- Use `org.jboss.logging.Logger` with format methods (`infof`, `errorf`, etc.). Log entries for service calls must use structured key=value format (e.g. `method=X splitId=Y result=Z duration=Nms`) to enable log parsing and filtering.