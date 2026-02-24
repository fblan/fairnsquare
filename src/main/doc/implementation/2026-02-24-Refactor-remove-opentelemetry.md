# Refactor: Remove OpenTelemetry

## What, Why and Constraints

Removed the OpenTelemetry extension and all related configuration from the application.

**Why**: Simplify deployment by eliminating the dependency on an OTLP collector. Without this change, deploying to a plain environment would require either a running collector or careful per-profile configuration to suppress export errors. This is a temporary removal — OpenTelemetry will be re-added once the observability infrastructure is in place.

**Constraints**: No functional behaviour was changed. The application's REST API, persistence, and business logic are untouched.

## How

### Files modified

**`fairnsquare-app/pom.xml`**
- Removed the `quarkus-opentelemetry` Maven dependency.

**`fairnsquare-app/src/main/resources/application.properties`**
- Removed the entire OpenTelemetry Configuration section (`quarkus.otel.*` properties: service name, OTLP trace/metrics endpoints, exporter settings).
- Removed per-profile OTel exporter overrides (`%dev`, `%test`, `%prod`).
- Simplified the console log format: removed `traceId=%X{traceId}, spanId=%X{spanId}` MDC placeholders that would have printed empty values without the OTel extension.

### Files deleted

**`fairnsquare-app/src/test/java/org/asymetrik/web/fairnsquare/OpenTelemetryTest.java`**
- Deleted the integration test class that verified OTel traces and trace context propagation. These tests are no longer relevant without the extension.

## Tests

No new tests were added. The deleted `OpenTelemetryTest` contained 4 tests:
- `openTelemetryExtensionIsActive` — verified health endpoint returns UP (still covered by `OpenApiTest`)
- `traceContextIsAvailableOnRequests` — verified live probe returns 200 (basic health, not OTel-specific)
- `traceparentHeaderIsAccepted` — verified ready probe accepts W3C traceparent header
- `traceIdFormatIsValid` — purely format-string validation, no application code involved

All remaining tests continue to pass. The removal of the Quarkus extension means the application starts without attempting to connect to any OTLP collector.