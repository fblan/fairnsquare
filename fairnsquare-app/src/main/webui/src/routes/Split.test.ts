import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import Split from './Split.svelte';

// Mock the router - use factory function to avoid hoisting issues
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
}));

// Mock the toast store
vi.mock('$lib/stores/toastStore.svelte', () => ({
  addToast: vi.fn(),
}));

import { getSplit, addParticipant } from '$lib/api/splits';
import { navigate, route } from '$lib/router';
import { addToast } from '$lib/stores/toastStore.svelte';

describe('Split', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Reset route params before each test
    (route as any).params = { splitId: 'test-split-id' };
    // Clear localStorage for smart default tests
    localStorage.removeItem('fairnsquare_lastParticipantNights');
  });

  it('shows loading state initially', () => {
    vi.mocked(getSplit).mockImplementation(() => new Promise(() => {})); // Never resolves
    render(Split);

    expect(screen.getByText('Loading split...')).toBeInTheDocument();
  });

  it('displays split overview when loaded successfully', async () => {
    const mockSplit = {
      id: 'test-split-id',
      name: 'Weekend Trip',
      createdAt: '2026-01-24T12:00:00Z',
      participants: [],
      expenses: [],
    };
    vi.mocked(getSplit).mockResolvedValue(mockSplit);

    render(Split);

    await waitFor(() => {
      expect(screen.getByText('Weekend Trip')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: 'Share' })).toBeInTheDocument();
    expect(screen.getByText('Participants')).toBeInTheDocument();
    expect(screen.getByText('No participants yet')).toBeInTheDocument();
    expect(screen.getByText('Expenses')).toBeInTheDocument();
    expect(screen.getByText('No expenses yet')).toBeInTheDocument();
    expect(screen.getByText('Balance Summary')).toBeInTheDocument();
  });

  it('displays participants when present', async () => {
    const mockSplit = {
      id: 'test-split-id',
      name: 'Weekend Trip',
      createdAt: '2026-01-24T12:00:00Z',
      participants: [
        { id: 'p1', name: 'Alice', nights: 2 },
        { id: 'p2', name: 'Bob', nights: 3 },
      ],
      expenses: [],
    };
    vi.mocked(getSplit).mockResolvedValue(mockSplit);

    render(Split);

    await waitFor(() => {
      expect(screen.getByText('Alice')).toBeInTheDocument();
    });

    expect(screen.getByText('Bob')).toBeInTheDocument();
    expect(screen.getByText('2 nights')).toBeInTheDocument();
    expect(screen.getByText('3 nights')).toBeInTheDocument();
  });

  it('displays expenses when present', async () => {
    const mockSplit = {
      id: 'test-split-id',
      name: 'Weekend Trip',
      createdAt: '2026-01-24T12:00:00Z',
      participants: [
        { id: 'p1', name: 'Alice', nights: 2 },
      ],
      expenses: [
        {
          id: 'e1',
          description: 'Dinner',
          amount: 50.00,
          payerId: 'p1',
          splitMode: 'EQUAL' as const,
          createdAt: '2026-01-24T12:00:00Z',
        },
      ],
    };
    vi.mocked(getSplit).mockResolvedValue(mockSplit);

    render(Split);

    await waitFor(() => {
      expect(screen.getByText('Dinner')).toBeInTheDocument();
    });

    expect(screen.getByText('Paid by Alice')).toBeInTheDocument();
    expect(screen.getByText('Equal')).toBeInTheDocument();
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

    const createButton = screen.getByRole('button', { name: 'Create a new split' });
    await fireEvent.click(createButton);

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

    const retryButton = screen.getByRole('button', { name: 'Retry' });
    await fireEvent.click(retryButton);

    await waitFor(() => {
      expect(screen.getByText('Weekend Trip')).toBeInTheDocument();
    });

    expect(getSplit).toHaveBeenCalledTimes(2);
  });

  // Story 3.1: Add Participant Tests
  describe('Add Participant (Story 3.1)', () => {
    const mockSplit = {
      id: 'test-split-id',
      name: 'Weekend Trip',
      createdAt: '2026-01-24T12:00:00Z',
      participants: [],
      expenses: [],
    };

    it('shows Add Participant button (AC 1)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplit);

      render(Split);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });
    });

    it('shows form when Add Participant button is clicked (AC 2)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplit);

      render(Split);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      const addButton = screen.getByRole('button', { name: /Add Participant/i });
      await fireEvent.click(addButton);

      expect(screen.getByLabelText('Name')).toBeInTheDocument();
      expect(screen.getByLabelText('Nights')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Add' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
    });

    it('defaults nights to 1 for first participant (AC 3)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplit);

      render(Split);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      const addButton = screen.getByRole('button', { name: /Add Participant/i });
      await fireEvent.click(addButton);

      const nightsInput = screen.getByLabelText('Nights') as HTMLInputElement;
      expect(nightsInput.value).toBe('1');
    });

    it('uses last entered value for subsequent additions (AC 4)', async () => {
      // Set a previous value in localStorage
      localStorage.setItem('fairnsquare_lastParticipantNights', '3');

      vi.mocked(getSplit).mockResolvedValue(mockSplit);

      render(Split);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      const addButton = screen.getByRole('button', { name: /Add Participant/i });
      await fireEvent.click(addButton);

      const nightsInput = screen.getByLabelText('Nights') as HTMLInputElement;
      expect(nightsInput.value).toBe('3');
    });

    it('shows validation error for empty name (AC 6)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplit);

      render(Split);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      const addButton = screen.getByRole('button', { name: /Add Participant/i });
      await fireEvent.click(addButton);

      // Submit with empty name
      const submitButton = screen.getByRole('button', { name: 'Add' });
      await fireEvent.click(submitButton);

      expect(screen.getByText('Name is required')).toBeInTheDocument();
      expect(addParticipant).not.toHaveBeenCalled();
    });

    it('shows validation error for nights less than 1 (AC 7)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplit);

      render(Split);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      const addButton = screen.getByRole('button', { name: /Add Participant/i });
      await fireEvent.click(addButton);

      // Fill in name and set nights to 0
      const nameInput = screen.getByLabelText('Name');
      const nightsInput = screen.getByLabelText('Nights');
      await fireEvent.input(nameInput, { target: { value: 'Alice' } });
      await fireEvent.input(nightsInput, { target: { value: '0' } });

      // Submit
      const submitButton = screen.getByRole('button', { name: 'Add' });
      await fireEvent.click(submitButton);

      expect(screen.getByText('Nights must be at least 1')).toBeInTheDocument();
      expect(addParticipant).not.toHaveBeenCalled();
    });

    it('shows validation error for nights greater than 365', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplit);

      render(Split);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      const addButton = screen.getByRole('button', { name: /Add Participant/i });
      await fireEvent.click(addButton);

      // Fill in name and set nights to 366
      const nameInput = screen.getByLabelText('Name');
      const nightsInput = screen.getByLabelText('Nights');
      await fireEvent.input(nameInput, { target: { value: 'Alice' } });
      await fireEvent.input(nightsInput, { target: { value: '366' } });

      // Submit
      const submitButton = screen.getByRole('button', { name: 'Add' });
      await fireEvent.click(submitButton);

      expect(screen.getByText('Nights cannot exceed 365')).toBeInTheDocument();
      expect(addParticipant).not.toHaveBeenCalled();
    });

    it('calls API and refreshes list on successful submission (AC 5)', async () => {
      const mockSplitWithParticipant = {
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

      // Open form
      const addButton = screen.getByRole('button', { name: /Add Participant/i });
      await fireEvent.click(addButton);

      // Fill in form
      const nameInput = screen.getByLabelText('Name');
      const nightsInput = screen.getByLabelText('Nights');
      await fireEvent.input(nameInput, { target: { value: 'Alice' } });
      await fireEvent.input(nightsInput, { target: { value: '2' } });

      // Submit
      const submitButton = screen.getByRole('button', { name: 'Add' });
      await fireEvent.click(submitButton);

      await waitFor(() => {
        expect(addParticipant).toHaveBeenCalledWith('test-split-id', {
          name: 'Alice',
          nights: 2,
        });
      });

      // Verify list was refreshed
      await waitFor(() => {
        expect(screen.getByText('Alice')).toBeInTheDocument();
      });

      // Verify toast was shown
      expect(addToast).toHaveBeenCalledWith({
        type: 'success',
        message: 'Participant added',
        duration: 3000,
      });
    });

    it('shows error toast and keeps form open on API error (AC 8)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplit);
      vi.mocked(addParticipant).mockRejectedValue({
        status: 500,
        detail: 'Server error',
      });

      render(Split);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
      });

      // Open form
      const addButton = screen.getByRole('button', { name: /Add Participant/i });
      await fireEvent.click(addButton);

      // Fill in form
      const nameInput = screen.getByLabelText('Name');
      await fireEvent.input(nameInput, { target: { value: 'Alice' } });

      // Submit
      const submitButton = screen.getByRole('button', { name: 'Add' });
      await fireEvent.click(submitButton);

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

      // Open form
      const addButton = screen.getByRole('button', { name: /Add Participant/i });
      await fireEvent.click(addButton);

      expect(screen.getByLabelText('Name')).toBeInTheDocument();

      // Click cancel
      const cancelButton = screen.getByRole('button', { name: 'Cancel' });
      await fireEvent.click(cancelButton);

      // Form should be closed, Add Participant button visible again
      await waitFor(() => {
        expect(screen.queryByLabelText('Name')).not.toBeInTheDocument();
      });
      expect(screen.getByRole('button', { name: /Add Participant/i })).toBeInTheDocument();
    });
  });
});
