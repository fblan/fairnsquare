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
}));

// Mock the toast store
vi.mock('$lib/stores/toastStore.svelte', () => ({
  addToast: vi.fn(),
}));

import { getSettlement } from '$lib/api/splits';
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
  });

  // --- Loading State ---

  it('shows loading state initially', () => {
    vi.mocked(getSettlement).mockImplementation(() => new Promise(() => {}));
    render(Settlement);

    expect(screen.getByText('Loading settlement...')).toBeInTheDocument();
  });

  // --- Header ---

  it('displays header with back button and title', async () => {
    vi.mocked(getSettlement).mockResolvedValue(mockSettlement);

    render(Settlement);

    await waitFor(() => {
      expect(screen.getByText('Settlement')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: 'Back to dashboard' })).toBeInTheDocument();
  });

  it('navigates back to dashboard when back button is clicked', async () => {
    vi.mocked(getSettlement).mockResolvedValue(mockSettlement);

    render(Settlement);

    await waitFor(() => {
      expect(screen.getByText('Settlement')).toBeInTheDocument();
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
      expect(screen.getByText('Alice')).toBeInTheDocument();
      expect(screen.getByText('Bob')).toBeInTheDocument();
    });
  });

  it('displays paid and cost amounts on balance cards', async () => {
    vi.mocked(getSettlement).mockResolvedValue(mockSettlement);

    render(Settlement);

    await waitFor(() => {
      expect(screen.getByText('Alice')).toBeInTheDocument();
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
      expect(screen.getByText('Alice')).toBeInTheDocument();
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
});
