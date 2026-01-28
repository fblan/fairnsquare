---
stepsCompleted: [1, 2, 3, 4, 5, 6, 7]
inputDocuments:
  - '_bmad-output/planning-artifacts/product-brief-FairNSquare-2026-01-16.md'
  - '_bmad-output/planning-artifacts/prd.md'
  - '_bmad-output/planning-artifacts/ux-design-specification.md'
  - '_bmad-output/project-context.md'
workflowType: 'architecture'
project_name: 'FairNSquare'
user_name: 'Fred'
date: '2026-01-17'
---

# Architecture Decision Document

_This document builds collaboratively through step-by-step discovery. Sections are appended as we work through each architectural decision together._

## Project Context Analysis

### Requirements Overview

**Functional Requirements:**

28 functional requirements across 5 domains:

| Domain | FRs | Architectural Implication |
|--------|-----|---------------------------|
| Split Management | FR1-FR4 | Core entity, shareable link generation, no-auth access pattern |
| Participant Management | FR5-FR9 | Participant entity with nights, smart defaults, referential integrity |
| Expense Management | FR10-FR17 | Expense entity, 3 split modes with calculation logic, CRUD operations |
| Balance & Settlement | FR18-FR21 | Balance calculation service, transaction optimizer algorithm |
| User Feedback | FR22-FR24 | Feedback entity, simple submission flow |
| System Operations | FR25-FR28 | Multi-tenancy, OpenTelemetry, analytics tracking, persistence |

**Non-Functional Requirements:**

| Category | Requirement | Architectural Impact |
|----------|-------------|---------------------|
| Performance | FCP < 1.5s, TTI < 3s | Lightweight SPA, optimized bundle, lazy loading |
| Performance | API p95 < 500ms | Efficient queries, connection pooling |
| Performance | Bundle < 200KB gzipped | Tree-shaking, minimal dependencies |
| Security | Tenant data isolation | Split-scoped queries, no cross-tenant leakage |
| Security | Cryptographic link IDs | UUID or similar for split identifiers |
| Security | HTTPS only | TLS termination, secure cookies |
| Reliability | 99%+ uptime | Health checks, graceful degradation |
| Reliability | Zero data loss | Transaction integrity, backup strategy |
| Observability | Full OpenTelemetry | Traces on all endpoints, correlated logs |

**Scale & Complexity:**

- Primary domain: Full-stack web application (SPA + REST API)
- Complexity level: Low-Medium
- Estimated architectural components: 8-10 modules

### Technical Constraints & Dependencies

| Constraint | Source | Impact |
|------------|--------|--------|
| Quarkus 3.30.x | User preference | Latest features, verify extension compatibility |
| Java 25 | Project context | Use latest language features, plan Java 29 LTS upgrade |
| Svelte 5 with runes | Project context | No SvelteKit, use client-side router |
| Quinoa extension | Project context | SPA mode, Vite dev server integration |
| Modular monolith | Project context | DDD shared kernel pattern, module boundaries |
| Integration tests primary | Project context | >90% coverage from integration tests |
| White-label support | Project context | CSS custom properties, configurable branding |

### Cross-Cutting Concerns Identified

| Concern | Scope | Approach |
|---------|-------|----------|
| Multi-tenancy | All data access | Split ID as tenant identifier, scoped queries |
| Observability | All REST endpoints | OpenTelemetry auto-instrumentation + custom spans |
| Error handling | All modules | Module-specific errors extending shared kernel base |
| Theming | All UI components | CSS custom properties, Tailwind config integration |
| Validation | All user inputs | Server-side validation, client-side UX feedback |

## Starter Template Evaluation

### Primary Technology Domain

Full-stack Web Application (SPA + REST API) based on project requirements analysis.

### Starter Options Considered

| Option | Description | Verdict |
|--------|-------------|---------|
| Quarkus CLI + Vite Svelte | Two-step init: Quarkus backend + Svelte 5 frontend | ✅ Selected |
| code.quarkus.io | Web-based starter | Same result, less automation |
| SvelteKit + adapter | Full-stack JS | ❌ Rejected - project requires Java backend |

### Selected Approach: Quarkus CLI + Vite Svelte Template

**Rationale for Selection:**
- Matches technical constraints (Java 25, Quarkus 3.30.x, Svelte 5 runes)
- Quinoa extension provides seamless SPA integration
- No SvelteKit means simpler mental model (vanilla Svelte + client-side router)
- JSON file persistence eliminates database complexity

**Initialization Commands:**

```bash
# Step 1: Create Quarkus backend
quarkus create app com.fairnsquare:fairnsquare \
  --extension=resteasy-reactive-jackson,quinoa,opentelemetry,smallrye-health \
  --java=25 \
  --no-code

# Step 2: Initialize Svelte 5 frontend (in src/main/webui/)
cd src/main/webui
npm create vite@latest . -- --template svelte-ts
```

### Architectural Decisions Provided by Starter

**Language & Runtime:**
- Java 25 with Quarkus 3.30.x
- TypeScript for frontend (Svelte 5)

**Persistence Solution:**
- JSON file-based storage (no database)
- Jackson for serialization/deserialization
- File-per-split pattern for natural tenant isolation

**Styling Solution:**
- Tailwind CSS (to be added post-initialization)
- CSS custom properties for white-label theming

**Build Tooling:**
- Maven for backend
- Vite for frontend (managed by Quinoa)
- Single `mvn quarkus:dev` starts both

**Testing Framework:**
- Quarkus @QuarkusTest for integration tests
- JaCoCo for coverage reporting

**Code Organization:**
- Modular monolith with DDD shared kernel
- Frontend in `src/main/webui/`
- REST resources in dedicated package

**Development Experience:**
- Hot reload for Java and Svelte
- Quinoa proxies frontend dev server
- Single command development: `mvn quarkus:dev`

**Note:** Project initialization using these commands should be the first implementation story.

## Core Architectural Decisions

### Decision Priority Analysis

**Critical Decisions (Block Implementation):**
- Data storage pattern: File-per-split with tenant path
- Split ID format: NanoID
- REST structure: Split-scoped paths
- Client-side router: sv-router

**Important Decisions (Shape Architecture):**
- Validation: Bean Validation
- Error format: Problem Details (RFC 9457)
- API docs: SmallRye OpenAPI
- State management: Svelte 5 runes only
- Containerization: Docker JVM mode

**Deferred Decisions (Post-MVP):**
- Native image compilation
- CI/CD pipeline specifics
- Scaling strategy

### Data Architecture

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Storage pattern | `data/{tenant-id}/{split-id}.json` | Natural tenant isolation, no locking issues |
| Validation | Bean Validation (`@Valid`, `@NotBlank`) | Built into Quarkus, declarative, auto 400 responses |
| Data path | Configurable via `FAIRNSQUARE_DATA_PATH`, default `./data` | Flexible for dev and container deployments |

### Authentication & Security

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Authentication | None (shared link model) | PRD design - zero friction entry |
| Split ID format | NanoID (21 chars) | Shorter URLs, URL-safe, cryptographically secure |
| Tenant isolation | Path-based (`/{tenant-id}/`) | Natural file system isolation |

### API & Communication Patterns

| Decision | Choice | Rationale |
|----------|--------|-----------|
| REST structure | Split-scoped paths (`/api/splits/{splitId}/...`) | Clear hierarchy, natural tenant context |
| Error format | Problem Details (RFC 9457) | Standard format, Quarkus built-in support |
| API documentation | SmallRye OpenAPI | Zero-config, Swagger UI at `/q/swagger-ui` |

### Frontend Architecture

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Router | sv-router | Native Svelte 5 support, type-safe routes |
| State management | Svelte 5 runes only | No extra dependencies, `.svelte.ts` for shared state |
| Styling | Tailwind CSS + CSS custom properties | From UX spec, white-label ready |

### Infrastructure & Deployment

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Containerization | Docker with Quarkus JVM mode | Simpler build, easier debugging |
| Environment config | Hybrid (profiles + env vars) | Defaults in properties, secrets via env vars |
| Data persistence | Mounted volume or configurable path | `FAIRNSQUARE_DATA_PATH` env var |

### Decision Impact Analysis

**Implementation Sequence:**
1. Project initialization (Quarkus + Svelte 5)
2. Data layer (JSON file persistence with tenant/split structure)
3. REST API layer (split-scoped endpoints)
4. Frontend routing and state management
5. UI components with Tailwind
6. Docker containerization

**Cross-Component Dependencies:**
- NanoID generation needed before split creation API
- Tenant path structure affects both backend persistence and API routing
- sv-router setup blocks all frontend navigation
- Bean Validation DTOs shared between API and persistence layers

## Implementation Patterns & Consistency Rules

### Pattern Categories Defined

**Critical Conflict Points Addressed:** 12 areas where AI agents could make different choices

### Naming Patterns

**JSON Field Naming:**
- Convention: camelCase
- Examples: `splitId`, `numberOfNights`, `createdAt`
- Enforcement: Jackson `PropertyNamingStrategies.LOWER_CAMEL_CASE`

**Java Naming:**
- Classes: PascalCase (`SplitResource`, `ExpenseService`)
- Packages: lowercase (`org.asymetrik.web.fairnsquare.split`)
- Methods: camelCase (`createSplit`, `calculateBalance`)
- Constants: UPPER_SNAKE_CASE (`DEFAULT_TENANT_ID`)

**Svelte/TypeScript Naming:**
- Components: PascalCase files (`ExpenseCard.svelte`, `SplitOverview.svelte`)
- Stores/state: camelCase files (`splitStore.svelte.ts`)
- Utilities: camelCase files (`formatCurrency.ts`)
- CSS classes: kebab-case via Tailwind (`text-primary`, `btn-action`)

**API Endpoint Naming:**
- Resources: Plural (`/splits`, `/participants`, `/expenses`)
- Path params: JAX-RS style `{splitId}`
- Example: `GET /api/splits/{splitId}/participants`

### Structure Patterns

**Backend Package Organization:**
```
src/main/java/org/asymetrik/web/fairnsquare/
├── sharedkernel/          # Cross-cutting concerns
│   ├── error/             # Base error types
│   ├── persistence/       # JSON file persistence abstractions
│   └── validation/        # Shared validators
├── split/                 # Split module
│   ├── api/               # REST resources
│   ├── domain/            # Entities, value objects
│   └── service/           # Business logic
├── participant/           # Participant module
├── expense/               # Expense module
├── settlement/            # Settlement module
└── feedback/              # Feedback module
```

**Frontend Structure:**
```
src/main/webui/src/
├── lib/
│   ├── components/        # Reusable UI components
│   ├── stores/            # Svelte 5 state (.svelte.ts)
│   ├── api/               # API client functions
│   └── utils/             # Helper functions
├── routes/                # Page components (sv-router)
├── App.svelte             # Root component
└── main.ts                # Entry point
```

**Test Organization:**
- Location: `src/test/java/` (Maven convention)
- Naming: `*Test.java` for normal test and integration tests
- Structure: Mirrors main source tree

### Format Patterns

**API Response Formats:**
- Success: Direct JSON response (no wrapper)
- Errors: Problem Details (RFC 9457)
- Dates: ISO 8601 strings (`"2026-01-17T14:30:00Z"`)

**Example Success Response:**
```json
{
  "id": "V1StGXR8_Z5jdHi6B-myT",
  "name": "Bordeaux Weekend 2026",
  "createdAt": "2026-01-17T10:00:00Z"
}
```

**Example Error Response:**
```json
{
  "type": "https://fairnsquare.app/errors/participant-not-found",
  "title": "Participant Not Found",
  "status": 404,
  "detail": "Participant with ID 'abc123' does not exist in this split"
}
```

### Process Patterns

**Loading State Pattern (Frontend):**
```typescript
// Per-component pattern
let isLoading = $state(false);
let error = $state<string | null>(null);
let data = $state<Split | null>(null);
```

**Error Display:**
- Inline: Form validation and field-specific errors
- Toast: Network errors, unexpected failures, success confirmations

**Logging Pattern (Backend):**
- Logger: `@Inject Logger log` (Quarkus CDI)
- Levels: ERROR (failures), WARN (recoverable), INFO (business events), DEBUG (dev)
- Format: Structured JSON in production, readable in development
- Context: Include `splitId`/`tenantId` via MDC

### Domain Model Patterns

**Value Objects Over Primitives:**

Domain object fields should use value objects, not raw primitives. Value objects encapsulate validation, provide type safety, and make the domain language explicit.

| Primitive | Value Object        | Benefits |
|-----------|---------------------|----------|
| `String name` | `Name name`         | Validation (non-blank, max length), domain semantics |
| `String id` | `Split.Id id`       | Format validation (NanoID), type safety |
| `BigDecimal amount` | `Money amount`      | Currency handling, rounding rules |
| `int nights` | `NightCount nights` | Non-negative validation, domain meaning |

All data in domain object must be bounded and protected against invalid data injection:

- String : length and format constraints (one line or mutli-line, wich characters allowed, pattern, etc)
- Numeric : range constraints

**Implementation Pattern - Inner Record Classes:**
```java
public class Split {
    // Value objects as inner records
    public record Name(String value) {
        public Name {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Split name cannot be blank");
            }
            if (value.length() > 100) {
                throw new IllegalArgumentException("Split name cannot exceed 100 characters");
            }
        }
    }

    private final SplitId id;
    private Name name;  // Value object, not String
    private final Instant createdAt;
    // ...
}
```

**When to Use Inner vs Top-Level Value Objects:**
- Inner record: Used only by one aggregate (e.g., `Split.Name`)
- Top-level class: Shared across modules (e.g., `SplitId`, `Money`)

**Rich Domain Models (No Anemic Objects):**

Domain objects like `Split`, `Participant`, and `Expense` must encapsulate behavior, not just data. Setters expose internal state and invite procedural code that belongs inside the domain.

| Principle | Do | Don't |
|-----------|-----|-------|
| Construction | Factory methods, builders, or constructors with validation | Public no-arg constructor + setters |
| State changes | Intention-revealing methods (`addParticipant()`, `recordExpense()`) | `setParticipants(list)` |
| Invariants | Enforce in domain object | Validate in service layer |
| Collections | Return unmodifiable views | Expose mutable collections |

**Example - Split Aggregate:**
```java
public class Split {
    // Inner value object
    public record Name(String value) {
        public Name {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Split name cannot be blank");
            }
        }
    }

    private final SplitId id;          // Value object (top-level, shared)
    private Name name;                  // Value object (inner record)
    private final Instant createdAt;
    private final List<Participant> participants = new ArrayList<>();
    private final List<Expense> expenses = new ArrayList<>();

    // Factory method for creation
    public static Split create(String name) {
        return new Split(SplitId.generate(), new Name(name), Instant.now());
    }

    // Behavior methods - not setters
    public void rename(String newName) {
        this.name = new Name(newName);  // Validation in value object
    }

    public void addParticipant(Participant participant) {
        // Invariant: no duplicate names
        participants.add(participant);
    }

    // Immutable view
    public List<Participant> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    // Getters return value objects, not primitives
    public Name getName() { return name; }
    public SplitId getId() { return id; }

    // NO setters - state changes through behavior methods only
}
```

**Exceptions for DTOs:**
Request/Response DTOs (e.g., `CreateSplitRequest`) may use setters or records for Jackson deserialization. These are data carriers, not domain objects.

### Enforcement Guidelines

**All AI Agents MUST:**
1. Follow naming conventions exactly as specified (no variations)
2. Place code in the correct package/directory structure
3. Use the standard response formats for all API endpoints
4. Include splitId/tenantId in all log statements
5. Use per-component loading state pattern in frontend

**Anti-Patterns to Avoid:**
- ❌ `split_id` (use `splitId`)
- ❌ `/api/split/{id}` (use `/api/splits/{splitId}`)
- ❌ `splitService.java` (use `SplitService.java`)
- ❌ Global loading state store
- ❌ Custom error response formats
- ❌ Anemic domain models with public setters (use behavior methods)
- ❌ `setParticipants(list)` (use `addParticipant()`)
- ❌ Exposing mutable collections (return `Collections.unmodifiableList()`)
- ❌ Primitive obsession in domain objects (`String name` → use `Name name`)


## Project Structure & Boundaries

### Complete Project Directory Structure

```
fairnsquare/
├── README.md
├── pom.xml
├── .gitignore
├── .env.example
├── Dockerfile
├── docker-compose.yml
│
├── src/
│   ├── main/
│   │   ├── java/org/asymetrik/web/fairnsquare/
│   │   │   │
│   │   │   ├── FairNSquareApplication.java          # Quarkus entry point (if needed)
│   │   │   │
│   │   │   ├── sharedkernel/
│   │   │   │   ├── error/
│   │   │   │   │   ├── BaseError.java               # Base error class
│   │   │   │   │   ├── ErrorType.java               # Error type enum
│   │   │   │   │   └── ProblemDetailMapper.java     # RFC 9457 exception mapper
│   │   │   │   ├── persistence/
│   │   │   │   │   ├── JsonFileRepository.java      # Generic JSON file operations
│   │   │   │   │   └── TenantPathResolver.java      # Resolves data/{tenant}/{split}.json
│   │   │   │   └── validation/
│   │   │   │       └── ValidationMessages.java      # Shared validation messages
│   │   │   │
│   │   │   ├── split/
│   │   │   │   ├── api/
│   │   │   │   │   └── SplitResource.java           # REST endpoints for splits
│   │   │   │   ├── domain/
│   │   │   │   │   ├── Split.java                   # Split aggregate root
│   │   │   │   │   ├── SplitId.java                 # NanoID value object
│   │   │   │   │   └── CreateSplitRequest.java      # DTO with validation
│   │   │   │   └── service/
│   │   │   │       └── SplitService.java            # Business logic
│   │   │   │
│   │   │   ├── participant/
│   │   │   │   ├── api/
│   │   │   │   │   └── ParticipantResource.java     # REST endpoints
│   │   │   │   ├── domain/
│   │   │   │   │   ├── Participant.java             # Entity
│   │   │   │   │   └── ParticipantRequest.java      # DTO
│   │   │   │   └── service/
│   │   │   │       └── ParticipantService.java      # Business logic
│   │   │   │
│   │   │   ├── expense/
│   │   │   │   ├── api/
│   │   │   │   │   └── ExpenseResource.java         # REST endpoints
│   │   │   │   ├── domain/
│   │   │   │   │   ├── Expense.java                 # Entity
│   │   │   │   │   ├── SplitMode.java               # BY_NIGHT, EQUAL, FREE enum
│   │   │   │   │   └── ExpenseRequest.java          # DTO
│   │   │   │   └── service/
│   │   │   │       ├── ExpenseService.java          # Business logic
│   │   │   │       └── SplitCalculator.java         # By-night/equal/free calculations
│   │   │   │
│   │   │   ├── settlement/
│   │   │   │   ├── api/
│   │   │   │   │   └── SettlementResource.java      # REST endpoints
│   │   │   │   ├── domain/
│   │   │   │   │   ├── Balance.java                 # Per-participant balance
│   │   │   │   │   └── Transfer.java                # Optimized transfer
│   │   │   │   └── service/
│   │   │   │       ├── BalanceService.java          # Calculate balances
│   │   │   │       └── TransactionOptimizer.java    # Minimize transfers algorithm
│   │   │   │
│   │   │   └── feedback/
│   │   │       ├── api/
│   │   │       │   └── FeedbackResource.java        # REST endpoints
│   │   │       ├── domain/
│   │   │       │   ├── Feedback.java                # Entity
│   │   │       │   └── FeedbackType.java            # THANKS, IMPROVEMENT enum
│   │   │       └── service/
│   │   │           └── FeedbackService.java         # Business logic
│   │   │
│   │   ├── resources/
│   │   │   ├── application.properties               # Quarkus config
│   │   │   └── application-dev.properties           # Dev profile overrides
│   │   │
│   │   └── webui/                                   # Svelte 5 frontend (Quinoa)
│   │       ├── package.json
│   │       ├── vite.config.ts
│   │       ├── svelte.config.js
│   │       ├── tsconfig.json
│   │       ├── tailwind.config.js
│   │       ├── postcss.config.js
│   │       ├── index.html
│   │       │
│   │       ├── src/
│   │       │   ├── main.ts                          # Entry point
│   │       │   ├── App.svelte                       # Root component + router
│   │       │   ├── app.css                          # Global styles + Tailwind
│   │       │   │
│   │       │   ├── lib/
│   │       │   │   ├── api/
│   │       │   │   │   ├── client.ts                # Fetch wrapper with error handling
│   │       │   │   │   ├── splits.ts                # Split API calls
│   │       │   │   │   ├── participants.ts          # Participant API calls
│   │       │   │   │   ├── expenses.ts              # Expense API calls
│   │       │   │   │   └── settlement.ts            # Settlement API calls
│   │       │   │   │
│   │       │   │   ├── stores/
│   │       │   │   │   ├── splitStore.svelte.ts     # Current split state
│   │       │   │   │   └── toastStore.svelte.ts     # Toast notifications
│   │       │   │   │
│   │       │   │   ├── components/
│   │       │   │   │   ├── ui/
│   │       │   │   │   │   ├── Button.svelte
│   │       │   │   │   │   ├── Input.svelte
│   │       │   │   │   │   ├── Card.svelte
│   │       │   │   │   │   ├── Toast.svelte
│   │       │   │   │   │   └── Modal.svelte
│   │       │   │   │   │
│   │       │   │   │   ├── split/
│   │       │   │   │   │   ├── SplitHeader.svelte
│   │       │   │   │   │   └── ShareLink.svelte
│   │       │   │   │   │
│   │       │   │   │   ├── participant/
│   │       │   │   │   │   ├── ParticipantList.svelte
│   │       │   │   │   │   ├── ParticipantCard.svelte
│   │       │   │   │   │   └── AddParticipantForm.svelte
│   │       │   │   │   │
│   │       │   │   │   ├── expense/
│   │       │   │   │   │   ├── ExpenseList.svelte
│   │       │   │   │   │   ├── ExpenseCard.svelte
│   │       │   │   │   │   └── AddExpenseForm.svelte
│   │       │   │   │   │
│   │       │   │   │   └── settlement/
│   │       │   │   │       ├── BalanceSummary.svelte
│   │       │   │   │       ├── TransferList.svelte
│   │       │   │   │       └── OptimizeButton.svelte
│   │       │   │   │
│   │       │   │   └── utils/
│   │       │   │       ├── formatCurrency.ts
│   │       │   │       ├── formatDate.ts
│   │       │   │       └── validation.ts
│   │       │   │
│   │       │   └── routes/
│   │       │       ├── Home.svelte                  # Landing / create split
│   │       │       ├── Split.svelte                 # Split overview page
│   │       │       ├── Participants.svelte          # Manage participants
│   │       │       ├── Expenses.svelte              # Manage expenses
│   │       │       └── Settlement.svelte            # Settlement page
│   │       │
│   │       └── static/
│   │           └── favicon.ico
│   │
│   └── test/
│       └── java/org/asymetrik/web/fairnsquare/
│           ├── split/
│           │   └── api/
│           │       └── SplitResourceIT.java         # Integration tests
│           ├── participant/
│           │   └── api/
│           │       └── ParticipantResourceIT.java
│           ├── expense/
│           │   └── api/
│           │       └── ExpenseResourceIT.java
│           ├── settlement/
│           │   └── service/
│           │       └── TransactionOptimizerIT.java
│           └── TestDataFactory.java                 # Shared test fixtures
│
└── data/                                            # JSON file storage (gitignored)
    └── {tenant-id}/
        └── {split-id}.json
```

### Architectural Boundaries

**API Boundaries:**

| Endpoint Pattern | Resource | Responsibility |
|-----------------|----------|----------------|
| `POST /api/splits` | SplitResource | Create new split |
| `GET /api/splits/{splitId}` | SplitResource | Get split with all data |
| `*/splits/{splitId}/participants/*` | ParticipantResource | CRUD participants |
| `*/splits/{splitId}/expenses/*` | ExpenseResource | CRUD expenses |
| `*/splits/{splitId}/settlement` | SettlementResource | Get balances, optimize |
| `*/splits/{splitId}/feedback` | FeedbackResource | Submit feedback |

**Module Boundaries:**

| Module | Depends On | Never Depends On |
|--------|------------|------------------|
| sharedkernel | Nothing | Any feature module |
| split | sharedkernel | participant, expense, settlement |
| participant | sharedkernel, split | expense, settlement |
| expense | sharedkernel, split, participant | settlement |
| settlement | sharedkernel, split, participant, expense | feedback |
| feedback | sharedkernel, split | participant, expense, settlement |

**Data Boundaries:**

| Boundary | Pattern |
|----------|---------|
| Tenant isolation | `data/{tenantId}/{splitId}.json` |
| Split aggregate | Single JSON file contains split + participants + expenses |
| Read/Write | File-level locking per split (Java NIO) |

### Integration Points

**Internal Communication:**
- Modules communicate via injected services (CDI)
- No direct cross-module entity access
- Settlement reads expense/participant data through services

**External Integrations:**
- None for MVP (no payment, auth, or third-party services)

**Data Flow:**
```
Frontend → REST API → Service → JsonFileRepository → File System
                ↓
         Bean Validation
                ↓
         Problem Details (errors)
```

### Development Workflow Integration

**Development:**
```bash
mvn quarkus:dev     # Starts backend + Vite dev server (via Quinoa)
```

**Build:**
```bash
mvn package         # Builds JAR with bundled frontend
```

**Docker:**
```bash
docker build -t fairnsquare .
docker run -v ./data:/data -p 8080:8080 fairnsquare
```

## Architecture Validation Results

### Coherence Validation ✅

**Decision Compatibility:** All technology choices verified compatible. Quarkus 3.30.x + Java 25 + Quinoa + Svelte 5 is a tested, supported stack.

**Pattern Consistency:** All naming conventions (camelCase JSON, PascalCase classes, kebab-case CSS) are consistently applied across frontend and backend.

**Structure Alignment:** Project structure directly supports modular monolith pattern with clear module boundaries.

### Requirements Coverage Validation ✅

**Functional Requirements:** All 28 FRs mapped to specific modules with clear ownership.

**Non-Functional Requirements:** All NFRs architecturally supported:
- Performance: Lightweight stack, no DB overhead
- Security: Path-based tenant isolation, NanoID for unpredictable URLs
- Reliability: Synchronous writes, health checks
- Observability: OpenTelemetry auto-instrumentation

### Implementation Readiness Validation ✅

**Decision Completeness:** All critical decisions documented with specific versions and rationale.

**Pattern Completeness:** 12 potential conflict points addressed with explicit patterns and examples.

**Structure Completeness:** Full project tree defined to file level with clear responsibilities.

### Gap Analysis Results

**Critical Gaps:** None

**Minor Gaps (resolved during implementation):**
- NanoID Java: Use `com.aventrix.jnanoid:jnanoid:2.0.0`
- Currency: Use `BigDecimal`, default EUR
- Timezone: Store UTC, display local

### Architecture Completeness Checklist

**✅ Requirements Analysis**
- [x] Project context thoroughly analyzed
- [x] Scale and complexity assessed (Low-Medium)
- [x] Technical constraints identified (Quarkus, Svelte 5, no DB)
- [x] Cross-cutting concerns mapped (multi-tenancy, observability, validation)

**✅ Architectural Decisions**
- [x] Critical decisions documented with versions
- [x] Technology stack fully specified
- [x] Integration patterns defined (CDI injection, REST)
- [x] Performance considerations addressed

**✅ Implementation Patterns**
- [x] Naming conventions established (12 patterns)
- [x] Structure patterns defined
- [x] Communication patterns specified
- [x] Process patterns documented (loading, error handling, logging)

**✅ Project Structure**
- [x] Complete directory structure defined (~60 files)
- [x] Component boundaries established (6 modules)
- [x] Integration points mapped
- [x] Requirements to structure mapping complete

### Architecture Readiness Assessment

**Overall Status:** READY FOR IMPLEMENTATION

**Confidence Level:** High

**Key Strengths:**
- Simple, focused architecture matching project scope
- Clear module boundaries prevent AI agent conflicts
- Comprehensive patterns with concrete examples
- File-based persistence eliminates DB complexity

**Areas for Future Enhancement:**
- Native image compilation for faster startup (post-MVP)
- Caching layer if performance becomes an issue
- Backup/archival strategy for old splits

### Implementation Handoff

**AI Agent Guidelines:**
1. Follow all architectural decisions exactly as documented
2. Use implementation patterns consistently across all components
3. Respect project structure and module boundaries
4. Refer to this document for all architectural questions

**First Implementation Priority:**
```bash
quarkus create app org.asymetrik.web:fairnsquare \
  --extension=resteasy-reactive-jackson,quinoa,opentelemetry,smallrye-health,smallrye-openapi \
  --java=25 \
  --no-code
```

---

## Technical Debt Epic: Code Quality & Maintainability Enhancement

**Epic ID:** TD-001  
**Created:** 2026-01-28  
**Status:** Planned  
**Architectural Theme:** Refactoring for extensibility, test quality, and documentation alignment

### Epic Overview

This epic addresses accumulated technical debt across three architectural dimensions:
1. **Domain Model Evolution** - Transition Expense to extensible polymorphic pattern
2. **Testing Infrastructure** - Modernize assertions, improve test organization, enforce integration patterns
3. **Documentation Synchronization** - Align architecture-code-docs after refactoring

**Business Value:** Improved maintainability, easier onboarding, reduced cognitive load for AI agents implementing future features.

**Architectural Impact:** Medium - touches core domain model and test infrastructure but preserves external APIs.

---

### Item 1: Expense Domain Model Refactoring

**Current State:**
- `Expense` is a concrete class with conditional logic for split calculation
- Split modes (BY_NIGHT, EQUAL) handled via switch/if statements

**Target State:**
- `Expense` becomes a **sealed abstract class** (Java 17+ syntax)
- Concrete implementations: `ExpenseByNight`, `ExpenseEqual`
- Each implementation encapsulates its own `calculateShare()` logic

**Architectural Rationale:**
- **Strategy Pattern:** Eliminates conditional complexity, each expense type is self-contained
- **Open/Closed Principle:** New expense types (e.g., BY_PERCENTAGE) can be added without modifying existing code
- **Sealed Classes:** Compiler-enforced exhaustiveness checks in pattern matching

**Design Constraints:**
- Must maintain JSON serialization compatibility (Jackson polymorphic deserialization)
- Database schema unchanged (if persistence layer uses discriminator column)
- External API contracts unchanged (REST endpoints still accept same JSON)

**Implementation Pattern:**
```java
public sealed abstract class Expense permits ExpenseByNight, ExpenseEqual {
    protected Long id;
    protected String description;
    protected BigDecimal amount;
    
    public abstract BigDecimal calculateShare(Participant participant);
}

public final class ExpenseByNight extends Expense {
    @Override
    public BigDecimal calculateShare(Participant participant) {
        // BY_NIGHT logic
    }
}

public final class ExpenseEqual extends Expense {
    @Override
    public BigDecimal calculateShare(Participant participant) {
        // EQUAL logic
    }
}
```

**Jackson Configuration:**
```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ExpenseByNight.class, name = "BY_NIGHT"),
    @JsonSubTypes.Type(value = ExpenseEqual.class, name = "EQUAL")
})
```

**Testing Impact:** All existing Expense tests must be updated to use concrete types.

---

### Item 2: AssertJ Migration

**Current State:**
- JUnit 5 assertions (`assertEquals`, `assertTrue`, etc.)

**Target State:**
- AssertJ fluent assertions throughout test suite

**Architectural Rationale:**
- **Readability:** Fluent API is more natural language-like
- **Error Messages:** Better default failure messages
- **Discoverability:** IDE autocomplete guides assertion discovery

**Migration Pattern:**
```java
// Before
assertEquals(expected, actual);
assertTrue(condition);
assertNotNull(value);

// After
assertThat(actual).isEqualTo(expected);
assertThat(condition).isTrue();
assertThat(value).isNotNull();
```

**Dependency Addition:**
```xml
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.25.1</version>
    <scope>test</scope>
</dependency>
```

**Testing Impact:** All test classes require assertion statement updates.

---

### Item 3: Service Layer Naming & Test Organization

**Current State:**
- Service class: `SplitService`
- Test class: `SplitServiceTest` (monolithic)

**Target State:**
- Service class: `SplitUseCases` (better DDD naming)
- Test classes: One per use case
  - `CreateSplitUseCaseTest`
  - `AddParticipantUseCaseTest`
  - `AddExpenseUseCaseTest`
  - `CalculateBalancesUseCaseTest`
  - etc.

**Architectural Rationale:**
- **Domain-Driven Design:** "Use Case" better reflects application service layer responsibility
- **Single Responsibility:** Each test class focuses on one behavioral scenario
- **Maintainability:** Easier to locate tests, smaller files, less merge conflicts

**Refactoring Steps:**
1. Rename `SplitService` → `SplitUseCases` (IDE refactor)
2. Update all injection points
3. Split `SplitServiceTest` by extracting test methods into new test classes
4. Each new test class follows pattern: `{UseCaseName}UseCaseTest`

**Testing Impact:** Test discovery unchanged (JUnit still finds all `*Test` classes).

---

### Item 4: Test Persistence Pattern Enforcement

**Current State:**
- Some tests directly access `data/` folder on disk
- Tests may leave residual files after execution

**Target State:**
- Tests verify persistence through **public API only**
- Integration test pattern: Create → Save → Load → Verify identity

**Architectural Rationale:**
- **Abstraction:** Tests don't depend on filesystem implementation details
- **Portability:** Tests work regardless of persistence mechanism (file, DB, in-memory)
- **Isolation:** No test pollution from leftover files

**Recommended Test Pattern:**
```java
@Test
void shouldPersistAndReloadSplitWithIdentity() {
    // Create first split
    Split split1 = createSplit("Split A");
    String split1Id = splitUseCases.saveSplit(split1);
    
    // Create second split (forces potential file overwrite issues)
    Split split2 = createSplit("Split B");
    String split2Id = splitUseCases.saveSplit(split2);
    
    // Reload first split
    Split reloadedSplit1 = splitUseCases.loadSplit(split1Id);
    
    // Verify identity
    assertThat(reloadedSplit1).isEqualTo(split1);
    assertThat(reloadedSplit1.getId()).isEqualTo(split1Id);
}
```

**Enforcement Mechanism:**
- Code review guideline: No `new File()`, `Path.of()`, `Files.readString()` in test code
- If filesystem abstraction needed for mocking, introduce `FileSystemService` interface

**Testing Impact:** Refactor any tests directly reading/writing `data/` folder.

---

### Item 5: Coverage Verification Test

**Current State:**
- No automated coverage enforcement

**Target State:**
- Test that verifies code coverage meets threshold
- Initial threshold: **80%**

**Architectural Rationale:**
- **Quality Gate:** Prevents coverage regression
- **Build Integration:** Fails build if coverage drops below threshold
- **Visibility:** Coverage becomes a first-class concern

**Implementation Options:**

**Option A: JaCoCo Maven Plugin (Recommended)**
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Option B: Custom Test (Alternative)**
```java
@Test
void shouldMeetCoverageThreshold() {
    // Read JaCoCo XML report
    // Parse coverage percentage
    // Assert coverage >= 80%
}
```

**Recommendation:** Use Option A (JaCoCo plugin) - standard approach, better tooling integration.

**Testing Impact:** Coverage reports generated during `mvn verify`.

---

### Item 6: Documentation Synchronization

**Current State:**
- Architecture document reflects pre-refactoring state

**Target State:**
- All documentation updated to reflect:
  - Sealed Expense class hierarchy
  - SplitUseCases naming
  - Test organization patterns
  - Coverage requirements

**Files to Update:**
- `architecture.md` (this document - patterns section)
- `project-context.md` (if references service naming)
- `README.md` (if contains code examples)
- Any inline code comments referencing old patterns

**Architectural Rationale:**
- **Consistency:** AI agents rely on documentation for implementation guidance
- **Onboarding:** New developers see accurate system state
- **Maintenance:** Prevents drift between docs and code

**Update Checklist:**
- [ ] Architecture decision document (add this epic section ✅)
- [ ] Implementation patterns section (update Expense example)
- [ ] Project structure section (update test class names)
- [ ] README code examples (if applicable)
- [ ] API documentation (if Expense structure exposed)

---

### Epic Phasing & Dependencies

**Phase 1: Foundation (Independent)**
- Item 2: AssertJ Migration (no dependencies)
- Item 5: Coverage Verification (no dependencies)

**Phase 2: Domain Refactoring (Depends on Phase 1 completion)**
- Item 1: Expense sealed class (easier with AssertJ assertions in place)

**Phase 3: Service Layer Refactoring (Depends on Phase 2 completion)**
- Item 3: Rename service + split tests (easier after Expense refactoring settles)

**Phase 4: Test Pattern Enforcement (Depends on Phase 3 completion)**
- Item 4: Forbid direct file access (do after test split to avoid massive refactor)

**Phase 5: Documentation Sync (Depends on all previous phases)**
- Item 6: Update all docs (final step after code stabilizes)

**Rationale for Phasing:**
- Minimizes merge conflicts
- Each phase can be tested independently
- Allows incremental value delivery

---

### Implementation Readiness Checklist

**Before Starting:**
- [ ] All existing tests passing
- [ ] Git branch created for epic work
- [ ] Baseline code coverage measured

**Per Phase:**
- [ ] Phase implementation complete
- [ ] All tests passing (including new AssertJ assertions)
- [ ] Code coverage maintained or improved
- [ ] Documentation updated for that phase

**Epic Completion Criteria:**
- [ ] All 6 items implemented
- [ ] Coverage ≥ 80% enforced
- [ ] No direct file access in tests
- [ ] All documentation synchronized
- [ ] Architecture document updated (this section merged)

---

### Architecture Impact Summary

| Dimension | Impact Level | Notes |
|-----------|--------------|-------|
| API Contracts | None | External REST APIs unchanged |
| Domain Model | Medium | Expense becomes abstract, but JSON schema identical |
| Service Layer | Low | Rename only, method signatures unchanged |
| Testing | High | All tests updated (assertions, organization, patterns) |
| Documentation | Medium | Multiple files require updates |
| Build Process | Low | JaCoCo plugin added, coverage gate enforced |

**Risk Assessment:**
- **Low Risk:** Items 2, 5, 6 (additive or isolated changes)
- **Medium Risk:** Items 1, 3 (require careful refactoring but well-understood patterns)
- **Medium Risk:** Item 4 (may reveal hidden assumptions about file system behavior)

**Mitigation:**
- Comprehensive test coverage before refactoring (Item 5 first)
- Incremental phasing (prevents big-bang failures)
- Use IDE refactoring tools (reduces manual errors)

---

**Epic Ready for Implementation.** 🚀

