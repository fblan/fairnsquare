import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import Settlement from './Settlement.svelte';
import type { Settlement as SettlementType } from '$lib/api/splits';

// Mock the router
vi.mock('$lib/router', () => {
  return {
    p: vi.fn((path: string) => path),
    navigate: vi.fn(),
    isActive: vi.fn(),
    route: {
      params: { splitId: 'test-split-id' },
      pathname: '/splits/test-split-id/settlement',
    },
  };
});

// Mock the API
vi.mock('$lib/api/splits', () => ({
  getSplit: vi.fn(),
  getSettlement: vi.fn(),
  resolveSettlement: vi.fn(),
  unsettleSettlement: vi.fn(),
  updateParticipant: vi.fn(),
}));

// Mock the toast store
vi.mock('$lib/stores/toastStore.svelte', () => ({
  addToast: vi.fn(),
}));

import { getSplit, getSettlement, resolveSettlement, unsettleSettlement, updateParticipant } from '$lib/api/splits';
import { navigate, route } from '$lib/router';
import { addToast } from '$lib/stores/toastStore.svelte';

const mockSettlement: SettlementType = {
  balances: [
    {
      participantId: 'p1',
      participantName: 'Alice',
      totalPaid: 100,
      totalCost: 50,
      balance: 50,
    },
    {
      participantId: 'p2',
      participantName: 'Bob',
      totalPaid: 0,
      totalCost: 50,
      balance: -50,
    },
  ],
  reimbursements: [
    {
      fromId: 'p2',
      fromName: 'Bob',
      toId: 'p1',
      toName: 'Alice',
      amount: 50,
    },
  ],
};

const mockSettlementAllSettled: SettlementType = {
  balances: [
    {
      participantId: 'p1',
      participantName: 'Alice',
      totalPaid: 50,
      totalCost: 50,
      balance: 0,
    },
    {
      participantId: 'p2',
      participantName: 'Bob',
      totalPaid: 50,
      totalCost: 50,
      balance: 0,
    },
  ],
  reimbursements: [],
};

const mockSplitNoSettlement = {
  id: 'test-split-id',
  name: 'Test Split',
  createdAt: '2026-01-01T00:00:00Z',
  participants: [
    { id: 'p1', name: 'Alice', nights: 3, share: 1, sharedAccountId: null, totalPaid: 100, totalCost: 50, balance: 50 },
    { id: 'p2', name: 'Bob', nights: 2, share: 1, sharedAccountId: null, totalPaid: 0, totalCost: 50, balance: -50 },
  ],
  expenses: [],
  settlement: null,
};

// A split where settlement was already persisted (non-null)
const mockSplitWithSettlement = {
  ...mockSplitNoSettlement,
  settlement: mockSettlement,
};

describe('Settlement', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (route as any).params = { splitId: 'test-split-id' };
    vi.mocked(getSplit).mockResolvedValue(mockSplitNoSettlement);
    vi.mocked(getSettlement).mockResolvedValue(mockSettlement);
    vi.mocked(resolveSettlement).mockResolvedValue(mockSettlement);
    vi.mocked(unsettleSettlement).mockResolvedValue(undefined);
    vi.mocked(updateParticipant).mockResolvedValue({
      id: 'p1', name: 'Alice', nights: 3, share: 1, sharedAccountId: null,
    });
  });

  // --- Loading State ---

  it('shows loading state initially', () => {
    vi.mocked(getSplit).mockImplementation(() => new Promise(() => {}));
    render(Settlement);

    expect(screen.getByText('Loading settlement...')).toBeInTheDocument();
  });

  // --- Header ---

  it('displays header with back button and split name after load', async () => {
    render(Settlement);

    await waitFor(() => {
      expect(screen.getByText('Test Split')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: 'Back to dashboard' })).toBeInTheDocument();
  });

  it('navigates back to dashboard when back button is clicked', async () => {
    render(Settlement);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Back to dashboard' })).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByRole('button', { name: 'Back to dashboard' }));
    expect(navigate).toHaveBeenCalledWith('/splits/test-split-id');
  });

  // --- Error Handling ---

  it('shows toast on API error during load', async () => {
    vi.mocked(getSplit).mockRejectedValue({
      status: 500,
      detail: 'Server error',
    });

    render(Settlement);

    await waitFor(() => {
      expect(addToast).toHaveBeenCalledWith({
        type: 'error',
        message: 'Server error',
      });
    });
  });

  it('shows toast when getSettlement fails for an already-settled split', async () => {
    vi.mocked(getSplit).mockResolvedValue(mockSplitWithSettlement);
    vi.mocked(getSettlement).mockRejectedValue({
      status: 500,
      detail: 'Load failed',
    });

    render(Settlement);

    await waitFor(() => {
      expect(addToast).toHaveBeenCalledWith({
        type: 'error',
        message: 'Load failed',
      });
    });
  });

  it('shows toast when resolveSettlement fails on Resolve click', async () => {
    vi.mocked(resolveSettlement).mockRejectedValue({
      status: 500,
      detail: 'Calculation failed',
    });

    render(Settlement);

    await waitFor(() => expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument());
    await fireEvent.click(screen.getByRole('button', { name: 'Resolve' }));

    await waitFor(() => {
      expect(addToast).toHaveBeenCalledWith({
        type: 'error',
        message: 'Calculation failed',
      });
    });
  });

  // --- Page load behaviour (no persisted settlement) ---

  it('does not call resolveSettlement on page load', async () => {
    render(Settlement);

    await waitFor(() => {
      expect(screen.getAllByText('Alice').length).toBeGreaterThan(0);
    });

    expect(resolveSettlement).not.toHaveBeenCalled();
  });

  it('shows balance cards after load without clicking Resolve', async () => {
    render(Settlement);

    await waitFor(() => {
      expect(screen.getAllByText('Alice').length).toBeGreaterThan(0);
      expect(screen.getAllByText('Bob').length).toBeGreaterThan(0);
    });
  });

  it('shows Resolve button on load when no settlement was previously persisted', async () => {
    render(Settlement);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument();
    });
  });

  it('does not show reimbursement details before clicking Resolve', async () => {
    render(Settlement);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument();
    });

    expect(screen.queryByText('Pay €50.00 to Alice')).not.toBeInTheDocument();
    expect(screen.queryByText('Receive €50.00 from Bob')).not.toBeInTheDocument();
  });

  // --- Resolve button action ---

  it('shows reimbursement details after clicking Resolve', async () => {
    render(Settlement);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByRole('button', { name: 'Resolve' }));

    await waitFor(() => {
      expect(screen.getByText('Pay €50.00 to Alice')).toBeInTheDocument();
      expect(screen.getByText('Receive €50.00 from Bob')).toBeInTheDocument();
    });
  });

  it('hides Resolve button and shows Export Settlement after clicking Resolve', async () => {
    render(Settlement);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByRole('button', { name: 'Resolve' }));

    await waitFor(() => {
      expect(screen.queryByRole('button', { name: 'Resolve' })).not.toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Export Settlement' })).toBeInTheDocument();
    });
  });

  it('calls resolveSettlement exactly once when Resolve is clicked', async () => {
    render(Settlement);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByRole('button', { name: 'Resolve' }));

    // resolveSettlement is called only on user action, never on initial page load
    await waitFor(() => expect(resolveSettlement).toHaveBeenCalledTimes(1));
  });

  // --- Already-persisted settlement ---

  it('shows reimbursements directly when split already has a persisted settlement', async () => {
    vi.mocked(getSplit).mockResolvedValue(mockSplitWithSettlement);

    render(Settlement);

    await waitFor(() => {
      expect(screen.getByText('Pay €50.00 to Alice')).toBeInTheDocument();
    });

    // Uses GET (getSettlement), never POST (resolveSettlement) on load
    expect(getSettlement).toHaveBeenCalledWith('test-split-id');
    expect(resolveSettlement).not.toHaveBeenCalled();
    expect(screen.queryByRole('button', { name: 'Resolve' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Export Settlement' })).toBeInTheDocument();
  });

  // --- Balance card display ---

  it('displays positive balance as "Owed" in green', async () => {
    render(Settlement);

    await waitFor(() => {
      expect(screen.getByText('Owed €50.00')).toBeInTheDocument();
    });

    expect(screen.getByText('Owed €50.00').className).toContain('text-green-600');
  });

  it('displays negative balance as "Owes" in red', async () => {
    render(Settlement);

    await waitFor(() => {
      expect(screen.getByText('Owes €50.00')).toBeInTheDocument();
    });

    expect(screen.getByText('Owes €50.00').className).toContain('text-red-600');
  });

  it('displays zero balance as "Settled"', async () => {
    vi.mocked(getSplit).mockResolvedValue({
      ...mockSplitNoSettlement,
      participants: [
        { id: 'p1', name: 'Alice', nights: 3, share: 1, sharedAccountId: null, totalPaid: 50, totalCost: 50, balance: 0 },
        { id: 'p2', name: 'Bob', nights: 2, share: 1, sharedAccountId: null, totalPaid: 50, totalCost: 50, balance: 0 },
      ],
    });

    render(Settlement);

    await waitFor(() => {
      expect(screen.getAllByText('Settled')).toHaveLength(2);
    });
  });

  it('shows "No participants" when balances are empty', async () => {
    vi.mocked(getSplit).mockResolvedValue({ ...mockSplitNoSettlement, participants: [] });

    render(Settlement);

    await waitFor(() => {
      expect(screen.getByText('No participants')).toBeInTheDocument();
    });
  });

  // --- Export Settlement ---

  describe('Export Settlement', () => {
    it('does not show Export Settlement button before clicking Resolve', async () => {
      render(Settlement);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument();
      });

      expect(screen.queryByRole('button', { name: 'Export Settlement' })).not.toBeInTheDocument();
    });

    it('shows Export Settlement button after clicking Resolve', async () => {
      render(Settlement);

      await waitFor(() => expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument());
      await fireEvent.click(screen.getByRole('button', { name: 'Resolve' }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Export Settlement' })).toBeInTheDocument();
      });
    });

    it('copies correct formatted text to clipboard when Export Settlement is clicked', async () => {
      const writeText = vi.fn().mockResolvedValue(undefined);
      Object.assign(navigator, { clipboard: { writeText } });

      vi.mocked(getSplit).mockResolvedValue({
        id: 'test-split-id',
        name: 'Trip to Paris',
        createdAt: '2026-01-01T00:00:00Z',
        participants: [
          { id: 'p1', name: 'Alice', nights: 3, share: 1, sharedAccountId: null, totalPaid: 100, totalCost: 50, balance: 50 },
          { id: 'p2', name: 'Bob', nights: 2, share: 1, sharedAccountId: null, totalPaid: 0, totalCost: 50, balance: -50 },
        ],
        expenses: [
          { id: 'e1', description: 'Dinner', amount: 100, payerId: 'p1', splitMode: 'EQUAL', createdAt: '2026-01-01T00:00:00Z', shares: [] },
        ],
        settlement: null,
      });

      render(Settlement);

      await waitFor(() => expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument());
      await fireEvent.click(screen.getByRole('button', { name: 'Resolve' }));

      await waitFor(() => expect(screen.getByRole('button', { name: 'Export Settlement' })).toBeInTheDocument());
      await fireEvent.click(screen.getByRole('button', { name: 'Export Settlement' }));

      await waitFor(() => {
        expect(writeText).toHaveBeenCalledWith(
          '=== Trip to Paris ===\n1 expense — Total: €100.00\n2 participants\n\nSettlements:\nBob → Alice: €50.00\n\nSummary:\nAlice receives €50.00\n\nhttp://localhost:3000/'
        );
        expect(addToast).toHaveBeenCalledWith({ type: 'success', message: 'Settlement copied to clipboard!' });
      });
    });

    it('shows info toast with text when clipboard is unavailable', async () => {
      Object.assign(navigator, { clipboard: undefined });

      render(Settlement);

      await waitFor(() => expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument());
      await fireEvent.click(screen.getByRole('button', { name: 'Resolve' }));

      await waitFor(() => expect(screen.getByRole('button', { name: 'Export Settlement' })).toBeInTheDocument());
      await fireEvent.click(screen.getByRole('button', { name: 'Export Settlement' }));

      await waitFor(() => {
        expect(addToast).toHaveBeenCalledWith(expect.objectContaining({ type: 'info' }));
      });
    });

    it('formats text with plural expense/participant labels correctly', async () => {
      const writeText = vi.fn().mockResolvedValue(undefined);
      Object.assign(navigator, { clipboard: { writeText } });

      vi.mocked(getSplit).mockResolvedValue({
        id: 'test-split-id',
        name: 'Weekend Trip',
        createdAt: '2026-01-01T00:00:00Z',
        participants: [
          { id: 'p1', name: 'Alice', nights: 3, share: 1, sharedAccountId: null, totalPaid: 150, totalCost: 50, balance: 100 },
          { id: 'p2', name: 'Bob', nights: 2, share: 1, sharedAccountId: null, totalPaid: 0, totalCost: 50, balance: -50 },
          { id: 'p3', name: 'Charlie', nights: 2, share: 1, sharedAccountId: null, totalPaid: 0, totalCost: 50, balance: -50 },
        ],
        expenses: [
          { id: 'e1', description: 'Hotel', amount: 90, payerId: 'p1', splitMode: 'EQUAL', createdAt: '2026-01-01T00:00:00Z', shares: [] },
          { id: 'e2', description: 'Food', amount: 60, payerId: 'p1', splitMode: 'EQUAL', createdAt: '2026-01-01T00:00:00Z', shares: [] },
        ],
        settlement: null,
      });
      vi.mocked(resolveSettlement).mockResolvedValue({
        balances: [
          { participantId: 'p1', participantName: 'Alice', totalPaid: 150, totalCost: 50, balance: 100 },
          { participantId: 'p2', participantName: 'Bob', totalPaid: 0, totalCost: 50, balance: -50 },
          { participantId: 'p3', participantName: 'Charlie', totalPaid: 0, totalCost: 50, balance: -50 },
        ],
        reimbursements: [
          { fromId: 'p2', fromName: 'Bob', toId: 'p1', toName: 'Alice', amount: 50 },
          { fromId: 'p3', fromName: 'Charlie', toId: 'p1', toName: 'Alice', amount: 50 },
        ],
      });

      render(Settlement);

      await waitFor(() => expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument());
      await fireEvent.click(screen.getByRole('button', { name: 'Resolve' }));

      await waitFor(() => expect(screen.getByRole('button', { name: 'Export Settlement' })).toBeInTheDocument());
      await fireEvent.click(screen.getByRole('button', { name: 'Export Settlement' }));

      await waitFor(() => {
        expect(writeText).toHaveBeenCalledWith(
          '=== Weekend Trip ===\n2 expenses — Total: €150.00\n3 participants\n\nSettlements:\nBob → Alice: €50.00\nCharlie → Alice: €50.00\n\nSummary:\nAlice receives €50.00 + €50.00 = €100.00\n\nhttp://localhost:3000/'
        );
      });
    });

    it('formats text with "All settled" message when no reimbursements needed', async () => {
      const writeText = vi.fn().mockResolvedValue(undefined);
      Object.assign(navigator, { clipboard: { writeText } });

      vi.mocked(resolveSettlement).mockResolvedValue(mockSettlementAllSettled);

      render(Settlement);

      await waitFor(() => expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument());
      await fireEvent.click(screen.getByRole('button', { name: 'Resolve' }));

      await waitFor(() => expect(screen.getByRole('button', { name: 'Export Settlement' })).toBeInTheDocument());
      await fireEvent.click(screen.getByRole('button', { name: 'Export Settlement' }));

      await waitFor(() => {
        const text = writeText.mock.calls[0][0] as string;
        expect(text).toContain('All settled — no transfers needed!');
      });
    });

    it('shows summary with multiple creditors when several people receive money', async () => {
      const writeText = vi.fn().mockResolvedValue(undefined);
      Object.assign(navigator, { clipboard: { writeText } });

      vi.mocked(getSplit).mockResolvedValue({
        id: 'test-split-id',
        name: 'Group Trip',
        createdAt: '2026-01-01T00:00:00Z',
        participants: [
          { id: 'p1', name: 'Alice', nights: 3, share: 1, sharedAccountId: null, totalPaid: 200, totalCost: 50, balance: 150 },
          { id: 'p2', name: 'Bob', nights: 2, share: 1, sharedAccountId: null, totalPaid: 100, totalCost: 50, balance: 50 },
          { id: 'p3', name: 'Charlie', nights: 2, share: 1, sharedAccountId: null, totalPaid: 0, totalCost: 100, balance: -100 },
          { id: 'p4', name: 'Diana', nights: 2, share: 1, sharedAccountId: null, totalPaid: 0, totalCost: 100, balance: -100 },
        ],
        expenses: [],
        settlement: null,
      });
      vi.mocked(resolveSettlement).mockResolvedValue({
        balances: [
          { participantId: 'p1', participantName: 'Alice', totalPaid: 200, totalCost: 50, balance: 150 },
          { participantId: 'p2', participantName: 'Bob', totalPaid: 100, totalCost: 50, balance: 50 },
          { participantId: 'p3', participantName: 'Charlie', totalPaid: 0, totalCost: 100, balance: -100 },
          { participantId: 'p4', participantName: 'Diana', totalPaid: 0, totalCost: 100, balance: -100 },
        ],
        reimbursements: [
          { fromId: 'p3', fromName: 'Charlie', toId: 'p1', toName: 'Alice', amount: 100 },
          { fromId: 'p4', fromName: 'Diana', toId: 'p1', toName: 'Alice', amount: 50 },
          { fromId: 'p4', fromName: 'Diana', toId: 'p2', toName: 'Bob', amount: 50 },
        ],
      });

      render(Settlement);

      await waitFor(() => expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument());
      await fireEvent.click(screen.getByRole('button', { name: 'Resolve' }));

      await waitFor(() => expect(screen.getByRole('button', { name: 'Export Settlement' })).toBeInTheDocument());
      await fireEvent.click(screen.getByRole('button', { name: 'Export Settlement' }));

      await waitFor(() => {
        const text = writeText.mock.calls[0][0] as string;
        expect(text).toContain('Alice receives €100.00 + €50.00 = €150.00');
        expect(text).toContain('Bob receives €50.00');
      });
    });

    it('sorts reimbursements alphabetically by payer name', async () => {
      const writeText = vi.fn().mockResolvedValue(undefined);
      Object.assign(navigator, { clipboard: { writeText } });

      vi.mocked(getSplit).mockResolvedValue({
        id: 'test-split-id',
        name: 'Test',
        createdAt: '2026-01-01T00:00:00Z',
        participants: [
          { id: 'p1', name: 'Alice', nights: 3, share: 1, sharedAccountId: null, totalPaid: 100, totalCost: 33, balance: 67 },
          { id: 'p2', name: 'Zoe', nights: 2, share: 1, sharedAccountId: null, totalPaid: 0, totalCost: 33, balance: -33 },
          { id: 'p3', name: 'Charlie', nights: 2, share: 1, sharedAccountId: null, totalPaid: 0, totalCost: 34, balance: -34 },
        ],
        expenses: [],
        settlement: null,
      });
      vi.mocked(resolveSettlement).mockResolvedValue({
        balances: [
          { participantId: 'p1', participantName: 'Alice', totalPaid: 100, totalCost: 33, balance: 67 },
          { participantId: 'p2', participantName: 'Zoe', totalPaid: 0, totalCost: 33, balance: -33 },
          { participantId: 'p3', participantName: 'Charlie', totalPaid: 0, totalCost: 34, balance: -34 },
        ],
        reimbursements: [
          { fromId: 'p2', fromName: 'Zoe', toId: 'p1', toName: 'Alice', amount: 33 },
          { fromId: 'p3', fromName: 'Charlie', toId: 'p1', toName: 'Alice', amount: 34 },
        ],
      });

      render(Settlement);

      await waitFor(() => expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument());
      await fireEvent.click(screen.getByRole('button', { name: 'Resolve' }));

      await waitFor(() => expect(screen.getByRole('button', { name: 'Export Settlement' })).toBeInTheDocument());
      await fireEvent.click(screen.getByRole('button', { name: 'Export Settlement' }));

      await waitFor(() => {
        const text = writeText.mock.calls[0][0] as string;
        const charlieIndex = text.indexOf('Charlie');
        const zoeIndex = text.indexOf('Zoe');
        expect(charlieIndex).toBeLessThan(zoeIndex);
      });
    });
  });

  // --- Unsettle ---

  describe('Unsettle', () => {
    it('does not show Unsettle button before clicking Resolve', async () => {
      render(Settlement);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument();
      });

      expect(screen.queryByRole('button', { name: 'Unsettle' })).not.toBeInTheDocument();
    });

    it('shows Unsettle button after clicking Resolve', async () => {
      render(Settlement);

      await waitFor(() => expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument());
      await fireEvent.click(screen.getByRole('button', { name: 'Resolve' }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Unsettle' })).toBeInTheDocument();
      });
    });

    it('shows Unsettle button when split already has a persisted settlement', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithSettlement);

      render(Settlement);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Unsettle' })).toBeInTheDocument();
      });
    });

    it('calls unsettleSettlement when Unsettle is clicked', async () => {
      render(Settlement);

      await waitFor(() => expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument());
      await fireEvent.click(screen.getByRole('button', { name: 'Resolve' }));

      await waitFor(() => expect(screen.getByRole('button', { name: 'Unsettle' })).toBeInTheDocument());
      await fireEvent.click(screen.getByRole('button', { name: 'Unsettle' }));

      await waitFor(() => {
        expect(unsettleSettlement).toHaveBeenCalledWith('test-split-id');
        // resolveSettlement called once (Resolve click) — never called again after Unsettle
        // to avoid immediately re-persisting the settlement that was just deleted
        expect(resolveSettlement).toHaveBeenCalledTimes(1);
      });
    });

    it('resets to pre-resolve state (shows Resolve button, hides reimbursements) after unsettling', async () => {
      render(Settlement);

      await waitFor(() => expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument());
      await fireEvent.click(screen.getByRole('button', { name: 'Resolve' }));

      await waitFor(() => {
        expect(screen.getByText('Pay €50.00 to Alice')).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Unsettle' }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument();
        expect(screen.queryByText('Pay €50.00 to Alice')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Unsettle' })).not.toBeInTheDocument();
      });
    });

    it('shows error toast when unsettleSettlement fails', async () => {
      vi.mocked(unsettleSettlement).mockRejectedValue({ detail: 'Unsettle failed' });

      render(Settlement);

      await waitFor(() => expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument());
      await fireEvent.click(screen.getByRole('button', { name: 'Resolve' }));

      await waitFor(() => expect(screen.getByRole('button', { name: 'Unsettle' })).toBeInTheDocument());
      await fireEvent.click(screen.getByRole('button', { name: 'Unsettle' }));

      await waitFor(() => {
        expect(addToast).toHaveBeenCalledWith({ type: 'error', message: 'Unsettle failed' });
      });
    });
  });

  // --- Shared Account Grouping ---

  describe('Shared Account Grouping', () => {
    const splitWithGroup = {
      ...mockSplitNoSettlement,
      participants: [
        { id: 'p1', name: 'Alice', nights: 3, share: 1, sharedAccountId: 'grp1', totalPaid: 100, totalCost: 50, balance: 50 },
        { id: 'p2', name: 'Bob', nights: 2, share: 1, sharedAccountId: 'grp1', totalPaid: 0, totalCost: 50, balance: -50 },
      ],
    };

    // --- Collapsed display ---

    it('shows grouped participants as a single combined card', async () => {
      vi.mocked(getSplit).mockResolvedValue(splitWithGroup);

      render(Settlement);

      await waitFor(() => {
        expect(screen.getByText('Alice & Bob')).toBeInTheDocument();
        expect(screen.queryByText('Alice')).not.toBeInTheDocument();
        expect(screen.queryByText('Bob')).not.toBeInTheDocument();
      });
    });

    it('shows combined totalPaid (€100) and totalCost (€100) for the group card', async () => {
      vi.mocked(getSplit).mockResolvedValue(splitWithGroup);

      render(Settlement);

      await waitFor(() => {
        // Alice paid €100, Bob paid €0 → combined Paid: €100.00
        // Alice cost €50, Bob cost €50 → combined Cost: €100.00
        const hundredEntries = screen.getAllByText('€100.00');
        expect(hundredEntries).toHaveLength(2);
      });
    });

    // --- Group with button ---

    it('shows a "Group with..." button for each standard participant when not settled', async () => {
      render(Settlement);

      await waitFor(() => {
        const buttons = screen.getAllByRole('button', { name: 'Group with...' });
        expect(buttons).toHaveLength(2);
      });
    });

    it('does not show "Group with..." buttons after clicking Resolve', async () => {
      render(Settlement);

      await waitFor(() => expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument());
      await fireEvent.click(screen.getByRole('button', { name: 'Resolve' }));

      await waitFor(() => {
        expect(screen.queryAllByRole('button', { name: 'Group with...' })).toHaveLength(0);
      });
    });

    it('does not show "Group with..." buttons when split is already settled on load', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithSettlement);

      render(Settlement);

      await waitFor(() => {
        expect(screen.queryAllByRole('button', { name: 'Group with...' })).toHaveLength(0);
      });
    });

    it('shows "Ungroup" button on a group card, not "Group with..."', async () => {
      vi.mocked(getSplit).mockResolvedValue(splitWithGroup);

      render(Settlement);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Ungroup' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Group with...' })).not.toBeInTheDocument();
      });
    });

    // --- Modal ---

    it('opens modal with participant name when "Group with..." is clicked', async () => {
      render(Settlement);

      await waitFor(() => expect(screen.getAllByRole('button', { name: 'Group with...' })).toHaveLength(2));
      await fireEvent.click(screen.getAllByRole('button', { name: 'Group with...' })[0]);

      await waitFor(() => {
        expect(screen.getByRole('dialog', { name: 'Group with' })).toBeInTheDocument();
        expect(screen.getByText(/Group Alice with/)).toBeInTheDocument();
      });
    });

    it('modal select lists other standard participants as options', async () => {
      render(Settlement);

      await waitFor(() => expect(screen.getAllByRole('button', { name: 'Group with...' })).toHaveLength(2));
      await fireEvent.click(screen.getAllByRole('button', { name: 'Group with...' })[0]);

      await waitFor(() => {
        const select = screen.getByRole('combobox');
        const options = Array.from((select as HTMLSelectElement).options).map(o => o.text);
        expect(options).toContain('(none)');
        expect(options).toContain('Bob');
        expect(options).not.toContain('Alice');
      });
    });

    it('modal select shows existing groups as a single collapsed entry', async () => {
      vi.mocked(getSplit).mockResolvedValue({
        ...mockSplitNoSettlement,
        participants: [
          { id: 'p1', name: 'Alice', nights: 3, share: 1, sharedAccountId: null, totalPaid: 100, totalCost: 33, balance: 67 },
          { id: 'p2', name: 'Bob', nights: 2, share: 1, sharedAccountId: 'grp1', totalPaid: 0, totalCost: 33, balance: -33 },
          { id: 'p3', name: 'Carol', nights: 2, share: 1, sharedAccountId: 'grp1', totalPaid: 0, totalCost: 34, balance: -34 },
        ],
      });

      render(Settlement);

      await waitFor(() => expect(screen.getByRole('button', { name: 'Group with...' })).toBeInTheDocument());
      await fireEvent.click(screen.getByRole('button', { name: 'Group with...' }));

      await waitFor(() => {
        const select = screen.getByRole('combobox');
        const options = Array.from((select as HTMLSelectElement).options).map(o => o.text);
        expect(options).toContain('(none)');
        expect(options).toContain('Bob & Carol');
        expect(options).not.toContain('Bob');
        expect(options).not.toContain('Carol');
      });
    });

    it('closes modal when Cancel is clicked without calling updateParticipant', async () => {
      render(Settlement);

      await waitFor(() => expect(screen.getAllByRole('button', { name: 'Group with...' })).toHaveLength(2));
      await fireEvent.click(screen.getAllByRole('button', { name: 'Group with...' })[0]);
      await waitFor(() => expect(screen.getByRole('dialog', { name: 'Group with' })).toBeInTheDocument());

      await fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

      await waitFor(() => {
        expect(screen.queryByRole('dialog', { name: 'Group with' })).not.toBeInTheDocument();
        expect(updateParticipant).not.toHaveBeenCalled();
      });
    });

    it('calls updateParticipant twice (new group) when OK is clicked for two ungrouped participants', async () => {
      vi.mocked(getSplit)
        .mockResolvedValueOnce(mockSplitNoSettlement)
        .mockResolvedValueOnce(mockSplitNoSettlement);

      render(Settlement);

      await waitFor(() => expect(screen.getAllByRole('button', { name: 'Group with...' })).toHaveLength(2));
      await fireEvent.click(screen.getAllByRole('button', { name: 'Group with...' })[0]);

      await waitFor(() => expect(screen.getByRole('combobox')).toBeInTheDocument());
      await fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p2' } });
      await fireEvent.click(screen.getByRole('button', { name: 'OK' }));

      await waitFor(() => {
        expect(updateParticipant).toHaveBeenCalledTimes(2);
        expect(updateParticipant).toHaveBeenCalledWith('test-split-id', 'p1', expect.objectContaining({ sharedAccountId: expect.any(String) }));
        expect(updateParticipant).toHaveBeenCalledWith('test-split-id', 'p2', expect.objectContaining({ sharedAccountId: expect.any(String) }));
      });
    });

    it('calls updateParticipant once (join group) when partner already has a sharedAccountId', async () => {
      vi.mocked(getSplit)
        .mockResolvedValueOnce({
          ...mockSplitNoSettlement,
          participants: [
            { id: 'p1', name: 'Alice', nights: 3, share: 1, sharedAccountId: null, totalPaid: 100, totalCost: 50, balance: 50 },
            { id: 'p2', name: 'Bob', nights: 2, share: 1, sharedAccountId: 'existing-grp', totalPaid: 0, totalCost: 50, balance: -50 },
          ],
        })
        .mockResolvedValueOnce(mockSplitNoSettlement);

      render(Settlement);

      await waitFor(() => expect(screen.getByRole('button', { name: 'Group with...' })).toBeInTheDocument());
      await fireEvent.click(screen.getByRole('button', { name: 'Group with...' }));

      await waitFor(() => expect(screen.getByRole('combobox')).toBeInTheDocument());
      await fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p2' } });
      await fireEvent.click(screen.getByRole('button', { name: 'OK' }));

      await waitFor(() => {
        expect(updateParticipant).toHaveBeenCalledTimes(1);
        expect(updateParticipant).toHaveBeenCalledWith('test-split-id', 'p1', expect.objectContaining({ sharedAccountId: 'existing-grp' }));
      });
    });

    it('closes modal immediately when OK is clicked', async () => {
      render(Settlement);

      await waitFor(() => expect(screen.getAllByRole('button', { name: 'Group with...' })).toHaveLength(2));
      await fireEvent.click(screen.getAllByRole('button', { name: 'Group with...' })[0]);
      await waitFor(() => expect(screen.getByRole('dialog', { name: 'Group with' })).toBeInTheDocument());

      await fireEvent.click(screen.getByRole('button', { name: 'OK' }));

      await waitFor(() => {
        expect(screen.queryByRole('dialog', { name: 'Group with' })).not.toBeInTheDocument();
      });
    });

    // --- Ungroup ---

    it('calls updateParticipant with null for all group members when Ungroup is clicked', async () => {
      vi.mocked(getSplit)
        .mockResolvedValueOnce(splitWithGroup)
        .mockResolvedValueOnce(mockSplitNoSettlement);

      render(Settlement);

      await waitFor(() => expect(screen.getByRole('button', { name: 'Ungroup' })).toBeInTheDocument());
      await fireEvent.click(screen.getByRole('button', { name: 'Ungroup' }));

      await waitFor(() => {
        expect(updateParticipant).toHaveBeenCalledTimes(2);
        expect(updateParticipant).toHaveBeenCalledWith('test-split-id', 'p1', expect.objectContaining({ sharedAccountId: null }));
        expect(updateParticipant).toHaveBeenCalledWith('test-split-id', 'p2', expect.objectContaining({ sharedAccountId: null }));
      });
    });

    it('shows error toast when updateParticipant fails', async () => {
      vi.mocked(updateParticipant).mockRejectedValue({ detail: 'Update failed' });

      render(Settlement);

      await waitFor(() => expect(screen.getAllByRole('button', { name: 'Group with...' })).toHaveLength(2));
      await fireEvent.click(screen.getAllByRole('button', { name: 'Group with...' })[0]);

      await waitFor(() => expect(screen.getByRole('combobox')).toBeInTheDocument());
      await fireEvent.change(screen.getByRole('combobox'), { target: { value: 'p2' } });
      await fireEvent.click(screen.getByRole('button', { name: 'OK' }));

      await waitFor(() => {
        expect(addToast).toHaveBeenCalledWith({ type: 'error', message: 'Update failed' });
      });
    });
  });
});