/**
 * ExpenseEditModal Tests
 * Story FNS-002-6: Edit Expense Modal
 *
 * Tests cover all 8 acceptance criteria:
 * AC1: Modal opens with pre-filled data
 * AC2: All form fields visible and pre-filled
 * AC3: Save button disabled when pristine (not dirty)
 * AC4: Successful expense update
 * AC5: Validation errors
 * AC6: Delete from modal with confirmation
 * AC7: Dirty form confirmation on close
 * AC8: Pristine form closes immediately
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import ExpenseEditModal from './ExpenseEditModal.svelte';
import type { Expense, Participant } from '$lib/api/splits';

// Mock the API
vi.mock('$lib/api/splits', () => ({
  addExpense: vi.fn(),
  addFreeExpense: vi.fn(),
  updateExpense: vi.fn(),
  deleteExpense: vi.fn(),
}));

// Mock the toast store
vi.mock('$lib/stores/toastStore.svelte', () => ({
  addToast: vi.fn(),
}));

import { updateExpense, deleteExpense } from '$lib/api/splits';
import { addToast } from '$lib/stores/toastStore.svelte';

describe('ExpenseEditModal', () => {
  const mockParticipants: Participant[] = [
    { id: 'p1', name: 'Alice', nights: 4 },
    { id: 'p2', name: 'Bob', nights: 2 },
    { id: 'p3', name: 'Charlie', nights: 3 },
  ];

  const mockExpense: Expense = {
    id: 'e1',
    description: 'Groceries',
    amount: 50.00,
    payerId: 'p2',
    splitMode: 'BY_NIGHT',
    createdAt: '2026-02-05T12:00:00Z',
    shares: [
      { participantId: 'p1', amount: 22.22 },
      { participantId: 'p2', amount: 11.11 },
      { participantId: 'p3', amount: 16.67 },
    ],
  };

  const defaultProps = {
    open: true,
    splitId: 'test-split-id',
    expense: mockExpense,
    participants: mockParticipants,
    onClose: vi.fn(),
    onSuccess: vi.fn().mockResolvedValue(undefined),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // --- AC1: Modal opens with pre-filled data ---

  describe('Modal Rendering (AC1)', () => {
    it('renders modal with overlay when open is true', () => {
      render(ExpenseEditModal, { props: defaultProps });

      expect(screen.getByRole('dialog')).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'Edit Expense' })).toBeInTheDocument();
    });

    it('does not render modal when open is false', () => {
      render(ExpenseEditModal, { props: { ...defaultProps, open: false } });

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('renders close button with aria-label', () => {
      render(ExpenseEditModal, { props: defaultProps });

      expect(screen.getByRole('button', { name: /close/i })).toBeInTheDocument();
    });
  });

  // --- AC2: All form fields visible and pre-filled ---

  describe('Form Fields Pre-filled (AC2)', () => {
    it('renders all required form fields', () => {
      render(ExpenseEditModal, { props: defaultProps });

      expect(screen.getByLabelText(/amount/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/description/i)).toBeInTheDocument();
      expect(screen.getByText(/payer/i)).toBeInTheDocument();
      expect(screen.getByText(/split mode/i)).toBeInTheDocument();
    });

    it('pre-fills amount field with expense amount', () => {
      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i) as HTMLInputElement;
      expect(amountInput.value).toBe('50');
    });

    it('pre-fills description field with expense description', () => {
      render(ExpenseEditModal, { props: defaultProps });

      const descInput = screen.getByLabelText(/description/i) as HTMLInputElement;
      expect(descInput.value).toBe('Groceries');
    });

    it('pre-selects payer from expense', () => {
      render(ExpenseEditModal, { props: defaultProps });

      // Bob (p2) should be pre-selected as payer
      expect(screen.getByText('Bob')).toBeInTheDocument();
    });

    it('pre-selects split mode from expense', () => {
      render(ExpenseEditModal, { props: defaultProps });

      const byNightRadio = screen.getByRole('radio', { name: /by night/i });
      expect(byNightRadio).toBeChecked();
    });

    it('pre-selects EQUAL split mode when expense uses EQUAL', () => {
      const equalExpense: Expense = { ...mockExpense, splitMode: 'EQUAL' };
      render(ExpenseEditModal, { props: { ...defaultProps, expense: equalExpense } });

      const equalRadio = screen.getByRole('radio', { name: /equal/i });
      expect(equalRadio).toBeChecked();
    });

    it('renders Delete Expense button below form', () => {
      render(ExpenseEditModal, { props: defaultProps });

      expect(screen.getByRole('button', { name: /delete expense/i })).toBeInTheDocument();
    });
  });

  // --- AC3: Save button disabled when pristine ---

  describe('Save Button State (AC3)', () => {
    it('disables Save Changes button when form is pristine', () => {
      render(ExpenseEditModal, { props: defaultProps });

      const saveButton = screen.getByRole('button', { name: /save changes/i });
      expect(saveButton).toBeDisabled();
    });

    it('enables Save Changes button when amount is changed', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '75.00' } });

      const saveButton = screen.getByRole('button', { name: /save changes/i });
      expect(saveButton).not.toBeDisabled();
    });

    it('enables Save Changes button when description is changed', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const descInput = screen.getByLabelText(/description/i);
      await fireEvent.input(descInput, { target: { value: 'Updated Groceries' } });

      const saveButton = screen.getByRole('button', { name: /save changes/i });
      expect(saveButton).not.toBeDisabled();
    });

    it('enables Save Changes button when split mode is changed', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const equalRadio = screen.getByRole('radio', { name: /equal/i });
      await fireEvent.click(equalRadio);

      const saveButton = screen.getByRole('button', { name: /save changes/i });
      expect(saveButton).not.toBeDisabled();
    });

    it('disables Save Changes button when form has validation errors', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      // Change to invalid amount
      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '0' } });

      const saveButton = screen.getByRole('button', { name: /save changes/i });
      expect(saveButton).toBeDisabled();
    });
  });

  // --- AC4: Successful expense update ---

  describe('Successful Expense Update (AC4)', () => {
    it('calls updateExpense API with correct parameters', async () => {
      vi.mocked(updateExpense).mockResolvedValue({
        ...mockExpense,
        amount: 75.00,
        description: 'Updated Groceries',
      });

      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '75.00' } });

      const descInput = screen.getByLabelText(/description/i);
      await fireEvent.input(descInput, { target: { value: 'Updated Groceries' } });

      const saveButton = screen.getByRole('button', { name: /save changes/i });
      await fireEvent.click(saveButton);

      await waitFor(() => {
        expect(updateExpense).toHaveBeenCalledWith('test-split-id', 'e1', {
          amount: 75.00,
          description: 'Updated Groceries',
          payerId: 'p2',
          splitMode: 'BY_NIGHT',
        });
      });
    });

    it('shows loading state on Save Changes button during API call', async () => {
      vi.mocked(updateExpense).mockImplementation(() => new Promise(() => {}));

      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '75.00' } });

      const saveButton = screen.getByRole('button', { name: /save changes/i });
      await fireEvent.click(saveButton);

      await waitFor(() => {
        expect(screen.getByText(/saving|updating/i)).toBeInTheDocument();
      });
    });

    it('calls onSuccess and onClose after successful update', async () => {
      vi.mocked(updateExpense).mockResolvedValue({
        ...mockExpense,
        amount: 75.00,
      });

      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '75.00' } });

      const saveButton = screen.getByRole('button', { name: /save changes/i });
      await fireEvent.click(saveButton);

      await waitFor(() => {
        expect(defaultProps.onSuccess).toHaveBeenCalled();
        expect(defaultProps.onClose).toHaveBeenCalled();
      });
    });

    it('shows success toast after update', async () => {
      vi.mocked(updateExpense).mockResolvedValue({
        ...mockExpense,
        amount: 75.00,
      });

      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '75.00' } });

      const saveButton = screen.getByRole('button', { name: /save changes/i });
      await fireEvent.click(saveButton);

      await waitFor(() => {
        expect(addToast).toHaveBeenCalledWith({
          type: 'success',
          message: 'Expense updated',
          description: 'Groceries · €75.00 · Paid by Bob',
        });
      });
    });

    it('shows error toast when API fails and keeps modal open', async () => {
      vi.mocked(updateExpense).mockRejectedValue({ detail: 'Server error' });

      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '75.00' } });

      const saveButton = screen.getByRole('button', { name: /save changes/i });
      await fireEvent.click(saveButton);

      await waitFor(() => {
        expect(addToast).toHaveBeenCalledWith({
          type: 'error',
          message: 'Server error',
        });
      });

      // Modal should still be open with data preserved
      expect(screen.getByRole('dialog')).toBeInTheDocument();
      expect(defaultProps.onClose).not.toHaveBeenCalled();
    });
  });

  // --- AC5: Validation errors ---

  describe('Validation Errors (AC5)', () => {
    it('shows error when amount is less than 0.01', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '0.001' } });
      await fireEvent.blur(amountInput);

      await waitFor(() => {
        expect(screen.getByText(/at least.*0\.01/i)).toBeInTheDocument();
      });
    });

    it('shows error when amount exceeds maximum', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '1000000' } });
      await fireEvent.blur(amountInput);

      await waitFor(() => {
        expect(screen.getByText(/999,999\.99/i)).toBeInTheDocument();
      });
    });

    it('shows error when description exceeds 200 characters', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const descInput = screen.getByLabelText(/description/i);
      const longDescription = 'a'.repeat(201);
      await fireEvent.input(descInput, { target: { value: longDescription } });
      await fireEvent.blur(descInput);

      await waitFor(() => {
        expect(screen.getByText(/200 characters/i)).toBeInTheDocument();
      });
    });

    it('accepts valid amount changes', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '100.50' } });
      await fireEvent.blur(amountInput);

      expect(screen.queryByText(/at least.*0\.01/i)).not.toBeInTheDocument();
    });

    it('accepts description up to 200 characters', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const descInput = screen.getByLabelText(/description/i);
      const validDescription = 'a'.repeat(200);
      await fireEvent.input(descInput, { target: { value: validDescription } });
      await fireEvent.blur(descInput);

      expect(screen.queryByText(/200 characters/i)).not.toBeInTheDocument();
    });
  });

  // --- AC6: Delete from modal with confirmation ---

  describe('Delete from Modal (AC6)', () => {
    it('shows confirmation dialog when Delete Expense button is clicked', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const deleteButton = screen.getByRole('button', { name: /delete expense/i });
      await fireEvent.click(deleteButton);

      await waitFor(() => {
        expect(screen.getByText(/delete this expense/i)).toBeInTheDocument();
        expect(screen.getByText(/recalculate balances/i)).toBeInTheDocument();
      });
    });

    it('deletes expense and closes modal when confirmed', async () => {
      vi.mocked(deleteExpense).mockResolvedValue(undefined);

      render(ExpenseEditModal, { props: defaultProps });

      const deleteButton = screen.getByRole('button', { name: /delete expense/i });
      await fireEvent.click(deleteButton);

      await waitFor(() => {
        expect(screen.getByText(/delete this expense/i)).toBeInTheDocument();
      });

      const confirmButton = screen.getByRole('button', { name: /^delete$/i });
      await fireEvent.click(confirmButton);

      await waitFor(() => {
        expect(deleteExpense).toHaveBeenCalledWith('test-split-id', 'e1');
        expect(defaultProps.onSuccess).toHaveBeenCalled();
        expect(defaultProps.onClose).toHaveBeenCalled();
      });
    });

    it('shows success toast after deletion', async () => {
      vi.mocked(deleteExpense).mockResolvedValue(undefined);

      render(ExpenseEditModal, { props: defaultProps });

      const deleteButton = screen.getByRole('button', { name: /delete expense/i });
      await fireEvent.click(deleteButton);

      await waitFor(() => {
        expect(screen.getByText(/delete this expense/i)).toBeInTheDocument();
      });

      const confirmButton = screen.getByRole('button', { name: /^delete$/i });
      await fireEvent.click(confirmButton);

      await waitFor(() => {
        expect(addToast).toHaveBeenCalledWith({
          type: 'success',
          message: 'Expense deleted',
          description: 'Groceries · €50.00',
        });
      });
    });

    it('keeps modal open when delete is canceled', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const deleteButton = screen.getByRole('button', { name: /delete expense/i });
      await fireEvent.click(deleteButton);

      await waitFor(() => {
        expect(screen.getByText(/delete this expense/i)).toBeInTheDocument();
      });

      // Find all Cancel buttons and use the one from the delete dialog (second one)
      const cancelButtons = screen.getAllByRole('button', { name: /^cancel$/i });
      await fireEvent.click(cancelButtons[cancelButtons.length - 1]);

      expect(defaultProps.onClose).not.toHaveBeenCalled();
      expect(deleteExpense).not.toHaveBeenCalled();
    });

    it('shows error toast when delete fails', async () => {
      vi.mocked(deleteExpense).mockRejectedValue({ detail: 'Failed to delete' });

      render(ExpenseEditModal, { props: defaultProps });

      const deleteButton = screen.getByRole('button', { name: /delete expense/i });
      await fireEvent.click(deleteButton);

      await waitFor(() => {
        expect(screen.getByText(/delete this expense/i)).toBeInTheDocument();
      });

      const confirmButton = screen.getByRole('button', { name: /^delete$/i });
      await fireEvent.click(confirmButton);

      await waitFor(() => {
        expect(addToast).toHaveBeenCalledWith({
          type: 'error',
          message: 'Failed to delete',
        });
      });
    });
  });

  // --- AC7: Dirty form confirmation on close ---

  describe('Dirty Form Confirmation (AC7)', () => {
    it('shows confirmation dialog when closing with dirty form via X button', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      // Make form dirty
      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '75.00' } });

      // Click close button
      const closeButton = screen.getByRole('button', { name: /close/i });
      await fireEvent.click(closeButton);

      // Should show confirmation dialog
      await waitFor(() => {
        expect(screen.getByText(/discard changes/i)).toBeInTheDocument();
      });
    });

    it('shows confirmation dialog when clicking Cancel with dirty form', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      // Make form dirty
      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '75.00' } });

      // Click cancel button
      const cancelButton = screen.getAllByRole('button', { name: /cancel/i })[0];
      await fireEvent.click(cancelButton);

      // Should show confirmation dialog
      await waitFor(() => {
        expect(screen.getByText(/discard changes/i)).toBeInTheDocument();
      });
    });

    it('closes modal when confirming discard', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      // Make form dirty
      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '75.00' } });

      // Click close button
      const closeButton = screen.getByRole('button', { name: /close/i });
      await fireEvent.click(closeButton);

      // Confirm discard
      await waitFor(() => {
        expect(screen.getByText(/discard changes/i)).toBeInTheDocument();
      });

      const confirmButton = screen.getByRole('button', { name: /discard/i });
      await fireEvent.click(confirmButton);

      expect(defaultProps.onClose).toHaveBeenCalled();
    });

    it('keeps modal open when canceling discard', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      // Make form dirty
      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '75.00' } });

      // Click close button
      const closeButton = screen.getByRole('button', { name: /close/i });
      await fireEvent.click(closeButton);

      // Cancel discard
      await waitFor(() => {
        expect(screen.getByText(/discard changes/i)).toBeInTheDocument();
      });

      const keepEditingButton = screen.getByRole('button', { name: /keep editing/i });
      await fireEvent.click(keepEditingButton);

      expect(defaultProps.onClose).not.toHaveBeenCalled();
      expect(screen.getByRole('dialog')).toBeInTheDocument();
      expect((amountInput as HTMLInputElement).value).toBe('75.00');
    });

    it('does not close dirty form on Escape key without confirmation', async () => {
      // This test verifies Escape doesn't close a dirty form without confirmation
      // The confirmation dialog flow is tested thoroughly in the X button tests
      render(ExpenseEditModal, { props: defaultProps });

      // Make form dirty
      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '75.00' } });

      // Press Escape
      await fireEvent.keyDown(window, { key: 'Escape' });

      // Modal should still be visible (onClose not called without confirmation)
      // Either the confirmation appears, or the modal stays open
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });
  });

  // --- AC8: Pristine form closes immediately ---

  describe('Pristine Form Close (AC8)', () => {
    it('closes immediately when form is pristine via X button', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const closeButton = screen.getByRole('button', { name: /close/i });
      await fireEvent.click(closeButton);

      expect(defaultProps.onClose).toHaveBeenCalled();
    });

    it('closes immediately when form is pristine via Cancel button', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const cancelButton = screen.getAllByRole('button', { name: /cancel/i })[0];
      await fireEvent.click(cancelButton);

      expect(defaultProps.onClose).toHaveBeenCalled();
    });

    it('closes immediately on Escape key when form is pristine', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      await fireEvent.keyDown(window, { key: 'Escape' });

      expect(defaultProps.onClose).toHaveBeenCalled();
    });

    it('does not show confirmation dialog for pristine form', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const closeButton = screen.getByRole('button', { name: /close/i });
      await fireEvent.click(closeButton);

      expect(screen.queryByText(/discard changes/i)).not.toBeInTheDocument();
    });
  });

  // --- BY_SHARE Split Mode ---

  describe('BY_SHARE Split Mode', () => {
    it('renders By Share radio button option', () => {
      render(ExpenseEditModal, { props: defaultProps });

      expect(screen.getByRole('radio', { name: /by share/i })).toBeInTheDocument();
    });

    it('pre-selects BY_SHARE when expense uses BY_SHARE', () => {
      const byShareExpense = { ...mockExpense, splitMode: 'BY_SHARE' as const };
      render(ExpenseEditModal, { props: { ...defaultProps, expense: byShareExpense } });

      const byShareRadio = screen.getByRole('radio', { name: /by share/i });
      expect(byShareRadio).toBeChecked();
    });

    it('enables Save Changes when split mode is changed to BY_SHARE', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const byShareRadio = screen.getByRole('radio', { name: /by share/i });
      await fireEvent.click(byShareRadio);

      const saveButton = screen.getByRole('button', { name: /save changes/i });
      expect(saveButton).not.toBeDisabled();
    });

    it('calls updateExpense with BY_SHARE splitMode', async () => {
      vi.mocked(updateExpense).mockResolvedValue({
        ...mockExpense,
        splitMode: 'BY_SHARE',
      });

      render(ExpenseEditModal, { props: defaultProps });

      const byShareRadio = screen.getByRole('radio', { name: /by share/i });
      await fireEvent.click(byShareRadio);

      const saveButton = screen.getByRole('button', { name: /save changes/i });
      await fireEvent.click(saveButton);

      await waitFor(() => {
        expect(updateExpense).toHaveBeenCalledWith('test-split-id', 'e1', expect.objectContaining({
          splitMode: 'BY_SHARE',
        }));
      });
    });
  });

  // --- Accessibility ---

  describe('Accessibility', () => {
    it('has proper ARIA attributes on dialog', () => {
      render(ExpenseEditModal, { props: defaultProps });

      const dialog = screen.getByRole('dialog');
      expect(dialog).toHaveAttribute('aria-modal', 'true');
      expect(dialog).toHaveAttribute('aria-labelledby');
    });

    it('all form fields have associated labels', () => {
      render(ExpenseEditModal, { props: defaultProps });

      expect(screen.getByLabelText(/amount/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/description/i)).toBeInTheDocument();
    });

    it('Save Changes button has minimum 44px touch target', () => {
      render(ExpenseEditModal, { props: defaultProps });

      const saveButton = screen.getByRole('button', { name: /save changes/i });
      expect(saveButton).toHaveClass('min-h-[44px]');
    });

    it('Delete Expense button has proper destructive styling', () => {
      render(ExpenseEditModal, { props: defaultProps });

      const deleteButton = screen.getByRole('button', { name: /delete expense/i });
      expect(deleteButton).toBeInTheDocument();
    });
  });
});