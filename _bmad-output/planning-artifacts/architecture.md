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
- Naming: `*IT.java` for integration tests
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
