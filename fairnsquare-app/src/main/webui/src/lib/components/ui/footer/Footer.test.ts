import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/svelte';
import Footer from './Footer.svelte';

vi.mock('$lib/api/info', () => ({
  getAppInfo: vi.fn(),
}));

import { getAppInfo } from '$lib/api/info';

describe('Footer', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders version and shortened commit after successful fetch', async () => {
    vi.mocked(getAppInfo).mockResolvedValue({ version: '1.0.0-SNAPSHOT', commit: 'a3f8c2d1e9b4ff00' });

    render(Footer);

    await waitFor(() => {
      expect(screen.getByRole('contentinfo')).toBeInTheDocument();
      expect(screen.getByText('a3f8c2d')).toBeInTheDocument();
    });
  });

  it('renders nothing when API call fails', async () => {
    vi.mocked(getAppInfo).mockRejectedValue(new Error('Network error'));

    render(Footer);

    await waitFor(() => {
      expect(screen.queryByRole('contentinfo')).not.toBeInTheDocument();
    });
  });

  it('renders nothing while loading', () => {
    vi.mocked(getAppInfo).mockReturnValue(new Promise(() => {})); // never resolves

    render(Footer);

    expect(screen.queryByRole('contentinfo')).not.toBeInTheDocument();
  });

  it('truncates long commit hash to 7 characters', async () => {
    vi.mocked(getAppInfo).mockResolvedValue({ version: '2.0.0', commit: '0123456789abcdef' });

    render(Footer);

    await waitFor(() => {
      expect(screen.getByText('0123456')).toBeInTheDocument();
    });
  });
});