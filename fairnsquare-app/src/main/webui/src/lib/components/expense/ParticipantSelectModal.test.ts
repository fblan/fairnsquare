/**
 * ParticipantSelectModal Tests
 * Issue #87: Allow editing participants for BY_NIGHT expenses (BY_NIGHT_CUSTOM)
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import ParticipantSelectModal from './ParticipantSelectModal.svelte';
import type { Participant } from '$lib/api/splits';

const mockParticipants: Participant[] = [
  { id: 'p1', name: 'Alice', nights: 4 },
  { id: 'p2', name: 'Bob', nights: 2 },
  { id: 'p3', name: 'Charlie', nights: 3 },
];

describe('ParticipantSelectModal', () => {
  const onConfirm = vi.fn();
  const onCancel = vi.fn();

  const defaultProps = {
    open: true,
    participants: mockParticipants,
    initialSelectedIds: ['p1', 'p2', 'p3'],
    onConfirm,
    onCancel,
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the modal when open', () => {
    render(ParticipantSelectModal, { props: defaultProps });
    expect(screen.getByRole('dialog')).toBeTruthy();
    expect(screen.getByText('Edit Participants')).toBeTruthy();
  });

  it('does not render when closed', () => {
    render(ParticipantSelectModal, { props: { ...defaultProps, open: false } });
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('renders all participants with checkboxes', () => {
    render(ParticipantSelectModal, { props: defaultProps });
    expect(screen.getByLabelText('Include Alice')).toBeTruthy();
    expect(screen.getByLabelText('Include Bob')).toBeTruthy();
    expect(screen.getByLabelText('Include Charlie')).toBeTruthy();
  });

  it('shows nights for each participant', () => {
    render(ParticipantSelectModal, { props: defaultProps });
    expect(screen.getByText(/4 nights/)).toBeTruthy();
    expect(screen.getByText(/2 nights/)).toBeTruthy();
    expect(screen.getByText(/3 nights/)).toBeTruthy();
  });

  it('pre-checks participants in initialSelectedIds', async () => {
    render(ParticipantSelectModal, {
      props: { ...defaultProps, initialSelectedIds: ['p1', 'p3'] },
    });
    await waitFor(() => {
      const aliceCheckbox = screen.getByLabelText('Include Alice') as HTMLInputElement;
      const bobCheckbox = screen.getByLabelText('Include Bob') as HTMLInputElement;
      const charlieCheckbox = screen.getByLabelText('Include Charlie') as HTMLInputElement;
      expect(aliceCheckbox.checked).toBe(true);
      expect(bobCheckbox.checked).toBe(false);
      expect(charlieCheckbox.checked).toBe(true);
    });
  });

  it('enables confirm button when at least one participant is selected', async () => {
    render(ParticipantSelectModal, { props: defaultProps });
    const confirmButton = screen.getByRole('button', { name: /Confirm/i });
    expect(confirmButton).not.toBeDisabled();
  });

  it('disables confirm button when no participants are selected', async () => {
    render(ParticipantSelectModal, {
      props: { ...defaultProps, initialSelectedIds: [] },
    });
    await waitFor(() => {
      const confirmButton = screen.getByRole('button', { name: /Confirm/i });
      expect(confirmButton).toBeDisabled();
    });
  });

  it('shows error message when no participants are selected', async () => {
    render(ParticipantSelectModal, {
      props: { ...defaultProps, initialSelectedIds: [] },
    });
    await waitFor(() => {
      expect(screen.getByText('At least one participant must be selected')).toBeTruthy();
    });
  });

  it('calls onConfirm with selected participant IDs', async () => {
    render(ParticipantSelectModal, {
      props: { ...defaultProps, initialSelectedIds: ['p1', 'p2'] },
    });
    const confirmButton = screen.getByRole('button', { name: /Confirm/i });
    await fireEvent.click(confirmButton);
    await waitFor(() => {
      expect(onConfirm).toHaveBeenCalledWith(expect.arrayContaining(['p1', 'p2']));
      expect(onConfirm.mock.calls[0][0]).toHaveLength(2);
    });
  });

  it('calls onCancel when cancel button is clicked', async () => {
    render(ParticipantSelectModal, { props: defaultProps });
    const cancelButton = screen.getByRole('button', { name: /Cancel/i });
    await fireEvent.click(cancelButton);
    expect(onCancel).toHaveBeenCalled();
  });

  it('calls onCancel when close button is clicked', async () => {
    render(ParticipantSelectModal, { props: defaultProps });
    const closeButton = screen.getByLabelText('Close');
    await fireEvent.click(closeButton);
    expect(onCancel).toHaveBeenCalled();
  });

  it('updates selection count in confirm button when participant is toggled', async () => {
    render(ParticipantSelectModal, {
      props: { ...defaultProps, initialSelectedIds: ['p1', 'p2', 'p3'] },
    });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Confirm \(3 selected\)/i })).toBeTruthy();
    });

    const bobCheckbox = screen.getByLabelText('Include Bob') as HTMLInputElement;
    await fireEvent.change(bobCheckbox, { target: { checked: false } });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Confirm \(2 selected\)/i })).toBeTruthy();
    });
  });

  it('excludes unchecked participants from onConfirm call', async () => {
    render(ParticipantSelectModal, {
      props: { ...defaultProps, initialSelectedIds: ['p1', 'p2', 'p3'] },
    });

    const bobCheckbox = screen.getByLabelText('Include Bob') as HTMLInputElement;
    await fireEvent.change(bobCheckbox, { target: { checked: false } });

    const confirmButton = screen.getByRole('button', { name: /Confirm/i });
    await fireEvent.click(confirmButton);

    await waitFor(() => {
      const calledWith: string[] = onConfirm.mock.calls[0][0];
      expect(calledWith).toContain('p1');
      expect(calledWith).toContain('p3');
      expect(calledWith).not.toContain('p2');
    });
  });

  it('dismisses on Escape key', async () => {
    render(ParticipantSelectModal, { props: defaultProps });
    await fireEvent.keyDown(window, { key: 'Escape' });
    expect(onCancel).toHaveBeenCalled();
  });

  it('dismisses on backdrop click', async () => {
    render(ParticipantSelectModal, { props: defaultProps });
    const backdrop = screen.getByRole('dialog');
    await fireEvent.click(backdrop);
    expect(onCancel).toHaveBeenCalled();
  });
});
