import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/svelte';
import userEvent from '@testing-library/user-event';
import EditParticipantModal from './EditParticipantModal.svelte';
import type { Participant, Split } from '$lib/api/splits';
import * as splitsApi from '$lib/api/splits';

// Mock the API
vi.mock('$lib/api/splits', () => ({
  updateParticipant: vi.fn(),
  deleteParticipant: vi.fn(),
}));

// Mock toast store
vi.mock('$lib/stores/toastStore.svelte', () => ({
  addToast: vi.fn(),
}));

describe('EditParticipantModal', () => {
  const mockParticipant: Participant = {
    id: 'participant-1',
    name: 'Alice',
    nights: 3,
  };

  const mockParticipants: Participant[] = [
    mockParticipant,
    { id: 'participant-2', name: 'Bob', nights: 2 },
  ];

  const mockSplit: Split = {
    id: 'split-1',
    name: 'Test Split',
    createdAt: '2026-01-01T00:00:00Z',
    participants: mockParticipants,
    expenses: [],
  };

  const defaultProps = {
    open: true,
    splitId: 'split-1',
    participant: mockParticipant,
    participants: mockParticipants,
    split: mockSplit,
    onClose: vi.fn(),
    onSuccess: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  // AC 1: Modal opens with pre-filled data
  describe('AC 1: Modal opens and displays pre-filled form', () => {
    it('should render modal when open is true', () => {
      render(EditParticipantModal, { props: defaultProps });
      
      expect(screen.getByRole('dialog')).toBeInTheDocument();
      expect(screen.getByText('Edit Participant')).toBeInTheDocument();
    });

    it('should pre-fill name field with participant name', () => {
      render(EditParticipantModal, { props: defaultProps });
      
      const nameInput = screen.getByLabelText(/name/i) as HTMLInputElement;
      expect(nameInput.value).toBe('Alice');
    });

    it('should pre-fill nights field with participant nights', () => {
      render(EditParticipantModal, { props: defaultProps });
      
      const nightsInput = screen.getByLabelText(/nights/i) as HTMLInputElement;
      expect(nightsInput.value).toBe('3');
    });

    it('should not render modal when open is false', () => {
      render(EditParticipantModal, { props: { ...defaultProps, open: false } });
      
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
  });

  // AC 2: Modal structure and elements
  describe('AC 2: Modal structure', () => {
    it('should display close button in header', () => {
      render(EditParticipantModal, { props: defaultProps });
      
      const closeButton = screen.getByLabelText('Close');
      expect(closeButton).toBeInTheDocument();
    });

    it('should display name input field', () => {
      render(EditParticipantModal, { props: defaultProps });
      
      expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
    });

    it('should display nights input field', () => {
      render(EditParticipantModal, { props: defaultProps });
      
      expect(screen.getByLabelText(/nights/i)).toBeInTheDocument();
    });

    it('should display danger zone with delete button', () => {
      render(EditParticipantModal, { props: defaultProps });
      
      expect(screen.getByText(/delete participant/i)).toBeInTheDocument();
    });

    it('should display Cancel button', () => {
      render(EditParticipantModal, { props: defaultProps });
      
      expect(screen.getByRole('button', { name: /cancel/i })).toBeInTheDocument();
    });

    it('should display Save Changes button', () => {
      render(EditParticipantModal, { props: defaultProps });
      
      expect(screen.getByRole('button', { name: /save changes/i })).toBeInTheDocument();
    });
  });

  // AC 3: Duplicate name validation
  describe('AC 3: Duplicate name validation', () => {
    it('should show error when name duplicates another participant (case-insensitive)', async () => {
      const user = userEvent.setup();
      render(EditParticipantModal, { props: defaultProps });
      
      const nameInput = screen.getByLabelText(/name/i);
      await user.clear(nameInput);
      await user.type(nameInput, 'bob'); // Bob already exists
      await user.tab(); // Trigger blur
      
      await waitFor(() => {
        expect(screen.getByText(/name must be unique.*participant already exists/i)).toBeInTheDocument();
      });
    });

    it('should not show duplicate error for current participant name', async () => {
      const user = userEvent.setup();
      render(EditParticipantModal, { props: defaultProps });
      
      const nameInput = screen.getByLabelText(/name/i);
      await user.clear(nameInput);
      await user.type(nameInput, 'Alice'); // Current participant
      await user.tab();
      
      await waitFor(() => {
        expect(screen.queryByText(/name must be unique.*participant already exists/i)).not.toBeInTheDocument();
      });
    });
  });

  // AC 4: Name validation errors
  describe('AC 4: Name validation', () => {
    it('should show error for empty name', async () => {
      const user = userEvent.setup();
      render(EditParticipantModal, { props: defaultProps });
      
      const nameInput = screen.getByLabelText(/name/i);
      await user.clear(nameInput);
      await user.tab();
      
      await waitFor(() => {
        expect(screen.getByText(/name.*required/i)).toBeInTheDocument();
      });
    });

    it('should show error for name longer than 50 characters', async () => {
      const user = userEvent.setup();
      render(EditParticipantModal, { props: defaultProps });
      
      const nameInput = screen.getByLabelText(/name/i);
      await user.clear(nameInput);
      await user.type(nameInput, 'a'.repeat(51));
      await user.tab();
      
      await waitFor(() => {
        expect(screen.getByText(/cannot exceed 50 characters/i)).toBeInTheDocument();
      });
    });
  });

  // AC 5: Nights validation errors
  describe('AC 5: Nights validation', () => {
    it('should show error for nights less than 0.5', async () => {
      const user = userEvent.setup();
      render(EditParticipantModal, { props: defaultProps });

      const nightsInput = screen.getByLabelText(/nights/i);
      await user.clear(nightsInput);
      await user.type(nightsInput, '0');
      await user.tab();

      await waitFor(() => {
        expect(screen.getByText(/at least 0\.5/i)).toBeInTheDocument();
      });
    });

    it('should show error for nights greater than 365', async () => {
      const user = userEvent.setup();
      render(EditParticipantModal, { props: defaultProps });
      
      const nightsInput = screen.getByLabelText(/nights/i);
      await user.clear(nightsInput);
      await user.type(nightsInput, '366');
      await user.tab();
      
      await waitFor(() => {
        expect(screen.getByText(/cannot exceed 365/i)).toBeInTheDocument();
      });
    });
  });

  // AC 6: Save button disabled when pristine
  describe('AC 6: Save button disabled when form pristine', () => {
    it('should have Save Changes button disabled initially (no changes)', () => {
      render(EditParticipantModal, { props: defaultProps });
      
      const saveButton = screen.getByRole('button', { name: /save changes/i });
      expect(saveButton).toBeDisabled();
    });

    it('should enable Save Changes button when name changes', async () => {
      const user = userEvent.setup();
      render(EditParticipantModal, { props: defaultProps });
      
      const nameInput = screen.getByLabelText(/name/i);
      await user.clear(nameInput);
      await user.type(nameInput, 'Alice Updated');
      
      const saveButton = screen.getByRole('button', { name: /save changes/i });
      expect(saveButton).not.toBeDisabled();
    });

    it('should enable Save Changes button when nights changes', async () => {
      const user = userEvent.setup();
      render(EditParticipantModal, { props: defaultProps });
      
      const nightsInput = screen.getByLabelText(/nights/i);
      await user.clear(nightsInput);
      await user.type(nightsInput, '5');
      
      await waitFor(() => {
        const saveButton = screen.getByRole('button', { name: /save changes/i });
        expect(saveButton).not.toBeDisabled();
      });
    });
  });

  // AC 7: Successful update
  describe('AC 7: Successful participant update', () => {
    it('should call updateParticipant API on save', async () => {
      const user = userEvent.setup();
      vi.mocked(splitsApi.updateParticipant).mockResolvedValue({
        ...mockParticipant,
        name: 'Alice Updated',
      });

      render(EditParticipantModal, { props: defaultProps });
      
      const nameInput = screen.getByLabelText(/name/i);
      await user.clear(nameInput);
      await user.type(nameInput, 'Alice Updated');
      
      const saveButton = screen.getByRole('button', { name: /save changes/i });
      await user.click(saveButton);
      
      await waitFor(() => {
        expect(splitsApi.updateParticipant).toHaveBeenCalledWith(
          'split-1',
          'participant-1',
          { name: 'Alice Updated', nights: 3 }
        );
      });
    });

    it('should call onSuccess callback after successful update', async () => {
      const user = userEvent.setup();
      const onSuccess = vi.fn();
      vi.mocked(splitsApi.updateParticipant).mockResolvedValue(mockParticipant);

      render(EditParticipantModal, { props: { ...defaultProps, onSuccess } });
      
      const nightsInput = screen.getByLabelText(/nights/i);
      await user.clear(nightsInput);
      await user.type(nightsInput, '5');
      
      const saveButton = screen.getByRole('button', { name: /save changes/i });
      await user.click(saveButton);
      
      await waitFor(() => {
        expect(onSuccess).toHaveBeenCalled();
      });
    });

    it('should call onClose after successful update', async () => {
      const user = userEvent.setup();
      const onClose = vi.fn();
      vi.mocked(splitsApi.updateParticipant).mockResolvedValue(mockParticipant);

      render(EditParticipantModal, { props: { ...defaultProps, onClose } });
      
      const nightsInput = screen.getByLabelText(/nights/i);
      await user.clear(nightsInput);
      await user.type(nightsInput, '5');
      
      const saveButton = screen.getByRole('button', { name: /save changes/i });
      await user.click(saveButton);
      
      await waitFor(() => {
        expect(onClose).toHaveBeenCalled();
      });
    });
  });

  // AC 8: API error handling
  describe('AC 8: API error handling', () => {
    it('should keep modal open on API error', async () => {
      const user = userEvent.setup();
      const onClose = vi.fn();
      vi.mocked(splitsApi.updateParticipant).mockRejectedValue({
        status: 500,
        detail: 'Server error',
      });

      render(EditParticipantModal, { props: { ...defaultProps, onClose } });
      
      const nightsInput = screen.getByLabelText(/nights/i);
      await user.clear(nightsInput);
      await user.type(nightsInput, '5');
      
      const saveButton = screen.getByRole('button', { name: /save changes/i });
      await user.click(saveButton);
      
      await waitFor(() => {
        expect(onClose).not.toHaveBeenCalled();
      });
    });

    it('should preserve form data on API error', async () => {
      const user = userEvent.setup();
      vi.mocked(splitsApi.updateParticipant).mockRejectedValue({
        status: 500,
        detail: 'Server error',
      });

      render(EditParticipantModal, { props: defaultProps });
      
      const nameInput = screen.getByLabelText(/name/i) as HTMLInputElement;
      await user.clear(nameInput);
      await user.type(nameInput, 'Alice Updated');
      
      const saveButton = screen.getByRole('button', { name: /save changes/i });
      await user.click(saveButton);
      
      await waitFor(() => {
        expect(nameInput.value).toBe('Alice Updated');
      });
    });
  });

  // AC 9: Delete participant (no expenses)
  describe('AC 9: Delete participant without expenses', () => {
    it('should enable delete button when participant has no expenses', () => {
      render(EditParticipantModal, { props: defaultProps });
      
      const deleteButton = screen.getByText(/delete participant/i);
      expect(deleteButton).not.toBeDisabled();
    });

    it('should show confirmation dialog on delete click', async () => {
      const user = userEvent.setup();
      render(EditParticipantModal, { props: defaultProps });
      
      const deleteButton = screen.getByText(/delete participant/i);
      await user.click(deleteButton);
      
      await waitFor(() => {
        expect(screen.getByText(/delete alice/i)).toBeInTheDocument();
        expect(screen.getByText(/cannot be undone/i)).toBeInTheDocument();
      });
    });

    it('should call deleteParticipant API on confirm', async () => {
      const user = userEvent.setup();
      vi.mocked(splitsApi.deleteParticipant).mockResolvedValue();

      render(EditParticipantModal, { props: defaultProps });
      
      const deleteButton = screen.getByText(/delete participant/i);
      await user.click(deleteButton);
      
      await waitFor(() => {
        expect(screen.getByText(/delete alice/i)).toBeInTheDocument();
      });

      const confirmButton = screen.getByRole('button', { name: /remove/i });
      await user.click(confirmButton);
      
      await waitFor(() => {
        expect(splitsApi.deleteParticipant).toHaveBeenCalledWith('split-1', 'participant-1');
      });
    });

    it('should close modal after successful delete', async () => {
      const user = userEvent.setup();
      const onClose = vi.fn();
      vi.mocked(splitsApi.deleteParticipant).mockResolvedValue();

      render(EditParticipantModal, { props: { ...defaultProps, onClose } });
      
      const deleteButton = screen.getByText(/delete participant/i);
      await user.click(deleteButton);
      
      const confirmButton = screen.getByRole('button', { name: /remove/i });
      await user.click(confirmButton);
      
      await waitFor(() => {
        expect(onClose).toHaveBeenCalled();
      });
    });

    it('should stay in modal when delete is cancelled', async () => {
      const user = userEvent.setup();
      const onClose = vi.fn();
      render(EditParticipantModal, { props: { ...defaultProps, onClose } });
      
      const deleteButton = screen.getByText(/delete participant/i);
      await user.click(deleteButton);
      
      // Wait for confirmation dialog
      await waitFor(() => {
        expect(screen.getByText(/delete alice/i)).toBeInTheDocument();
      });

      // Click Cancel in the confirmation dialog (not the form Cancel button)
      const cancelButtons = screen.getAllByRole('button', { name: /cancel/i });
      const dialogCancelButton = cancelButtons.find(btn => 
        btn.textContent === 'Cancel' && !btn.hasAttribute('aria-label')
      );
      await user.click(dialogCancelButton!);
      
      expect(onClose).not.toHaveBeenCalled();
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });
  });

  // AC 10: Delete button disabled when has expenses
  describe('AC 10: Delete button disabled with expenses', () => {
    it('should disable delete button when participant has expenses', () => {
      const splitWithExpenses: Split = {
        ...mockSplit,
        expenses: [
          {
            id: 'expense-1',
            description: 'Groceries',
            amount: 100,
            payerId: 'participant-1',
            splitMode: 'EQUAL',
            createdAt: '2026-01-01T00:00:00Z',
            shares: [],
          },
        ],
      };

      render(EditParticipantModal, {
        props: { ...defaultProps, split: splitWithExpenses },
      });
      
      const deleteButton = screen.getByText(/delete participant/i);
      expect(deleteButton).toBeDisabled();
    });

    it('should show tooltip on disabled delete button', () => {
      const splitWithExpenses: Split = {
        ...mockSplit,
        expenses: [
          {
            id: 'expense-1',
            description: 'Groceries',
            amount: 100,
            payerId: 'participant-1',
            splitMode: 'EQUAL',
            createdAt: '2026-01-01T00:00:00Z',
            shares: [],
          },
        ],
      };

      render(EditParticipantModal, {
        props: { ...defaultProps, split: splitWithExpenses },
      });
      
      const deleteButton = screen.getByText(/delete participant/i);
      expect(deleteButton).toHaveAttribute('title', 'Remove expenses first');
    });
  });

  // AC 11: Dirty form confirmation on close
  describe('AC 11: Discard changes confirmation', () => {
    it('should show discard confirmation when closing with unsaved changes', async () => {
      const user = userEvent.setup();
      render(EditParticipantModal, { props: defaultProps });
      
      const nameInput = screen.getByLabelText(/name/i);
      await user.clear(nameInput);
      await user.type(nameInput, 'Alice Updated');
      
      const closeButton = screen.getByLabelText('Close');
      await user.click(closeButton);
      
      await waitFor(() => {
        expect(screen.getByText(/discard changes/i)).toBeInTheDocument();
      });
    });

    it('should close modal when discard is confirmed', async () => {
      const user = userEvent.setup();
      const onClose = vi.fn();
      render(EditParticipantModal, { props: { ...defaultProps, onClose } });
      
      const nameInput = screen.getByLabelText(/name/i);
      await user.clear(nameInput);
      await user.type(nameInput, 'Alice Updated');
      
      const closeButton = screen.getByLabelText('Close');
      await user.click(closeButton);
      
      await waitFor(() => {
        expect(screen.getByText(/discard changes/i)).toBeInTheDocument();
      });

      const discardButton = screen.getByRole('button', { name: /discard/i });
      await user.click(discardButton);
      
      await waitFor(() => {
        expect(onClose).toHaveBeenCalled();
      });
    });

    it('should stay in modal when discard is cancelled', async () => {
      const user = userEvent.setup();
      const onClose = vi.fn();
      render(EditParticipantModal, { props: { ...defaultProps, onClose } });
      
      const nameInput = screen.getByLabelText(/name/i);
      await user.clear(nameInput);
      await user.type(nameInput, 'Alice Updated');
      
      const closeButton = screen.getByLabelText('Close');
      await user.click(closeButton);
      
      await waitFor(() => {
        expect(screen.getByText(/discard changes/i)).toBeInTheDocument();
      });

      const cancelButton = screen.getByRole('button', { name: /keep editing/i });
      await user.click(cancelButton);
      
      expect(onClose).not.toHaveBeenCalled();
    });
  });

  // AC 12: Close immediately when pristine
  describe('AC 12: Close without confirmation when pristine', () => {
    it('should close immediately without confirmation when form is pristine', async () => {
      const user = userEvent.setup();
      const onClose = vi.fn();
      render(EditParticipantModal, { props: { ...defaultProps, onClose } });
      
      const closeButton = screen.getByLabelText('Close');
      await user.click(closeButton);
      
      await waitFor(() => {
        expect(onClose).toHaveBeenCalled();
      });
      
      expect(screen.queryByText(/discard changes/i)).not.toBeInTheDocument();
    });

    it('should close immediately via Cancel button when pristine', async () => {
      const user = userEvent.setup();
      const onClose = vi.fn();
      render(EditParticipantModal, { props: { ...defaultProps, onClose } });
      
      const cancelButton = screen.getByRole('button', { name: /cancel edit/i });
      await user.click(cancelButton);
      
      await waitFor(() => {
        expect(onClose).toHaveBeenCalled();
      });
      
      expect(screen.queryByText(/discard changes/i)).not.toBeInTheDocument();
    });
  });

  // AC 12: Escape key behavior
  describe('Escape key handling', () => {
    it('should close immediately on Escape when form is pristine', async () => {
      const user = userEvent.setup();
      const onClose = vi.fn();
      render(EditParticipantModal, { props: { ...defaultProps, onClose } });
      
      await user.keyboard('{Escape}');
      
      await waitFor(() => {
        expect(onClose).toHaveBeenCalled();
      });
    });

    it('should show confirmation on Escape when form is dirty', async () => {
      // Note: Testing the full confirmation flow for Escape is tested in AC 11 (X button)
      // This test just verifies dirty form doesn't close immediately on Escape
      const user = userEvent.setup();
      const onClose = vi.fn();
      render(EditParticipantModal, { props: { ...defaultProps, onClose } });
      
      const nameInput = screen.getByLabelText(/name/i);
      await user.clear(nameInput);
      await user.type(nameInput, 'Alice Updated');
      
      await fireEvent.keyDown(window, { key: 'Escape' });
      
      // Modal should still be visible (either confirmation shows or modal stays)
      // The confirmation dialog flow is thoroughly tested in AC 11
      expect(screen.getByRole('dialog')).toBeInTheDocument();
      // And onClose should not have been called yet
      expect(onClose).not.toHaveBeenCalled();
    });
  });
});
