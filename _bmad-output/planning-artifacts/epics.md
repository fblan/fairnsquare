---
stepsCompleted: [1, 2, 3]
storiesComplete:
  - epic1
  - epic2
  - epic3
  - epic4
storiesPending:
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

## Epic 3: Participant Management - Stories

### Story 3.1: Add Participant with Smart Defaults

**As a** user managing a split,
**I want** to add participants with their name and number of nights,
**So that** expenses can be fairly calculated based on stay duration.

**Acceptance Criteria:**

**Given** I am viewing a split overview
**When** I look at the Participants section
**Then** I see an "Add Participant" button with a plus icon
**And** the button uses touch-friendly sizing (min 44px height)

**Given** I click "Add Participant"
**When** the add form appears
**Then** I see input fields for:
  - Name (text input, required)
  - Nights (number input, required, min 1)
**And** the form appears inline or as a slide-up panel on mobile
**And** the Nights field is pre-filled with the smart default value

**Given** no participants have been added to this split yet
**When** adding the first participant
**Then** the Nights field defaults to 1

**Given** I previously added a participant with 3 nights
**When** I add another participant
**Then** the Nights field defaults to 3 (last entered value)

**Given** I enter a valid name "Alice" and nights "2"
**When** I click "Add" or submit the form
**Then** the API is called POST `/api/splits/{splitId}/participants`
**And** a loading indicator appears on the submit button
**And** on success, the new participant appears in the list
**And** the form clears and closes
**And** a success toast shows "Participant added"

**Given** I try to add a participant with empty name
**When** I submit the form
**Then** validation error "Name is required" is shown inline
**And** the API is not called

**Given** I try to add a participant with nights less than 1
**When** I submit the form
**Then** validation error "Nights must be at least 1" is shown inline
**And** the API is not called

**Given** the API returns an error
**When** adding a participant
**Then** an error toast displays the error message
**And** the form remains open for retry

**Given** a POST request to `/api/splits/{splitId}/participants` with body `{"name": "Alice", "nights": 2}`
**When** the split exists
**Then** response status is 201 Created
**And** response body contains:
  - `id`: generated NanoID (21 chars)
  - `name`: "Alice"
  - `nights`: 2
**And** the participant is persisted in the split's JSON file

**Given** a POST request with invalid data
**When** name is empty or nights < 1
**Then** response status is 400 Bad Request
**And** response follows Problem Details format

**Given** a POST request to a non-existent split
**When** the splitId is invalid
**Then** response status is 404 Not Found

---

### Story 3.2: Edit Participant Inline

**As a** user managing a split,
**I want** to edit a participant's name or nights inline,
**So that** I can correct mistakes without navigating away.

**Acceptance Criteria:**

**Given** I am viewing a split with participants
**When** I tap/click on a participant card
**Then** the card enters edit mode inline
**And** the name becomes an editable text input
**And** the nights becomes an editable number input
**And** I see "Save" and "Cancel" buttons

**Given** I am in edit mode for a participant
**When** I change the name to "Bob" and nights to 4
**And** I click "Save"
**Then** the API is called PUT `/api/splits/{splitId}/participants/{participantId}`
**And** a loading indicator appears
**And** on success, the card shows updated values
**And** edit mode closes
**And** a success toast shows "Participant updated"

**Given** I am in edit mode
**When** I click "Cancel"
**Then** edit mode closes
**And** original values are restored
**And** no API call is made

**Given** I try to save with empty name
**When** I click "Save"
**Then** validation error "Name is required" is shown
**And** the API is not called

**Given** I try to save with nights less than 1
**When** I click "Save"
**Then** validation error "Nights must be at least 1" is shown
**And** the API is not called

**Given** a PUT request to `/api/splits/{splitId}/participants/{participantId}` with body `{"name": "Bob", "nights": 4}`
**When** the participant exists
**Then** response status is 200 OK
**And** response body contains the updated participant
**And** the split's JSON file is updated

**Given** a PUT request to a non-existent participant
**When** the participantId is invalid
**Then** response status is 404 Not Found
**And** response follows Problem Details format

**Given** I am viewing on mobile (< 768px)
**When** editing a participant
**Then** the edit inputs are full-width within the card
**And** touch targets are at least 44px
**And** the keyboard appears with appropriate input type (text/number)

---

### Story 3.3: Remove Participant with Expense Constraint

**As a** user managing a split,
**I want** to remove a participant who has no expenses,
**So that** I can correct the participant list without breaking expense records.

**Acceptance Criteria:**

**Given** I am viewing a split with participants
**When** I look at a participant card
**Then** I see a delete/remove button (trash icon)
**And** the button is styled as a subtle secondary action

**Given** I click delete on a participant with NO associated expenses
**When** the confirmation dialog appears
**Then** I see "Remove [Name]?" with the participant's name
**And** I see "This cannot be undone" warning
**And** I see "Remove" (destructive) and "Cancel" buttons

**Given** I confirm deletion of a participant with no expenses
**When** I click "Remove"
**Then** the API is called DELETE `/api/splits/{splitId}/participants/{participantId}`
**And** on success, the participant is removed from the list
**And** a success toast shows "Participant removed"

**Given** I click "Cancel" on the confirmation dialog
**When** the dialog closes
**Then** the participant remains in the list
**And** no API call is made

**Given** I click delete on a participant WITH associated expenses
**When** the API returns 409 Conflict
**Then** an error message is shown: "Cannot remove [Name] - they have associated expenses"
**And** the message suggests: "Remove or reassign their expenses first"
**And** the participant remains in the list

**Given** a DELETE request to `/api/splits/{splitId}/participants/{participantId}`
**When** the participant has NO expenses (payerId or share allocations)
**Then** response status is 204 No Content
**And** the participant is removed from the split's JSON file

**Given** a DELETE request to `/api/splits/{splitId}/participants/{participantId}`
**When** the participant HAS associated expenses
**Then** response status is 409 Conflict
**And** response follows Problem Details format with:
  - `type`: participant-has-expenses
  - `title`: "Participant Has Expenses"
  - `detail`: "Cannot remove participant with associated expenses. Remove or reassign expenses first."

**Given** a DELETE request to a non-existent participant
**When** the participantId is invalid
**Then** response status is 404 Not Found

**Given** I am viewing on mobile
**When** interacting with delete
**Then** the confirmation dialog is a bottom sheet or centered modal
**And** buttons are full-width and touch-friendly (min 44px)

---

## Epic 4: Expense Tracking - Stories

### Story 4.1: Add Expense with Split Mode Selection

**As a** user managing a split,
**I want** to add an expense with amount, description, payer, and split mode,
**So that** the system can calculate fair shares based on my chosen distribution method.

**Acceptance Criteria:**

**Given** I am viewing a split overview with at least one participant
**When** I look at the Expenses section
**Then** I see an "Add Expense" button with a plus icon
**And** the button uses touch-friendly sizing (min 44px height)

**Given** I click "Add Expense"
**When** the add form appears
**Then** I see input fields for:
  - Amount (number input, required, min 0.01)
  - Description (text input, required)
  - Payer (dropdown/select, required, populated with participants)
  - Split Mode (radio/segmented control, default: "By Night")
**And** the form appears inline or as a slide-up panel on mobile
**And** Split Mode shows three options: "By Night", "Equal", "Free"

**Given** I enter valid expense data: amount "150.00", description "Groceries", payer "Alice", mode "By Night"
**When** I click "Add" or submit the form
**Then** the API is called POST `/api/splits/{splitId}/expenses`
**And** a loading indicator appears on the submit button
**And** on success, the new expense appears in the expenses list
**And** the expense card shows: description, amount (€150.00), payer name, split mode badge
**And** the form clears and closes
**And** a success toast shows "Expense added"
**And** running balances update immediately

**Given** I select "Equal" split mode
**When** I submit the expense
**Then** the expense is recorded with splitMode = "EQUAL"
**And** shares are calculated equally among all participants

**Given** I select "By Night" split mode (default)
**When** I submit the expense
**Then** the expense is recorded with splitMode = "BY_NIGHT"
**And** shares are calculated proportionally to each participant's nights

**Given** I try to add an expense with empty description
**When** I submit the form
**Then** validation error "Description is required" is shown inline
**And** the API is not called

**Given** I try to add an expense with amount less than 0.01
**When** I submit the form
**Then** validation error "Amount must be at least €0.01" is shown inline
**And** the API is not called

**Given** I try to add an expense without selecting a payer
**When** I submit the form
**Then** validation error "Payer is required" is shown inline
**And** the API is not called

**Given** the split has no participants
**When** I try to add an expense
**Then** I see a message "Add participants before adding expenses"
**And** the "Add Expense" button is disabled

**Given** the API returns an error
**When** adding an expense
**Then** an error toast displays the error message
**And** the form remains open for retry

**Given** a POST request to `/api/splits/{splitId}/expenses` with body:
```json
{
  "amount": 150.00,
  "description": "Groceries",
  "payerId": "abc123",
  "splitMode": "BY_NIGHT"
}
```
**When** the split and payer exist
**Then** response status is 201 Created
**And** response body contains:
  - `id`: generated NanoID (21 chars)
  - `amount`: 150.00
  - `description`: "Groceries"
  - `payerId`: "abc123"
  - `splitMode`: "BY_NIGHT"
  - `createdAt`: ISO 8601 timestamp
  - `shares`: calculated share per participant
**And** the expense is persisted in the split's JSON file

**Given** a POST request with invalid payerId
**When** the payerId doesn't exist in the split
**Then** response status is 400 Bad Request
**And** response follows Problem Details format with detail about invalid payer

**Given** a POST request to a non-existent split
**When** the splitId is invalid
**Then** response status is 404 Not Found

---

### Story 4.2: Calculate Shares by Split Mode (By-Night and Equal)

**As a** user,
**I want** the system to automatically calculate each participant's share based on the split mode,
**So that** expenses are distributed fairly without manual math.

**Acceptance Criteria:**

**Given** an expense of €180.00 with split mode "BY_NIGHT"
**And** participants: Alice (4 nights), Bob (2 nights), Charlie (3 nights) = 9 total nights
**When** the expense is saved
**Then** shares are calculated proportionally:
  - Alice: €180 × (4/9) = €80.00
  - Bob: €180 × (2/9) = €40.00
  - Charlie: €180 × (3/9) = €60.00
**And** each participant's share is stored in the expense record
**And** shares sum exactly to the expense amount (no rounding errors that lose cents)

**Given** an expense of €90.00 with split mode "EQUAL"
**And** 3 participants in the split
**When** the expense is saved
**Then** shares are calculated equally:
  - Each participant: €90 / 3 = €30.00
**And** each participant's share is stored in the expense record

**Given** an expense of €100.00 with split mode "EQUAL"
**And** 3 participants (indivisible amount)
**When** the expense is saved
**Then** shares are distributed fairly with rounding:
  - Two participants: €33.33
  - One participant: €33.34 (to ensure total = €100.00)
**And** the rounding difference is assigned to the first participant alphabetically (or by creation order)

**Given** an expense of €200.00 with split mode "BY_NIGHT"
**And** participants with varying nights that create rounding scenarios
**When** the expense is saved
**Then** shares sum exactly to €200.00
**And** any rounding remainder is distributed fairly (no money lost or gained)

**Given** I view an expense in the expenses list
**When** I tap on the expense card to expand details
**Then** I see the calculation breakdown showing each participant's share
**And** for "By Night" mode, I see the night-based calculation (e.g., "Alice: 4/9 nights = €80.00")
**And** for "Equal" mode, I see "Split equally: €30.00 each"

**Given** the backend calculates shares
**When** an expense is created or updated
**Then** the `shares` array in the expense includes:
  - `participantId`: ID of the participant
  - `amount`: calculated share amount (BigDecimal, 2 decimal places)
**And** the sum of all share amounts equals the expense amount exactly

---

### Story 4.3: Free Mode Manual Share Specification

**As a** user,
**I want** to manually specify each participant's share for an expense,
**So that** I can handle custom splitting scenarios that don't fit by-night or equal.

**Acceptance Criteria:**

**Given** I am adding an expense and select "Free" split mode
**When** the form updates
**Then** I see a list of all participants with individual amount inputs
**And** each input is pre-filled with €0.00
**And** I see a running total showing "Total: €X / €Y" (entered vs expense amount)

**Given** I am in Free mode for a €100.00 expense with 3 participants
**When** I enter: Alice €50, Bob €30, Charlie €20
**Then** the running total shows "Total: €100.00 / €100.00" (green, valid)
**And** the submit button is enabled

**Given** I am in Free mode and enter shares that don't sum to the expense amount
**When** the total is €80.00 but expense is €100.00
**Then** the running total shows "Total: €80.00 / €100.00" (red, invalid)
**And** validation error "Shares must equal the expense amount" is shown
**And** the submit button is disabled

**Given** I am in Free mode and enter shares that exceed the expense amount
**When** the total is €120.00 but expense is €100.00
**Then** the running total shows "Total: €120.00 / €100.00" (red, invalid)
**And** validation error "Shares must equal the expense amount" is shown
**And** the submit button is disabled

**Given** I am in Free mode and change the expense amount after entering shares
**When** I change amount from €100 to €150
**Then** the validation updates to reflect the new target
**And** I see "Total: €100.00 / €150.00" (red, invalid)

**Given** I submit a valid Free mode expense
**When** the API request is made
**Then** the request body includes:
```json
{
  "amount": 100.00,
  "description": "Custom split",
  "payerId": "abc123",
  "splitMode": "FREE",
  "shares": [
    {"participantId": "p1", "amount": 50.00},
    {"participantId": "p2", "amount": 30.00},
    {"participantId": "p3", "amount": 20.00}
  ]
}
```

**Given** a POST request with Free mode and invalid shares total
**When** shares don't sum to expense amount
**Then** response status is 400 Bad Request
**And** response follows Problem Details format with detail "Shares must sum to expense amount"

**Given** a POST request with Free mode and missing participant shares
**When** not all participants have a share specified
**Then** response status is 400 Bad Request
**And** response follows Problem Details format with detail "All participants must have a share specified"

**Given** I am viewing on mobile (< 768px)
**When** in Free mode
**Then** participant share inputs are stacked vertically
**And** each row shows: participant name, amount input
**And** touch targets are at least 44px
**And** the keyboard shows numeric input type

---

### Story 4.4: Edit Expense

**As a** user managing a split,
**I want** to edit an existing expense (amount, description, payer, split mode),
**So that** I can correct mistakes without deleting and re-adding.

**Acceptance Criteria:**

**Given** I am viewing a split with expenses
**When** I tap/click on an expense card
**Then** the card expands to show expense details and an "Edit" button
**And** I see the calculation breakdown (shares per participant)

**Given** I click "Edit" on an expense
**When** the edit form appears
**Then** all fields are pre-filled with current values:
  - Amount: current amount
  - Description: current description
  - Payer: current payer selected
  - Split Mode: current mode selected
**And** I see "Save" and "Cancel" buttons

**Given** I am editing an expense with "By Night" or "Equal" mode
**When** I change the amount from €100 to €150
**And** I click "Save"
**Then** the API is called PUT `/api/splits/{splitId}/expenses/{expenseId}`
**And** shares are recalculated based on the new amount
**And** on success, the expense card shows updated values
**And** running balances update immediately
**And** a success toast shows "Expense updated"

**Given** I am editing an expense and change the split mode from "Equal" to "By Night"
**When** I click "Save"
**Then** shares are recalculated using the new mode
**And** the expense card shows the new split mode badge

**Given** I am editing an expense and switch to "Free" mode
**When** the form updates
**Then** I see the participant share inputs
**And** shares are pre-filled with the previous calculated amounts (from Equal or By Night)
**And** I can adjust individual shares before saving

**Given** I am editing a "Free" mode expense
**When** I change individual participant shares
**Then** the running total validation applies
**And** I cannot save until shares sum to expense amount

**Given** I click "Cancel" while editing
**When** the edit mode closes
**Then** original values are restored
**And** no API call is made

**Given** I try to save with invalid data (empty description, invalid amount)
**When** I click "Save"
**Then** validation errors are shown inline
**And** the API is not called

**Given** a PUT request to `/api/splits/{splitId}/expenses/{expenseId}` with updated data
**When** the expense exists
**Then** response status is 200 OK
**And** response body contains the updated expense with recalculated shares
**And** the split's JSON file is updated

**Given** a PUT request to a non-existent expense
**When** the expenseId is invalid
**Then** response status is 404 Not Found
**And** response follows Problem Details format

**Given** I am viewing on mobile (< 768px)
**When** editing an expense
**Then** the edit form is full-width
**And** touch targets are at least 44px
**And** the keyboard appears with appropriate input type

---

### Story 4.5: Remove Expense

**As a** user managing a split,
**I want** to remove an expense,
**So that** I can correct the split by removing incorrectly added expenses.

**Acceptance Criteria:**

**Given** I am viewing a split with expenses
**When** I look at an expense card (expanded view)
**Then** I see a delete/remove button (trash icon)
**And** the button is styled as a subtle secondary action

**Given** I click delete on an expense
**When** the confirmation dialog appears
**Then** I see "Remove expense '[description]'?" with the expense description
**And** I see the amount being removed (e.g., "€150.00 paid by Alice")
**And** I see "This cannot be undone" warning
**And** I see "Remove" (destructive) and "Cancel" buttons

**Given** I confirm deletion of an expense
**When** I click "Remove"
**Then** the API is called DELETE `/api/splits/{splitId}/expenses/{expenseId}`
**And** a loading indicator appears on the Remove button
**And** on success, the expense is removed from the list
**And** running balances update immediately (recalculated without this expense)
**And** a success toast shows "Expense removed"

**Given** I click "Cancel" on the confirmation dialog
**When** the dialog closes
**Then** the expense remains in the list
**And** no API call is made

**Given** a DELETE request to `/api/splits/{splitId}/expenses/{expenseId}`
**When** the expense exists
**Then** response status is 204 No Content
**And** the expense is removed from the split's JSON file

**Given** a DELETE request to a non-existent expense
**When** the expenseId is invalid
**Then** response status is 404 Not Found
**And** response follows Problem Details format

**Given** I am viewing on mobile
**When** interacting with delete
**Then** the confirmation dialog is a bottom sheet or centered modal
**And** buttons are full-width and touch-friendly (min 44px)

---

### Story 4.6: Display Expenses List with Running Balances

**As a** user viewing a split,
**I want** to see all expenses in a clear list with visual indicators,
**So that** I can quickly understand what's been paid and by whom.

**Acceptance Criteria:**

**Given** I am viewing a split overview
**When** there are no expenses
**Then** I see "No expenses yet" placeholder message
**And** I see the "Add Expense" button prominently

**Given** I am viewing a split with expenses
**When** the expenses list loads
**Then** expenses are displayed in chronological order (newest first)
**And** each expense card shows:
  - Description (primary text)
  - Amount (prominent, formatted with currency symbol)
  - Payer name with avatar/initial
  - Split mode badge ("By Night", "Equal", or "Free")
  - Date added (relative format: "2 hours ago", "Yesterday")

**Given** I am viewing an expense card
**When** the split mode is "By Night"
**Then** the badge shows "By Night" with a moon/night icon
**And** badge uses primary color

**Given** I am viewing an expense card
**When** the split mode is "Equal"
**Then** the badge shows "Equal" with an equals icon
**And** badge uses neutral/secondary color

**Given** I am viewing an expense card
**When** the split mode is "Free"
**Then** the badge shows "Free" with a custom/edit icon
**And** badge uses distinct color to indicate manual entry

**Given** I tap/click an expense card
**When** the card expands
**Then** I see the share breakdown for each participant
**And** format shows: participant name, their share amount
**And** for By Night: shows calculation (e.g., "4/9 nights = €80.00")

**Given** the API returns expenses in the split data
**When** GET `/api/splits/{splitId}` is called
**Then** the split response includes an `expenses` array
**And** each expense includes all fields: id, amount, description, payerId, splitMode, createdAt, shares

**Given** I am viewing on mobile (< 768px)
**When** the expenses list renders
**Then** cards are full-width with 16px padding
**And** the layout is optimized for vertical scrolling
**And** touch targets for expand/edit/delete are at least 44px

---

<!-- Stories for Epics 5-6 pending -->