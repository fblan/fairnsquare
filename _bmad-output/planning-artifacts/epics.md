---
stepsCompleted: [1, 2, 3]
storiesComplete:
  - epic1
  - epic2
storiesPending:
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

### Story 1.4: Add shadcn-svelte Components

**As a** developer,
**I want** shadcn-svelte UI components installed and configured,
**So that** I can build accessible, consistent UI components with the design system.

**Acceptance Criteria:**

**Given** the Svelte 5 frontend from Story 1.2
**When** shadcn-svelte is initialized
**Then** the CLI completes without errors
**And** base components are available: Button, Input, Card, Label
**And** components use the project's CSS custom properties for theming
**And** components follow the UX spec color palette (Teal primary #0D9488)
**And** all components are accessible (keyboard navigation, focus states)
**And** components work with Svelte 5 runes syntax
**And** the project builds successfully with `npm run build`

---

## Epic 2: Split Creation & Access - Stories

### Story 2.1: Create Split Backend API

**As a** developer integrating with FairNSquare,
**I want** a REST API to create splits with automatic NanoID generation and JSON file persistence,
**So that** splits can be created and stored reliably with proper tenant isolation.

**Acceptance Criteria:**

**Given** the API is running
**When** a POST request is sent to `/api/splits` with body `{"name": "Bordeaux Weekend 2026"}`
**Then** the response status is 201 Created
**And** the response body contains:
  - `id`: a 21-character NanoID (URL-safe characters only)
  - `name`: the provided split name
  - `createdAt`: ISO 8601 timestamp
  - `participants`: empty array
  - `expenses`: empty array

**Given** a split is created successfully
**When** the file system is checked
**Then** a JSON file exists at `data/default/{splitId}.json`
**And** the file contains the complete split data with all fields

**Given** an invalid request with empty name `{"name": ""}`
**When** POST is sent to `/api/splits`
**Then** the response status is 400 Bad Request
**And** the response follows Problem Details (RFC 9457) format with:
  - `type`: error type URI
  - `title`: "Validation Error"
  - `status`: 400
  - `detail`: description of the validation failure

**Given** an invalid request with missing name `{}`
**When** POST is sent to `/api/splits`
**Then** the response status is 400 Bad Request
**And** the response follows Problem Details format

**Given** the data directory does not exist
**When** a split is created
**Then** the directory structure `data/default/` is created automatically
**And** the split file is persisted successfully

**Given** the `FAIRNSQUARE_DATA_PATH` environment variable is set to `/custom/path`
**When** a split is created
**Then** the split file is stored at `/custom/path/default/{splitId}.json`

---

### Story 2.2: Create Split Frontend

**As a** user,
**I want** to create a new split by entering a name and instantly receive a shareable link,
**So that** I can start tracking expenses for my group with zero friction.

**Acceptance Criteria:**

**Given** I am on the home page
**When** the page loads
**Then** I see a form with:
  - A text input for split name with placeholder "e.g., Bordeaux Weekend 2026"
  - A "Create Split" button
  - The button uses the primary teal color (#0D9488)

**Given** I am on the home page
**When** I enter a split name "Beach Trip 2026" and click "Create Split"
**Then** a loading state is shown on the button
**And** the API is called to create the split
**And** on success, I am shown the shareable link prominently

**Given** a split was just created successfully
**When** the success state is displayed
**Then** I see the full shareable URL (e.g., `https://app.fairnsquare.com/splits/{splitId}`)
**And** I see a "Copy Link" button next to the URL
**And** clicking "Copy Link" copies the URL to clipboard and shows confirmation
**And** I see a "Go to Split" button to navigate to the split overview

**Given** I try to create a split with an empty name
**When** I click "Create Split"
**Then** the form shows a validation error "Split name is required"
**And** the API is not called

**Given** the API returns an error
**When** creating a split
**Then** an error toast is displayed with the error message
**And** the form remains editable for retry

**Given** I am on a mobile device (< 768px)
**When** viewing the create split form
**Then** the form is centered with max-width 420px
**And** touch targets are at least 44px in height
**And** the input and button are full-width

---

### Story 2.3: Access Split via Link & View Overview

**As a** user with a shared link,
**I want** to access the split directly and see an overview of the split state,
**So that** I can immediately understand who's participating and what expenses exist.

**Acceptance Criteria:**

**Given** I have a valid split link `https://app.fairnsquare.com/splits/{splitId}`
**When** I navigate to that URL
**Then** the split overview page loads
**And** I see the split name as the page title/header
**And** I see a "Share" button to copy the link

**Given** I am viewing a split overview
**When** the page loads
**Then** I see the following sections:
  - **Header**: Split name + share button
  - **Participants**: List showing "No participants yet" or participant names with nights
  - **Expenses**: List showing "No expenses yet" or expense summaries
  - **Balance Summary**: Shows "Add participants and expenses to see balances"

**Given** the split has participants (from future epic)
**When** viewing the overview
**Then** each participant card shows: name, number of nights, running balance
**And** participants are displayed in a vertical list

**Given** the split has expenses (from future epic)
**When** viewing the overview
**Then** each expense shows: description, amount, payer name, split mode badge
**And** expenses are displayed in chronological order (newest first)

**Given** I navigate to a split that doesn't exist
**When** the page attempts to load
**Then** I see a 404 error page with message "Split not found"
**And** I see a link to "Create a new split" that goes to the home page

**Given** the API call to fetch the split fails (network error)
**When** loading the split page
**Then** I see an error message "Failed to load split. Please try again."
**And** I see a "Retry" button to attempt loading again

**Given** I am viewing the split on mobile (< 768px)
**When** the page renders
**Then** the layout is single-column
**And** cards have 16px padding and 8px border radius
**And** the max content width is 420px, centered

**Given** the split URL uses sv-router
**When** I navigate to `/splits/{splitId}`
**Then** the route parameter `splitId` is extracted
**And** the split data is fetched from `GET /api/splits/{splitId}`

**Given** a GET request is made to `/api/splits/{splitId}`
**When** the split exists
**Then** the response status is 200 OK
**And** the response contains the full split data (id, name, createdAt, participants, expenses)

**Given** a GET request is made to `/api/splits/{splitId}`
**When** the split does not exist
**Then** the response status is 404 Not Found
**And** the response follows Problem Details format

---

<!-- Stories for Epics 3-6 pending -->