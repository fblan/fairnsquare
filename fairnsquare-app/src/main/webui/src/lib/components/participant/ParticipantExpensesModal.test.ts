import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, within } from '@testing-library/svelte';
import ParticipantExpensesModal from './ParticipantExpensesModal.svelte';
import type { Participant, Expense } from '$lib/api/splits';

describe('ParticipantExpensesModal', () => {
  const alice: Participant = { id: 'p-alice', name: 'Alice', nights: 3, share: 1 };
  const bob: Participant = { id: 'p-bob', name: 'Bob', nights: 2, share: 1 };
  const participants: Participant[] = [alice, bob];

  // Alice paid €100, Bob owes €40, Alice owes €60
  const expenseAlicePaid: Expense = {
    id: 'e-1',
    description: 'Groceries',
    amount: 100,
    payerId: 'p-alice',
    splitMode: 'EQUAL',
    createdAt: '2026-01-01T00:00:00Z',
    shares: [
      { participantId: 'p-alice', amount: 60 },
      { participantId: 'p-bob', amount: 40 },
    ],
  };

  // Bob paid €60, Alice owes €30
  const expenseBobPaid: Expense = {
    id: 'e-2',
    description: 'Dinner',
    amount: 60,
    payerId: 'p-bob',
    splitMode: 'BY_NIGHT',
    createdAt: '2026-01-02T00:00:00Z',
    shares: [
      { participantId: 'p-alice', amount: 30 },
      { participantId: 'p-bob', amount: 30 },
    ],
  };

  // Bob paid €50, Alice has no share
  const expenseAliceNotInvolved: Expense = {
    id: 'e-3',
    description: 'Bob solo expense',
    amount: 50,
    payerId: 'p-bob',
    splitMode: 'FREE',
    createdAt: '2026-01-03T00:00:00Z',
    shares: [{ participantId: 'p-bob', amount: 50 }],
  };

  // Bob paid €80, Alice has a zero-amount share (e.g. BY_NIGHT with 0 nights)
  const expenseAliceZeroShare: Expense = {
    id: 'e-4',
    description: 'Zero share expense',
    amount: 80,
    payerId: 'p-bob',
    splitMode: 'BY_NIGHT',
    createdAt: '2026-01-04T00:00:00Z',
    shares: [
      { participantId: 'p-alice', amount: 0 },
      { participantId: 'p-bob', amount: 80 },
    ],
  };

  const defaultProps = {
    open: true,
    participant: alice,
    expenses: [expenseAlicePaid, expenseBobPaid, expenseAliceNotInvolved],
    participants,
    onClose: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  // --- Rendering ---

  describe('rendering', () => {
    it('renders the modal when open is true', () => {
      render(ParticipantExpensesModal, { props: defaultProps });
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('does not render the modal when open is false', () => {
      render(ParticipantExpensesModal, { props: { ...defaultProps, open: false } });
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('shows the participant name in the header', () => {
      render(ParticipantExpensesModal, { props: defaultProps });
      expect(screen.getByText("Alice's Expenses")).toBeInTheDocument();
    });
  });

  // --- Expense Filtering ---

  describe('expense filtering', () => {
    it('shows only expenses where participant is payer or has a share', () => {
      render(ParticipantExpensesModal, { props: defaultProps });
      expect(screen.getByText('Groceries')).toBeInTheDocument();
      expect(screen.getByText('Dinner')).toBeInTheDocument();
      expect(screen.queryByText('Bob solo expense')).not.toBeInTheDocument();
    });

    it('shows empty state when participant has no expenses', () => {
      render(ParticipantExpensesModal, {
        props: { ...defaultProps, expenses: [expenseAliceNotInvolved] },
      });
      expect(screen.getByText(/no expenses found/i)).toBeInTheDocument();
    });

    it('hides expenses where participant has a zero-amount share and is not payer', () => {
      render(ParticipantExpensesModal, {
        props: { ...defaultProps, expenses: [expenseAlicePaid, expenseAliceZeroShare] },
      });
      expect(screen.getByText('Groceries')).toBeInTheDocument();
      expect(screen.queryByText('Zero share expense')).not.toBeInTheDocument();
    });
  });

  // --- Data Display ---

  describe('expense data columns', () => {
    it('shows expense description', () => {
      render(ParticipantExpensesModal, { props: defaultProps });
      expect(screen.getByText('Groceries')).toBeInTheDocument();
    });

    it('shows split mode as formatted label', () => {
      render(ParticipantExpensesModal, { props: defaultProps });
      expect(screen.getByText('Equal')).toBeInTheDocument();
      expect(screen.getByText('By Night')).toBeInTheDocument();
    });

    it('shows spent = expense amount when participant is payer', () => {
      render(ParticipantExpensesModal, {
        props: { ...defaultProps, expenses: [expenseAlicePaid] },
      });
      // Groceries: Alice paid €100, owned €60, balance €40
      // The table row should contain: amount €100, spent €100, owned €60
      const cells = screen.getAllByText('€100.00');
      // Both "Amount" column and "Spent" column show €100
      expect(cells.length).toBeGreaterThanOrEqual(2);
    });

    it('shows spent = 0 when participant is not payer', () => {
      render(ParticipantExpensesModal, {
        props: { ...defaultProps, expenses: [expenseBobPaid] },
      });
      // Dinner: Bob paid €60, Alice owes €30, spent = 0, balance = -30
      const zeroAmounts = screen.getAllByText('€0.00');
      expect(zeroAmounts.length).toBeGreaterThanOrEqual(1);
    });

    it('shows owned = participant share amount', () => {
      render(ParticipantExpensesModal, {
        props: { ...defaultProps, expenses: [expenseBobPaid] },
      });
      // Alice's share in Dinner is €30 — the owned section is titled "Owned"
      const ownedSection = document.querySelector('[title="Owned"]');
      expect(ownedSection?.textContent).toContain('€30.00');
    });

    it('shows positive balance in green when participant spent more than owned', () => {
      render(ParticipantExpensesModal, {
        props: { ...defaultProps, expenses: [expenseAlicePaid] },
      });
      // Groceries: balance €40 — color is on the wrapper span, not the inner amount span
      const balanceAmount = screen.getByText('€40.00');
      expect(balanceAmount.closest('.text-green-600')).not.toBeNull();
    });

    it('shows negative balance in red when participant owes more than spent', () => {
      render(ParticipantExpensesModal, {
        props: { ...defaultProps, expenses: [expenseBobPaid] },
      });
      // Dinner: balance -€30, shown as €30.00 — color is on the wrapper span
      const balanceAmounts = screen.getAllByText('€30.00');
      const redBalanceAmount = balanceAmounts.find((el) => el.closest('.text-red-600'));
      expect(redBalanceAmount).toBeDefined();
    });
  });

  // --- Close behaviour ---

  describe('close behaviour', () => {
    it('calls onClose when the X button is clicked', async () => {
      const onClose = vi.fn();
      render(ParticipantExpensesModal, { props: { ...defaultProps, onClose } });
      const header = screen.getByRole('dialog').querySelector('.border-b')!;
      fireEvent.click(within(header as HTMLElement).getByRole('button', { name: /close/i }));
      expect(onClose).toHaveBeenCalledOnce();
    });

    it('calls onClose when the Close button in the footer is clicked', async () => {
      const onClose = vi.fn();
      render(ParticipantExpensesModal, { props: { ...defaultProps, onClose } });
      // Footer has a "Close" button — the X button has aria-label="Close"
      // There may be two elements matching "Close"; use the footer button specifically
      const closeButtons = screen.getAllByRole('button', { name: /close/i });
      fireEvent.click(closeButtons[closeButtons.length - 1]);
      expect(onClose).toHaveBeenCalledOnce();
    });

    it('calls onClose when Escape key is pressed', async () => {
      const onClose = vi.fn();
      render(ParticipantExpensesModal, { props: { ...defaultProps, onClose } });
      fireEvent.keyDown(window, { key: 'Escape' });
      expect(onClose).toHaveBeenCalledOnce();
    });

    it('calls onClose when backdrop is clicked', async () => {
      const onClose = vi.fn();
      render(ParticipantExpensesModal, { props: { ...defaultProps, onClose } });
      const backdrop = screen.getByRole('dialog');
      fireEvent.click(backdrop);
      expect(onClose).toHaveBeenCalledOnce();
    });
  });
});
