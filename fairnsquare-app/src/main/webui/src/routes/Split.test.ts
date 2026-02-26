import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import Split from './Split.svelte';
import type { Split as SplitType } from '$lib/api/splits';

// Mock the router
vi.mock('$lib/router', () => {
  return {
    p: vi.fn((path: string) => path),
    navigate: vi.fn(),
    isActive: vi.fn(),
    route: {
      params: { splitId: 'test-split-id' },
      pathname: '/splits/test-split-id',
    },
  };
});

// Mock the API
vi.mock('$lib/api/splits', () => ({
  getSplit: vi.fn(),
}));

// Mock the toast store
vi.mock('$lib/stores/toastStore.svelte', () => ({
  addToast: vi.fn(),
}));

import { getSplit } from '$lib/api/splits';
import { navigate, route } from '$lib/router';
import { addToast } from '$lib/stores/toastStore.svelte';

describe('Split', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (route as any).params = { splitId: 'test-split-id' };
  });

  // --- Loading / Error / 404 States ---

  it('shows loading state initially', () => {
    vi.mocked(getSplit).mockImplementation(() => new Promise(() => {}));
    render(Split);

    expect(screen.getByText('Loading split...')).toBeInTheDocument();
  });

  it('automatically redirects to home when split not found (404)', async () => {
    vi.mocked(getSplit).mockRejectedValue({ status: 404, detail: 'Not found' });

    render(Split);

    await waitFor(() => {
      expect(navigate).toHaveBeenCalledWith('/');
    });
  });

  it('shows info toast when split not found (404)', async () => {
    vi.mocked(getSplit).mockRejectedValue({ status: 404, detail: 'Not found' });

    render(Split);

    await waitFor(() => {
      expect(addToast).toHaveBeenCalledWith({
        type: 'info',
        message: 'Split not found — create a new one.',
      });
    });
  });

  it('shows error state with retry button on network error', async () => {
    vi.mocked(getSplit).mockRejectedValue({ status: 500, detail: 'Server error' });

    render(Split);

    await waitFor(() => {
      expect(screen.getByText('Server error')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument();
  });

  it('retries loading when clicking Retry button', async () => {
    vi.mocked(getSplit)
      .mockRejectedValueOnce({ status: 500, detail: 'Server error' })
      .mockResolvedValueOnce({
        id: 'test-split-id',
        name: 'Weekend Trip',
        createdAt: '2026-01-24T12:00:00Z',
        participants: [],
        expenses: [],
        settlement: null,
      });

    render(Split);

    await waitFor(() => {
      expect(screen.getByText('Server error')).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByRole('button', { name: 'Retry' }));

    await waitFor(() => {
      expect(screen.getByText('Weekend Trip')).toBeInTheDocument();
    });

    expect(getSplit).toHaveBeenCalledTimes(2);
  });

  // --- Dashboard Layout ---

  describe('Dashboard Layout', () => {
    const mockSplitEmpty: SplitType = {
      id: 'test-split-id',
      name: 'Weekend Trip',
      createdAt: '2026-01-24T12:00:00Z',
      participants: [],
      expenses: [],
      settlement: null,
    };

    const mockSplitWithData: SplitType = {
      id: 'test-split-id',
      name: 'Weekend Trip',
      createdAt: '2026-01-24T12:00:00Z',
      participants: [
        { id: 'p1', name: 'Alice', nights: 4, numberOfPersons: 1 },
        { id: 'p2', name: 'Bob', nights: 2, numberOfPersons: 1 },
      ],
      expenses: [
        {
          id: 'e1',
          description: 'Groceries',
          amount: 90.00,
          payerId: 'p1',
          splitMode: 'BY_NIGHT' as const,
          createdAt: '2026-01-25T12:00:00Z',
          shares: [
            { participantId: 'p1', amount: 60.00 },
            { participantId: 'p2', amount: 30.00 },
          ],
        },
        {
          id: 'e2',
          description: 'Dinner',
          amount: 60.00,
          payerId: 'p2',
          splitMode: 'EQUAL' as const,
          createdAt: '2026-01-26T12:00:00Z',
          shares: [
            { participantId: 'p1', amount: 30.00 },
            { participantId: 'p2', amount: 30.00 },
          ],
        },
      ],
      settlement: null,
    };

    it('displays split name as header', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('Weekend Trip')).toBeInTheDocument();
      });
    });

    it('displays share button', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Split);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Share' })).toBeInTheDocument();
      });
    });

    it('copies URL to clipboard when share button clicked', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);
      const mockWriteText = vi.fn().mockResolvedValue(undefined);
      Object.assign(navigator, { clipboard: { writeText: mockWriteText } });

      render(Split);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Share' })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Share' }));

      await waitFor(() => {
        expect(mockWriteText).toHaveBeenCalled();
        expect(addToast).toHaveBeenCalledWith({
          type: 'success',
          message: 'Link copied!',
        });
      });
    });

    it('shows URL in toast when clipboard API fails', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);
      const mockWriteText = vi.fn().mockRejectedValue(new Error('Clipboard denied'));
      Object.assign(navigator, { clipboard: { writeText: mockWriteText } });

      render(Split);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Share' })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Share' }));

      await waitFor(() => {
        expect(addToast).toHaveBeenCalledWith(
          expect.objectContaining({
            message: expect.stringContaining('Share link:'),
          }),
        );
      });
    });

    // --- Expense Summary Card ---

    it('displays expense summary card with count and total', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('2 expenses')).toBeInTheDocument();
      });

      // Total: 90 + 60 = 150
      expect(screen.getByText('€150.00')).toBeInTheDocument();
    });

    it('displays expense summary with zero when no expenses', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('0 expenses')).toBeInTheDocument();
      });

      expect(screen.getByText('€0.00')).toBeInTheDocument();
    });

    it('navigates to expense list when expense card is clicked', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('2 expenses')).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'View all expenses' }));

      expect(navigate).toHaveBeenCalledWith('/splits/test-split-id/expenses');
    });

    // --- Participant Summary Card ---

    it('displays participant summary card with count', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('2 participants')).toBeInTheDocument();
      });
    });

    it('displays singular "participant" when only one', async () => {
      const mockSplitOneParticipant: SplitType = {
        ...mockSplitEmpty,
        participants: [{ id: 'p1', name: 'Alice', nights: 3, numberOfPersons: 1 }],
      };
      vi.mocked(getSplit).mockResolvedValue(mockSplitOneParticipant);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('1 participant')).toBeInTheDocument();
      });
    });

    it('displays participant names with nights in summary', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('Alice (4n), Bob (2n)')).toBeInTheDocument();
      });
    });

    it('displays persons info in participant summary when numberOfPersons > 1', async () => {
      const mockSplitFamily: SplitType = {
        ...mockSplitEmpty,
        participants: [
          { id: 'p1', name: 'Alice', nights: 4, numberOfPersons: 2 },
          { id: 'p2', name: 'Bob', nights: 2, numberOfPersons: 1 },
        ],
      };
      vi.mocked(getSplit).mockResolvedValue(mockSplitFamily);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('Alice (4n, 2p), Bob (2n)')).toBeInTheDocument();
      });
    });

    it('displays zero participants when empty', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('0 participants')).toBeInTheDocument();
      });
    });

    it('does not show participant list when no participants', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('0 participants')).toBeInTheDocument();
      });

      // No summary line since participantSummary is empty
      expect(screen.queryByText(',')).not.toBeInTheDocument();
    });

    it('navigates to participants page when participant card is clicked', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('2 participants')).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'View all participants' }));

      expect(navigate).toHaveBeenCalledWith('/splits/test-split-id/participants');
    });

    // --- Solve Card ---

    it('shows Solve card when split has both participants and expenses', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('Solve')).toBeInTheDocument();
      });

      expect(screen.getByRole('button', { name: 'View settlement' })).toBeInTheDocument();
    });

    it('does not show Solve card when split has no expenses', async () => {
      const mockSplitNoExpenses: SplitType = {
        ...mockSplitEmpty,
        participants: [{ id: 'p1', name: 'Alice', nights: 3, numberOfPersons: 1 }],
      };
      vi.mocked(getSplit).mockResolvedValue(mockSplitNoExpenses);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('1 participant')).toBeInTheDocument();
      });

      expect(screen.queryByText('Solve')).not.toBeInTheDocument();
    });

    it('does not show Solve card when split has no participants', async () => {
      const mockSplitNoParticipants: SplitType = {
        ...mockSplitEmpty,
        expenses: [
          {
            id: 'e1',
            description: 'Test',
            amount: 10,
            payerId: 'p1',
            splitMode: 'EQUAL' as const,
            createdAt: '2026-01-25T12:00:00Z',
            shares: [],
          },
        ],
      };
      vi.mocked(getSplit).mockResolvedValue(mockSplitNoParticipants);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('1 expense')).toBeInTheDocument();
      });

      expect(screen.queryByText('Solve')).not.toBeInTheDocument();
    });

    it('does not show Solve card when split is empty', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('0 expenses')).toBeInTheDocument();
      });

      expect(screen.queryByText('Solve')).not.toBeInTheDocument();
    });

    it('navigates to settlement page without query param when not settled', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('Solve')).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'View settlement' }));

      expect(navigate).toHaveBeenCalledWith('/splits/test-split-id/settlement');
    });

    it('shows "Settled" when settlement is persisted', async () => {
      const mockSplitSettled: SplitType = {
        ...mockSplitWithData,
        settlement: {
          balances: [
            { participantId: 'p1', participantName: 'Alice', totalPaid: 90, totalCost: 75, balance: 15 },
            { participantId: 'p2', participantName: 'Bob', totalPaid: 60, totalCost: 75, balance: -15 },
          ],
          reimbursements: [
            { fromId: 'p2', fromName: 'Bob', toId: 'p1', toName: 'Alice', amount: 15 },
          ],
        },
      };
      vi.mocked(getSplit).mockResolvedValue(mockSplitSettled);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('Settled')).toBeInTheDocument();
      });

      expect(screen.queryByText('Solve')).not.toBeInTheDocument();
    });

    it('sets sessionStorage flag and navigates when settlement is persisted', async () => {
      const mockSplitSettled: SplitType = {
        ...mockSplitWithData,
        settlement: {
          balances: [
            { participantId: 'p1', participantName: 'Alice', totalPaid: 90, totalCost: 75, balance: 15 },
            { participantId: 'p2', participantName: 'Bob', totalPaid: 60, totalCost: 75, balance: -15 },
          ],
          reimbursements: [
            { fromId: 'p2', fromName: 'Bob', toId: 'p1', toName: 'Alice', amount: 15 },
          ],
        },
      };
      vi.mocked(getSplit).mockResolvedValue(mockSplitSettled);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('Settled')).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'View settlement' }));

      expect(sessionStorage.getItem('settlement-resolved')).toBe('true');
      expect(navigate).toHaveBeenCalledWith('/splits/test-split-id/settlement');
    });

    it('shows "Solve" when settlement is null', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('Solve')).toBeInTheDocument();
      });

      expect(screen.queryByText('Settled')).not.toBeInTheDocument();
    });
  });
});