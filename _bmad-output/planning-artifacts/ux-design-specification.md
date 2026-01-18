---
stepsCompleted:
  - 'step-01-init'
  - 'step-02-discovery'
  - 'step-03-core-experience'
  - 'step-04-emotional-response'
  - 'step-05-inspiration'
  - 'step-06-design-system'
  - 'step-07-defining-experience'
  - 'step-08-visual-foundation'
inputDocuments:
  - '_bmad-output/planning-artifacts/prd.md'
  - '_bmad-output/planning-artifacts/product-brief-FairNSquare-2026-01-16.md'
  - '_bmad-output/project-context.md'
date: '2026-01-16'
author: 'Fred'
projectName: 'FairNSquare'
---

# UX Design Specification - FairNSquare

**Author:** Fred
**Date:** 2026-01-16

---

## Executive Summary

### Project Vision

**FairNSquare** is a frictionless expense-sharing web app for groups with variable participation - particularly holidays where people stay different numbers of nights. The core UX promise is "That was easy!" - users should complete settlement in under 5 minutes with zero confusion.

The product's unique value comes from treating "by-night" splitting as a first-class concept, automatically calculating fair shares proportional to stay duration. Combined with a zero-friction entry model (no app, no account, just a shareable link), the UX must deliver instant comprehension and effortless interaction.

### Target Users

**Primary Persona: Sophie, 34**
- Holiday organizer coordinating 6+ friends with variable stay durations
- Comfortable sharing links via WhatsApp/Messenger
- Hates spreadsheet chaos and awkward money conversations
- Success: "I just want to send a link and see 'you owe X to Y' at the end"

**Usage Patterns:**
- **The Settler (majority):** Enters everything at trip end - needs fast bulk entry
- **The Organizer:** Tracks real-time during trip - needs running balance visibility
- **The Control Freak:** Sets up upfront - needs easy participant management

**Tech Context:** Mobile-first (tapping links from chat apps), modern browsers only, no app installation tolerance.

### Key Design Challenges

1. **Instant Comprehension** - Zero onboarding possible; split state must be immediately obvious
2. **"By-night" Concept Clarity** - Core differentiator must be understood without explanation
3. **Mobile Data Entry** - Forms must feel effortless on phone screens
4. **Trust in Calculations** - Settlement amounts must feel transparent and verifiable
5. **Multi-user Editing** - Graceful handling of concurrent edits without real-time sync
6. **White-label Theming** - Design system must support CSS custom property theming

### Design Opportunities

1. **"That was easy!" Emotion** - UX simplicity IS the competitive advantage
2. **Smart Defaults** - Reduce cognitive load (nights, split mode, payer defaults)
3. **Transaction Optimizer as "Magic"** - Delightful "6 → 3 transfers" moment
4. **Instant Shareable Link** - Celebrate the zero-friction entry
5. **Progressive Disclosure** - Hide complexity until needed

## Core User Experience

### Defining Experience

**Primary Action:** Adding expenses is the core loop - the action users perform most frequently. This must feel effortless on mobile (< 15 seconds, minimal taps).

**Payoff Action:** Viewing settlement is where users feel success - the moment that justifies using the product. This must inspire trust and clarity.

**Entry Point:** Sharing/receiving the link is the zero-friction gateway. First-time users must understand the split state immediately.

### Platform Strategy

| Aspect | Decision |
|--------|----------|
| Platform | Mobile-first web SPA |
| Primary Input | Touch (phone screens) |
| Entry Point | Shared link via chat apps |
| Offline | Not required |
| Browsers | Modern evergreen only |

**Key Constraint:** Most users will interact on phones, tapping links from WhatsApp/Messenger. Desktop is secondary.

### Effortless Interactions

| Interaction | Effortless Target |
|-------------|-------------------|
| First visit via shared link | Instant comprehension of split state |
| Add expense | < 15 seconds, minimal taps |
| See who owes whom | One glance, no calculation needed |
| Optimize transactions | One tap, immediate result |
| Edit participant nights | Inline, no modal navigation |

### Critical Success Moments

| Moment | Success Criteria |
|--------|------------------|
| **First view of split** | User understands participants, nights, expenses at a glance |
| **After adding first expense** | User sees calculation update and trusts it |
| **Settlement view** | User says "that looks right" without checking math |
| **After optimize** | User experiences delight ("6 → 3 transfers, nice!") |

### Experience Principles

1. **Instant Clarity** - Every screen answers "what am I looking at?" in under 3 seconds
2. **Minimal Taps** - Core actions complete in ≤ 3 taps
3. **Trust Through Transparency** - Show the math, let users verify
4. **Smart Defaults, Easy Override** - Reduce decisions, but never trap users
5. **Delight in Simplicity** - The magic is what users DON'T have to do

## Desired Emotional Response

### Primary Emotional Goals

**North Star Emotion:** "That was easy!"

| Emotion | Priority | Context |
|---------|----------|---------|
| Relief | Primary | After settlement - mental burden lifted |
| Confidence | Primary | Throughout - trusting the calculations |
| Fairness | Primary | By-night math - everyone pays their share |
| Simplicity | Supporting | Minimal cognitive load throughout |
| Delight | Supporting | Optimizer moment, micro-interactions |

### Emotional Journey Mapping

| Stage | Target Emotion | Anti-Pattern |
|-------|----------------|--------------|
| Receive link | Curiosity, ease | Confusion |
| First view | Clarity, orientation | Overwhelm |
| Add expense | Efficiency, control | Frustration |
| See balance update | Trust, satisfaction | Doubt |
| View settlement | Relief, confidence | Anxiety |
| After optimize | Delight, surprise | Indifference |
| Mark as settled | Accomplishment, closure | Uncertainty |

### Micro-Emotions

**Design For:**
- Confidence over Confusion
- Trust over Skepticism
- Relief over Anxiety
- Delight over Indifference
- Accomplishment over Frustration

### Design Implications

| Emotion | UX Approach |
|---------|-------------|
| Relief | Clear "done" states, visual closure |
| Confidence | Calculation breakdowns on demand |
| Trust | Consistent behavior, visible amounts |
| Simplicity | Minimal fields, smart defaults |
| Delight | Optimizer animation, satisfying interactions |
| Accomplishment | Celebrate completion, success feedback |

### Emotional Design Principles

1. **Clarity Breeds Confidence** - When users understand instantly, they trust
2. **Less is Relief** - Every removed step reduces anxiety
3. **Math Should Feel Fair** - Transparency creates acceptance
4. **Celebrate Closure** - Settlement completion deserves acknowledgment
5. **Surprise with Simplicity** - Delight comes from "I expected more steps"

## UX Pattern Analysis & Inspiration

### Inspiring Products Analysis

**Doctolib** - Medical appointment booking

| Aspect | What They Do Well |
|--------|-------------------|
| Single Focus | One clear action: find slot → book → done |
| Minimal Steps | 3-4 taps from search to confirmed appointment |
| Clear Information | Doctor, time, address - all visible at a glance |
| No Cognitive Load | Available slots highlighted, unavailable greyed out |
| Confirmation Clarity | Unambiguous "you're booked" with all details |
| Mobile-First | Designed for phone, desktop follows |

**Why It Resonates:** Doctolib proves that complex tasks (finding a doctor, checking availability, booking) can feel effortless when designed with ruthless simplicity.

### Anti-Patterns to Avoid

**Learned from Tricount and similar apps:**

| Anti-Pattern | Problem | FairNSquare Alternative |
|--------------|---------|------------------------|
| Requires app install | Friction before value | Web-based, shareable link |
| Account required | Barrier to participation | No accounts, just links |
| Complex split options | Overwhelming choices | 3 modes with smart default |
| Missing by-night | Manual workarounds | First-class concept |
| Feature overload | Cluttered UI | Progressive disclosure |

### Transferable UX Patterns

**From Doctolib to FairNSquare:**

| Pattern | Application |
|---------|-------------|
| Single-screen clarity | Split overview shows everything in one view |
| Minimal steps | Add expense in ≤ 3 taps |
| Visual state clarity | Clear who owes / who is owed |
| Unambiguous confirmation | "Expense added" with instant balance update |
| Mobile-native design | Touch targets, thumb-friendly, no hover dependencies |

### Design Inspiration Strategy

**Adopt:**
- Doctolib's ruthless simplicity and single-task focus
- Mobile-first interaction patterns
- Clear confirmation and state visibility

**Adapt:**
- Appointment slots → expense entries (list management)
- Doctor selection → participant/payer selection
- Booking confirmation → settlement confirmation

**Avoid:**
- Tricount's app-install requirement
- Account creation barriers
- Complex configuration options upfront
- Feature-heavy interfaces that overwhelm casual users

## Design System Foundation

### Design System Choice

**Selected:** Tailwind CSS (Pure)

| Aspect | Decision |
|--------|----------|
| Framework | Tailwind CSS v3.x |
| Component Library | None - custom Svelte 5 components |
| Theming Strategy | CSS custom properties + Tailwind config |
| Responsive Approach | Mobile-first breakpoints |
| Dark Mode | Deferred (architecture ready) |

### Rationale for Selection

1. **Full Control** - No component library constraints; build exactly what's needed
2. **White-Label Ready** - CSS custom properties integrate naturally with Tailwind config
3. **Mobile-First** - Tailwind's responsive utilities designed for mobile-first
4. **Solo Developer Speed** - Utility classes faster than writing custom CSS
5. **No Lock-In** - Pure CSS output, no framework dependency beyond build
6. **Future Flexibility** - DaisyUI or other plugins can be added later if needed

### Implementation Approach

**Tailwind Configuration:**
- Extend default theme with CSS custom properties
- Define semantic color tokens (primary, secondary, success, danger)
- Configure breakpoints matching PRD (320px, 768px, 1024px)
- Set up consistent spacing and border-radius scales

**Component Strategy:**
- Build reusable Svelte 5 components with Tailwind classes
- Use `$props()` for component customization
- Keep components small, composable, single-purpose
- Document component API for consistency

### Customization Strategy

**CSS Custom Properties for White-Label:**
- All brand colors via CSS variables
- Typography scale via variables
- Spacing and radius via variables
- Tenant theme = CSS variable override file

**Tailwind Integration:**
- `tailwind.config.js` references CSS variables
- Theme switching = swap CSS variable values
- No rebuild needed for tenant customization

## Defining Experience

### The Core Interaction

**Defining Experience:** "See exactly who owes whom, with by-night math that feels obviously fair."

This is the moment users will describe to friends:
> "I shared a link, we all added what we paid, and at the end it showed exactly who owes whom - Marc paid less because he stayed fewer nights. That's exactly right."

Everything else (link sharing, expense entry, optimizer) serves this moment.

### User Mental Model

| Aspect | Expectation |
|--------|-------------|
| Current solution | Spreadsheets, mental math, awkward conversations |
| Mental model | "Split = who paid → who owes whom" |
| Expectation | Calculator accuracy without doing math |
| Confusion risk | "By-night" is novel - needs subtle education |

### Success Criteria

| Criteria | Target |
|----------|--------|
| Comprehension speed | < 5 seconds to understand who owes whom |
| Trust | No need to verify with calculator |
| Fairness perception | By-night amounts feel obviously correct |
| Action clarity | Clear next step (pay, mark settled) |

### Novel UX Patterns

**Established Patterns (leverage familiarity):**
- Link sharing, expense lists, debits/credits views

**Novel Patterns (require education):**
- "By-night" calculation → show math breakdown on demand
- Transaction optimizer → before/after comparison visualization

**Strategy:** Inline education - users can verify once, then trust.

### Experience Mechanics

**Settlement Flow:**

1. **Initiation** - Tap "Settlement" or view balance summary
2. **View** - Who owes/is owed, amounts prominent
3. **Detail** - Tap amount → see calculation breakdown
4. **Optimize** - "Simplify transfers" → before/after comparison
5. **Action** - "Mark as paid" per transfer
6. **Completion** - "All settled!" with celebration

## Visual Design Foundation

### Color System

**Primary Palette:**

| Role | Color | Hex | Usage |
|------|-------|-----|-------|
| Primary | Teal | `#0D9488` | Actions, links, active states |
| Primary Light | Light Teal | `#14B8A6` | Hover states, avatars |
| Primary Dark | Dark Teal | `#0F766E` | Pressed states |

**Semantic Colors:**

| Role | Color | Hex | Usage |
|------|-------|-----|-------|
| Success | Green | `#16A34A` | Settled, paid, positive balance |
| Warning | Amber | `#F59E0B` | Attention states |
| Danger | Red | `#DC2626` | Debts, errors, negative balance |

**Neutrals:**

| Role | Color | Hex | Usage |
|------|-------|-----|-------|
| Background | Slate 50 | `#F8FAFC` | Page background |
| Surface | White | `#FFFFFF` | Cards, modals |
| Border | Slate 200 | `#E2E8F0` | Dividers, input borders |
| Text | Slate 800 | `#1E293B` | Primary text |
| Text Muted | Slate 500 | `#64748B` | Secondary text |

### Typography System

**Font Stack:** System fonts (`-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif`)

**Type Scale:**

| Element | Size | Weight | Line Height |
|---------|------|--------|-------------|
| h1 | 24px | 600 | 1.25 |
| h2 | 20px | 600 | 1.25 |
| h3 | 18px | 600 | 1.25 |
| Body | 16px | 400 | 1.5 |
| Small | 14px | 400 | 1.5 |
| Caption | 12px | 500 | 1.25 |

**Number Display:** `font-variant-numeric: tabular-nums` for aligned amounts.

### Spacing & Layout Foundation

**Base Unit:** 4px

**Spacing Scale:** 4, 8, 12, 16, 24, 32, 48px

**Layout:**

| Aspect | Value |
|--------|-------|
| Max content width | 420px (mobile optimized) |
| Card padding | 16px |
| Component gap | 16px |
| Border radius | 8px |
| Touch target min | 44px height |

**Visual Density:** Airy - comfortable spacing, not cramped.

### Accessibility Considerations

- All color combinations meet WCAG 2.1 AA contrast ratios
- Touch targets minimum 44px for mobile accessibility
- Focus states visible with primary color ring
- No color-only indicators (icons + color for states)

### Preview Reference

Visual preview available: `_bmad-output/planning-artifacts/ux-visual-preview.html`