# Backend Development Rules

## Domain Model

- In mutation methods on aggregate roots, `validate()` must always be the last method call. Any side effects (e.g. `clearSettlement()`) must happen before validation.

## Dev Infrastructure

- Dev-only beans (seeders, data fixtures, etc.) must be placed in a dedicated `*.dev` sub-package and annotated with `@IfBuildProfile("dev")`. They must never be referenced from production code.

## Scheduled Jobs

- Scheduled jobs (classes annotated with `@Scheduled`) must be placed in the same package as the service they delegate to, and must be thin wrappers — they must contain no business logic themselves. All logic belongs in the injected service.

## Infrastructure Errors

- Errors related to infrastructure constraints (storage, rate limits, etc.) must extend `BaseError` and be placed in the relevant infrastructure package (e.g. `persistence/`), not in the domain layer. They must use appropriate HTTP status codes (e.g. 507 for storage limits) rather than generic 4xx codes.