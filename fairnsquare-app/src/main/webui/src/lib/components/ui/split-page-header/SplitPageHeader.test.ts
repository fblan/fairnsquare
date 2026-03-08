import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import SplitPageHeader from './SplitPageHeader.svelte';

vi.mock('$lib/router', () => ({
  p: vi.fn((path: string) => path),
  navigate: vi.fn(),
  isActive: vi.fn(),
  route: { params: {}, pathname: '/' },
}));

vi.mock('$lib/stores/toastStore.svelte', () => ({
  addToast: vi.fn(),
}));

import { navigate } from '$lib/router';
import { addToast } from '$lib/stores/toastStore.svelte';

const SPLIT_ID = 'split-abc';
const SPLIT_NAME = 'Weekend in Paris';

describe('SplitPageHeader', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // --- Split name ---

  it('renders the split name', () => {
    render(SplitPageHeader, { props: { splitName: SPLIT_NAME, splitId: SPLIT_ID } });

    expect(screen.getByText(SPLIT_NAME)).toBeInTheDocument();
  });

  // --- Share button ---

  it('renders a Share button', () => {
    render(SplitPageHeader, { props: { splitName: SPLIT_NAME, splitId: SPLIT_ID } });

    expect(screen.getByRole('button', { name: 'Share' })).toBeInTheDocument();
  });

  it('copies URL to clipboard and shows success toast when Share is clicked', async () => {
    const mockWriteText = vi.fn().mockResolvedValue(undefined);
    Object.assign(navigator, { clipboard: { writeText: mockWriteText } });

    render(SplitPageHeader, { props: { splitName: SPLIT_NAME, splitId: SPLIT_ID } });

    await fireEvent.click(screen.getByRole('button', { name: 'Share' }));

    await waitFor(() => {
      expect(mockWriteText).toHaveBeenCalled();
      expect(addToast).toHaveBeenCalledWith({ type: 'success', message: 'Link copied!' });
    });
  });

  it('shows URL in toast when clipboard API fails', async () => {
    const mockWriteText = vi.fn().mockRejectedValue(new Error('Clipboard denied'));
    Object.assign(navigator, { clipboard: { writeText: mockWriteText } });

    render(SplitPageHeader, { props: { splitName: SPLIT_NAME, splitId: SPLIT_ID } });

    await fireEvent.click(screen.getByRole('button', { name: 'Share' }));

    await waitFor(() => {
      expect(addToast).toHaveBeenCalledWith(
        expect.objectContaining({ message: expect.stringContaining('Share link:') }),
      );
    });
  });

  // --- Back button ---

  it('does not render a back button by default', () => {
    render(SplitPageHeader, { props: { splitName: SPLIT_NAME, splitId: SPLIT_ID } });

    expect(screen.queryByRole('button', { name: 'Back to dashboard' })).not.toBeInTheDocument();
  });

  it('renders a back button when showBackButton is true', () => {
    render(SplitPageHeader, { props: { splitName: SPLIT_NAME, splitId: SPLIT_ID, showBackButton: true } });

    expect(screen.getByRole('button', { name: 'Back to dashboard' })).toBeInTheDocument();
  });

  it('navigates to the split dashboard when back button is clicked', async () => {
    render(SplitPageHeader, { props: { splitName: SPLIT_NAME, splitId: SPLIT_ID, showBackButton: true } });

    await fireEvent.click(screen.getByRole('button', { name: 'Back to dashboard' }));

    expect(navigate).toHaveBeenCalledWith(`/splits/${SPLIT_ID}`);
  });
});