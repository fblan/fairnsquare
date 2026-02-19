# Backend Development Rules

## Domain Model

- In mutation methods on aggregate roots, `validate()` must always be the last method call. Any side effects (e.g. `clearSettlement()`) must happen before validation.

## Dev Infrastructure

- Dev-only beans (seeders, data fixtures, etc.) must be placed in a dedicated `*.dev` sub-package and annotated with `@IfBuildProfile("dev")`. They must never be referenced from production code.