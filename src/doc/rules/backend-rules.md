# Backend Development Rules

## Domain Model

- In mutation methods on aggregate roots, `validate()` must always be the last method call. Any side effects (e.g. `clearSettlement()`) must happen before validation.