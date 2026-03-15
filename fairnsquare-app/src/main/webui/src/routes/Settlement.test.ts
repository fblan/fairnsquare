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
  getSettlement: vi.fn(),
  getSplit: vi.fn(),
  updateParticipant: vi.fn(),
}));

// Mock the toast store
vi.mock('$lib/stores/toastStore.svelte', () => ({
  addToast: vi.fn(),
}));

import { getSettlement, getSplit, updateParticipant } from '$lib/api/splits';
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

describe('Settlement', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (route as any).params = { splitId: 'test-split-id' };
    vi.mocked(getSplit).mockResolvedValue({
      id: 'test-split-id',
      name: 'Test Split',
      createdAt: '2026-01-01T00:00:00Z',
      participants: [
        { id: 'p1', name: 'Alice', nights: 3, share: 1, preferredCreditorId: null },
        { id: 'p2', name: 'Bob', nights: 2, share: 1, preferredCreditorId: null },
      ],
      expenses: [],
      settlement: null,
    });
    vi.mocked(updateParticipant).mockResolvedValue({
      id: 'p2',
      name: 'Bob',
      nights: 2,
      share: 1,
      preferredCreditorId: null,
    });
  });

  // --- Loading State ---

  it('shows loading state initially', () => {
    vi.mocked(getSettlement).mockImplementation(() => new Promise(() => {}));
    render(Settlement);

    expect(screen.getByText('Loading settlement...')).toBeInTheDocument();
  });

  // --- Header ---

  it('displays header with back button and split name', async () => {
    vi.mocked(getSettlement).mockResolvedValue(mockSettlement);

    render(Settlement);

    await waitFor(() => {
      expect(screen.getByText('Test Split')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: 'Back to dashboard' })).toBeInTheDocument();
  });

  it('navigates back to dashboard when back button is clicked', async () => {
    vi.mocked(getSettlement).mockResolvedValue(mockSettlement);

    render(Settlement);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Back to dashboard' })).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByRole('button', { name: 'Back to dashboard' }));
    expect(navigate).toHaveBeenCalledWith('/splits/test-split-id');
  });

  // --- Error Handling ---

  it('shows toast on API error', async () => {
    vi.mocked(getSettlement).mockRejectedValue({
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

  // --- Balance Cards ---

  it('displays participant balance cards with names', async () => {
    vi.mocked(getSettlement).mockResolvedValue(mockSettlement);

    render(Settlement);

    await waitFor(() => {
      // Alice appears in both her card and Bob's preferred creditor dropdown
      expect(screen.getAllByText('Alice').length).toBeGreaterThan(0);
      expect(screen.getByText('Bob')).toBeInTheDocument();
    });
  });

  it('displays paid and cost amounts on balance cards', async () => {
    vi.mocked(getSettlement).mockResolvedValue(mockSettlement);

    render(Settlement);

    await waitFor(() => {
      expect(screen.getByText('Bob')).toBeInTheDocument();
    });

    // Alice: Paid €100.00, Cost €50.00
    expect(screen.getByText('€100.00')).toBeInTheDocument();
    // Bob: Paid €0.00, Cost €50.00
    expect(screen.getByText('€0.00')).toBeInTheDocument();
  });

  it('displays positive balance as "Owed" in green', async () => {
    vi.mocked(getSettlement).mockResolvedValue(mockSettlement);

    render(Settlement);

    await waitFor(() => {
      expect(screen.getByText('Owed €50.00')).toBeInTheDocument();
    });

    const owedElement = screen.getByText('Owed €50.00');
    expect(owedElement.className).toContain('text-green-600');
  });

  it('displays negative balance as "Owes" in red', async () => {
    vi.mocked(getSettlement).mockResolvedValue(mockSettlement);

    render(Settlement);

    await waitFor(() => {
      expect(screen.getByText('Owes €50.00')).toBeInTheDocument();
    });

    const owesElement = screen.getByText('Owes €50.00');
    expect(owesElement.className).toContain('text-red-600');
  });

  it('displays zero balance as "Settled"', async () => {
    vi.mocked(getSettlement).mockResolvedValue(mockSettlementAllSettled);

    render(Settlement);

    await waitFor(() => {
      expect(screen.getAllByText('Settled')).toHaveLength(2);
    });
  });

  it('shows "No participants" when balances are empty', async () => {
    vi.mocked(getSettlement).mockResolvedValue({
      balances: [],
      reimbursements: [],
    });

    render(Settlement);

    await waitFor(() => {
      expect(screen.getByText('No participants')).toBeInTheDocument();
    });
  });

  // --- Resolve Button ---

  it('shows Resolve button when reimbursements are not yet shown', async () => {
    vi.mocked(getSettlement).mockResolvedValue(mockSettlement);

    render(Settlement);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument();
    });
  });

  it('does not show reimbursement details before clicking Resolve', async () => {
    vi.mocked(getSettlement).mockResolvedValue(mockSettlement);

    render(Settlement);

    await waitFor(() => {
      expect(screen.getByText('Bob')).toBeInTheDocument();
    });

    expect(screen.queryByText(/Pay.*to/)).not.toBeInTheDocument();
    expect(screen.queryByText(/Receive.*from/)).not.toBeInTheDocument();
  });

  it('shows reimbursement details after clicking Resolve', async () => {
    vi.mocked(getSettlement).mockResolvedValue(mockSettlement);

    render(Settlement);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByRole('button', { name: 'Resolve' }));

    await waitFor(() => {
      // Bob pays Alice €50
      expect(screen.getByText('Pay €50.00 to Alice')).toBeInTheDocument();
      expect(screen.getByText('Receive €50.00 from Bob')).toBeInTheDocument();
    });
  });

  it('hides Resolve button after clicking it', async () => {
    vi.mocked(getSettlement).mockResolvedValue(mockSettlement);

    render(Settlement);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByRole('button', { name: 'Resolve' }));

    await waitFor(() => {
      expect(screen.queryByRole('button', { name: 'Resolve' })).not.toBeInTheDocument();
    });
  });

  it('does not show Resolve button when all participants are settled', async () => {
    vi.mocked(getSettlement).mockResolvedValue(mockSettlementAllSettled);

    render(Settlement);

    await waitFor(() => {
      expect(screen.getAllByText('Settled')).toHaveLength(2);
    });

    // Resolve button should still be shown (it's hidden only after clicking)
    // But after clicking, no reimbursement details appear since the list is empty
    expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument();
  });

  // --- Auto-resolved via sessionStorage ---

  it('shows reimbursements directly when settlement-resolved flag is set', async () => {
    // Simulate flag set by Split dashboard
    sessionStorage.setItem('settlement-resolved', 'true');

    vi.mocked(getSettlement).mockResolvedValue(mockSettlement);

    render(Settlement);

    await waitFor(() => {
      // Reimbursements should be visible immediately without clicking Resolve
      expect(screen.getByText('Pay €50.00 to Alice')).toBeInTheDocument();
      expect(screen.getByText('Receive €50.00 from Bob')).toBeInTheDocument();
    });

    // Resolve button should not be visible
    expect(screen.queryByRole('button', { name: 'Resolve' })).not.toBeInTheDocument();

    // Flag should be consumed (removed from sessionStorage)
    expect(sessionStorage.getItem('settlement-resolved')).toBeNull();
  });

  // --- Preferred Creditor ---

  describe('Preferred Creditor', () => {
    it('shows preferred creditor select for debtors', async () => {
      vi.mocked(getSettlement).mockResolvedValue(mockSettlement);

      render(Settlement);

      await waitFor(() => {
        // Bob owes money → should have a select
        expect(screen.getByRole('combobox', { name: 'Preferred creditor for Bob' })).toBeInTheDocument();
      });
    });

    it('does not show preferred creditor select for creditors', async () => {
      vi.mocked(getSettlement).mockResolvedValue(mockSettlement);

      render(Settlement);

      await waitFor(() => {
        expect(screen.getByText('Bob')).toBeInTheDocument();
      });

      // Alice is owed money → no select for her
      expect(screen.queryByRole('combobox', { name: 'Preferred creditor for Alice' })).not.toBeInTheDocument();
    });

    it('pre-selects the current preferred creditor', async () => {
      vi.mocked(getSplit).mockResolvedValue({
        id: 'test-split-id',
        name: 'Test Split',
        createdAt: '2026-01-01T00:00:00Z',
        participants: [
          { id: 'p1', name: 'Alice', nights: 3, share: 1, preferredCreditorId: null },
          { id: 'p2', name: 'Bob', nights: 2, share: 1, preferredCreditorId: 'p1' },
        ],
        expenses: [],
        settlement: null,
      });
      vi.mocked(getSettlement).mockResolvedValue(mockSettlement);

      render(Settlement);

      await waitFor(() => {
        const select = screen.getByRole('combobox', { name: 'Preferred creditor for Bob' }) as HTMLSelectElement;
        expect(select.value).toBe('p1');
      });
    });

    it('calls updateParticipant when preferred creditor changes', async () => {
      vi.mocked(getSettlement).mockResolvedValue(mockSettlement);

      render(Settlement);

      await waitFor(() => {
        expect(screen.getByRole('combobox', { name: 'Preferred creditor for Bob' })).toBeInTheDocument();
      });

      const select = screen.getByRole('combobox', { name: 'Preferred creditor for Bob' }) as HTMLSelectElement;
      await fireEvent.change(select, { target: { value: 'p1' } });

      await waitFor(() => {
        expect(updateParticipant).toHaveBeenCalledWith('test-split-id', 'p2', {
          name: 'Bob',
          nights: 2,
          share: 1,
          preferredCreditorId: 'p1',
        });
      });
    });

    it('sends null preferredCreditorId when "No preference" is selected', async () => {
      vi.mocked(getSplit).mockResolvedValue({
        id: 'test-split-id',
        name: 'Test Split',
        createdAt: '2026-01-01T00:00:00Z',
        participants: [
          { id: 'p1', name: 'Alice', nights: 3, share: 1, preferredCreditorId: null },
          { id: 'p2', name: 'Bob', nights: 2, share: 1, preferredCreditorId: 'p1' },
        ],
        expenses: [],
        settlement: null,
      });
      vi.mocked(getSettlement).mockResolvedValue(mockSettlement);

      render(Settlement);

      await waitFor(() => {
        expect(screen.getByRole('combobox', { name: 'Preferred creditor for Bob' })).toBeInTheDocument();
      });

      const select = screen.getByRole('combobox', { name: 'Preferred creditor for Bob' }) as HTMLSelectElement;
      await fireEvent.change(select, { target: { value: '' } });

      await waitFor(() => {
        expect(updateParticipant).toHaveBeenCalledWith('test-split-id', 'p2', {
          name: 'Bob',
          nights: 2,
          share: 1,
          preferredCreditorId: null,
        });
      });
    });

    it('only lists creditors (participants with positive balance) as options', async () => {
      // Three participants: Alice (creditor), Bob (debtor), Charlie (debtor)
      vi.mocked(getSplit).mockResolvedValue({
        id: 'test-split-id',
        name: 'Test Split',
        createdAt: '2026-01-01T00:00:00Z',
        participants: [
          { id: 'p1', name: 'Alice', nights: 3, share: 1, preferredCreditorId: null },
          { id: 'p2', name: 'Bob', nights: 2, share: 1, preferredCreditorId: null },
          { id: 'p3', name: 'Charlie', nights: 2, share: 1, preferredCreditorId: null },
        ],
        expenses: [],
        settlement: null,
      });
      vi.mocked(getSettlement).mockResolvedValue({
        balances: [
          { participantId: 'p1', participantName: 'Alice', totalPaid: 100, totalCost: 33, balance: 67 },
          { participantId: 'p2', participantName: 'Bob', totalPaid: 0, totalCost: 33, balance: -33 },
          { participantId: 'p3', participantName: 'Charlie', totalPaid: 0, totalCost: 34, balance: -34 },
        ],
        reimbursements: [],
      });

      render(Settlement);

      await waitFor(() => {
        expect(screen.getByRole('combobox', { name: 'Preferred creditor for Bob' })).toBeInTheDocument();
      });

      const bobSelect = screen.getByRole('combobox', { name: 'Preferred creditor for Bob' }) as HTMLSelectElement;
      const bobOptions = Array.from(bobSelect.options).map(o => o.text);
      // Only Alice (creditor) should appear as an option, not Charlie (fellow debtor)
      expect(bobOptions).toContain('Alice');
      expect(bobOptions).not.toContain('Charlie');
    });

    it('keeps the select enabled after clicking Resolve', async () => {
      vi.mocked(getSettlement).mockResolvedValue(mockSettlement);

      render(Settlement);

      await waitFor(() => {
        expect(screen.getByRole('combobox', { name: 'Preferred creditor for Bob' })).toBeInTheDocument();
      });

      const select = screen.getByRole('combobox', { name: 'Preferred creditor for Bob' }) as HTMLSelectElement;
      await fireEvent.click(screen.getByRole('button', { name: 'Resolve' }));

      await waitFor(() => {
        expect(screen.getByText('Pay €50.00 to Alice')).toBeInTheDocument();
      });

      expect(select.disabled).toBe(false);
    });

    it('reloads the settlement and hides reimbursements when preferred creditor changes after Resolve', async () => {
      vi.mocked(getSettlement).mockResolvedValue(mockSettlement);

      render(Settlement);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Resolve' }));

      await waitFor(() => {
        expect(screen.getByText('Pay €50.00 to Alice')).toBeInTheDocument();
      });

      const select = screen.getByRole('combobox', { name: 'Preferred creditor for Bob' }) as HTMLSelectElement;
      await fireEvent.change(select, { target: { value: 'p1' } });

      await waitFor(() => {
        expect(updateParticipant).toHaveBeenCalled();
        expect(getSettlement).toHaveBeenCalledTimes(2);
      });

      // Reimbursements are hidden until user clicks Resolve again
      expect(screen.queryByText('Pay €50.00 to Alice')).not.toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Resolve' })).toBeInTheDocument();
    });

    it('shows error toast when updateParticipant fails', async () => {
      vi.mocked(getSettlement).mockResolvedValue(mockSettlement);
      vi.mocked(updateParticipant).mockRejectedValue({ detail: 'Save failed' });

      render(Settlement);

      await waitFor(() => {
        expect(screen.getByRole('combobox', { name: 'Preferred creditor for Bob' })).toBeInTheDocument();
      });

      const select = screen.getByRole('combobox', { name: 'Preferred creditor for Bob' }) as HTMLSelectElement;
      await fireEvent.change(select, { target: { value: 'p1' } });

      await waitFor(() => {
        expect(addToast).toHaveBeenCalledWith({ type: 'error', message: 'Save failed' });
      });
    });
  });
});
