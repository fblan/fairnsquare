---
project_name: '2601-splitnshare'
user_name: 'Fred'
date: '2026-01-15'
sections_completed: ['technology_stack']
existing_patterns_found: 0
---

# Project Context for AI Agents

_This file contains critical rules and patterns that AI agents must follow when implementing code in this project. Focus on unobvious details that agents might otherwise miss._

---

## Technology Stack & Versions

### Backend
- **Java 25** (non-LTS) - use latest language features, plan upgrade to Java 29 LTS
- **Quarkus 3.17.x** (latest stable) - pin exact version in pom.xml
- **Jakarta EE** compatible extensions preferred (`jakarta.*` packages)

### Frontend
- **Svelte 5** with runes (`$state`, `$derived`, `$effect`)
- **SPA mode** via **Quinoa extension** - client-side routing only
- **TypeScript** for type safety

### Testing
- **Quarkus integration tests** (`@QuarkusTest`) as primary strategy
- **Coverage target: >90%** from integration tests alone

### Architecture Tools
- **org.asymetrik:modular** jars for internal module dependency validation

### Critical Version Rules for AI Agents
- Always use Svelte 5 runes syntax, never legacy `let` reactivity
- Use RESTEasy Reactive, not classic RESTEasy
- Pin all dependency versions explicitly in pom.xml

### Maven Dependency Management Rules
- **ALL versions MUST be defined in root pom.xml `<dependencyManagement>` section**
- **Sub-module pom.xml files MUST NOT specify versions in `<dependencies>`**
- Sub-modules inherit versions from parent - only declare groupId and artifactId

### Quinoa + Svelte 5 Integration Rules

#### Project Structure
- Frontend source location: `src/main/webui/` (Quinoa default)
- Svelte build output: configure `quarkus.quinoa.build-dir` to match Svelte's output directory
- REST resources: dedicated package (e.g., `com.splitnshare.api.resources`)

#### Svelte 5 SPA Mode Rules
- This is **vanilla Svelte 5**, NOT SvelteKit - no `+page.svelte` or file-based routing
- Use a client-side router library (e.g., `svelte-routing`, `@mateothegreat/svelte5-router`)
- Configure Quinoa SPA fallback: `quarkus.quinoa.enable-spa-routing=true`
- Use `$props()` for component props, never `export let` (Svelte 4 syntax)

#### White Label & Theming Rules
- **UI MUST support white label deployment** - easy theme customization required
- Use CSS custom properties (variables) for all themeable values (colors, fonts, spacing, borders)
- Define theme variables in a centralized theme file (e.g., `theme.css` or `variables.css`)
- Never hardcode colors or brand-specific values in component styles
- Support runtime theme switching via CSS variable overrides
- Logo, app name, and brand assets must be configurable (not hardcoded)
- Consider a theme configuration file (JSON/YAML) that generates CSS variables

#### Dev Mode Behavior
- `mvn quarkus:dev` proxies frontend requests to Vite dev server
- Hot reload works for both Java and Svelte changes
- Do NOT start Vite separately - Quinoa manages it

#### Testing Integration
- Use `@TestHTTPResource` to inject test URLs, never hardcode `localhost:8080`
- JaCoCo coverage via `quarkus-jacoco` extension
- Configure JaCoCo in parent pom `<pluginManagement>`

#### Module Dependency Validation
- use `org.asymetrik.modular:api` to define modules and `org.asymetrik.modular:verification` in a dedicated unit test to ensure architecture correctness
- Do not include in default build lifecycle

### Compatibility & Upgrade Rules

- Pin exact versions: Quinoa extension, Svelte, and all key dependencies
- Before upgrading Quarkus: verify Quinoa extension compatibility
- Before upgrading Svelte: review changelog for runes API changes
- Run modular verification test early and frequently during development
- Prefer libraries with active maintenance and quick Java version support
- Quarkus extensions are generally Java 25 compatible - prefer them over raw libraries

### Testing Strategy Clarifications

- Integration tests are PRIMARY testing strategy
- Pure utility functions with complex branching logic MAY have unit tests
- Coverage target (>90%) measured on integration tests
- Don't write trivial unit tests to inflate coverage numbers
- Focus integration tests on behavior verification, not line coverage

## Critical Implementation Rules

### Observability Rules

- **ALL REST endpoints MUST be monitored with OpenTelemetry**
- Use `quarkus-opentelemetry` extension for automatic instrumentation
- Traces, metrics, and logs should be correlated via trace context
- Configure exporter based on environment (OTLP for production, console/logging for dev)
- Custom spans for significant business operations beyond HTTP boundaries

### Error Handling Rules

- Use `@ServerExceptionMapper` for global REST exception handling
- Each module MUST define its own error type(s) in that module's package
- All module error types MUST extend the common error type from `sharedkernel`
- Never throw raw exceptions from business logic - use module-specific error types
- Exception mapper converts module errors to appropriate HTTP responses

### Multi-Tenancy Rules

- **Backend MUST support multi-tenant architecture**
- Each tenant operates with isolated data - no data leakage between tenants
- New tenant provisioning must be straightforward (start with empty data set)
- Tenant identification via request context (header, subdomain, or JWT claim)
- All database queries MUST be tenant-scoped (enforce at repository/query level)
- Consider tenant-aware base entity or query filters for automatic scoping
- Tenant configuration (features, limits) should be externalized
- Support tenant-specific customization without code changes

### Module Architecture Rules

- Project follows modular monolith / DDD shared kernel pattern
- `sharedkernel` module contains:
  - Common base error type
  - Cross-cutting value objects
  - Shared interfaces/contracts
- Module-specific code stays in its own module - do NOT put module logic in sharedkernel
- Dependencies flow INWARD: modules depend on sharedkernel, not on each other (unless explicitly allowed by modular verification)
