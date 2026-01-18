---
stepsCompleted: [1, 2, '3-partial']
storiesComplete:
  - epic1
storiesPending:
  - epic2
  - epic3
  - epic4
  - epic5
  - epic6
inputDocuments:
  - '_bmad-output/planning-artifacts/prd.md'
  - '_bmad-output/planning-artifacts/architecture.md'
  - '_bmad-output/planning-artifacts/ux-design-specification.md'
  - '_bmad-output/planning-artifacts/product-brief-FairNSquare-2026-01-16.md'
date: '2026-01-17'
author: 'Fred'
projectName: 'FairNSquare'
---

# FairNSquare - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for FairNSquare, decomposing the requirements from the PRD, UX Design, and Architecture into implementable stories.

## Requirements Inventory

### Functional Requirements

**Split Management**
- FR1: User can create a new split with a name
- FR2: System generates a unique shareable link for each split
- FR3: User can access an existing split via its shareable link without authentication
- FR4: User can view the split overview (name, participants, expenses, balances)

**Participant Management**
- FR5: User can add a participant with a name and number of nights
- FR6: User can edit a participant's name or number of nights
- FR7: User can remove a participant who has no associated expenses
- FR8: System prevents removal of participants with associated expenses
- FR9: System defaults the "nights" field to the last entered value when adding participants

**Expense Management**
- FR10: User can add an expense with amount, description, and payer
- FR11: User can select a split mode for each expense: by-night (default), equal, or free
- FR12: System calculates each participant's share based on the selected split mode
- FR13: For "by-night" mode, system distributes expense proportionally to nights stayed
- FR14: For "equal" mode, system distributes expense equally among all participants
- FR15: For "free" mode, user can manually specify each participant's share
- FR16: User can edit an existing expense (amount, description, payer, split mode)
- FR17: User can remove an expense

**Balance & Settlement**
- FR18: User can view running balance per participant at any time
- FR19: User can view settlement summary showing who owes whom and how much
- FR20: User can optimize transactions to minimize the number of transfers needed
- FR21: System calculates optimized transfer list from debits/credits

**User Feedback**
- FR22: User can submit positive feedback via "Thanks!" button on settlement page
- FR23: User can submit improvement ideas via form on settlement page
- FR24: System records feedback submissions for later review

**System Operations**
- FR25: System isolates data between tenants (no data leakage)
- FR26: System instruments all REST endpoints with OpenTelemetry
- FR27: System records split-mode usage per expense for analytics
- FR28: System persists split data reliably across sessions

### NonFunctional Requirements

**Performance**
- NFR1: First Contentful Paint < 1.5s
- NFR2: Time to Interactive < 3s
- NFR3: API Response Time < 500ms (p95)
- NFR4: Bundle Size < 200KB gzipped
- NFR5: Lighthouse Performance Score > 80

**Security**
- NFR6: Each tenant's data is strictly isolated; no cross-tenant data leakage
- NFR7: All traffic over HTTPS
- NFR8: All user inputs validated server-side to prevent injection
- NFR9: Split links use cryptographically random identifiers (NanoID)

**Reliability**
- NFR10: 99%+ uptime during holiday seasons
- NFR11: Zero data loss - splits must persist reliably
- NFR12: Graceful degradation on failures - show error states, don't crash
- NFR13: Regular data backups for recovery

**Observability**
- NFR14: OpenTelemetry traces on all REST endpoints
- NFR15: Response time, error rate, request count metrics per endpoint
- NFR16: Structured logs correlated with trace context
- NFR17: Baseline alerts for error rate spikes, latency anomalies

### Additional Requirements

**From Architecture - Starter Template (CRITICAL: Epic 1, Story 1)**
- Project initialization using Quarkus CLI + Vite Svelte template
- Quarkus 3.30.x with Java 25
- Extensions: resteasy-reactive-jackson, quinoa, opentelemetry, smallrye-health, smallrye-openapi
- Svelte 5 with TypeScript in src/main/webui/

**From Architecture - Data Layer**
- JSON file-based storage pattern: `data/{tenant-id}/{split-id}.json`
- NanoID (21 chars) for split identifiers
- Jackson for serialization/deserialization
- File-per-split pattern for natural tenant isolation
- Configurable data path via `FAIRNSQUARE_DATA_PATH` env var

**From Architecture - API Design**
- Split-scoped REST paths: `/api/splits/{splitId}/...`
- Problem Details (RFC 9457) for error responses
- Bean Validation for request validation
- SmallRye OpenAPI for documentation

**From Architecture - Frontend**
- sv-router for client-side routing
- Svelte 5 runes for state management (no external state libraries)
- Tailwind CSS for styling
- Per-component loading state pattern

**From Architecture - Infrastructure**
- Docker with Quarkus JVM mode
- Modular monolith with DDD shared kernel pattern
- Package structure: sharedkernel, split, participant, expense, settlement, feedback

**From UX Design**
- Mobile-first design (320px - 767px primary)
- Touch targets minimum 44px
- System fonts (`-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif`)
- Teal color palette (#0D9488 primary)
- CSS custom properties for white-label theming
- WCAG 2.1 Level A accessibility (keyboard nav, alt text, form labels, focus visible)
- Max content width 420px
- Border radius 8px, card padding 16px

### FR Coverage Map

| FR | Epic | Description |
|----|------|-------------|
| FR1 | Epic 2 | Create split with name |
| FR2 | Epic 2 | Generate shareable link |
| FR3 | Epic 2 | Access via link (no auth) |
| FR4 | Epic 2 | View split overview |
| FR5 | Epic 3 | Add participant with name + nights |
| FR6 | Epic 3 | Edit participant details |
| FR7 | Epic 3 | Remove participant (no expenses) |
| FR8 | Epic 3 | Prevent removal with expenses |
| FR9 | Epic 3 | Smart default for nights |
| FR10 | Epic 4 | Add expense (amount, description, payer) |
| FR11 | Epic 4 | Select split mode |
| FR12 | Epic 4 | Calculate shares by mode |
| FR13 | Epic 4 | By-night proportional distribution |
| FR14 | Epic 4 | Equal distribution |
| FR15 | Epic 4 | Free manual distribution |
| FR16 | Epic 4 | Edit expense |
| FR17 | Epic 4 | Remove expense |
| FR18 | Epic 5 | Running balance per participant |
| FR19 | Epic 5 | Settlement summary |
| FR20 | Epic 5 | Optimize transactions |
| FR21 | Epic 5 | Calculate optimized transfers |
| FR22 | Epic 6 | Thanks button |
| FR23 | Epic 6 | Improvement idea form |
| FR24 | Epic 6 | Record feedback |
| FR25 | Epic 2 | Tenant data isolation |
| FR26 | Epic 1 | OpenTelemetry instrumentation |
| FR27 | Epic 4 | Split-mode analytics |
| FR28 | Epic 2 | Reliable data persistence |

## Epic List

### Epic 1: Project Foundation
**Goal:** Development team has a running application skeleton with proper tooling, observability, and dev experience ready for feature development.

**User Value:** Without the foundation, nothing else works. This is the "power on" moment that enables all subsequent functionality.

**FRs Covered:** FR26

**Key Deliverables:**
- Quarkus 3.30.x + Java 25 project initialized
- Svelte 5 + TypeScript frontend via Quinoa
- OpenTelemetry instrumentation configured
- Health endpoints operational
- Development workflow (hot reload) working
- Basic project structure per Architecture document

---

### Epic 2: Split Creation & Access
**Goal:** Users can create a new split, get a shareable link, and anyone with the link can access and view the split.

**User Value:** The entry point to FairNSquare - zero-friction split creation and sharing.

**FRs Covered:** FR1, FR2, FR3, FR4, FR25, FR28

**Key Deliverables:**
- Create split with name
- Generate shareable NanoID link
- Access split via link (no authentication)
- View split overview page
- JSON file-based persistence
- Tenant data isolation

---

### Epic 3: Participant Management
**Goal:** Users can add, edit, and remove participants with their names and nights stayed. Smart defaults reduce friction.

**User Value:** Set up who's in the split with their stay duration - the foundation for fair "by-night" calculations.

**FRs Covered:** FR5, FR6, FR7, FR8, FR9

**Key Deliverables:**
- Add participant with name + nights
- Edit participant details (inline)
- Remove participant (with constraint check)
- Prevent removal if participant has expenses
- Smart default: nights defaults to last entered value

---

### Epic 4: Expense Tracking
**Goal:** Users can log expenses with three split modes (by-night, equal, free) - the core value proposition.

**User Value:** The heart of FairNSquare - effortless expense entry with automatic fair calculations.

**FRs Covered:** FR10, FR11, FR12, FR13, FR14, FR15, FR16, FR17, FR27

**Key Deliverables:**
- Add expense (amount, description, payer selection)
- Three split modes: by-night (default), equal, free
- Automatic share calculation per mode
- By-night: proportional to nights stayed
- Equal: even split among all participants
- Free: manual share specification
- Edit/remove expenses
- Split-mode usage analytics

---

### Epic 5: Balance & Settlement
**Goal:** Users can see who owes whom and optimize transactions to minimize transfers - the "that was easy!" payoff moment.

**User Value:** The moment of truth - clear settlement view and the delightful optimizer that reduces 6 transfers to 3.

**FRs Covered:** FR18, FR19, FR20, FR21

**Key Deliverables:**
- Running balance per participant (real-time)
- Settlement summary: who owes whom, how much
- Transaction optimizer algorithm
- Optimized transfer list display
- Clear "who pays whom" presentation

---

### Epic 6: User Feedback
**Goal:** Users can provide feedback on the settlement page - positive ("Thanks!") or improvement suggestions.

**User Value:** Capture user sentiment at the moment of success and gather improvement ideas.

**FRs Covered:** FR22, FR23, FR24

**Key Deliverables:**
- "Thanks!" button on settlement page
- "Improvement idea" form with text input
- Feedback storage for later review
- Success confirmation after submission

---

## Epic 1: Project Foundation - Stories

### Story 1.1: Initialize Quarkus Backend Project

**As a** developer,
**I want** a Quarkus backend project initialized with the required extensions and package structure,
**So that** I have a solid foundation for building the FairNSquare API.

**Acceptance Criteria:**

**Given** a fresh project directory
**When** the Quarkus project is created using CLI
**Then** the project compiles successfully with `mvn compile`
**And** the following extensions are included: resteasy-reactive-jackson, quinoa, opentelemetry, smallrye-health, smallrye-openapi
**And** Java 25 is configured in pom.xml
**And** the package structure follows Architecture: `org.asymetrik.web.fairnsquare` with `sharedkernel/` subdirectory created
**And** `GET /q/health` returns 200 OK with status "UP"
**And** `GET /q/health/ready` returns 200 OK
**And** `GET /q/health/live` returns 200 OK

---

### Story 1.2: Initialize Svelte 5 Frontend

**As a** developer,
**I want** a Svelte 5 frontend with TypeScript and Tailwind CSS configured,
**So that** I can build the mobile-first UI with the design system tokens.

**Acceptance Criteria:**

**Given** the Quarkus project from Story 1.1
**When** the Svelte 5 frontend is initialized in `src/main/webui/`
**Then** `npm install` completes without errors
**And** Svelte 5 with runes is configured (not legacy mode)
**And** TypeScript is enabled with strict mode
**And** Tailwind CSS is installed and configured
**And** CSS custom properties are defined for theming (primary: #0D9488, etc. per UX spec)
**And** the frontend directory structure matches Architecture: `lib/`, `routes/`, `App.svelte`, `main.ts`
**And** running `mvn quarkus:dev` serves the frontend at the root path
**And** hot reload works for both Java and Svelte changes

---

### Story 1.3: Configure OpenTelemetry & Dev Experience

**As a** developer,
**I want** OpenTelemetry configured with structured logging,
**So that** all API endpoints are automatically instrumented for observability.

**Acceptance Criteria:**

**Given** the project from Stories 1.1 and 1.2
**When** OpenTelemetry is configured in `application.properties`
**Then** traces are generated for all REST endpoint calls
**And** trace IDs appear in log output
**And** logs use structured JSON format in production profile
**And** logs use readable format in dev profile
**And** `GET /q/swagger-ui` displays the OpenAPI documentation
**And** the complete dev workflow runs with single command: `mvn quarkus:dev`
**And** the application starts in under 10 seconds in dev mode

---

<!-- Stories for Epics 2-6 pending - to be created after Epic 1 implementation -->