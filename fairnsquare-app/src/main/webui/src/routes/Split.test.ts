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
}));

// Mock the toast store
vi.mock('$lib/stores/toastStore.svelte', () => ({
  addToast: vi.fn(),
}));

import { getSplit } from '$lib/api/splits';
import { navigate, route } from '$lib/router';

describe('Split', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Reset route params before each test
    (route as any).params = { splitId: 'test-split-id' };
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
});
