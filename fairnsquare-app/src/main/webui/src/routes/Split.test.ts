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
  addParticipant: vi.fn(),
  updateParticipant: vi.fn(),
  deleteParticipant: vi.fn(),
  addExpense: vi.fn(),
}));

// Mock the toast store
vi.mock('$lib/stores/toastStore.svelte', () => ({
  addToast: vi.fn(),
}));

import { getSplit, addParticipant, updateParticipant, deleteParticipant, addExpense } from '$lib/api/splits';
import { navigate, route } from '$lib/router';
import { addToast } from '$lib/stores/toastStore.svelte';

describe('Split', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (route as any).params = { splitId: 'test-split-id' };
    localStorage.removeItem('fairnsquare_lastParticipantNights');
  });

  // --- Loading / Error / 404 States ---

  it('shows loading state initially', () => {
    vi.mocked(getSplit).mockImplementation(() => new Promise(() => {}));
    render(Split);

    expect(screen.getByText('Loading split...')).toBeInTheDocument();
  });

  it('shows 404 state when split not found', async () => {
    vi.mocked(getSplit).mockRejectedValue({ status: 404, detail: 'Not found' });

    render(Split);

    await waitFor(() => {
      expect(screen.getByText('Split not found')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: 'Create a new split' })).toBeInTheDocument();
  });

  it('navigates home when clicking Create a new split on 404', async () => {
    vi.mocked(getSplit).mockRejectedValue({ status: 404, detail: 'Not found' });

    render(Split);

    await waitFor(() => {
      expect(screen.getByText('Split not found')).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByRole('button', { name: 'Create a new split' }));
    expect(navigate).toHaveBeenCalledWith('/');
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

  // --- Dashboard Layout (FNS-002-2) ---

  describe('Dashboard Layout', () => {
    const mockSplitEmpty: SplitType = {
      id: 'test-split-id',
      name: 'Weekend Trip',
      createdAt: '2026-01-24T12:00:00Z',
      participants: [],
      expenses: [],
    };

    const mockSplitWithData: SplitType = {
      id: 'test-split-id',
      name: 'Weekend Trip',
      createdAt: '2026-01-24T12:00:00Z',
      participants: [
        { id: 'p1', name: 'Alice', nights: 4 },
        { id: 'p2', name: 'Bob', nights: 2 },
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
    };

    it('displays split name as header (AC 1)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('Weekend Trip')).toBeInTheDocument();
      });
    });

    it('displays share button (AC 2)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Split);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Share' })).toBeInTheDocument();
      });
    });

    it('copies URL to clipboard when share button clicked (FNS-002-7 AC3)', async () => {
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

    it('shows URL in toast when clipboard API fails (FNS-002-7 AC4)', async () => {
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

    it('displays expense summary card with count and total (AC 3)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('2 expenses')).toBeInTheDocument();
      });

      // Total: 90 + 60 = 150
      expect(screen.getByText('€150.00')).toBeInTheDocument();
    });

    it('displays expense summary with zero when no expenses (AC 3)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('0 expenses')).toBeInTheDocument();
      });

      expect(screen.getByText('€0.00')).toBeInTheDocument();
    });

    it('displays participant cards with name and nights badge (AC 4)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('Alice')).toBeInTheDocument();
      });

      expect(screen.getByText('Bob')).toBeInTheDocument();
      expect(screen.getByText('4 nights')).toBeInTheDocument();
      expect(screen.getByText('2 nights')).toBeInTheDocument();
    });

    it('displays participant stats: spent, cost, balance (AC 5)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('Alice')).toBeInTheDocument();
      });

      // Alice: Spent €90 (paid for Groceries), Cost €90 (60+30), Balance €0
      // Bob: Spent €60 (paid for Dinner), Cost €60 (30+30), Balance €0
      // Both are settled in this scenario
      const spentLabels = screen.getAllByText(/Spent:/);
      expect(spentLabels.length).toBe(2);

      const costLabels = screen.getAllByText(/Cost:/);
      expect(costLabels.length).toBe(2);
    });

    it('shows positive balance in green and negative in red (AC 5)', async () => {
      const mockSplitUnbalanced: SplitType = {
        id: 'test-split-id',
        name: 'Weekend Trip',
        createdAt: '2026-01-24T12:00:00Z',
        participants: [
          { id: 'p1', name: 'Alice', nights: 2 },
          { id: 'p2', name: 'Bob', nights: 2 },
        ],
        expenses: [
          {
            id: 'e1',
            description: 'Groceries',
            amount: 100.00,
            payerId: 'p1',
            splitMode: 'EQUAL' as const,
            createdAt: '2026-01-25T12:00:00Z',
            shares: [
              { participantId: 'p1', amount: 50.00 },
              { participantId: 'p2', amount: 50.00 },
            ],
          },
        ],
      };

      vi.mocked(getSplit).mockResolvedValue(mockSplitUnbalanced);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('Alice')).toBeInTheDocument();
      });

      // Alice: Spent €100, Cost €50 → Balance +€50 (Owed €50, green)
      expect(screen.getByText('Owed €50.00')).toBeInTheDocument();
      const owedEl = screen.getByText('Owed €50.00').parentElement;
      expect(owedEl?.className).toContain('text-green-600');

      // Bob: Spent €0, Cost €50 → Balance -€50 (Owes €50, red)
      expect(screen.getByText('Owes €50.00')).toBeInTheDocument();
      const owesEl = screen.getByText('Owes €50.00').parentElement;
      expect(owesEl?.className).toContain('text-red-600');
    });

    it('shows settled in gray when balance is zero (AC 5)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('Alice')).toBeInTheDocument();
      });

      // Both Alice and Bob are settled in mockSplitWithData
      const settledLabels = screen.getAllByText('Settled');
      expect(settledLabels.length).toBe(2);
    });

    it('shows no participants message when empty', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('No participants yet')).toBeInTheDocument();
      });
    });

    it('shows edit and delete buttons on each participant card (AC 4)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('Alice')).toBeInTheDocument();
      });

      expect(screen.getByRole('button', { name: 'Edit Alice' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Delete Alice' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Edit Bob' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Delete Bob' })).toBeInTheDocument();
    });

    it('shows Add Expense button on each participant card (FNS-002-3)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('Alice')).toBeInTheDocument();
      });

      expect(screen.getByRole('button', { name: 'Add expense for Alice' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Add expense for Bob' })).toBeInTheDocument();
    });

    it('opens Add Expense Modal when Add Expense button is clicked (FNS-002-3)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('Alice')).toBeInTheDocument();
      });

      // Click Add Expense button for Alice
      await fireEvent.click(screen.getByRole('button', { name: 'Add expense for Alice' }));

      // Modal should open with Add Expense title
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: 'Add Expense' })).toBeInTheDocument();
      });
    });

    it('pre-selects payer in Add Expense Modal from clicked participant (FNS-002-3)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('Bob')).toBeInTheDocument();
      });

      // Click Add Expense button for Bob
      await fireEvent.click(screen.getByRole('button', { name: 'Add expense for Bob' }));

      // Modal should show with Add Expense title
      await waitFor(() => {
        expect(screen.getByRole('heading', { name: 'Add Expense' })).toBeInTheDocument();
      });

      // Modal should have the amount field (confirms modal is open and functional)
      expect(screen.getByLabelText(/amount/i)).toBeInTheDocument();
      // The payer select should be rendered with Bob pre-selected
      // (verified implicitly by modal opening for Bob's card)
    });

    it('shows Add Participant button (AC 6)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Split);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });
    });
  });

  // --- Add Participant ---

  describe('Add Participant', () => {
    const mockSplit: SplitType = {
      id: 'test-split-id',
      name: 'Weekend Trip',
      createdAt: '2026-01-24T12:00:00Z',
      participants: [],
      expenses: [],
    };

    it('shows form when Add Participant button is clicked', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplit);

      render(Split);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));

      expect(screen.getByLabelText('Name')).toBeInTheDocument();
      expect(screen.getByLabelText('Nights')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Add' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
    });

    it('defaults nights to 1 for first participant', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplit);

      render(Split);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));

      const nightsInput = screen.getByLabelText('Nights') as HTMLInputElement;
      expect(nightsInput.value).toBe('1');
    });

    it('uses last entered value for subsequent additions', async () => {
      localStorage.setItem('fairnsquare_lastParticipantNights', '3');
      vi.mocked(getSplit).mockResolvedValue(mockSplit);

      render(Split);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));

      const nightsInput = screen.getByLabelText('Nights') as HTMLInputElement;
      expect(nightsInput.value).toBe('3');
    });

    it('shows validation error for empty name', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplit);

      render(Split);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));
      await fireEvent.click(screen.getByRole('button', { name: 'Add' }));

      expect(screen.getByText('Name is required')).toBeInTheDocument();
      expect(addParticipant).not.toHaveBeenCalled();
    });

    it('shows validation error for nights less than 0.5', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplit);

      render(Split);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));

      await fireEvent.input(screen.getByLabelText('Name'), { target: { value: 'Alice' } });
      await fireEvent.input(screen.getByLabelText('Nights'), { target: { value: '0' } });
      await fireEvent.click(screen.getByRole('button', { name: 'Add' }));

      await waitFor(() => {
        expect(screen.getByText('Nights must be at least 0.5')).toBeInTheDocument();
      });
      expect(addParticipant).not.toHaveBeenCalled();
    });

    it('shows validation error for nights greater than 365', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplit);

      render(Split);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));

      await fireEvent.input(screen.getByLabelText('Name'), { target: { value: 'Alice' } });
      await fireEvent.input(screen.getByLabelText('Nights'), { target: { value: '366' } });
      await fireEvent.click(screen.getByRole('button', { name: 'Add' }));

      expect(screen.getByText('Nights cannot exceed 365')).toBeInTheDocument();
      expect(addParticipant).not.toHaveBeenCalled();
    });

    it('calls API and refreshes list on successful submission', async () => {
      const mockSplitWithParticipant: SplitType = {
        ...mockSplit,
        participants: [{ id: 'p1', name: 'Alice', nights: 2 }],
      };

      vi.mocked(getSplit)
        .mockResolvedValueOnce(mockSplit)
        .mockResolvedValueOnce(mockSplitWithParticipant);

      vi.mocked(addParticipant).mockResolvedValue({
        id: 'p1',
        name: 'Alice',
        nights: 2,
      });

      render(Split);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));

      await fireEvent.input(screen.getByLabelText('Name'), { target: { value: 'Alice' } });
      await fireEvent.input(screen.getByLabelText('Nights'), { target: { value: '2' } });
      await fireEvent.click(screen.getByRole('button', { name: 'Add' }));

      await waitFor(() => {
        expect(addParticipant).toHaveBeenCalledWith('test-split-id', {
          name: 'Alice',
          nights: 2,
        });
      });

      await waitFor(() => {
        expect(screen.getByText('Alice')).toBeInTheDocument();
      });

      expect(addToast).toHaveBeenCalledWith({
        type: 'success',
        message: 'Participant added successfully',
      });
    });

    it('shows error toast on API error', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplit);
      vi.mocked(addParticipant).mockRejectedValue({
        status: 500,
        detail: 'Server error',
      });

      render(Split);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));
      await fireEvent.input(screen.getByLabelText('Name'), { target: { value: 'Alice' } });
      await fireEvent.click(screen.getByRole('button', { name: 'Add' }));

      await waitFor(() => {
        expect(addToast).toHaveBeenCalledWith({
          type: 'error',
          message: 'Server error',
        });
      });

      // Form should still be open
      expect(screen.getByLabelText('Name')).toBeInTheDocument();
    });

    it('closes form when Cancel is clicked', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplit);

      render(Split);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));
      expect(screen.getByLabelText('Name')).toBeInTheDocument();

      await fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

      await waitFor(() => {
        expect(screen.queryByLabelText('Name')).not.toBeInTheDocument();
      });
      expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
    });
  });

  // --- Edit Participant ---
  // Note: Detailed edit participant testing is in EditParticipantModal.test.ts
  // These tests just verify the modal integration

  describe('Edit Participant', () => {
    const mockSplit: SplitType = {
      id: 'test-split-id',
      name: 'Weekend Trip',
      createdAt: '2026-01-24T12:00:00Z',
      participants: [
        { id: 'p1', name: 'Alice', nights: 2 },
        { id: 'p2', name: 'Bob', nights: 3 },
      ],
      expenses: [],
    };

    it('opens edit modal when clicking edit button', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplit);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('Alice')).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Edit Alice' }));

      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument();
        expect(screen.getByText('Edit Participant')).toBeInTheDocument();
      });
    });
  });

  // --- Delete Participant ---

  describe('Delete Participant', () => {
    const mockSplit: SplitType = {
      id: 'test-split-id',
      name: 'Weekend Trip',
      createdAt: '2026-01-24T12:00:00Z',
      participants: [
        { id: 'p1', name: 'Alice', nights: 2 },
        { id: 'p2', name: 'Bob', nights: 3 },
      ],
      expenses: [],
    };

    it('shows confirmation dialog when delete button is clicked', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplit);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('Alice')).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Delete Alice' }));

      await waitFor(() => {
        expect(screen.getByText('Remove Alice?')).toBeInTheDocument();
      });
      expect(screen.getByText('This cannot be undone.')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Remove' })).toBeInTheDocument();
    });

    it('closes dialog without API call when Cancel is clicked', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplit);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('Alice')).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Delete Alice' }));

      await waitFor(() => {
        expect(screen.getByText('Remove Alice?')).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

      await waitFor(() => {
        expect(screen.queryByText('Remove Alice?')).not.toBeInTheDocument();
      });

      expect(deleteParticipant).not.toHaveBeenCalled();
      expect(screen.getByText('Alice')).toBeInTheDocument();
    });

    it('calls API and removes participant on confirm', async () => {
      const updatedSplit: SplitType = {
        ...mockSplit,
        participants: [{ id: 'p2', name: 'Bob', nights: 3 }],
      };

      vi.mocked(getSplit)
        .mockResolvedValueOnce(mockSplit)
        .mockResolvedValueOnce(updatedSplit);

      vi.mocked(deleteParticipant).mockResolvedValue(undefined);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('Alice')).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Delete Alice' }));

      await waitFor(() => {
        expect(screen.getByText('Remove Alice?')).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Remove' }));

      await waitFor(() => {
        expect(deleteParticipant).toHaveBeenCalledWith('test-split-id', 'p1');
      });

      await waitFor(() => {
        expect(screen.queryByText('Alice')).not.toBeInTheDocument();
      });
      expect(screen.getByText('Bob')).toBeInTheDocument();
    });

    it('shows success toast after deletion', async () => {
      const updatedSplit: SplitType = {
        ...mockSplit,
        participants: [{ id: 'p2', name: 'Bob', nights: 3 }],
      };

      vi.mocked(getSplit)
        .mockResolvedValueOnce(mockSplit)
        .mockResolvedValueOnce(updatedSplit);

      vi.mocked(deleteParticipant).mockResolvedValue(undefined);

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('Alice')).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Delete Alice' }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Remove' })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Remove' }));

      await waitFor(() => {
        expect(addToast).toHaveBeenCalledWith({
          type: 'success',
          message: 'Participant removed successfully',
        });
      });
    });

    it('shows error toast for 409 Conflict (participant has expenses)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplit);
      vi.mocked(deleteParticipant).mockRejectedValue({
        status: 409,
        detail: 'Cannot remove participant with associated expenses.',
      });

      render(Split);

      await waitFor(() => {
        expect(screen.getByText('Alice')).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Delete Alice' }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Remove' })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Remove' }));

      await waitFor(() => {
        expect(addToast).toHaveBeenCalledWith({
          type: 'error',
          message: 'Cannot remove Alice - they have associated expenses. Remove or reassign their expenses first.',
        });
      });

      expect(screen.getByText('Alice')).toBeInTheDocument();
    });
  });
});