import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import Home from './Home.svelte';

// Mock the router
vi.mock('$lib/router', () => ({
  p: vi.fn((path: string) => path),
  navigate: vi.fn(),
  isActive: vi.fn(),
  route: { params: {}, pathname: '/' },
}));

// Mock the API
vi.mock('$lib/api/splits', () => ({
  createSplit: vi.fn(),
}));

// Mock the toast store
vi.mock('$lib/stores/toastStore.svelte', () => ({
  addToast: vi.fn(),
}));

import { createSplit } from '$lib/api/splits';
import { navigate } from '$lib/router';

describe('Home', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the create split form', () => {
    render(Home);

    expect(screen.getByText('FairNSquare')).toBeInTheDocument();
    expect(screen.getByText('Create a New Split')).toBeInTheDocument();
    expect(screen.getByLabelText('Split Name')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Create Split' })).toBeInTheDocument();
  });

  it('shows validation error when submitting empty name', async () => {
    render(Home);

    const submitButton = screen.getByRole('button', { name: 'Create Split' });
    await fireEvent.click(submitButton);

    expect(screen.getByText('Split name is required')).toBeInTheDocument();
  });

  it('clears validation error when typing', async () => {
    render(Home);

    // Submit empty to trigger error
    const submitButton = screen.getByRole('button', { name: 'Create Split' });
    await fireEvent.click(submitButton);
    expect(screen.getByText('Split name is required')).toBeInTheDocument();

    // Type in the input
    const input = screen.getByLabelText('Split Name');
    await fireEvent.input(input, { target: { value: 'Test' } });

    expect(screen.queryByText('Split name is required')).not.toBeInTheDocument();
  });

  it('creates split and shows success state', async () => {
    const mockSplit = {
      id: 'abc123',
      name: 'Test Split',
      createdAt: '2026-01-24T12:00:00Z',
      participants: [],
      expenses: [],
    };
    vi.mocked(createSplit).mockResolvedValue(mockSplit);

    render(Home);

    const input = screen.getByLabelText('Split Name');
    await fireEvent.input(input, { target: { value: 'Test Split' } });

    const submitButton = screen.getByRole('button', { name: 'Create Split' });
    await fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText('Split Created!')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: 'Go to Split' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Create Another Split' })).toBeInTheDocument();
  });

  it('navigates to split when clicking Go to Split', async () => {
    const mockSplit = {
      id: 'abc123',
      name: 'Test Split',
      createdAt: '2026-01-24T12:00:00Z',
      participants: [],
      expenses: [],
    };
    vi.mocked(createSplit).mockResolvedValue(mockSplit);

    render(Home);

    const input = screen.getByLabelText('Split Name');
    await fireEvent.input(input, { target: { value: 'Test Split' } });

    const submitButton = screen.getByRole('button', { name: 'Create Split' });
    await fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText('Split Created!')).toBeInTheDocument();
    });

    const goToSplitButton = screen.getByRole('button', { name: 'Go to Split' });
    await fireEvent.click(goToSplitButton);

    expect(navigate).toHaveBeenCalledWith('/splits/:splitId', { params: { splitId: 'abc123' } });
  });

  it('resets form when clicking Create Another Split', async () => {
    const mockSplit = {
      id: 'abc123',
      name: 'Test Split',
      createdAt: '2026-01-24T12:00:00Z',
      participants: [],
      expenses: [],
    };
    vi.mocked(createSplit).mockResolvedValue(mockSplit);

    render(Home);

    const input = screen.getByLabelText('Split Name');
    await fireEvent.input(input, { target: { value: 'Test Split' } });

    const submitButton = screen.getByRole('button', { name: 'Create Split' });
    await fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText('Split Created!')).toBeInTheDocument();
    });

    const createAnotherButton = screen.getByRole('button', { name: 'Create Another Split' });
    await fireEvent.click(createAnotherButton);

    expect(screen.getByText('Create a New Split')).toBeInTheDocument();
    expect(screen.getByLabelText('Split Name')).toHaveValue('');
  });
});
