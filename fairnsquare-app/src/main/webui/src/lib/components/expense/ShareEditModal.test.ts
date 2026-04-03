/**
 * ShareEditModal Tests
 * Issue #87: Copy nights shares in custom expense
 *
 * AC1: "Copy nights shares" button is rendered
 * AC2: Clicking the button shows the warning banner
 * AC3: Warning banner explains values will be overridden and not synchronized
 * AC4: Cancelling the warning dismisses it without changing values
 * AC5: Applying overwrites all parts with nights × share and checks all participants
 * AC6: Applying closes the warning banner
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/svelte';
import ShareEditModal from './ShareEditModal.svelte';
import type { Participant } from '$lib/api/splits';

const mockParticipants: Participant[] = [
  { id: 'p1', name: 'Alice', nights: 3, share: 1 },
  { id: 'p2', name: 'Bob', nights: 2, share: 2 },
  { id: 'p3', name: 'Charlie', nights: 4, share: 1 },
];

const defaultInitialData = {
  shareParts: { p1: 5, p2: 5, p3: 5 },
  shareChecked: { p1: true, p2: true, p3: true },
  sharePreviousValues: { p1: 5, p2: 5, p3: 5 },
};

const defaultProps = {
  open: true,
  participants: mockParticipants,
  initialData: defaultInitialData,
  onConfirm: vi.fn(),
  onCancel: vi.fn(),
};

beforeEach(() => {
  vi.clearAllMocks();
});

describe('ShareEditModal – Copy nights shares (AC1)', () => {
  it('renders the "Copy nights shares" button', () => {
    render(ShareEditModal, { props: defaultProps });
    expect(screen.getByRole('button', { name: /copy nights shares/i })).toBeInTheDocument();
  });
});

describe('ShareEditModal – Copy nights shares (AC2)', () => {
  it('clicking the button shows the warning banner', async () => {
    render(ShareEditModal, { props: defaultProps });

    fireEvent.click(screen.getByRole('button', { name: /copy nights shares/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });
  });

  it('warning banner contains Apply and Cancel actions', async () => {
    render(ShareEditModal, { props: defaultProps });

    fireEvent.click(screen.getByRole('button', { name: /copy nights shares/i }));

    await waitFor(() => {
      const alert = screen.getByRole('alert');
      expect(within(alert).getByRole('button', { name: /^apply$/i })).toBeInTheDocument();
      expect(within(alert).getByRole('button', { name: /^cancel$/i })).toBeInTheDocument();
    });
  });
});

describe('ShareEditModal – Copy nights shares (AC3)', () => {
  it('warning banner mentions override and static snapshot', async () => {
    render(ShareEditModal, { props: defaultProps });

    fireEvent.click(screen.getByRole('button', { name: /copy nights shares/i }));

    await waitFor(() => {
      const alert = screen.getByRole('alert');
      expect(alert.textContent).toMatch(/overwrite/i);
      expect(alert.textContent).toMatch(/static snapshot/i);
    });
  });
});

describe('ShareEditModal – Copy nights shares (AC4)', () => {
  it('cancelling the warning dismisses it without changing input values', async () => {
    render(ShareEditModal, { props: defaultProps });

    fireEvent.click(screen.getByRole('button', { name: /copy nights shares/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });

    const alert = screen.getByRole('alert');
    fireEvent.click(within(alert).getByRole('button', { name: /^cancel$/i }));

    await waitFor(() => {
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    // Values unchanged: Alice still 5
    const aliceInput = screen.getByRole('spinbutton', { name: /alice parts/i });
    expect(aliceInput).toHaveValue(5);
  });
});

describe('ShareEditModal – Copy nights shares (AC5 & AC6)', () => {
  it('applying overwrites each participant parts with nights × share', async () => {
    render(ShareEditModal, { props: defaultProps });

    fireEvent.click(screen.getByRole('button', { name: /copy nights shares/i }));

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /^apply$/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /^apply$/i }));

    await waitFor(() => {
      // Alice: 3 × 1 = 3
      expect(screen.getByRole('spinbutton', { name: /alice parts/i })).toHaveValue(3);
      // Bob: 2 × 2 = 4
      expect(screen.getByRole('spinbutton', { name: /bob parts/i })).toHaveValue(4);
      // Charlie: 4 × 1 = 4
      expect(screen.getByRole('spinbutton', { name: /charlie parts/i })).toHaveValue(4);
    });
  });

  it('applying closes the warning banner', async () => {
    render(ShareEditModal, { props: defaultProps });

    fireEvent.click(screen.getByRole('button', { name: /copy nights shares/i }));

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /^apply$/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /^apply$/i }));

    await waitFor(() => {
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });
  });

  it('applying enables (checks) all participants', async () => {
    const propsWithUnchecked = {
      ...defaultProps,
      initialData: {
        shareParts: { p1: 5, p2: 0, p3: 5 },
        shareChecked: { p1: true, p2: false, p3: true },
        sharePreviousValues: { p1: 5, p2: 0, p3: 5 },
      },
    };

    render(ShareEditModal, { props: propsWithUnchecked });

    fireEvent.click(screen.getByRole('button', { name: /copy nights shares/i }));

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /^apply$/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /^apply$/i }));

    await waitFor(() => {
      // Bob's input should now be enabled (checked)
      const bobInput = screen.getByRole('spinbutton', { name: /bob parts/i });
      expect(bobInput).not.toBeDisabled();
    });
  });
});
