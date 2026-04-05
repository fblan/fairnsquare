/**
 * Home page tests
 * Issue #94: Rework welcome page
 *
 * AC1: Only split name field is shown (no participant form)
 * AC2: Split name validation
 * AC3: Create split calls only createSplit then navigates to participants page
 * AC4: Recent splits list — verified splits shown, stale ones removed
 * AC5: Individual dismiss per split
 * AC6: Error handling
 * AC7: CAPTCHA integration
 */

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
  getSplit: vi.fn(),
}));

// Mock the toast store
vi.mock('$lib/stores/toastStore.svelte', () => ({
  addToast: vi.fn(),
}));

// Mock the lastSplitStore
vi.mock('$lib/stores/lastSplitStore', () => ({
  loadLastSplits: vi.fn(),
  removeLastSplit: vi.fn(),
  saveLastSplit: vi.fn(),
}));

// Mock the captcha store — default: token is valid so modal is suppressed
vi.mock('$lib/stores/captchaStore', () => ({
  hasValidToken: vi.fn(() => true),
  loadToken: vi.fn(() => 'mock-captcha-token'),
  clearToken: vi.fn(),
  saveToken: vi.fn(),
  CAPTCHA_TOKEN_HEADER: 'X-Captcha-Token',
}));

// Mock the captcha API (used by CaptchaModal, kept inert in most tests)
vi.mock('$lib/api/captcha', () => ({
  createChallenge: vi.fn(),
  verifyChallenge: vi.fn(),
}));

import { createSplit, getSplit } from '$lib/api/splits';
import { navigate } from '$lib/router';
import { addToast } from '$lib/stores/toastStore.svelte';
import { loadLastSplits, removeLastSplit, saveLastSplit } from '$lib/stores/lastSplitStore';
import { hasValidToken, loadToken, clearToken } from '$lib/stores/captchaStore';

const mockSplit = {
  id: 'abc123',
  name: 'Weekend Trip',
  createdAt: '2026-02-04T12:00:00Z',
  participants: [],
  expenses: [],
  settlement: null,
};

describe('Home', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(loadLastSplits).mockReturnValue([]);
  });

  // --- AC1: Form renders only split name field ---

  describe('Form rendering (AC1)', () => {
    it('renders the split name field and Create Split button', () => {
      render(Home);

      expect(screen.getByText('FairNSquare')).toBeInTheDocument();
      expect(screen.getByText('Create a New Split')).toBeInTheDocument();
      expect(screen.getByLabelText('Split Name')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Create Split' })).toBeInTheDocument();
    });

    it('does not render participant form fields', () => {
      render(Home);

      expect(screen.queryByText('First Participant')).not.toBeInTheDocument();
      expect(screen.queryByLabelText('Name')).not.toBeInTheDocument();
      expect(screen.queryByLabelText('Nights')).not.toBeInTheDocument();
      expect(screen.queryByLabelText('Share')).not.toBeInTheDocument();
    });
  });

  // --- AC2: Validation ---

  describe('Validation (AC2)', () => {
    it('shows split name required error when submitting empty form', async () => {
      render(Home);

      await fireEvent.click(screen.getByRole('button', { name: 'Create Split' }));

      expect(screen.getByText('Split name is required')).toBeInTheDocument();
    });

    it('shows error when split name exceeds 100 characters', async () => {
      render(Home);

      await fireEvent.input(screen.getByLabelText('Split Name'), {
        target: { value: 'a'.repeat(101) },
      });

      expect(screen.getByText('Split name cannot exceed 100 characters')).toBeInTheDocument();
    });

    it('does not call createSplit when form is invalid', async () => {
      render(Home);

      await fireEvent.click(screen.getByRole('button', { name: 'Create Split' }));

      expect(createSplit).not.toHaveBeenCalled();
    });
  });

  // --- AC3: Create flow ---

  describe('Create flow (AC3)', () => {
    it('calls createSplit with split name and navigates to participants page', async () => {
      vi.mocked(createSplit).mockResolvedValue(mockSplit);

      render(Home);

      await fireEvent.input(screen.getByLabelText('Split Name'), {
        target: { value: 'Weekend Trip' },
      });
      await fireEvent.click(screen.getByRole('button', { name: 'Create Split' }));

      await waitFor(() => {
        expect(createSplit).toHaveBeenCalledWith(
          { name: 'Weekend Trip' },
          { 'X-Captcha-Token': 'mock-captcha-token' }
        );
        expect(navigate).toHaveBeenCalledWith('/splits/:splitId/participants', {
          params: { splitId: 'abc123' },
        });
      });
    });

    it('saves the new split to lastSplitStore after creation', async () => {
      vi.mocked(createSplit).mockResolvedValue(mockSplit);

      render(Home);

      await fireEvent.input(screen.getByLabelText('Split Name'), {
        target: { value: 'Weekend Trip' },
      });
      await fireEvent.click(screen.getByRole('button', { name: 'Create Split' }));

      await waitFor(() => {
        expect(saveLastSplit).toHaveBeenCalledWith({ id: 'abc123', name: 'Weekend Trip' });
      });
    });

    it('does not call addParticipant during split creation', async () => {
      vi.mocked(createSplit).mockResolvedValue(mockSplit);

      render(Home);

      await fireEvent.input(screen.getByLabelText('Split Name'), {
        target: { value: 'Weekend Trip' },
      });
      await fireEvent.click(screen.getByRole('button', { name: 'Create Split' }));

      await waitFor(() => expect(navigate).toHaveBeenCalled());

      // addParticipant is not in the mock — createSplit is the only API call
      expect(createSplit).toHaveBeenCalledTimes(1);
    });
  });

  // --- AC4: Recent splits list ---

  describe('Recent splits list (AC4)', () => {
    it('does not show recent splits section when list is empty', async () => {
      vi.mocked(loadLastSplits).mockReturnValue([]);

      render(Home);

      await waitFor(() => {
        expect(screen.queryByText('Recent splits')).not.toBeInTheDocument();
      });
    });

    it('shows verified splits from the store', async () => {
      vi.mocked(loadLastSplits).mockReturnValue([
        { id: 's1', name: 'Beach House' },
        { id: 's2', name: 'Road Trip' },
      ]);
      vi.mocked(getSplit)
        .mockResolvedValueOnce({ ...mockSplit, id: 's1', name: 'Beach House' })
        .mockResolvedValueOnce({ ...mockSplit, id: 's2', name: 'Road Trip' });

      render(Home);

      await waitFor(() => {
        expect(screen.getByText('Recent splits')).toBeInTheDocument();
        expect(screen.getByText('Beach House')).toBeInTheDocument();
        expect(screen.getByText('Road Trip')).toBeInTheDocument();
      });
    });

    it('removes stale splits (not found on server) from storage and list', async () => {
      vi.mocked(loadLastSplits).mockReturnValue([
        { id: 'gone', name: 'Old Split' },
        { id: 's2', name: 'Active Split' },
      ]);
      vi.mocked(getSplit)
        .mockRejectedValueOnce({ status: 404 })
        .mockResolvedValueOnce({ ...mockSplit, id: 's2', name: 'Active Split' });

      render(Home);

      await waitFor(() => {
        expect(removeLastSplit).toHaveBeenCalledWith('gone');
        expect(screen.queryByText('Old Split')).not.toBeInTheDocument();
        expect(screen.getByText('Active Split')).toBeInTheDocument();
      });
    });

    it('navigates to split when Resume is clicked', async () => {
      vi.mocked(loadLastSplits).mockReturnValue([{ id: 's1', name: 'Beach House' }]);
      vi.mocked(getSplit).mockResolvedValue({ ...mockSplit, id: 's1', name: 'Beach House' });

      render(Home);

      await waitFor(() => expect(screen.getByText('Beach House')).toBeInTheDocument());

      await fireEvent.click(screen.getByRole('button', { name: 'Resume' }));

      expect(navigate).toHaveBeenCalledWith('/splits/:splitId', { params: { splitId: 's1' } });
    });
  });

  // --- AC5: Individual dismiss ---

  describe('Individual dismiss (AC5)', () => {
    it('removes a dismissed split from the list and calls removeLastSplit', async () => {
      vi.mocked(loadLastSplits).mockReturnValue([
        { id: 's1', name: 'Beach House' },
        { id: 's2', name: 'Road Trip' },
      ]);
      vi.mocked(getSplit)
        .mockResolvedValueOnce({ ...mockSplit, id: 's1', name: 'Beach House' })
        .mockResolvedValueOnce({ ...mockSplit, id: 's2', name: 'Road Trip' });

      render(Home);

      await waitFor(() => expect(screen.getAllByText('Dismiss')).toHaveLength(2));

      await fireEvent.click(screen.getAllByText('Dismiss')[0]);

      await waitFor(() => {
        expect(removeLastSplit).toHaveBeenCalledWith('s1');
        expect(screen.queryByText('Beach House')).not.toBeInTheDocument();
        expect(screen.getByText('Road Trip')).toBeInTheDocument();
      });
    });
  });

  // --- AC6: Error handling ---

  describe('Error handling (AC6)', () => {
    it('shows toast on createSplit API error and preserves form data', async () => {
      vi.mocked(createSplit).mockRejectedValue({ detail: 'Server error' });

      render(Home);

      await fireEvent.input(screen.getByLabelText('Split Name'), {
        target: { value: 'Weekend Trip' },
      });
      await fireEvent.click(screen.getByRole('button', { name: 'Create Split' }));

      await waitFor(() => {
        expect(addToast).toHaveBeenCalledWith({ type: 'error', message: 'Server error' });
      });

      expect((screen.getByLabelText('Split Name') as HTMLInputElement).value).toBe('Weekend Trip');
      expect(navigate).not.toHaveBeenCalled();
    });

    it('shows default error message when API error has no detail', async () => {
      vi.mocked(createSplit).mockRejectedValue({});

      render(Home);

      await fireEvent.input(screen.getByLabelText('Split Name'), {
        target: { value: 'Weekend Trip' },
      });
      await fireEvent.click(screen.getByRole('button', { name: 'Create Split' }));

      await waitFor(() => {
        expect(addToast).toHaveBeenCalledWith({ type: 'error', message: 'Failed to create split' });
      });
    });
  });

  // --- AC7: CAPTCHA integration ---

  describe('CAPTCHA integration (AC7)', () => {
    it('shows captcha modal when no valid token and form is valid', async () => {
      vi.mocked(hasValidToken).mockReturnValue(false);

      render(Home);

      await fireEvent.input(screen.getByLabelText('Split Name'), { target: { value: 'Trip' } });
      await fireEvent.click(screen.getByRole('button', { name: 'Create Split' }));

      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument();
        expect(screen.getByText('Human Verification')).toBeInTheDocument();
      });
      expect(createSplit).not.toHaveBeenCalled();
    });

    it('does not show captcha modal when valid token exists', async () => {
      vi.mocked(hasValidToken).mockReturnValue(true);
      vi.mocked(createSplit).mockResolvedValue(mockSplit);

      render(Home);

      await fireEvent.input(screen.getByLabelText('Split Name'), { target: { value: 'Trip' } });
      await fireEvent.click(screen.getByRole('button', { name: 'Create Split' }));

      await waitFor(() => expect(createSplit).toHaveBeenCalled());
      expect(screen.queryByText('Human Verification')).not.toBeInTheDocument();
    });

    it('clears token and shows captcha modal when createSplit returns 401', async () => {
      vi.mocked(createSplit).mockRejectedValue({ status: 401 });

      render(Home);

      await fireEvent.input(screen.getByLabelText('Split Name'), { target: { value: 'Trip' } });
      await fireEvent.click(screen.getByRole('button', { name: 'Create Split' }));

      await waitFor(() => {
        expect(clearToken).toHaveBeenCalled();
        expect(screen.getByRole('dialog')).toBeInTheDocument();
      });
      expect(addToast).not.toHaveBeenCalled();
    });

    it('does not show captcha modal on page load', () => {
      vi.mocked(hasValidToken).mockReturnValue(false);
      render(Home);
      expect(screen.queryByText('Human Verification')).not.toBeInTheDocument();
    });
  });
});
