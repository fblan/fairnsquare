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
  addParticipant: vi.fn(),
}));

// Mock the toast store
vi.mock('$lib/stores/toastStore.svelte', () => ({
  addToast: vi.fn(),
}));

import { createSplit, addParticipant } from '$lib/api/splits';
import { navigate } from '$lib/router';
import { addToast } from '$lib/stores/toastStore.svelte';

describe('Home', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // --- Task 1: Form renders all fields (AC: 1) ---

  it('renders all form fields: split name, participant name, nights', () => {
    render(Home);

    expect(screen.getByText('FairNSquare')).toBeInTheDocument();
    expect(screen.getByText('Create a New Split')).toBeInTheDocument();
    expect(screen.getByLabelText('Split Name')).toBeInTheDocument();
    expect(screen.getByText('First Participant')).toBeInTheDocument();
    expect(screen.getByLabelText('Name')).toBeInTheDocument();
    expect(screen.getByLabelText('Number of Nights')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Create Split' })).toBeInTheDocument();
  });

  it('defaults nights to 1', () => {
    render(Home);

    const nightsInput = screen.getByLabelText('Number of Nights') as HTMLInputElement;
    expect(nightsInput.value).toBe('1');
  });

  // --- Task 2: Validation (AC: 2, 3, 4, 5) ---

  it('shows validation errors when submitting with empty fields', async () => {
    render(Home);

    const submitButton = screen.getByRole('button', { name: 'Create Split' });
    expect(submitButton).not.toBeDisabled();

    await fireEvent.click(submitButton);

    expect(screen.getByText('Split name is required')).toBeInTheDocument();
    expect(screen.getByText('Participant name is required')).toBeInTheDocument();
  });

  it('enables submit button when all fields are valid', async () => {
    render(Home);

    await fireEvent.input(screen.getByLabelText('Split Name'), {
      target: { value: 'Weekend Trip' },
    });
    await fireEvent.input(screen.getByLabelText('Name'), {
      target: { value: 'Alice' },
    });

    const submitButton = screen.getByRole('button', { name: 'Create Split' });
    expect(submitButton).not.toBeDisabled();
  });

  it('shows validation error for split name exceeding 100 characters', async () => {
    render(Home);

    const longName = 'a'.repeat(101);
    const input = screen.getByLabelText('Split Name');
    await fireEvent.input(input, { target: { value: longName } });

    expect(
      screen.getByText('Split name cannot exceed 100 characters')
    ).toBeInTheDocument();
  });

  it('shows validation error for participant name exceeding 50 characters', async () => {
    render(Home);

    const longName = 'a'.repeat(51);
    const input = screen.getByLabelText('Name');
    await fireEvent.input(input, { target: { value: longName } });

    expect(
      screen.getByText('Participant name cannot exceed 50 characters')
    ).toBeInTheDocument();
  });

  it('shows validation error for nights less than 0.5', async () => {
    render(Home);

    const nightsInput = screen.getByLabelText('Number of Nights');
    await fireEvent.input(nightsInput, { target: { value: 0 } });

    expect(screen.getByText('Nights must be at least 0.5')).toBeInTheDocument();
  });

  it('shows validation error for nights greater than 365', async () => {
    render(Home);

    const nightsInput = screen.getByLabelText('Number of Nights');
    await fireEvent.input(nightsInput, { target: { value: 366 } });

    expect(screen.getByText('Nights cannot exceed 365')).toBeInTheDocument();
  });

  // --- Task 3: Create flow & redirect (AC: 6) ---

  it('creates split, adds participant, and navigates to split page', async () => {
    const mockSplit = {
      id: 'abc123',
      name: 'Weekend Trip',
      createdAt: '2026-02-04T12:00:00Z',
      participants: [],
      expenses: [],
    };
    const mockParticipant = {
      id: 'p1',
      name: 'Alice',
      nights: 3,
    };
    vi.mocked(createSplit).mockResolvedValue(mockSplit);
    vi.mocked(addParticipant).mockResolvedValue(mockParticipant);

    render(Home);

    await fireEvent.input(screen.getByLabelText('Split Name'), {
      target: { value: 'Weekend Trip' },
    });
    await fireEvent.input(screen.getByLabelText('Name'), {
      target: { value: 'Alice' },
    });
    await fireEvent.input(screen.getByLabelText('Number of Nights'), {
      target: { value: 3 },
    });

    const submitButton = screen.getByRole('button', { name: 'Create Split' });
    await fireEvent.click(submitButton);

    await waitFor(() => {
      expect(createSplit).toHaveBeenCalledWith({ name: 'Weekend Trip' });
      expect(addParticipant).toHaveBeenCalledWith('abc123', {
        name: 'Alice',
        nights: 3,
      });
      expect(navigate).toHaveBeenCalledWith('/splits/:splitId', {
        params: { splitId: 'abc123' },
      });
    });
  });

  // --- Task 4: No intermediate share-link screen (AC: 6) ---

  it('does not show share-link screen after creation', async () => {
    const mockSplit = {
      id: 'abc123',
      name: 'Weekend Trip',
      createdAt: '2026-02-04T12:00:00Z',
      participants: [],
      expenses: [],
    };
    vi.mocked(createSplit).mockResolvedValue(mockSplit);
    vi.mocked(addParticipant).mockResolvedValue({
      id: 'p1',
      name: 'Alice',
      nights: 1,
    });

    render(Home);

    await fireEvent.input(screen.getByLabelText('Split Name'), {
      target: { value: 'Weekend Trip' },
    });
    await fireEvent.input(screen.getByLabelText('Name'), {
      target: { value: 'Alice' },
    });

    await fireEvent.click(screen.getByRole('button', { name: 'Create Split' }));

    await waitFor(() => {
      expect(navigate).toHaveBeenCalled();
    });

    // These elements from the old share-link screen must NOT appear
    expect(screen.queryByText('Split Created!')).not.toBeInTheDocument();
    expect(screen.queryByText('Go to Split')).not.toBeInTheDocument();
    expect(screen.queryByText('Create Another Split')).not.toBeInTheDocument();
    expect(screen.queryByText('Copy Link')).not.toBeInTheDocument();
  });

  // --- Task 5: Error handling (AC: 7) ---

  it('shows toast on createSplit API error and preserves form data', async () => {
    vi.mocked(createSplit).mockRejectedValue({
      type: 'error',
      title: 'Error',
      status: 500,
      detail: 'Server error',
    });

    render(Home);

    await fireEvent.input(screen.getByLabelText('Split Name'), {
      target: { value: 'Weekend Trip' },
    });
    await fireEvent.input(screen.getByLabelText('Name'), {
      target: { value: 'Alice' },
    });

    await fireEvent.click(screen.getByRole('button', { name: 'Create Split' }));

    await waitFor(() => {
      expect(addToast).toHaveBeenCalledWith({
        type: 'error',
        message: 'Server error',
      });
    });

    // Form data preserved
    expect((screen.getByLabelText('Split Name') as HTMLInputElement).value).toBe(
      'Weekend Trip'
    );
    expect((screen.getByLabelText('Name') as HTMLInputElement).value).toBe('Alice');
    expect(navigate).not.toHaveBeenCalled();
  });

  it('shows toast on addParticipant API error and preserves form data', async () => {
    const mockSplit = {
      id: 'abc123',
      name: 'Weekend Trip',
      createdAt: '2026-02-04T12:00:00Z',
      participants: [],
      expenses: [],
    };
    vi.mocked(createSplit).mockResolvedValue(mockSplit);
    vi.mocked(addParticipant).mockRejectedValue({
      type: 'error',
      title: 'Error',
      status: 400,
      detail: 'Invalid participant',
    });

    render(Home);

    await fireEvent.input(screen.getByLabelText('Split Name'), {
      target: { value: 'Weekend Trip' },
    });
    await fireEvent.input(screen.getByLabelText('Name'), {
      target: { value: 'Alice' },
    });

    await fireEvent.click(screen.getByRole('button', { name: 'Create Split' }));

    await waitFor(() => {
      expect(addToast).toHaveBeenCalledWith({
        type: 'error',
        message: 'Invalid participant',
      });
    });

    expect(navigate).not.toHaveBeenCalled();
  });

  it('shows default error message when API error has no detail', async () => {
    vi.mocked(createSplit).mockRejectedValue({});

    render(Home);

    await fireEvent.input(screen.getByLabelText('Split Name'), {
      target: { value: 'Weekend Trip' },
    });
    await fireEvent.input(screen.getByLabelText('Name'), {
      target: { value: 'Alice' },
    });

    await fireEvent.click(screen.getByRole('button', { name: 'Create Split' }));

    await waitFor(() => {
      expect(addToast).toHaveBeenCalledWith({
        type: 'error',
        message: 'Failed to create split',
      });
    });
  });
});