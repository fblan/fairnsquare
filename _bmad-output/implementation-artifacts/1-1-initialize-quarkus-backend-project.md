# Story 1.1: Initialize Quarkus Backend Project

Status: review

## Story

As a **developer**,
I want **a Quarkus backend project initialized with the required extensions and package structure**,
So that **I have a solid foundation for building the FairNSquare API**.

## Acceptance Criteria

1. **Given** a fresh project directory **When** the Quarkus project is created using CLI **Then** the project compiles successfully with `mvn compile`
2. **And** the following extensions are included: resteasy-reactive-jackson, quinoa, opentelemetry, smallrye-health, smallrye-openapi
3. **And** Java 25 is configured in pom.xml
4. **And** the package structure follows Architecture: `org.asymetrik.web.fairnsquare` with `sharedkernel/` subdirectory created
5. **And** `GET /q/health` returns 200 OK with status "UP"
6. **And** `GET /q/health/ready` returns 200 OK
7. **And** `GET /q/health/live` returns 200 OK

## Tasks / Subtasks

- [x] Task 1: Create Quarkus project using CLI (AC: 1, 2, 3)
  - [x] 1.1: Run `quarkus create app` with correct group/artifact and extensions
  - [x] 1.2: Verify pom.xml has Java 25 configured (`<maven.compiler.release>25</maven.compiler.release>`)
  - [x] 1.3: Verify all required extensions are present in pom.xml
  - [x] 1.4: Run `mvn compile` and confirm successful compilation
- [x] Task 2: Set up package structure (AC: 4)
  - [x] 2.1: Create base package `org.asymetrik.web.fairnsquare`
  - [x] 2.2: Create `sharedkernel/` subdirectory with placeholder package-info.java
  - [x] 2.3: Create subdirectories per Architecture: `sharedkernel/error/`, `sharedkernel/persistence/`, `sharedkernel/validation/`
- [x] Task 3: Verify health endpoints (AC: 5, 6, 7)
  - [x] 3.1: Start application with `mvn quarkus:dev`
  - [x] 3.2: Write integration test for `GET /q/health` returning 200 with status "UP"
  - [x] 3.3: Write integration test for `GET /q/health/ready` returning 200
  - [x] 3.4: Write integration test for `GET /q/health/live` returning 200
  - [x] 3.5: Run tests and confirm all pass

## Dev Notes

### Critical Architecture Requirements

**From Architecture Document:**
- Package base: `org.asymetrik.web.fairnsquare`
- Modular monolith with DDD shared kernel pattern
- Module structure: `sharedkernel/`, `split/`, `participant/`, `expense/`, `settlement/`, `feedback/`
- For Story 1.1: Only create `sharedkernel/` - other modules come in later stories

**Quarkus CLI Command (from Architecture):**
```bash
quarkus create app org.asymetrik.web:fairnsquare \
  --extension=resteasy-reactive-jackson,quinoa,opentelemetry,smallrye-health,smallrye-openapi \
  --java=25 \
  --no-code
```

**IMPORTANT:** The `--no-code` flag prevents generation of example code. We want a clean slate.

### Version Specifics (Web Research 2026-01-17)

- **Quarkus version:** Use 3.30.x (latest stable as of Jan 2026)
- **Java 25:** Supported by Quarkus 3.30.x
- **Extension IDs:** Use short names (e.g., `smallrye-health` not full artifact coordinates)

### Project Context Rules (MUST FOLLOW)

From `project-context.md`:
- Use RESTEasy Reactive, not classic RESTEasy (already satisfied by `resteasy-reactive-jackson`)
- Pin all dependency versions explicitly in pom.xml
- ALL versions MUST be defined in root pom.xml `<dependencyManagement>` section
- Use `quarkus-opentelemetry` extension for automatic instrumentation
- Integration tests are PRIMARY testing strategy (`@QuarkusTest`)
- Use `@TestHTTPResource` to inject test URLs, never hardcode `localhost:8080`

### Testing Requirements

- Write integration tests using `@QuarkusTest`
- Test health endpoints return correct status codes and JSON structure
- Health response format: `{"status": "UP", "checks": [...]}`

### Package Structure to Create

```
src/main/java/org/asymetrik/web/fairnsquare/
├── sharedkernel/
│   ├── package-info.java
│   ├── error/
│   │   └── package-info.java
│   ├── persistence/
│   │   └── package-info.java
│   └── validation/
│       └── package-info.java
```

### Project Structure Notes

- This story creates the backend project in the `fairnsquare-app/` directory (or root if single module)
- Quinoa extension is included but frontend initialization is Story 1.2
- OpenTelemetry extension is included but configuration is Story 1.3

### References

- [Source: architecture.md#Starter-Template-Evaluation] - CLI command and extension list
- [Source: architecture.md#Project-Structure-Boundaries] - Package organization
- [Source: project-context.md#Technology-Stack-Versions] - Java 25, testing strategy
- [Source: epics.md#Story-1.1] - Acceptance criteria

## Dev Agent Record

### Agent Model Used

Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References

### Completion Notes List

### File List

