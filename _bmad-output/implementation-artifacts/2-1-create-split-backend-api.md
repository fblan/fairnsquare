# Story 2.1: Create Split Backend API

Status: ready-for-dev

## Story

As a **developer integrating with FairNSquare**,
I want **a REST API to create splits with automatic NanoID generation and JSON file persistence**,
So that **splits can be created and stored reliably with proper tenant isolation**.

## Acceptance Criteria

1. **Given** the API is running **When** a POST request is sent to `/api/splits` with body `{"name": "Bordeaux Weekend 2026"}` **Then** the response status is 201 Created **And** the response body contains:
   - `id`: a 21-character NanoID (URL-safe characters only)
   - `name`: the provided split name
   - `createdAt`: ISO 8601 timestamp
   - `participants`: empty array
   - `expenses`: empty array

2. **Given** a split is created successfully **When** the file system is checked **Then** a JSON file exists at `data/default/{splitId}.json` **And** the file contains the complete split data with all fields

3. **Given** an invalid request with empty name `{"name": ""}` **When** POST is sent to `/api/splits` **Then** the response status is 400 Bad Request **And** the response follows Problem Details (RFC 9457) format with:
   - `type`: error type URI
   - `title`: "Validation Error"
   - `status`: 400
   - `detail`: description of the validation failure

4. **Given** an invalid request with missing name `{}` **When** POST is sent to `/api/splits` **Then** the response status is 400 Bad Request **And** the response follows Problem Details format

5. **Given** the data directory does not exist **When** a split is created **Then** the directory structure `data/default/` is created automatically **And** the split file is persisted successfully

6. **Given** the `FAIRNSQUARE_DATA_PATH` environment variable is set to `/custom/path` **When** a split is created **Then** the split file is stored at `/custom/path/default/{splitId}.json`

## Tasks / Subtasks

- [ ] Task 1: Add NanoID dependency (AC: 1)
  - [ ] 1.1: Add `com.aventrix.jnanoid:jnanoid:2.0.0` to pom.xml dependencyManagement
  - [ ] 1.2: Add dependency to fairnsquare-app module (no version, inherits from parent)

- [ ] Task 2: Create sharedkernel persistence layer (AC: 2, 5, 6)
  - [ ] 2.1: Create `TenantPathResolver.java` in sharedkernel/persistence
  - [ ] 2.2: Create `JsonFileRepository.java` in sharedkernel/persistence
  - [ ] 2.3: Implement configurable data path via `fairnsquare.data-path` config property
  - [ ] 2.4: Implement automatic directory creation on write

- [ ] Task 3: Create sharedkernel error handling (AC: 3, 4)
  - [ ] 3.1: Create `BaseError.java` in sharedkernel/error
  - [ ] 3.2: Create `ProblemDetailMapper.java` exception mapper (@ServerExceptionMapper)
  - [ ] 3.3: Implement RFC 9457 Problem Details format

- [ ] Task 4: Create split domain model (AC: 1)
  - [ ] 4.1: Create `Split.java` entity in split/domain
  - [ ] 4.2: Create `SplitId.java` value object wrapping NanoID
  - [ ] 4.3: Create `CreateSplitRequest.java` DTO with Bean Validation

- [ ] Task 5: Create split service layer (AC: 1, 2)
  - [ ] 5.1: Create `SplitService.java` in split/service
  - [ ] 5.2: Implement `createSplit()` method with NanoID generation
  - [ ] 5.3: Integrate with JsonFileRepository for persistence

- [ ] Task 6: Create split REST resource (AC: 1, 3, 4)
  - [ ] 6.1: Create `SplitResource.java` in split/api
  - [ ] 6.2: Implement `POST /api/splits` endpoint
  - [ ] 6.3: Add `@Valid` annotation for request validation
  - [ ] 6.4: Return 201 Created with Location header

- [ ] Task 7: Write integration tests (AC: 1-6)
  - [ ] 7.1: Create `SplitResourceIT.java` in test/split/api
  - [ ] 7.2: Test successful split creation (201, response body, file created)
  - [ ] 7.3: Test validation error (empty name, missing name → 400)
  - [ ] 7.4: Test directory auto-creation
  - [ ] 7.5: Test custom data path via config override

## Dev Notes

### Critical Architecture Compliance

**Package Structure (MUST follow exactly):**
```
src/main/java/org/asymetrik/web/fairnsquare/
├── sharedkernel/
│   ├── error/
│   │   ├── BaseError.java
│   │   └── ProblemDetailMapper.java
│   └── persistence/
│       ├── JsonFileRepository.java
│       └── TenantPathResolver.java
├── split/
│   ├── api/
│   │   └── SplitResource.java
│   ├── domain/
│   │   ├── Split.java
│   │   ├── SplitId.java
│   │   └── CreateSplitRequest.java
│   └── service/
│       └── SplitService.java
```

**JSON Field Naming:** camelCase (Jackson default, enforce via `PropertyNamingStrategies.LOWER_CAMEL_CASE`)

**REST Endpoint Pattern:**
- `POST /api/splits` - Create new split
- Response: Direct JSON (no wrapper), 201 Created with Location header

### NanoID Implementation

```java
// Add to pom.xml dependencyManagement (root)
<dependency>
    <groupId>com.aventrix.jnanoid</groupId>
    <artifactId>jnanoid</artifactId>
    <version>2.0.0</version>
</dependency>

// Usage in SplitService
import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

String splitId = NanoIdUtils.randomNanoId(); // 21 chars, URL-safe
```

### Data Persistence Pattern

**File Path Resolution:**
```
${fairnsquare.data-path:./data}/${tenantId}/${splitId}.json

Default tenant: "default"
Example: ./data/default/V1StGXR8_Z5jdHi6B-myT.json
```

**Configuration Property:**
```properties
# application.properties
fairnsquare.data-path=${FAIRNSQUARE_DATA_PATH:./data}
```

### Split Domain Model

```java
public class Split {
    private String id;           // NanoID (21 chars)
    private String name;         // User-provided name
    private Instant createdAt;   // ISO 8601 timestamp
    private List<Participant> participants = new ArrayList<>();
    private List<Expense> expenses = new ArrayList<>();
}
```

### Problem Details (RFC 9457) Format

**Error Response Example:**
```json
{
  "type": "https://fairnsquare.app/errors/validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Split name is required"
}
```

**Implementation with Quarkus:**
```java
@ServerExceptionMapper
public RestResponse<ProblemDetail> mapValidationException(ConstraintViolationException e) {
    // Map to Problem Details
}
```

### Bean Validation

```java
public class CreateSplitRequest {
    @NotBlank(message = "Split name is required")
    private String name;
}
```

### Testing Strategy

**Integration Tests Primary (>90% coverage target):**
- Use `@QuarkusTest` annotation
- Use `@TestHTTPResource` for URL injection (never hardcode localhost:8080)
- Test file system state after API calls
- Use `@TempDir` for isolated test data directories

**Test Configuration Override:**
```properties
# application-test.properties
fairnsquare.data-path=${java.io.tmpdir}/fairnsquare-test
```

### Previous Story Intelligence (Epic 1)

**Learnings from Story 1.4:**
- shadcn components properly configured with FairNSquare theme colors
- $lib path alias configured in tsconfig.json and vite.config.ts
- 11 tests currently passing (3 HealthCheck, 4 OpenAPI, 4 OpenTelemetry)
- Code review process catches issues like stale config files

**Established Patterns from Epic 1:**
- Integration tests use `@QuarkusTest`
- OpenTelemetry already configured and tracing endpoints
- Health checks at `/q/health/*`
- OpenAPI docs at `/q/swagger-ui`

### Git Context

Recent commits show feature branch workflow:
- `story-1-4 add shadcn (#5)`
- `story-1-3 init open api and open telemetry (#4)`
- `story-1-2 init web ui (#3)`
- `Story/1 1 initialize quarkus backend project (#2)`

**Branch naming:** `story/2-1-create-split-backend-api`

### Project Structure Notes

**Module Location:** `fairnsquare-app/` is the main application module

**Existing Structure (from Epic 1):**
- `src/main/java/org/asymetrik/web/fairnsquare/` - Java source root
- `src/main/resources/application.properties` - Quarkus config
- `src/test/java/` - Integration tests
- `src/main/webui/` - Svelte 5 frontend (Quinoa)

### References

- [Source: architecture.md#Data-Architecture] - JSON file storage pattern
- [Source: architecture.md#API-&-Communication-Patterns] - REST structure, Problem Details
- [Source: architecture.md#Project-Structure-&-Boundaries] - Package organization
- [Source: prd.md#Split-Management] - FR1, FR2, FR25, FR28
- [Source: project-context.md#Multi-Tenancy-Rules] - Tenant isolation requirements
- [Source: project-context.md#Error-Handling-Rules] - Exception mapper pattern

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List