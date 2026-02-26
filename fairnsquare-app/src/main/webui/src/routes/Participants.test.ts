import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import Participants from './Participants.svelte';
import type { Split as SplitType } from '$lib/api/splits';

// Mock the router
vi.mock('$lib/router', () => {
  return {
    p: vi.fn((path: string) => path),
    navigate: vi.fn(),
    isActive: vi.fn(),
    route: {
      params: { splitId: 'test-split-id' },
      pathname: '/splits/test-split-id/participants',
      search: {},
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

import { getSplit, addParticipant, deleteParticipant } from '$lib/api/splits';
import { navigate, route } from '$lib/router';
import { addToast } from '$lib/stores/toastStore.svelte';

describe('Participants', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (route as any).params = { splitId: 'test-split-id' };
    (route as any).search = {};
    localStorage.removeItem('fairnsquare_lastParticipantNights');
  });

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

  // --- Loading State ---

  it('shows loading state initially', () => {
    vi.mocked(getSplit).mockImplementation(() => new Promise(() => {}));
    render(Participants);

    expect(screen.getByText('Loading participants...')).toBeInTheDocument();
  });

  // --- Page Header ---

  it('displays Participants title with count', async () => {
    vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

    render(Participants);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Participants (0)' })).toBeInTheDocument();
    });
  });

  it('displays participant count in title', async () => {
    vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

    render(Participants);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Participants (2)' })).toBeInTheDocument();
    });
  });

  it('redirects to home and shows info toast when split not found (404)', async () => {
    vi.mocked(getSplit).mockRejectedValue({ status: 404, detail: 'Not found' });

    render(Participants);

    await waitFor(() => {
      expect(navigate).toHaveBeenCalledWith('/');
      expect(addToast).toHaveBeenCalledWith({
        type: 'info',
        message: 'Split not found — create a new one.',
      });
    });
  });

  it('redirects to home and shows info toast when split ID is invalid (400)', async () => {
    vi.mocked(getSplit).mockRejectedValue({ status: 400, detail: 'Invalid split ID format' });

    render(Participants);

    await waitFor(() => {
      expect(navigate).toHaveBeenCalledWith('/');
      expect(addToast).toHaveBeenCalledWith({
        type: 'info',
        message: 'Split not found — create a new one.',
      });
    });
  });

  it('navigates back to dashboard when back button is clicked', async () => {
    vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

    render(Participants);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Back to dashboard' })).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByRole('button', { name: 'Back to dashboard' }));

    expect(navigate).toHaveBeenCalledWith('/splits/test-split-id');
  });

  // --- Summary Card ---

  it('displays participant summary card with names only (no count)', async () => {
    vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

    render(Participants);

    await waitFor(() => {
      expect(screen.getByText('Alice (4n), Bob (2n)')).toBeInTheDocument();
    });

    expect(screen.queryByText('2 participants')).not.toBeInTheDocument();
  });

  it('does not display summary card when no participants', async () => {
    vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

    render(Participants);

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Participants (0)' })).toBeInTheDocument();
    });

    expect(screen.queryByText('Alice')).not.toBeInTheDocument();
  });

  it('displays participants sorted alphabetically', async () => {
    const mockSplitUnsorted: SplitType = {
      id: 'test-split-id',
      name: 'Weekend Trip',
      createdAt: '2026-01-24T12:00:00Z',
      participants: [
        { id: 'p3', name: 'Charlie', nights: 3, numberOfPersons: 1 },
        { id: 'p1', name: 'Alice', nights: 2, numberOfPersons: 1 },
        { id: 'p2', name: 'Bob', nights: 1, numberOfPersons: 1 },
      ],
      expenses: [],
      settlement: null,
    };

    vi.mocked(getSplit).mockResolvedValue(mockSplitUnsorted);

    render(Participants);

    await waitFor(() => {
      expect(screen.getByText('Alice')).toBeInTheDocument();
    });

    // Summary card shows names in sorted order
    expect(screen.getByText('Alice (2n), Bob (1n), Charlie (3n)')).toBeInTheDocument();
  });

  // --- Participant Cards ---

  it('displays participant cards with name and nights badge', async () => {
    vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

    render(Participants);

    await waitFor(() => {
      expect(screen.getByText('Alice')).toBeInTheDocument();
    });

    expect(screen.getByText('Bob')).toBeInTheDocument();
    expect(screen.getByText('4 nights')).toBeInTheDocument();
    expect(screen.getByText('2 nights')).toBeInTheDocument();
  });

  it('displays expense count badge on each participant card', async () => {
    vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

    render(Participants);

    await waitFor(() => {
      expect(screen.getByText('Alice')).toBeInTheDocument();
    });

    // Alice paid e1, Bob paid e2 → each has 1 expense paid
    const expenseBadges = screen.getAllByText(/\d+ expense/);
    expect(expenseBadges.length).toBe(2);
    expect(expenseBadges[0].textContent).toMatch('1 expense');
    expect(expenseBadges[1].textContent).toMatch('1 expense');
  });

  it('displays participant stats: spent, cost, balance', async () => {
    vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

    render(Participants);

    await waitFor(() => {
      expect(screen.getByText('Alice')).toBeInTheDocument();
    });

    const spentLabels = screen.getAllByTitle('Spent');
    expect(spentLabels.length).toBe(2);

    const costLabels = screen.getAllByTitle('Cost');
    expect(costLabels.length).toBe(2);
  });

  it('shows positive balance in green and negative in red', async () => {
    const mockSplitUnbalanced: SplitType = {
      id: 'test-split-id',
      name: 'Weekend Trip',
      createdAt: '2026-01-24T12:00:00Z',
      participants: [
        { id: 'p1', name: 'Alice', nights: 2, numberOfPersons: 1 },
        { id: 'p2', name: 'Bob', nights: 2, numberOfPersons: 1 },
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
      settlement: null,
    };

    vi.mocked(getSplit).mockResolvedValue(mockSplitUnbalanced);

    render(Participants);

    await waitFor(() => {
      expect(screen.getByText('Alice')).toBeInTheDocument();
    });

    // Alice: Spent €100, Cost €50 → Balance +€50 (green, icon + €50.00)
    const balanceSpans = screen.getAllByTitle('Balance');
    const aliceBalance = balanceSpans[0];
    expect(aliceBalance.className).toContain('text-green-600');
    expect(aliceBalance.textContent).toContain('€50.00');

    // Bob: Spent €0, Cost €50 → Balance -€50 (red, icon + €50.00)
    const bobBalance = balanceSpans[1];
    expect(bobBalance.className).toContain('text-red-600');
    expect(bobBalance.textContent).toContain('€50.00');
  });

  it('shows settled in gray when balance is zero', async () => {
    vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

    render(Participants);

    await waitFor(() => {
      expect(screen.getByText('Alice')).toBeInTheDocument();
    });

    // Both Alice and Bob are settled in mockSplitWithData
    const settledLabels = screen.getAllByText('Settled');
    expect(settledLabels.length).toBe(2);
  });

  it('shows no participants message when empty', async () => {
    vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

    render(Participants);

    await waitFor(() => {
      expect(screen.getByText('No participants yet')).toBeInTheDocument();
    });
  });

  // --- Action Buttons ---

  it('shows edit and delete buttons on each participant card', async () => {
    vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

    render(Participants);

    await waitFor(() => {
      expect(screen.getByText('Alice')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: 'Edit Alice' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Delete Alice' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Edit Bob' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Delete Bob' })).toBeInTheDocument();
  });

  it('shows Add Expense button on each participant card', async () => {
    vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

    render(Participants);

    await waitFor(() => {
      expect(screen.getByText('Alice')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: 'Add expense for Alice' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Add expense for Bob' })).toBeInTheDocument();
  });

  it('opens Add Expense Modal when Add Expense button is clicked', async () => {
    vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

    render(Participants);

    await waitFor(() => {
      expect(screen.getByText('Alice')).toBeInTheDocument();
    });

    await fireEvent.click(screen.getByRole('button', { name: 'Add expense for Alice' }));

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Add Expense' })).toBeInTheDocument();
    });
  });

  // --- Edit Participant ---

  describe('Edit Participant', () => {
    const mockSplit: SplitType = {
      id: 'test-split-id',
      name: 'Weekend Trip',
      createdAt: '2026-01-24T12:00:00Z',
      participants: [
        { id: 'p1', name: 'Alice', nights: 2, numberOfPersons: 1 },
        { id: 'p2', name: 'Bob', nights: 3, numberOfPersons: 1 },
      ],
      expenses: [],
      settlement: null,
    };

    it('opens edit modal when clicking edit button', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplit);

      render(Participants);

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

  // --- Add Participant ---

  describe('Add Participant', () => {
    it('shows Add Participant button', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Participants);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });
    });

    it('shows form when Add Participant button is clicked', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Participants);

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
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Participants);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));

      const nightsInput = screen.getByLabelText('Nights') as HTMLInputElement;
      expect(nightsInput.value).toBe('1');
    });

    it('uses last entered value for subsequent additions', async () => {
      localStorage.setItem('fairnsquare_lastParticipantNights', '3');
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Participants);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));

      const nightsInput = screen.getByLabelText('Nights') as HTMLInputElement;
      expect(nightsInput.value).toBe('3');
    });

    it('shows validation error for empty name', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Participants);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));
      await fireEvent.click(screen.getByRole('button', { name: 'Add' }));

      expect(screen.getByText('Name is required')).toBeInTheDocument();
      expect(addParticipant).not.toHaveBeenCalled();
    });

    it('shows duplicate name error while typing without submitting', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

      render(Participants);

      await waitFor(() => {
        expect(screen.getByText('Alice')).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));
      await fireEvent.input(screen.getByLabelText('Name'), { target: { value: 'Alice' } });

      expect(screen.getByText('A participant with this name already exists')).toBeInTheDocument();
      expect(addParticipant).not.toHaveBeenCalled();
    });

    it('shows length error while typing without submitting', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

      render(Participants);

      await waitFor(() => {
        expect(screen.getByText('Alice')).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));
      await fireEvent.input(screen.getByLabelText('Name'), { target: { value: 'A'.repeat(51) } });

      expect(screen.getByText('Name cannot exceed 50 characters')).toBeInTheDocument();
      expect(addParticipant).not.toHaveBeenCalled();
    });

    it('does not show required error while field is empty during typing', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

      render(Participants);

      await waitFor(() => {
        expect(screen.getByText('Alice')).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));
      await fireEvent.input(screen.getByLabelText('Name'), { target: { value: '' } });

      expect(screen.queryByText('Name is required')).not.toBeInTheDocument();
    });

    it('shows validation error when name duplicates an existing participant', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

      render(Participants);

      await waitFor(() => {
        expect(screen.getByText('Alice')).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));
      await fireEvent.input(screen.getByLabelText('Name'), { target: { value: 'Alice' } });
      await fireEvent.click(screen.getByRole('button', { name: 'Add' }));

      expect(screen.getByText('A participant with this name already exists')).toBeInTheDocument();
      expect(addParticipant).not.toHaveBeenCalled();
    });

    it('duplicate name validation is case-insensitive', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithData);

      render(Participants);

      await waitFor(() => {
        expect(screen.getByText('Alice')).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));
      await fireEvent.input(screen.getByLabelText('Name'), { target: { value: 'aLiCe' } });
      await fireEvent.click(screen.getByRole('button', { name: 'Add' }));

      expect(screen.getByText('A participant with this name already exists')).toBeInTheDocument();
      expect(addParticipant).not.toHaveBeenCalled();
    });

    it('shows nights too low error while typing without submitting', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Participants);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));
      await fireEvent.input(screen.getByLabelText('Nights'), { target: { value: '0' } });

      expect(screen.getByText('Nights must be at least 0.5')).toBeInTheDocument();
      expect(addParticipant).not.toHaveBeenCalled();
    });

    it('shows nights too high error while typing without submitting', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Participants);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));
      await fireEvent.input(screen.getByLabelText('Nights'), { target: { value: '400' } });

      expect(screen.getByText('Nights cannot exceed 365')).toBeInTheDocument();
      expect(addParticipant).not.toHaveBeenCalled();
    });

    it('shows validation error for nights less than 0.5', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Participants);

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
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Participants);

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
        ...mockSplitEmpty,
        participants: [{ id: 'p1', name: 'Alice', nights: 2, numberOfPersons: 1 }],
      };

      vi.mocked(getSplit)
        .mockResolvedValueOnce(mockSplitEmpty)
        .mockResolvedValueOnce(mockSplitWithParticipant);

      vi.mocked(addParticipant).mockResolvedValue({
        id: 'p1',
        name: 'Alice',
        nights: 2,
        numberOfPersons: 1,
      });

      render(Participants);

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
          numberOfPersons: 1,
        });
      });

      await waitFor(() => {
        expect(screen.getByText('Alice')).toBeInTheDocument();
      });

      expect(addToast).toHaveBeenCalledWith({
        type: 'success',
        message: 'Alice successfully added',
        duration: 4000,
      });
    });

    it('shows error toast on API error', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);
      vi.mocked(addParticipant).mockRejectedValue({
        status: 500,
        detail: 'Server error',
      });

      render(Participants);

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
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Participants);

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

  // --- Delete Participant ---

  describe('Delete Participant', () => {
    const mockSplit: SplitType = {
      id: 'test-split-id',
      name: 'Weekend Trip',
      createdAt: '2026-01-24T12:00:00Z',
      participants: [
        { id: 'p1', name: 'Alice', nights: 2, numberOfPersons: 1 },
        { id: 'p2', name: 'Bob', nights: 3, numberOfPersons: 1 },
      ],
      expenses: [],
      settlement: null,
    };

    it('shows confirmation dialog when delete button is clicked', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplit);

      render(Participants);

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

      render(Participants);

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
        participants: [{ id: 'p2', name: 'Bob', nights: 3, numberOfPersons: 1 }],
      };

      vi.mocked(getSplit)
        .mockResolvedValueOnce(mockSplit)
        .mockResolvedValueOnce(updatedSplit);

      vi.mocked(deleteParticipant).mockResolvedValue(undefined);

      render(Participants);

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
        participants: [{ id: 'p2', name: 'Bob', nights: 3, numberOfPersons: 1 }],
      };

      vi.mocked(getSplit)
        .mockResolvedValueOnce(mockSplit)
        .mockResolvedValueOnce(updatedSplit);

      vi.mocked(deleteParticipant).mockResolvedValue(undefined);

      render(Participants);

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

      render(Participants);

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

  // --- Number of Persons ---

  describe('Number of Persons', () => {
    it('shows Persons input field in add form', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Participants);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));

      expect(screen.getByLabelText('Persons')).toBeInTheDocument();
    });

    it('defaults Persons to 1 in add form', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Participants);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));

      const personsInput = screen.getByLabelText('Persons') as HTMLInputElement;
      expect(personsInput.value).toBe('1');
    });

    it('sends numberOfPersons in API call', async () => {
      const mockSplitWithParticipant: SplitType = {
        ...mockSplitEmpty,
        participants: [{ id: 'p1', name: 'Alice', nights: 2, numberOfPersons: 2 }],
      };

      vi.mocked(getSplit)
        .mockResolvedValueOnce(mockSplitEmpty)
        .mockResolvedValueOnce(mockSplitWithParticipant);

      vi.mocked(addParticipant).mockResolvedValue({
        id: 'p1',
        name: 'Alice',
        nights: 2,
        numberOfPersons: 2,
      });

      render(Participants);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));

      await fireEvent.input(screen.getByLabelText('Name'), { target: { value: 'Alice' } });
      await fireEvent.input(screen.getByLabelText('Nights'), { target: { value: '2' } });
      await fireEvent.input(screen.getByLabelText('Persons'), { target: { value: '2' } });
      await fireEvent.click(screen.getByRole('button', { name: 'Add' }));

      await waitFor(() => {
        expect(addParticipant).toHaveBeenCalledWith('test-split-id', {
          name: 'Alice',
          nights: 2,
          numberOfPersons: 2,
        });
      });
    });

    it('shows persons badge on participant card when numberOfPersons > 1', async () => {
      const mockSplitFamily: SplitType = {
        ...mockSplitEmpty,
        participants: [
          { id: 'p1', name: 'Alice', nights: 4, numberOfPersons: 2.5 },
          { id: 'p2', name: 'Bob', nights: 2, numberOfPersons: 1 },
        ],
      };

      vi.mocked(getSplit).mockResolvedValue(mockSplitFamily);

      render(Participants);

      await waitFor(() => {
        expect(screen.getByText('Alice')).toBeInTheDocument();
      });

      // Alice has 2.5 persons → badge visible
      expect(screen.getByText('2.5 persons')).toBeInTheDocument();
      // Bob has 1 person → no badge
      expect(screen.queryByText('1 person')).not.toBeInTheDocument();
    });

    it('shows persons info in summary card when numberOfPersons > 1', async () => {
      const mockSplitFamily: SplitType = {
        ...mockSplitEmpty,
        participants: [
          { id: 'p1', name: 'Alice', nights: 4, numberOfPersons: 2 },
          { id: 'p2', name: 'Bob', nights: 2, numberOfPersons: 1 },
        ],
      };

      vi.mocked(getSplit).mockResolvedValue(mockSplitFamily);

      render(Participants);

      await waitFor(() => {
        expect(screen.getByText('Alice (4n, 2p), Bob (2n)')).toBeInTheDocument();
      });
    });

    it('shows persons too low error while typing', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Participants);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));
      await fireEvent.input(screen.getByLabelText('Persons'), { target: { value: '0' } });

      expect(screen.getByText('Must be at least 0.5')).toBeInTheDocument();
    });

    it('shows persons too high error while typing', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);

      render(Participants);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: /Add Participant/i }));
      await fireEvent.input(screen.getByLabelText('Persons'), { target: { value: '51' } });

      expect(screen.getByText('Cannot exceed 50')).toBeInTheDocument();
    });
  });

  describe('Auto-open from Home creation flow', () => {
    it('auto-opens add form when addParticipant search param is present', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);
      (route as any).search = { addParticipant: 'true' };

      render(Participants);

      await waitFor(() => {
        expect(screen.getByText('New Participant')).toBeInTheDocument();
      });
    });

    it('does not auto-open add form when addParticipant search param is absent', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);
      (route as any).search = {};

      render(Participants);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      expect(screen.queryByText('New Participant')).not.toBeInTheDocument();
    });
  });
});