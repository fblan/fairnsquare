/**
 * Story 4.2: Calculate Shares by Split Mode
 * Tests for ExpenseCard component - share breakdown display
 */

import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/svelte';
import ExpenseCard from './ExpenseCard.svelte';
import type { Expense, Split } from '$lib/api/splits';

describe('ExpenseCard', () => {
  let mockSplit: Split;
  let mockExpenseByNight: Expense;
  let mockExpenseEqual: Expense;

  beforeEach(() => {
    mockSplit = {
      id: 'split-1',
      name: 'Test Split',
      createdAt: '2024-01-01T00:00:00Z',
      participants: [
        { id: 'alice-id', name: 'Alice', nights: 4 },
        { id: 'bob-id', name: 'Bob', nights: 2 },
        { id: 'charlie-id', name: 'Charlie', nights: 3 },
      ],
      expenses: [],
    };

    mockExpenseByNight = {
      id: 'expense-1',
      description: 'Accommodation',
      amount: 180.00,
      payerId: 'alice-id',
      splitMode: 'BY_NIGHT',
      createdAt: '2024-01-01T10:00:00Z',
      shares: [
        { participantId: 'alice-id', amount: 80.00 },
        { participantId: 'bob-id', amount: 40.00 },
        { participantId: 'charlie-id', amount: 60.00 },
      ],
    };

    mockExpenseEqual = {
      id: 'expense-2',
      description: 'Dinner',
      amount: 90.00,
      payerId: 'bob-id',
      splitMode: 'EQUAL',
      createdAt: '2024-01-01T12:00:00Z',
      shares: [
        { participantId: 'alice-id', amount: 30.00 },
        { participantId: 'bob-id', amount: 30.00 },
        { participantId: 'charlie-id', amount: 30.00 },
      ],
    };
  });

  it('renders collapsed by default', () => {
    render(ExpenseCard, {
      props: {
        expense: mockExpenseByNight,
        split: mockSplit,
        expanded: false,
        onToggle: () => {},
      },
    });

    expect(screen.getByText('Accommodation')).toBeInTheDocument();
    expect(screen.getByText('€180.00')).toBeInTheDocument();
    expect(screen.queryByText('Share Breakdown')).not.toBeInTheDocument();
  });

  it('shows chevron down icon when collapsed', () => {
    const { container } = render(ExpenseCard, {
      props: {
        expense: mockExpenseByNight,
        split: mockSplit,
        expanded: false,
        onToggle: () => {},
      },
    });

    const button = container.querySelector('button[aria-expanded="false"]');
    expect(button).toBeInTheDocument();
  });

  it('shows chevron up icon when expanded', () => {
    const { container } = render(ExpenseCard, {
      props: {
        expense: mockExpenseByNight,
        split: mockSplit,
        expanded: true,
        onToggle: () => {},
      },
    });

    const button = container.querySelector('button[aria-expanded="true"]');
    expect(button).toBeInTheDocument();
  });

  it('calls onToggle when clicked', async () => {
    let toggleCalled = false;
    const { container } = render(ExpenseCard, {
      props: {
        expense: mockExpenseByNight,
        split: mockSplit,
        expanded: false,
        onToggle: () => {
          toggleCalled = true;
        },
      },
    });

    const button = container.querySelector('button');
    expect(button).toBeInTheDocument();
    button!.click();

    expect(toggleCalled).toBe(true);
  });

  it('shows BY_NIGHT breakdown when expanded', () => {
    render(ExpenseCard, {
      props: {
        expense: mockExpenseByNight,
        split: mockSplit,
        expanded: true,
        onToggle: () => {},
      },
    });

    expect(screen.getByText('Share Breakdown')).toBeInTheDocument();
    expect(screen.getByText('Alice')).toBeInTheDocument();
    expect(screen.getByText('Bob')).toBeInTheDocument();
    expect(screen.getByText('Charlie')).toBeInTheDocument();
    expect(screen.getByText('4/9 nights')).toBeInTheDocument();
    expect(screen.getByText('2/9 nights')).toBeInTheDocument();
    expect(screen.getByText('3/9 nights')).toBeInTheDocument();
  });

  it('shows EQUAL breakdown when expanded', () => {
    render(ExpenseCard, {
      props: {
        expense: mockExpenseEqual,
        split: mockSplit,
        expanded: true,
        onToggle: () => {},
      },
    });

    expect(screen.getByText('Share Breakdown')).toBeInTheDocument();
    const splitEquallyTexts = screen.getAllByText('Split equally');
    expect(splitEquallyTexts).toHaveLength(3);
  });

  it('shows all participants with correct amounts in breakdown', () => {
    render(ExpenseCard, {
      props: {
        expense: mockExpenseByNight,
        split: mockSplit,
        expanded: true,
        onToggle: () => {},
      },
    });

    expect(screen.getByText('Alice')).toBeInTheDocument();
    expect(screen.getByText('Bob')).toBeInTheDocument();
    expect(screen.getByText('Charlie')).toBeInTheDocument();

    // Check amounts are displayed (multiple €80.00, €40.00, €60.00 may exist)
    expect(screen.getByText(/€80\.00/)).toBeInTheDocument();
    expect(screen.getByText(/€40\.00/)).toBeInTheDocument();
    expect(screen.getByText(/€60\.00/)).toBeInTheDocument();
  });

  it('shows total that matches expense amount with checkmark', () => {
    render(ExpenseCard, {
      props: {
        expense: mockExpenseByNight,
        split: mockSplit,
        expanded: true,
        onToggle: () => {},
      },
    });

    expect(screen.getByText('Total')).toBeInTheDocument();
    expect(screen.getByText(/€180\.00 ✓/)).toBeInTheDocument();
  });

  it('displays payer name correctly', () => {
    render(ExpenseCard, {
      props: {
        expense: mockExpenseByNight,
        split: mockSplit,
        expanded: false,
        onToggle: () => {},
      },
    });

    expect(screen.getByText('Paid by Alice')).toBeInTheDocument();
  });

  it('displays split mode badge', () => {
    render(ExpenseCard, {
      props: {
        expense: mockExpenseByNight,
        split: mockSplit,
        expanded: false,
        onToggle: () => {},
      },
    });

    expect(screen.getByText('By Night')).toBeInTheDocument();
  });

  it('formats currency with 2 decimal places', () => {
    const expenseWithCents = {
      ...mockExpenseByNight,
      amount: 123.45,
      shares: [{ participantId: 'alice-id', amount: 123.45 }],
    };

    render(ExpenseCard, {
      props: {
        expense: expenseWithCents,
        split: mockSplit,
        expanded: true,
        onToggle: () => {},
      },
    });

    // Multiple €123.45 elements exist (in share and total), check at least one exists
    const amountElements = screen.getAllByText('€123.45');
    expect(amountElements.length).toBeGreaterThan(0);
  });

  // --- Story 4.3: FREE Mode Display Tests (AC10) ---

  describe('FREE Mode Display (Story 4.3 AC10)', () => {
    let mockExpenseFree: Expense;

    beforeEach(() => {
      mockExpenseFree = {
        id: 'expense-free-1',
        description: 'Custom Split Dinner',
        amount: 100.00,
        payerId: 'alice-id',
        splitMode: 'FREE',
        type: 'FREE',
        createdAt: '2024-01-02T12:00:00Z',
        shares: [
          { participantId: 'alice-id', amount: 40.00, parts: 2 },
          { participantId: 'bob-id', amount: 60.00, parts: 3 },
          { participantId: 'charlie-id', amount: 0.00, parts: 0 },
        ],
      };
    });

    it('displays "Manual split" for FREE mode expenses (AC10)', () => {
      render(ExpenseCard, {
        props: {
          expense: mockExpenseFree,
          split: mockSplit,
          expanded: true,
          onToggle: () => {},
        },
      });

      const manualSplitTexts = screen.getAllByText('Manual split');
      expect(manualSplitTexts.length).toBeGreaterThan(0);
    });

    it('does NOT show "Split equally" for FREE mode', () => {
      render(ExpenseCard, {
        props: {
          expense: mockExpenseFree,
          split: mockSplit,
          expanded: true,
          onToggle: () => {},
        },
      });

      expect(screen.queryByText('Split equally')).not.toBeInTheDocument();
    });

    it('does NOT show nights fraction for FREE mode', () => {
      render(ExpenseCard, {
        props: {
          expense: mockExpenseFree,
          split: mockSplit,
          expanded: true,
          onToggle: () => {},
        },
      });

      expect(screen.queryByText(/nights/i)).not.toBeInTheDocument();
    });

    it('displays FREE mode badge', () => {
      render(ExpenseCard, {
        props: {
          expense: mockExpenseFree,
          split: mockSplit,
          expanded: false,
          onToggle: () => {},
        },
      });

      expect(screen.getByText('Free')).toBeInTheDocument();
    });

    it('shows participant names with manually-specified amounts', () => {
      render(ExpenseCard, {
        props: {
          expense: mockExpenseFree,
          split: mockSplit,
          expanded: true,
          onToggle: () => {},
        },
      });

      expect(screen.getByText('Alice')).toBeInTheDocument();
      expect(screen.getByText('Bob')).toBeInTheDocument();
      expect(screen.getByText('Charlie')).toBeInTheDocument();
      expect(screen.getByText('€40.00')).toBeInTheDocument();
      expect(screen.getByText('€60.00')).toBeInTheDocument();
    });

    it('shows zero-amount participants in FREE mode breakdown', () => {
      render(ExpenseCard, {
        props: {
          expense: mockExpenseFree,
          split: mockSplit,
          expanded: true,
          onToggle: () => {},
        },
      });

      // Charlie has €0.00 - verify displayed
      const charlieRow = screen.getByText('Charlie').closest('div');
      expect(charlieRow).toBeInTheDocument();
      expect(screen.getByText('€0.00')).toBeInTheDocument();
    });

    it('total matches expense amount with checkmark for FREE mode', () => {
      render(ExpenseCard, {
        props: {
          expense: mockExpenseFree,
          split: mockSplit,
          expanded: true,
          onToggle: () => {},
        },
      });

      expect(screen.getByText('Total')).toBeInTheDocument();
      expect(screen.getByText(/€100\.00 ✓/)).toBeInTheDocument();
    });
  });
});
