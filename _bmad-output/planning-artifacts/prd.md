---
stepsCompleted:
  - 'step-01-init'
  - 'step-02-discovery'
  - 'step-03-success'
  - 'step-04-journeys'
  - 'step-05-domain'
  - 'step-06-innovation'
  - 'step-07-project-type'
  - 'step-08-scoping'
  - 'step-09-functional'
  - 'step-10-nonfunctional'
  - 'step-11-polish'
inputDocuments:
  - '_bmad-output/planning-artifacts/product-brief-FairNSquare-2026-01-16.md'
  - '_bmad-output/project-context.md'
documentCounts:
  briefs: 1
  research: 0
  brainstorming: 0
  projectDocs: 1
classification:
  projectType: 'web_app'
  domain: 'general'
  complexity: 'low'
  projectContext: 'greenfield'
  architecturalNote: 'Multi-tenant by design for group isolation'
workflowType: 'prd'
date: '2026-01-16'
author: 'Fred'
projectName: 'FairNSquare'
---

# Product Requirements Document - FairNSquare

**Author:** Fred
**Date:** 2026-01-16

## Executive Summary

**FairNSquare** is a frictionless expense-sharing web app for groups with variable participation - particularly holidays where people stay different numbers of nights.

**Core Innovation:** "By-night" splitting as a first-class concept, automatically calculating fair shares proportional to stay duration.

**Zero Friction:** No app install, no account required, just a shareable link. Anyone with the link can add participants and expenses.

**Target Outcome:** Users finish thinking "That was easy!"

| Aspect | Decision |
|--------|----------|
| Project Type | Web App (SPA) |
| Stack | Svelte 5 + Quarkus (Java 25) |
| Domain Complexity | Low |
| Project Context | Greenfield, multi-tenant by design |

## Success Criteria

### User Success

| Metric | Target | Measurement |
|--------|--------|-------------|
| Settlement Speed | < 5 minutes | Time from opening settlement page to completion |
| Fairness Confidence | Positive feedback | "Thanks!" button clicks on final page |
| Completion Rate | 100% | Started splits reach full settlement |
| User Delight | "That was easy!" | Absence of "improvement idea" submissions for core flow |

**Feedback Loop:** Final settlement page includes:
- "Thanks!" button (positive signal)
- "Improvement idea" button with mini-form (captures friction points)

### Business Success

| Metric | Target |
|--------|--------|
| Personal Utility | Fred completes real holiday splits |
| Organic Adoption | 2-3 friend/family groups use it independently |
| Uptime | 99%+ during holiday seasons |
| Maintenance Burden | Minimal - not a second job |
| Deployment Speed | < 30 min for updates |

### Technical Success

| Metric | Target | Measurement |
|--------|--------|-------------|
| Observability | Full coverage | OpenTelemetry on all REST endpoints |
| Anomaly Detection | Response time baselines | Timer metrics per endpoint, alert on deviation |
| Reliability | No data loss | Splits persist correctly across sessions |
| Multi-tenant Isolation | Zero data leakage | Tenant-scoped queries enforced |

### Differentiator Validation

The "by night" mode is the core differentiator. Tracked via REST endpoint telemetry:
- Split mode selection per expense (by-night / equal / free)
- Hypothesis: Majority of expenses use "by-night" mode
- If not validated → re-examine if differentiator resonates

## Product Scope

### MVP - Minimum Viable Product

- Split management (create, share link, view)
- Participants with name + nights (smart defaults)
- Expenses with 3 modes: by-night (default), equal, free
- Settlement: debits/credits view, optimizer, mark as settled
- Feedback buttons on final page
- Multi-tenant architecture, OpenTelemetry instrumentation

### Out of Scope (MVP)

- User accounts / authentication
- Multi-currency
- Receipt photos, categories, payment integration
- Push notifications, export, i18n
- White-label admin UI

### Growth Features (Post-MVP)

- v1.1: Categories, CSV export
- v1.2: Multi-currency, historical archive

### Vision (Future)

- v2.0: White-label configuration UI, custom themes
- Beyond: Payment integration, receipt OCR, PWA

## User Journeys

### Journey 1: The Creator (Sophie)

**Opening Scene:**
Sophie is back from a weekend getaway with 6 friends. WhatsApp is full of "who paid for what?" messages. She decides to sort it out.

**Rising Action:**
Sophie opens FairNSquare and creates a new split: "Bordeaux Weekend 2026". She gets a shareable link instantly - no signup, no account.

She adds the 6 participants with their nights:
- Sophie: 4 nights
- Julie: 4 nights
- Marc: 2 nights
- Thomas: 3 nights
- Emma: 3 nights
- Lucas: 2 nights

She adds the expenses she remembers paying (accommodation €650, by-night split). Then she shares the link in the group chat: "Everyone add what you paid!"

**Climax:**
Over the next hour, expenses appear as others add theirs. Sophie checks the settlement view - clear debits/credits. She clicks "Optimize" and sees only 3 transfers needed instead of 6.

**Resolution:**
Sophie sends a final message: "Here's who pays whom!" Settlement happens. Ten minutes of her time. "That was easy."

**Capabilities revealed:** Create split, shareable link generation, add participants with nights, add expenses, settlement view, transaction optimizer

---

### Journey 2: The Participant (Marc)

**Opening Scene:**
Marc receives Sophie's WhatsApp: "Add your expenses here: [link]"

**Rising Action:**
Marc taps the link. No app download, no login. The split opens in his browser. He sees the participant list (including himself, 2 nights), and some expenses already logged.

Marc paid for the train tickets (€240 for everyone). He taps "Add expense", enters the amount, selects "equal split" (train cost the same regardless of nights), confirms. 30 seconds.

He notices Thomas's nights are wrong - Thomas stayed 3 nights, not 2. Marc edits it directly. Done.

**Climax:**
At settlement, Marc sees he owes Sophie €47 (he stayed fewer nights, so his share is smaller). The "by night" math worked exactly as expected - fair.

**Resolution:**
Marc pays Sophie via bank transfer, marks it mentally as done. No disputes, no awkward conversations.

**Capabilities revealed:** Instant link access (no auth), view split state, add expenses, edit participants (full access), settlement clarity

---

### Journey Requirements Summary

| Capability | Creator | Participant |
|------------|---------|-------------|
| Create split + shareable link | ✓ | - |
| Access split via link (no auth) | ✓ | ✓ |
| Add/edit/remove participants | ✓ | ✓ |
| Participant removal constraint (has expenses) | ✓ | ✓ |
| Add/edit/remove expenses | ✓ | ✓ |
| Three split modes (by-night, equal, free) | ✓ | ✓ |
| Smart defaults (nights, split mode) | ✓ | ✓ |
| Running balance view | ✓ | ✓ |
| Settlement debits/credits view | ✓ | ✓ |
| Transaction optimizer | ✓ | ✓ |

**Key insight:** Creator and Participant have identical capabilities once the split exists. The only difference is who creates the initial link.

## Innovation & Novel Patterns

### Detected Innovation Areas

| Aspect | Description |
|--------|-------------|
| **Innovation Type** | Conceptual addition to splitting paradigm |
| **What's Novel** | "By night" as first-class split mode with auto-calculation |
| **Scope** | Safety net for variable-participation scenarios, not revolutionary |

**The "by night" innovation:**
- Treats nights-stayed as a first-class dimension that automatically calculates fair shares
- Challenges the assumption that expense splitting is either "equal" or "manually specify each person's share"
- Eliminates mental math burden for variable-participation groups

### Validation Approach

Telemetry on split-mode usage per expense:
- If by-night gets used for ~20-30% of expenses (big-ticket items) → hypothesis confirmed
- If rarely used → equal split may be sufficient for most groups

### Risk Mitigation

- **Low-risk innovation:** If "by night" isn't used, it doesn't break anything
- **Graceful degradation:** App works perfectly with just "equal" splits
- **Minimal investment:** Small feature addition, not a bet-the-company innovation
- **Fallback:** If rarely used, consider demoting from default or simplifying UI

## Web App Specific Requirements

### Project-Type Overview

| Aspect | Decision |
|--------|----------|
| Architecture | SPA (Single Page Application) |
| Framework | Svelte 5 with runes, via Quinoa extension |
| Backend | Quarkus REST API (Java 25) |
| SEO | Not required - share-link access model |
| Real-time | Not required - manual refresh acceptable |

### Browser Support Matrix

| Browser | Version | Support Level |
|---------|---------|---------------|
| Chrome | Latest 2 versions | Full |
| Firefox | Latest 2 versions | Full |
| Safari | Latest 2 versions | Full |
| Edge | Latest 2 versions | Full |
| IE11 | - | Not supported |
| Older browsers | - | Not supported |

**Rationale:** Modern evergreen browsers only. Users sharing links via WhatsApp/Messenger are already on modern devices.

### Responsive Design

| Breakpoint | Target |
|------------|--------|
| Mobile | 320px - 767px (primary) |
| Tablet | 768px - 1023px |
| Desktop | 1024px+ |

**Mobile-first approach:** Most users will access via phone (tapping links from chat apps). Desktop is secondary.

### Performance Targets

| Metric | Target | Rationale |
|--------|--------|-----------|
| First Contentful Paint | < 1.5s | Quick load on mobile networks |
| Time to Interactive | < 3s | Usable quickly |
| Lighthouse Performance | > 80 | Good baseline |
| Bundle size | < 200KB gzipped | Keep it light |

### Accessibility Level

**Target: WCAG 2.1 Level A (Basic)**

| Requirement | Implementation |
|-------------|----------------|
| Keyboard navigation | All interactive elements focusable |
| Alt text | Images have descriptive alt attributes |
| Form labels | All inputs have associated labels |
| Focus visible | Clear focus indicators on interactive elements |
| Semantic HTML | Proper heading hierarchy, landmarks |

**Not in scope for MVP:** Color contrast (AA), skip links, screen reader optimization (AAA)

### Implementation Considerations

- **No SSR needed:** Pure client-side SPA, Quinoa serves static assets
- **API-first:** All data via REST endpoints, OpenTelemetry instrumented
- **White-label ready:** CSS custom properties for theming (from project context)
- **Offline:** Not required - always online assumption for MVP

## Project Scoping & Phased Development

### MVP Strategy & Philosophy

**MVP Approach:** Problem-solving MVP - deliver the core value proposition (fair expense splitting with "by night" mode) with minimum friction.

**Resource Requirements:** Solo developer (Fred), side project capacity. Tight scope is essential.

### MVP Feature Set (Phase 1)

**Core User Journeys Supported:**
- Creator journey: Create split, add participants/expenses, share link, settle
- Participant journey: Access via link, add/edit participants/expenses, view settlement

**Must-Have Capabilities:**
| Capability | Included | Rationale |
|------------|----------|-----------|
| Create split + shareable link | ✓ | Core entry point |
| Participants with name + nights | ✓ | Enables "by night" differentiator |
| Expenses with 3 split modes | ✓ | Core value proposition |
| Settlement view + optimizer | ✓ | Core outcome delivery |
| Feedback buttons | ✓ | User success measurement |
| Multi-tenant isolation | ✓ | Architectural foundation |
| OpenTelemetry instrumentation | ✓ | Operational visibility |

### Explicitly Out of Scope (MVP)

| Feature | Rationale |
|---------|-----------|
| User accounts / authentication | Shared-link model is simpler |
| Multi-currency | Single currency for v1 |
| Receipt photos | Complexity without core value |
| Categories / tags | Nice-to-have enhancement |
| Payment integration | Manual settlement acceptable |
| Export / reporting | Can be added later |
| i18n | English first |
| White-label admin UI | Architecture supports it, UI not needed yet |

### Post-MVP Roadmap

**Phase 2 - v1.1 (Growth):**
- Expense categories for organization
- CSV export for record-keeping

**Phase 2 - v1.2 (Enhancement):**
- Multi-currency support
- Historical split archive

**Phase 3 - v2.0 (Expansion):**
- White-label configuration UI
- Custom themes per tenant

**Beyond:**
- Payment integration (PayPal, etc.)
- Receipt OCR
- Progressive Web App (PWA)

### Risk Mitigation Strategy

| Risk Type | Risk | Mitigation |
|-----------|------|------------|
| Technical | Multi-tenant complexity | Simple tenant-per-request isolation, proven Quarkus patterns |
| Market | "By night" not valued | Telemetry validates usage; app works fine with just "equal" splits |
| Resource | Solo developer burnout | Tight MVP scope, minimal maintenance burden as explicit goal |

## Functional Requirements

### Split Management

- **FR1:** User can create a new split with a name
- **FR2:** System generates a unique shareable link for each split
- **FR3:** User can access an existing split via its shareable link without authentication
- **FR4:** User can view the split overview (name, participants, expenses, balances)

### Participant Management

- **FR5:** User can add a participant with a name and number of nights
- **FR6:** User can edit a participant's name or number of nights
- **FR7:** User can remove a participant who has no associated expenses
- **FR8:** System prevents removal of participants with associated expenses
- **FR9:** System defaults the "nights" field to the last entered value when adding participants

### Expense Management

- **FR10:** User can add an expense with amount, description, and payer
- **FR11:** User can select a split mode for each expense: by-night (default), equal, or free
- **FR12:** System calculates each participant's share based on the selected split mode
- **FR13:** For "by-night" mode, system distributes expense proportionally to nights stayed
- **FR14:** For "equal" mode, system distributes expense equally among all participants
- **FR15:** For "free" mode, user can manually specify each participant's share
- **FR16:** User can edit an existing expense (amount, description, payer, split mode)
- **FR17:** User can remove an expense

### Balance & Settlement

- **FR18:** User can view running balance per participant at any time
- **FR19:** User can view settlement summary showing who owes whom and how much
- **FR20:** User can optimize transactions to minimize the number of transfers needed
- **FR21:** System calculates optimized transfer list from debits/credits

### User Feedback

- **FR22:** User can submit positive feedback via "Thanks!" button on settlement page
- **FR23:** User can submit improvement ideas via form on settlement page
- **FR24:** System records feedback submissions for later review

### System Operations

- **FR25:** System isolates data between tenants (no data leakage)
- **FR26:** System instruments all REST endpoints with OpenTelemetry
- **FR27:** System records split-mode usage per expense for analytics
- **FR28:** System persists split data reliably across sessions

## Non-Functional Requirements

### Performance

| Requirement | Target | Rationale |
|-------------|--------|-----------|
| First Contentful Paint | < 1.5s | Mobile users expect quick loads |
| Time to Interactive | < 3s | Usable quickly on 3G/4G |
| API Response Time | < 500ms (p95) | Smooth interactions |
| Bundle Size | < 200KB gzipped | Fast initial download |
| Lighthouse Score | > 80 | Baseline quality |

### Security

| Requirement | Description |
|-------------|-------------|
| Data Isolation | Each tenant's data is strictly isolated; no cross-tenant data leakage |
| Transport Security | All traffic over HTTPS |
| Input Validation | All user inputs validated server-side to prevent injection |
| Link Unpredictability | Split links use cryptographically random identifiers (not sequential) |

**Note:** No authentication required per design (shared-link model), but link unpredictability provides security-through-obscurity for split access.

### Reliability

| Requirement | Target | Rationale |
|-------------|--------|-----------|
| Uptime | 99%+ during holiday seasons | When it matters, it works |
| Data Persistence | Zero data loss | Splits must persist reliably |
| Error Recovery | Graceful degradation on failures | Show error states, don't crash |
| Backup Strategy | Regular database backups | Recovery from catastrophic failure |

### Observability

| Requirement | Description |
|-------------|-------------|
| Tracing | OpenTelemetry traces on all REST endpoints |
| Metrics | Response time, error rate, request count per endpoint |
| Logging | Structured logs correlated with trace context |
| Alerting | Baseline alerts for error rate spikes, latency anomalies |
