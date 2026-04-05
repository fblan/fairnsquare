/**
 * SplitPageHeader tests
 * Issue #93: Add navigation menu to switch between split views
 *
 * AC1: All 4 navigation tabs are rendered
 * AC2: Clicking a tab navigates to the correct route
 * AC3: Active tab reflects the current pathname
 * AC4: Share button works
 * AC5: Split name is rendered
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import SplitPageHeader from './SplitPageHeader.svelte';

const mockRoute = vi.hoisted(() => ({ params: {}, pathname: '/splits/split-abc' }));

vi.mock('$lib/router', () => ({
  p: vi.fn((path: string) => path),
  navigate: vi.fn(),
  isActive: vi.fn(),
  route: mockRoute,
}));

vi.mock('$lib/stores/toastStore.svelte', () => ({
  addToast: vi.fn(),
}));

import { navigate } from '$lib/router';
import { addToast } from '$lib/stores/toastStore.svelte';

const SPLIT_ID = 'split-abc';
const SPLIT_NAME = 'Weekend in Paris';

const defaultProps = { splitName: SPLIT_NAME, splitId: SPLIT_ID };

describe('SplitPageHeader', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockRoute.pathname = `/splits/${SPLIT_ID}`;
  });

  // --- AC5: Split name ---

  it('renders the split name', () => {
    render(SplitPageHeader, { props: defaultProps });
    expect(screen.getByText(SPLIT_NAME)).toBeInTheDocument();
  });

  // --- AC1: Navigation tabs rendered ---

  describe('Navigation tabs (AC1)', () => {
    it('renders all 4 navigation tabs', () => {
      render(SplitPageHeader, { props: defaultProps });

      expect(screen.getByRole('button', { name: /home/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /participants/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /expenses/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /settlement/i })).toBeInTheDocument();
    });

    it('renders a close button', () => {
      render(SplitPageHeader, { props: defaultProps });
      expect(screen.getByRole('button', { name: 'Close split' })).toBeInTheDocument();
    });

    it('renders a nav landmark with aria-label', () => {
      render(SplitPageHeader, { props: defaultProps });
      expect(screen.getByRole('navigation', { name: 'Split navigation' })).toBeInTheDocument();
    });
  });

  // --- AC2: Tab navigation ---

  describe('Tab navigation (AC2)', () => {
    it('navigates to the dashboard when Home tab is clicked', async () => {
      render(SplitPageHeader, { props: defaultProps });

      await fireEvent.click(screen.getByRole('button', { name: /^home$/i }));

      expect(navigate).toHaveBeenCalledWith(`/splits/${SPLIT_ID}`);
    });

    it('navigates to home page when Close split button is clicked', async () => {
      render(SplitPageHeader, { props: defaultProps });

      await fireEvent.click(screen.getByRole('button', { name: 'Close split' }));

      expect(navigate).toHaveBeenCalledWith('/');
    });

    it('navigates to participants when Participants tab is clicked', async () => {
      render(SplitPageHeader, { props: defaultProps });

      await fireEvent.click(screen.getByRole('button', { name: /participants/i }));

      expect(navigate).toHaveBeenCalledWith(`/splits/${SPLIT_ID}/participants`);
    });

    it('navigates to expenses when Expenses tab is clicked', async () => {
      render(SplitPageHeader, { props: defaultProps });

      await fireEvent.click(screen.getByRole('button', { name: /expenses/i }));

      expect(navigate).toHaveBeenCalledWith(`/splits/${SPLIT_ID}/expenses`);
    });

    it('navigates to settlement when Settlement tab is clicked', async () => {
      render(SplitPageHeader, { props: defaultProps });

      await fireEvent.click(screen.getByRole('button', { name: /settlement/i }));

      expect(navigate).toHaveBeenCalledWith(`/splits/${SPLIT_ID}/settlement`);
    });
  });

  // --- AC3: Active tab ---

  describe('Active tab (AC3)', () => {
    it('marks Home tab as active on the split dashboard route', () => {
      mockRoute.pathname = `/splits/${SPLIT_ID}`;
      render(SplitPageHeader, { props: defaultProps });

      expect(screen.getByRole('button', { name: /^home$/i })).toHaveAttribute('aria-current', 'page');
      expect(screen.getByRole('button', { name: /participants/i })).not.toHaveAttribute('aria-current');
    });

    it('marks Participants tab as active on the participants route', () => {
      mockRoute.pathname = `/splits/${SPLIT_ID}/participants`;
      render(SplitPageHeader, { props: defaultProps });

      expect(screen.getByRole('button', { name: /participants/i })).toHaveAttribute('aria-current', 'page');
      expect(screen.getByRole('button', { name: /^home$/i })).not.toHaveAttribute('aria-current');
    });

    it('marks Expenses tab as active on the expenses route', () => {
      mockRoute.pathname = `/splits/${SPLIT_ID}/expenses`;
      render(SplitPageHeader, { props: defaultProps });

      expect(screen.getByRole('button', { name: /expenses/i })).toHaveAttribute('aria-current', 'page');
    });

    it('marks Settlement tab as active on the settlement route', () => {
      mockRoute.pathname = `/splits/${SPLIT_ID}/settlement`;
      render(SplitPageHeader, { props: defaultProps });

      expect(screen.getByRole('button', { name: /settlement/i })).toHaveAttribute('aria-current', 'page');
    });
  });

  // --- AC4: Share button ---

  describe('Share button (AC4)', () => {
    it('renders a Share button', () => {
      render(SplitPageHeader, { props: defaultProps });
      expect(screen.getByRole('button', { name: 'Share link' })).toBeInTheDocument();
    });

    it('copies URL to clipboard and shows success toast when Share is clicked', async () => {
      const mockWriteText = vi.fn().mockResolvedValue(undefined);
      Object.assign(navigator, { clipboard: { writeText: mockWriteText } });

      render(SplitPageHeader, { props: defaultProps });

      await fireEvent.click(screen.getByRole('button', { name: 'Share link' }));

      await waitFor(() => {
        expect(mockWriteText).toHaveBeenCalled();
        expect(addToast).toHaveBeenCalledWith({ type: 'success', message: 'Link copied!' });
      });
    });

    it('shows URL in toast when clipboard API fails', async () => {
      const mockWriteText = vi.fn().mockRejectedValue(new Error('Clipboard denied'));
      Object.assign(navigator, { clipboard: { writeText: mockWriteText } });

      render(SplitPageHeader, { props: defaultProps });

      await fireEvent.click(screen.getByRole('button', { name: 'Share link' }));

      await waitFor(() => {
        expect(addToast).toHaveBeenCalledWith(
          expect.objectContaining({ message: expect.stringContaining('Share link:') }),
        );
      });
    });
  });

  // --- Navigation Guard ---

  describe('Navigation Guard', () => {
    it('navigates immediately when no guard is provided', async () => {
      render(SplitPageHeader, { props: defaultProps });
      await fireEvent.click(screen.getByRole('button', { name: /^home$/i }));
      await waitFor(() => {
        expect(navigate).toHaveBeenCalledWith(`/splits/${SPLIT_ID}`);
      });
    });

    it('shows info modal and blocks navigation when guard returns a warning string', async () => {
      const navigateGuard = vi.fn(() => 'warning');
      render(SplitPageHeader, { props: { ...defaultProps, navigateGuard } });
      await fireEvent.click(screen.getByRole('button', { name: /^home$/i }));
      await waitFor(() => {
        expect(screen.getByText('A participant is required')).toBeInTheDocument();
      });
      expect(navigate).not.toHaveBeenCalled();
    });

    it('dismisses the info modal when Got it is clicked without navigating', async () => {
      const navigateGuard = vi.fn(() => 'warning');
      render(SplitPageHeader, { props: { ...defaultProps, navigateGuard } });
      await fireEvent.click(screen.getByRole('button', { name: /^home$/i }));
      await waitFor(() => {
        expect(screen.getByText('A participant is required')).toBeInTheDocument();
      });
      await fireEvent.click(screen.getByRole('button', { name: 'Got it' }));
      await waitFor(() => {
        expect(screen.queryByText('A participant is required')).not.toBeInTheDocument();
      });
      expect(navigate).not.toHaveBeenCalled();
    });

    it('navigates immediately when guard returns null', async () => {
      const navigateGuard = vi.fn(() => null);
      render(SplitPageHeader, { props: { ...defaultProps, navigateGuard } });
      await fireEvent.click(screen.getByRole('button', { name: /^home$/i }));
      await waitFor(() => {
        expect(navigate).toHaveBeenCalledWith(`/splits/${SPLIT_ID}`);
      });
      expect(screen.queryByText('A participant is required')).not.toBeInTheDocument();
    });
  });
});
