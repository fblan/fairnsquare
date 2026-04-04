/**
 * Admin page tests
 * Issue #107: Admin view
 *
 * AC1: Shows password form when not authenticated
 * AC2: Submits password in X-Admin-Token header via API
 * AC3: Shows stats after successful login
 * AC4: Shows error on 401
 * AC5: Shows error on 503 (admin not configured)
 * AC6: Sign out clears state and returns to form
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import Admin from './Admin.svelte';

vi.mock('$lib/api/admin', () => ({
  getAdminStats: vi.fn(),
  ADMIN_TOKEN_HEADER: 'X-Admin-Token',
}));

import { getAdminStats } from '$lib/api/admin';

const mockStats = {
  totalSplits: 3,
  lastUpdated: '2026-04-04T10:00:00Z',
  splits: [
    {
      idHash: 'aabbccdd11223344',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-04-01T00:00:00Z',
      participantCount: 2,
      expenseCount: 3,
      expenseTypes: ['BY_NIGHT', 'EQUAL'],
    },
    {
      idHash: 'bbccddee22334455',
      createdAt: '2026-02-01T00:00:00Z',
      updatedAt: '2026-04-02T00:00:00Z',
      participantCount: 4,
      expenseCount: 1,
      expenseTypes: ['FREE'],
    },
    {
      idHash: 'ccddeeff33445566',
      createdAt: '2026-03-01T00:00:00Z',
      updatedAt: '2026-03-01T00:00:00Z',
      participantCount: 1,
      expenseCount: 0,
      expenseTypes: [],
    },
  ],
};

describe('Admin', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
  });

  // --- AC1: Password form ---

  describe('Password form (AC1)', () => {
    it('shows password form when not authenticated', () => {
      render(Admin);

      expect(screen.getByLabelText('Password')).toBeInTheDocument();
      expect(screen.queryByText('Total splits')).not.toBeInTheDocument();
    });

    it('submit button is disabled when password is empty', () => {
      render(Admin);

      const button = screen.getAllByRole('button', { name: 'Sign in' })[0];
      expect(button).toBeDisabled();
    });

    it('submit button is enabled when password is typed', async () => {
      render(Admin);

      await fireEvent.input(screen.getByLabelText('Password'), { target: { value: 'secret' } });

      const button = screen.getAllByRole('button', { name: 'Sign in' })[0];
      expect(button).not.toBeDisabled();
    });
  });

  // --- AC2: API call ---

  describe('API call (AC2)', () => {
    it('calls getAdminStats with typed password on submit', async () => {
      vi.mocked(getAdminStats).mockResolvedValue(mockStats);

      render(Admin);

      await fireEvent.input(screen.getByLabelText('Password'), { target: { value: 'mypassword' } });
      await fireEvent.click(screen.getAllByRole('button', { name: 'Sign in' })[0]);

      await waitFor(() => {
        expect(getAdminStats).toHaveBeenCalledWith('mypassword');
      });
    });
  });

  // --- AC3: Stats display ---

  describe('Stats display (AC3)', () => {
    it('shows total splits count after login', async () => {
      vi.mocked(getAdminStats).mockResolvedValue(mockStats);

      render(Admin);

      await fireEvent.input(screen.getByLabelText('Password'), { target: { value: 'secret' } });
      await fireEvent.click(screen.getAllByRole('button', { name: 'Sign in' })[0]);

      await waitFor(() => {
        expect(screen.getByText('Total splits')).toBeInTheDocument();
      });
    });

    it('shows id hashes of splits', async () => {
      vi.mocked(getAdminStats).mockResolvedValue(mockStats);

      render(Admin);

      await fireEvent.input(screen.getByLabelText('Password'), { target: { value: 'secret' } });
      await fireEvent.click(screen.getAllByRole('button', { name: 'Sign in' })[0]);

      await waitFor(() => {
        expect(screen.getAllByText('aabbccdd11223344').length).toBeGreaterThan(0);
      });
    });

    it('shows oldest updated and most recently updated sections', async () => {
      vi.mocked(getAdminStats).mockResolvedValue(mockStats);

      render(Admin);

      await fireEvent.input(screen.getByLabelText('Password'), { target: { value: 'secret' } });
      await fireEvent.click(screen.getAllByRole('button', { name: 'Sign in' })[0]);

      await waitFor(() => {
        expect(screen.getByText('Oldest updated splits')).toBeInTheDocument();
        expect(screen.getByText('Most recently updated splits')).toBeInTheDocument();
      });
    });

    it('saves token to sessionStorage on success', async () => {
      vi.mocked(getAdminStats).mockResolvedValue(mockStats);

      render(Admin);

      await fireEvent.input(screen.getByLabelText('Password'), { target: { value: 'secret' } });
      await fireEvent.click(screen.getAllByRole('button', { name: 'Sign in' })[0]);

      await waitFor(() => expect(screen.getByText('Total splits')).toBeInTheDocument());

      expect(sessionStorage.getItem('admin_token')).toBe('secret');
    });

    it('auto-loads stats on mount when session token exists', async () => {
      sessionStorage.setItem('admin_token', 'stored-token');
      vi.mocked(getAdminStats).mockResolvedValue(mockStats);

      render(Admin);

      await waitFor(() => {
        expect(getAdminStats).toHaveBeenCalledWith('stored-token');
        expect(screen.getByText('Total splits')).toBeInTheDocument();
      });
    });
  });

  // --- AC4: 401 error ---

  describe('401 error (AC4)', () => {
    it('shows invalid password message on 401', async () => {
      vi.mocked(getAdminStats).mockRejectedValue({ status: 401, detail: 'Unauthorized' });

      render(Admin);

      await fireEvent.input(screen.getByLabelText('Password'), { target: { value: 'wrong' } });
      await fireEvent.click(screen.getAllByRole('button', { name: 'Sign in' })[0]);

      await waitFor(() => {
        expect(screen.getByText('Invalid password.')).toBeInTheDocument();
      });
      expect(screen.queryByText('Total splits')).not.toBeInTheDocument();
    });

    it('clears sessionStorage on 401', async () => {
      sessionStorage.setItem('admin_token', 'old-token');
      vi.mocked(getAdminStats).mockRejectedValue({ status: 401, detail: 'Unauthorized' });

      render(Admin);

      await waitFor(() => {
        expect(screen.getByText('Invalid password.')).toBeInTheDocument();
      });
      expect(sessionStorage.getItem('admin_token')).toBeNull();
    });
  });

  // --- AC5: 503 error ---

  describe('503 error (AC5)', () => {
    it('shows not configured message on 503', async () => {
      vi.mocked(getAdminStats).mockRejectedValue({ status: 503, detail: 'Admin access is not configured.' });

      render(Admin);

      await fireEvent.input(screen.getByLabelText('Password'), { target: { value: 'anything' } });
      await fireEvent.click(screen.getAllByRole('button', { name: 'Sign in' })[0]);

      await waitFor(() => {
        expect(screen.getByText('Admin access is not configured on this server.')).toBeInTheDocument();
      });
    });
  });

  // --- AC6: Sign out ---

  describe('Sign out (AC6)', () => {
    it('returns to password form after sign out', async () => {
      vi.mocked(getAdminStats).mockResolvedValue(mockStats);

      render(Admin);

      await fireEvent.input(screen.getByLabelText('Password'), { target: { value: 'secret' } });
      await fireEvent.click(screen.getAllByRole('button', { name: 'Sign in' })[0]);
      await waitFor(() => expect(screen.getByText('Total splits')).toBeInTheDocument());

      await fireEvent.click(screen.getByRole('button', { name: 'Sign out' }));

      expect(screen.getByLabelText('Password')).toBeInTheDocument();
      expect(screen.queryByText('Total splits')).not.toBeInTheDocument();
      expect(sessionStorage.getItem('admin_token')).toBeNull();
    });
  });

});
