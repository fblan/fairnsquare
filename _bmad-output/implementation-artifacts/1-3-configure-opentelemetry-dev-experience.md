# Story 1.3: Configure OpenTelemetry & Dev Experience

Status: done

## Story

As a **developer**,
I want **OpenTelemetry configured with structured logging**,
So that **all API endpoints are automatically instrumented for observability**.

## Acceptance Criteria

1. **Given** the project from Stories 1.1 and 1.2 **When** OpenTelemetry is configured in `application.properties` **Then** traces are generated for all REST endpoint calls
2. **And** trace IDs appear in log output
3. **And** logs use structured JSON format in production profile
4. **And** logs use readable format in dev profile
5. **And** `GET /q/swagger-ui` displays the OpenAPI documentation
6. **And** the complete dev workflow runs with single command: `mvn quarkus:dev`
7. **And** the application starts in under 10 seconds in dev mode

## Tasks / Subtasks

- [x] Task 1: Verify OpenTelemetry auto-instrumentation (AC: 1)
  - [x] 1.1: Verify quarkus-opentelemetry extension is included
  - [x] 1.2: OpenTelemetry extension active in installed features
  - [x] 1.3: Write integration test verifying OpenTelemetry is enabled
- [x] Task 2: Configure trace ID in logs (AC: 2)
  - [x] 2.1: Configure log format with traceId, parentId, spanId, sampled placeholders
  - [x] 2.2: Verified trace context placeholders appear in log output
- [x] Task 3: Configure logging profiles (AC: 3, 4)
  - [x] 3.1: Dev profile uses readable format with trace context
  - [x] 3.2: Test profile uses JSON format
  - [x] 3.3: Prod profile uses JSON format
- [x] Task 4: Verify OpenAPI/Swagger UI (AC: 5)
  - [x] 4.1: Write integration test for /q/swagger-ui endpoint (200 OK)
  - [x] 4.2: Write integration test for /q/openapi endpoint (200 OK, YAML content)
  - [x] 4.3: Verify OpenAPI contains application name "fairnsquare"
- [x] Task 5: Verify dev workflow (AC: 6, 7)
  - [x] 5.1: `mvn quarkus:dev` starts full stack (Quarkus + Vite)
  - [x] 5.2: Startup time measured: 7.084s (under 10 seconds)
  - [x] 5.3: Frontend forwarded to Vite dev server on port 5173

## Dev Notes

### Critical Architecture Requirements

**From Architecture Document:**
- Use `quarkus-opentelemetry` extension for automatic instrumentation
- Traces, metrics, and logs should be correlated via trace context
- Configure exporter based on environment (OTLP for production, console/logging for dev)
- Custom spans for significant business operations beyond HTTP boundaries

**From Project Context:**
- ALL REST endpoints MUST be monitored with OpenTelemetry
- Include splitId/tenantId in all log statements via MDC

### Final Configuration (application.properties)

```properties
# OpenTelemetry Configuration
quarkus.otel.service.name=fairnsquare
quarkus.otel.resource.attributes=service.version=1.0.0-SNAPSHOT
quarkus.otel.exporter.otlp.traces.endpoint=http://localhost:4317

# Logging - trace context in all profiles
quarkus.log.console.format=%d{HH:mm:ss} %-5p traceId=%X{traceId}, parentId=%X{parentId}, spanId=%X{spanId}, sampled=%X{sampled} [%c{2.}] (%t) %s%e%n

# Dev profile - no OTLP export, readable logs
%dev.quarkus.otel.exporter.otlp.traces.endpoint=
%dev.quarkus.otel.traces.exporter=none
%dev.quarkus.log.console.json.enabled=false

# Test profile - no OTLP export, JSON logs
%test.quarkus.otel.traces.exporter=none
%test.quarkus.log.console.json.enabled=true

# Prod profile - OTLP export, JSON logs
%prod.quarkus.log.console.json.enabled=true
```

### Test Results

- **Total tests:** 7 (3 HealthCheckTest + 4 OpenTelemetryIT)
- **All tests passing**
- **Installed features:** cdi, opentelemetry, quinoa, rest, rest-jackson, smallrye-context-propagation, smallrye-health, smallrye-openapi, swagger-ui, vertx

### Dev Workflow

Single command to start full stack:
```bash
mvn quarkus:dev
```

This starts:
- Quarkus backend on http://localhost:8080
- Vite dev server on http://localhost:5173 (managed by Quinoa)
- Hot reload for both Java and Svelte changes
- Debug port on 5005

### References

- [Source: architecture.md#Observability] - OpenTelemetry requirements
- [Source: project-context.md#Observability-Rules] - Monitoring mandates
- [Source: epics.md#Story-1.3] - Acceptance criteria

## Dev Agent Record

### Agent Model Used

Claude Opus 4.5 (claude-opus-4-5-20251101)

### Debug Log References

### Completion Notes List

- OpenTelemetry extension already included from Story 1.1
- Enhanced application.properties with complete trace context format (traceId, spanId)
- Added service.version resource attribute for better trace identification
- Configured separate logging profiles: dev (readable), test (JSON), prod (JSON)
- Created OpenTelemetryIT integration test with 4 test cases for tracing
- Created OpenApiIT integration test with 4 test cases for Swagger/OpenAPI
- Verified Swagger UI accessible at /q/swagger-ui
- Verified OpenAPI spec accessible at /q/openapi
- Dev workflow starts in 7.084 seconds (well under 10 second requirement)
- Trace IDs will be populated during actual HTTP request handling

**Code Review Fixes Applied (2026-01-18):**
- H1/H2: Rewrote OpenTelemetryIT with proper trace verification tests (W3C traceparent header)
- M1: Documented that traces are generated in dev but not exported (by design - no collector locally)
- M2: Split tests into OpenTelemetryIT (tracing) + OpenApiIT (Swagger/OpenAPI)
- M3: Added metrics exporter configuration (OTLP for prod, none for dev/test)
- L1: Improved Swagger UI test assertions (checks for "OpenAPI UI" and content type)
- L2: Made pretty-print config consistent across profiles
- Added OpenAPI info metadata (title, description, version)

### File List

- `fairnsquare-app/src/main/resources/application.properties` - Updated with complete OpenTelemetry, metrics, and logging configuration
- `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/OpenTelemetryIT.java` - Integration tests for tracing (4 tests)
- `fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/OpenApiIT.java` - Integration tests for Swagger/OpenAPI (4 tests)