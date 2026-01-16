---
stepsCompleted: [1, 2, 3, 4, 5]
inputDocuments:
  - '_bmad-output/analysis/brainstorming-session-2026-01-16.md'
  - '_bmad-output/project-context.md'
date: '2026-01-16'
author: 'Fred'
previousName: 'SplitNShare'
---

# Product Brief: FairNSquare

## Executive Summary

**FairNSquare** is a frictionless, web-based expense sharing solution designed for groups with variable participation - particularly holiday groups where people stay different numbers of nights. Unlike existing solutions that require app installations or lack nuanced splitting options, FairNSquare introduces the innovative "by night" expense distribution as its default mode, ensuring fair cost sharing proportional to each participant's actual stay duration.

The product prioritizes simplicity above all: no app to install, no account required, just a shareable link. Users create a split, add participants with their names and night counts, then log expenses as they go. At settlement time, a clear debits/credits view and transaction-minimizing algorithm make reconciliation painless.

**Target outcome:** Users finish thinking "That was easy!"

---

## Core Vision

### Problem Statement

When groups travel together - friends on holiday, family reunions, weekend getaways - expenses pile up quickly: accommodation, groceries, restaurants, activities. The challenge intensifies when participants stay different numbers of nights. Current solutions force an uncomfortable choice:

- **Too simple:** Equal splits that unfairly burden short-stay participants
- **Too heavy:** Complex apps requiring downloads, accounts, and manual per-expense adjustments
- **Missing concept:** No existing solution treats "nights stayed" as a first-class splitting dimension

### Problem Impact

Without a fair, easy solution:
- Awkward end-of-trip conversations about who owes what
- Manual spreadsheet calculations prone to errors
- Resentment when splits feel unfair
- Friction from requiring everyone to install yet another app

### Why Existing Solutions Fall Short

**Tricount and similar apps:**
- Require app installation - barrier for casual participants
- Lack "by night" expense concept - forcing manual workarounds
- Either oversimplified (equal splits only) or overcomplicated (too many options)

### Proposed Solution

**FairNSquare** reimagines expense sharing for variable-participation groups:

1. **Zero friction entry:** Web-based, shareable link - no app, no accounts
2. **Smart defaults:** "By night" splitting as default, with equality and free repartition as alternatives
3. **Effortless setup:** Add participant name + nights stayed (defaulting to last entered value)
4. **Flexible timing:** Start tracking anytime - during or after the trip
5. **Clear settlement:** Debits/credits view, choose who to reimburse, one-click transaction minimization

### Key Differentiators

| Differentiator | Value |
|----------------|-------|
| **By-night default** | Fair distribution without manual math - unique to FairNSquare |
| **No installation** | Share a link, everyone participates instantly |
| **Smart UX defaults** | Last-entered nights value, by-night as default type |
| **Transaction optimizer** | Minimize the number of reimbursements needed |
| **White-label ready** | Multi-tenant architecture enables branded deployments |

---

## Target Users

### Primary Users

**The Holiday Participant**

FairNSquare has a deliberately flat user model - everyone is equal. Any participant can create a split, add expenses, view balances, and settle up. This simplicity is a core design principle.

#### Persona: Sophie, 34

**Context:** Sophie is going on a long weekend getaway with 6 friends to a rental house. Three friends are staying Friday-Sunday (2 nights), two are staying Thursday-Sunday (3 nights), and Sophie is staying the full Thursday-Monday (4 nights).

**Problem Experience:** Last time, she used a spreadsheet that became a nightmare. People forgot who paid for groceries, and the "by night" calculations took an hour of awkward discussion at checkout.

**Tech Comfort:** Comfortable sharing links via WhatsApp/Messenger. Doesn't want to ask everyone to install an app - half the group would never do it.

**Success Vision:** "I just want to send a link, have everyone add what they paid, and see a clear 'you owe X to Y' at the end. Ten minutes, done."

---

### Usage Patterns

Users engage with FairNSquare at different moments based on their personality:

| Pattern | When | Behavior | Motivation |
|---------|------|----------|------------|
| **The Control Freak** | Start of trip | Creates split, adds all participants upfront | Wants structure from day one |
| **The Organizer** | During trip | Tracks expenses in real-time, monitors balances | Asks debtors to pay next expense, minimizing end-of-trip transfers |
| **The Settler** (vast majority) | End of trip | Creates split or catches up, enters all expenses at once | Just wants to figure out who owes whom and be done |

---

### Secondary Users

**N/A** - FairNSquare intentionally has no admin, manager, or special roles. This simplicity removes friction and supports the "just share a link" model.

---

### User Journey

**Discovery:** A friend sends a link in the group chat: "Add your expenses here!"

**Onboarding:** Click link → See the split → Understand immediately (participants + nights visible)

**Core Usage:**
- Add expense → Select type (by night / equal / free) → Done
- View running totals anytime

**"Aha!" Moment:** Seeing the automatic "by night" calculation work perfectly: "Wait, it already figured out that Marc (2 nights) pays less than Julie (4 nights)? That's exactly right!"

**Settlement:**
- View debits/credits summary
- Choose who to reimburse (preference)
- Or click "Optimize" to minimize transactions
- Mark as settled → Done!

**Outcome Emotion:** "That was easy!"

---

## Success Metrics

### User Success Metrics

| Metric | Target | How We Know |
|--------|--------|-------------|
| **Settlement Speed** | < 5 minutes | From opening settlement page to "done" |
| **Fairness Confidence** | High | Users don't dispute the calculations |
| **Completion Rate** | 100% of started splits | Splits reach full settlement (not abandoned) |

**Key User Success Moments:**
- Seeing the transaction list and thinking "that looks right"
- The "by night" calculation matching their mental math
- Clicking "Optimize transactions" and seeing fewer transfers needed

---

### Business Objectives

*Context: FairNSquare is a personal side project, not a commercial venture.*

| Objective | What Success Looks Like |
|-----------|------------------------|
| **Personal Utility** | Fred uses it for his own holidays |
| **Operational Simplicity** | Easy to deploy and monitor (aligned with OpenTelemetry setup) |
| **Organic Adoption** | Relatives and friends use it without being pushed |

---

### Key Performance Indicators

For a side project, KPIs should be lightweight and practical:

| KPI | Target | Notes |
|-----|--------|-------|
| **Uptime** | 99%+ during holiday seasons | When it matters, it works |
| **Splits Completed** | At least 1 per holiday season | Personal use validated |
| **Word-of-Mouth Adoption** | 2-3 friend/family groups using it | Organic validation |
| **Maintenance Burden** | Minimal | Should not feel like a second job |
| **Deployment Ease** | < 30 minutes to deploy updates | Supports white-label/multi-tenant goal |

---

## MVP Scope

### Core Features

**1. Split Management**
- Create a new split (generates shareable link)
- Access split via shared link (no account required)
- View split overview (participants, nights, expenses)

**2. Participant Management**
- Add participant with name and number of nights
- Smart default: nights value defaults to last entered value
- Edit/remove participants

**3. Expense Tracking**
- Add expense with amount, description, and who paid
- Three split modes:
  - **By night** (default) - proportional to nights stayed
  - **Equal** - split evenly regardless of nights
  - **Free** - custom manual distribution
- Edit/remove expenses
- View live running balance per participant

**4. Settlement**
- View debits/credits summary (who owes what)
- Choose preferred reimbursement recipient
- Transaction optimizer (minimize number of transfers)
- Mark transactions as settled

**5. Non-Functional Requirements**
- Secure (HTTPS, no data leakage between tenants)
- Monitorable (OpenTelemetry instrumentation)
- Easy to deploy (containerized, simple configuration)
- Data persistence (splits retained for reasonable duration)
- Mobile-friendly responsive web UI

---

### Out of Scope for MVP

| Feature | Rationale |
|---------|-----------|
| User accounts / authentication | Shared link model is simpler |
| Currency management / conversion | Single currency assumption for v1 |
| Receipt photo upload | Adds complexity without core value |
| Expense categories / tags | Nice-to-have, not essential |
| Payment integration (PayPal, etc.) | Manual settlement is acceptable |
| Push notifications | Web-only for simplicity |
| Export / reporting | Can be added later |
| Multiple languages (i18n) | English first |
| White-label admin UI | Architecture supports it, but no UI in v1 |

---

### MVP Success Criteria

The MVP is successful when:

- [ ] Fred completes a real holiday expense split using FairNSquare
- [ ] Settlement takes < 5 minutes with no disputes
- [ ] At least one friend/family group uses it independently
- [ ] Application runs reliably with observable metrics
- [ ] Deployment process is straightforward (< 30 min)

---

### Future Vision

If MVP succeeds, potential enhancements:

| Phase | Features |
|-------|----------|
| **v1.1** | Expense categories, basic export (CSV) |
| **v1.2** | Multi-currency support, historical split archive |
| **v2.0** | White-label tenant configuration UI, custom themes |
| **Beyond** | Payment integration, receipt OCR, mobile app (PWA) |
