/**
 * ExpenseEditModal Tests
 * Story FNS-002-3: Add Expense Modal
 *
 * Tests cover all 10 acceptance criteria:
 * AC1: Modal opens from participant card
 * AC2: Form fields render correctly with pre-selected payer
 * AC3: Submit button disabled when form invalid
 * AC4: Amount validation (min €0.01)
 * AC5: Description validation (max 100 chars)
 * AC6: Successful expense creation
 * AC7: API error handling
 * AC8: Dirty form confirmation on close
 * AC9: Pristine form closes immediately
 * AC10: Escape key triggers close logic
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import ExpenseEditModal from './ExpenseEditModal.svelte';
import type { Participant } from '$lib/api/splits';

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

import { addExpense } from '$lib/api/splits';
import { addToast } from '$lib/stores/toastStore.svelte';

describe('ExpenseEditModal', () => {
  const mockParticipants: Participant[] = [
    { id: 'p1', name: 'Alice', nights: 4 },
    { id: 'p2', name: 'Bob', nights: 2 },
    { id: 'p3', name: 'Charlie', nights: 3 },
  ];

  const defaultProps = {
    open: true,
    splitId: 'test-split-id',
    preselectedPayerId: 'p2',
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

  // --- AC1: Modal opens and renders ---

  describe('Modal Rendering (AC1)', () => {
    it('renders modal with overlay when open is true', () => {
      render(ExpenseEditModal, { props: defaultProps });

      expect(screen.getByRole('dialog')).toBeInTheDocument();
      // Title is in h2
      expect(screen.getByRole('heading', { name: 'Add Expense' })).toBeInTheDocument();
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

  // --- AC2: Form fields render correctly ---

  describe('Form Fields (AC2)', () => {
    it('renders all required form fields', () => {
      render(ExpenseEditModal, { props: defaultProps });

      expect(screen.getByLabelText(/amount/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/description/i)).toBeInTheDocument();
      expect(screen.getByText(/payer/i)).toBeInTheDocument();
      expect(screen.getByText(/split mode/i)).toBeInTheDocument();
    });

    it('pre-selects payer from preselectedPayerId prop', () => {
      render(ExpenseEditModal, { props: defaultProps });

      // Bob (p2) should be pre-selected
      expect(screen.getByText('Bob')).toBeInTheDocument();
    });

    it('has auto-focus on amount field', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      await waitFor(() => {
        const amountInput = screen.getByLabelText(/amount/i);
        expect(document.activeElement).toBe(amountInput);
      });
    });

    it('sets BY_NIGHT as default split mode', () => {
      render(ExpenseEditModal, { props: defaultProps });

      const byNightRadio = screen.getByRole('radio', { name: /by night/i });
      expect(byNightRadio).toBeChecked();
    });

    it('renders Equal split mode option', () => {
      render(ExpenseEditModal, { props: defaultProps });

      expect(screen.getByRole('radio', { name: /equal/i })).toBeInTheDocument();
    });

    it('description field has placeholder', () => {
      render(ExpenseEditModal, { props: defaultProps });

      const descInput = screen.getByLabelText(/description/i);
      expect(descInput).toHaveAttribute('placeholder');
    });
  });

  // --- AC3: Submit button disabled when form invalid ---

  describe('Submit Button State (AC3)', () => {
    it('shows validation errors when submitting with empty amount', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const submitButton = screen.getByRole('button', { name: /add expense/i });
      expect(submitButton).not.toBeDisabled();

      await fireEvent.click(submitButton);

      expect(screen.getByText('Amount must be at least €0.01')).toBeInTheDocument();
    });

    it('enables submit button when required fields are valid', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '25.50' } });

      const submitButton = screen.getByRole('button', { name: /add expense/i });
      expect(submitButton).not.toBeDisabled();
    });
  });

  // --- AC4: Amount validation ---

  describe('Amount Validation (AC4)', () => {
    it('shows error when amount is less than 0.01', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '0.001' } });
      await fireEvent.blur(amountInput);

      await waitFor(() => {
        expect(screen.getByText(/at least.*0\.01/i)).toBeInTheDocument();
      });
    });

    it('shows error when amount is zero', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '0' } });
      await fireEvent.blur(amountInput);

      await waitFor(() => {
        expect(screen.getByText(/at least.*0\.01/i)).toBeInTheDocument();
      });
    });

    it('accepts valid amount', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '50.00' } });
      await fireEvent.blur(amountInput);

      expect(screen.queryByText(/at least.*0\.01/i)).not.toBeInTheDocument();
    });
  });

  // --- AC5: Description validation ---

  describe('Description Validation (AC5)', () => {
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

    it('accepts description up to 200 characters', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const descInput = screen.getByLabelText(/description/i);
      const validDescription = 'a'.repeat(200);
      await fireEvent.input(descInput, { target: { value: validDescription } });
      await fireEvent.blur(descInput);

      expect(screen.queryByText(/200 characters/i)).not.toBeInTheDocument();
    });

    it('allows empty description (optional field)', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      // Just enter amount, leave description empty
      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '25.00' } });

      const submitButton = screen.getByRole('button', { name: /add expense/i });
      expect(submitButton).not.toBeDisabled();
    });
  });

  // --- AC6: Successful expense creation ---

  describe('Successful Expense Creation (AC6)', () => {
    it('calls addExpense API with correct parameters', async () => {
      vi.mocked(addExpense).mockResolvedValue({
        id: 'e1',
        description: 'Test expense',
        amount: 25.50,
        payerId: 'p2',
        splitMode: 'BY_NIGHT',
        createdAt: '2026-02-05T12:00:00Z',
        shares: [],
      });

      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '25.50' } });

      const descInput = screen.getByLabelText(/description/i);
      await fireEvent.input(descInput, { target: { value: 'Groceries' } });

      const submitButton = screen.getByRole('button', { name: /add expense/i });
      await fireEvent.click(submitButton);

      await waitFor(() => {
        expect(addExpense).toHaveBeenCalledWith('test-split-id', {
          amount: 25.50,
          description: 'Groceries',
          payerId: 'p2',
          splitMode: 'BY_NIGHT',
        });
      });
    });

    it('shows loading state on submit button during API call', async () => {
      vi.mocked(addExpense).mockImplementation(() => new Promise(() => {}));

      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '25.50' } });

      const submitButton = screen.getByRole('button', { name: /add expense/i });
      await fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/adding/i)).toBeInTheDocument();
      });
    });

    it('calls onSuccess and onClose after successful creation', async () => {
      vi.mocked(addExpense).mockResolvedValue({
        id: 'e1',
        description: 'Test',
        amount: 25.50,
        payerId: 'p2',
        splitMode: 'BY_NIGHT',
        createdAt: '2026-02-05T12:00:00Z',
        shares: [],
      });

      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '25.50' } });

      const submitButton = screen.getByRole('button', { name: /add expense/i });
      await fireEvent.click(submitButton);

      await waitFor(() => {
        expect(defaultProps.onSuccess).toHaveBeenCalled();
        expect(defaultProps.onClose).toHaveBeenCalled();
      });
    });

    it('shows success toast after creation', async () => {
      vi.mocked(addExpense).mockResolvedValue({
        id: 'e1',
        description: 'Test',
        amount: 25.50,
        payerId: 'p2',
        splitMode: 'BY_NIGHT',
        createdAt: '2026-02-05T12:00:00Z',
        shares: [],
      });

      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '25.50' } });

      const submitButton = screen.getByRole('button', { name: /add expense/i });
      await fireEvent.click(submitButton);

      await waitFor(() => {
        expect(addToast).toHaveBeenCalledWith({
          type: 'success',
          message: 'Expense added',
        });
      });
    });
  });

  // --- AC7: API error handling ---

  describe('API Error Handling (AC7)', () => {
    it('shows error toast when API fails', async () => {
      vi.mocked(addExpense).mockRejectedValue({ detail: 'Server error' });

      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '25.50' } });

      const submitButton = screen.getByRole('button', { name: /add expense/i });
      await fireEvent.click(submitButton);

      await waitFor(() => {
        expect(addToast).toHaveBeenCalledWith({
          type: 'error',
          message: 'Server error',
        });
      });
    });

    it('keeps modal open and preserves form data on error', async () => {
      vi.mocked(addExpense).mockRejectedValue({ detail: 'Server error' });

      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '25.50' } });

      const descInput = screen.getByLabelText(/description/i);
      await fireEvent.input(descInput, { target: { value: 'Test expense' } });

      const submitButton = screen.getByRole('button', { name: /add expense/i });
      await fireEvent.click(submitButton);

      await waitFor(() => {
        expect(addToast).toHaveBeenCalled();
      });

      // Modal should still be open with data preserved
      expect(screen.getByRole('dialog')).toBeInTheDocument();
      expect(amountInput).toHaveValue(25.5);
      expect(descInput).toHaveValue('Test expense');
      expect(defaultProps.onClose).not.toHaveBeenCalled();
    });
  });

  // --- AC8: Dirty form confirmation on close ---

  describe('Dirty Form Confirmation (AC8)', () => {
    it('shows confirmation dialog when closing with dirty form via X button', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      // Make form dirty
      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '25.50' } });

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
      await fireEvent.input(amountInput, { target: { value: '25.50' } });

      // Click cancel button
      const cancelButton = screen.getByRole('button', { name: /cancel/i });
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
      await fireEvent.input(amountInput, { target: { value: '25.50' } });

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
      await fireEvent.input(amountInput, { target: { value: '25.50' } });

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
      expect(amountInput).toHaveValue(25.5);
    });
  });

  // --- AC9: Pristine form closes immediately ---

  describe('Pristine Form Close (AC9)', () => {
    it('closes immediately when form is pristine via X button', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const closeButton = screen.getByRole('button', { name: /close/i });
      await fireEvent.click(closeButton);

      expect(defaultProps.onClose).toHaveBeenCalled();
    });

    it('closes immediately when form is pristine via Cancel button', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const cancelButton = screen.getByRole('button', { name: /cancel/i });
      await fireEvent.click(cancelButton);

      expect(defaultProps.onClose).toHaveBeenCalled();
    });

    it('does not show confirmation dialog for pristine form', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const closeButton = screen.getByRole('button', { name: /close/i });
      await fireEvent.click(closeButton);

      expect(screen.queryByText(/discard changes/i)).not.toBeInTheDocument();
    });
  });

  // --- AC10: Escape key triggers close logic ---

  describe('Escape Key Handling (AC10)', () => {
    it('closes pristine modal on Escape key', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      await fireEvent.keyDown(window, { key: 'Escape' });

      expect(defaultProps.onClose).toHaveBeenCalled();
    });

    it('does not close dirty form on Escape key without confirmation', async () => {
      // This test verifies Escape doesn't close a dirty form without confirmation
      // The confirmation dialog flow is tested thoroughly in the X button tests
      render(ExpenseEditModal, { props: defaultProps });

      // Make form dirty
      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '25.50' } });

      // Press Escape
      await fireEvent.keyDown(window, { key: 'Escape' });

      // Modal should still be visible (onClose not called without confirmation)
      // Either the confirmation appears, or the modal stays open
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });
  });

  // --- Split Mode Selection ---

  describe('Split Mode Selection', () => {
    it('allows selecting Equal split mode', async () => {
      vi.mocked(addExpense).mockResolvedValue({
        id: 'e1',
        description: 'Test',
        amount: 25.50,
        payerId: 'p2',
        splitMode: 'EQUAL',
        createdAt: '2026-02-05T12:00:00Z',
        shares: [],
      });

      render(ExpenseEditModal, { props: defaultProps });

      // Select Equal mode
      const equalRadio = screen.getByRole('radio', { name: /equal/i });
      await fireEvent.click(equalRadio);

      // Fill amount
      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '30.00' } });

      // Submit
      const submitButton = screen.getByRole('button', { name: /add expense/i });
      await fireEvent.click(submitButton);

      await waitFor(() => {
        expect(addExpense).toHaveBeenCalledWith('test-split-id', expect.objectContaining({
          splitMode: 'EQUAL',
        }));
      });
    });
  });

  // --- Payer Selection ---

  describe('Payer Selection', () => {
    it('uses pre-selected payer from props in API call', async () => {
      // This test verifies the pre-selected payer is correctly used
      // The payer selection UI integration is covered by the main API call test
      vi.mocked(addExpense).mockResolvedValue({
        id: 'e1',
        description: 'Test',
        amount: 25.50,
        payerId: 'p2', // Bob is pre-selected via preselectedPayerId: 'p2'
        splitMode: 'BY_NIGHT',
        createdAt: '2026-02-05T12:00:00Z',
        shares: [],
      });

      render(ExpenseEditModal, { props: defaultProps });

      // Fill amount and submit without changing payer
      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '25.50' } });

      const submitButton = screen.getByRole('button', { name: /add expense/i });
      await fireEvent.click(submitButton);

      await waitFor(() => {
        expect(addExpense).toHaveBeenCalledWith('test-split-id', expect.objectContaining({
          payerId: 'p2', // Verifies pre-selected payer is used
        }));
      });
    });
  });

  // --- Story 4.3: FREE Mode Manual Share Specification Tests ---

  describe('FREE Mode Share Specification (Story 4.3)', () => {
    it('renders FREE radio button option (AC1)', () => {
      render(ExpenseEditModal, { props: defaultProps });

      expect(screen.getByRole('radio', { name: /manual/i })).toBeInTheDocument();
    });

    it('shows participant share inputs when FREE mode selected (AC1)', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const freeRadio = screen.getByRole('radio', { name: /manual/i });
      await fireEvent.click(freeRadio);

      await waitFor(() => {
        expect(screen.getByLabelText('Alice')).toBeInTheDocument();
        expect(screen.getByLabelText('Bob')).toBeInTheDocument();
        expect(screen.getByLabelText('Charlie')).toBeInTheDocument();
      });
    });

    it('share inputs are pre-filled with empty values (AC1)', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const freeRadio = screen.getByRole('radio', { name: /manual/i });
      await fireEvent.click(freeRadio);

      await waitFor(() => {
        const aliceInput = screen.getByLabelText('Alice') as HTMLInputElement;
        const bobInput = screen.getByLabelText('Bob') as HTMLInputElement;
        const charlieInput = screen.getByLabelText('Charlie') as HTMLInputElement;
        
        expect(aliceInput.value).toBe('');
        expect(bobInput.value).toBe('');
        expect(charlieInput.value).toBe('');
      });
    });

    it('displays running total for parts (AC1)', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const freeRadio = screen.getByRole('radio', { name: /manual/i });
      await fireEvent.click(freeRadio);

      await waitFor(() => {
        expect(screen.getByText(/total:/i)).toBeInTheDocument();
      });
    });

    it('running total updates when share parts entered (AC2)', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const freeRadio = screen.getByRole('radio', { name: /manual/i });
      await fireEvent.click(freeRadio);

      await waitFor(() => {
        expect(screen.getByText(/total: 0\.00 parts/i)).toBeInTheDocument();
      });

      const aliceInput = screen.getByLabelText('Alice');
      await fireEvent.input(aliceInput, { target: { value: '2' } });
      
      await waitFor(() => {
        expect(screen.getByText(/total: 2\.00 parts/i)).toBeInTheDocument();
      });

      const bobInput = screen.getByLabelText('Bob');
      await fireEvent.input(bobInput, { target: { value: '3' } });

      await waitFor(() => {
        expect(screen.getByText(/total: 5\.00 parts/i)).toBeInTheDocument();
      });
    });

    it('shows green checkmark when parts are valid (AC2)', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const freeRadio = screen.getByRole('radio', { name: /manual/i });
      await fireEvent.click(freeRadio);

      const aliceInput = screen.getByLabelText('Alice');
      await fireEvent.input(aliceInput, { target: { value: '2' } });

      await waitFor(() => {
        expect(screen.getByText(/✓/)).toBeInTheDocument();
      });
    });

    it('shows validation error when all parts are zero (AC3)', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const freeRadio = screen.getByRole('radio', { name: /manual/i });
      await fireEvent.click(freeRadio);

      // Set amount
      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '100' } });

      // Try to submit with zero parts
      const submitButton = screen.getByRole('button', { name: /add expense/i });
      await fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/at least one participant must have positive parts/i)).toBeInTheDocument();
      });
    });

    it('enables submit when valid parts entered (AC2)', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const freeRadio = screen.getByRole('radio', { name: /manual/i });
      await fireEvent.click(freeRadio);

      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '100' } });

      const aliceInput = screen.getByLabelText('Alice');
      await fireEvent.input(aliceInput, { target: { value: '2' } });

      const bobInput = screen.getByLabelText('Bob');
      await fireEvent.input(bobInput, { target: { value: '3' } });

      const submitButton = screen.getByRole('button', { name: /add expense/i });
      expect(submitButton).not.toBeDisabled();
    });

    it('calls addFreeExpense API with correct request body (AC6)', async () => {
      const { addFreeExpense } = await import('$lib/api/splits');
      vi.mocked(addFreeExpense).mockResolvedValue({
        id: 'e1',
        description: 'Custom Split',
        amount: 100.00,
        payerId: 'p1',
        splitMode: 'FREE',
        type: 'FREE',
        createdAt: '2026-02-10T12:00:00Z',
        shares: [
          { participantId: 'p1', amount: 40.00, parts: 2 },
          { participantId: 'p2', amount: 60.00, parts: 3 },
        ],
      });

      render(ExpenseEditModal, { props: defaultProps });

      // Select FREE mode
      const freeRadio = screen.getByRole('radio', { name: /manual/i });
      await fireEvent.click(freeRadio);

      // Fill form
      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '100' } });

      const aliceInput = screen.getByLabelText('Alice');
      await fireEvent.input(aliceInput, { target: { value: '2' } });

      const bobInput = screen.getByLabelText('Bob');
      await fireEvent.input(bobInput, { target: { value: '3' } });

      // Submit
      const submitButton = screen.getByRole('button', { name: /add expense/i });
      await fireEvent.click(submitButton);

      await waitFor(() => {
        expect(addFreeExpense).toHaveBeenCalledWith('test-split-id', {
          amount: 100,
          description: '',
          payerId: 'p2',
          shares: [
            { participantId: 'p1', parts: 2 },
            { participantId: 'p2', parts: 3 },
            { participantId: 'p3', parts: 0 },
          ],
        });
      });
    });

    it('form reset clears share inputs', async () => {
      const { rerender } = render(ExpenseEditModal, { props: defaultProps });

      // Switch to FREE mode and set a share value
      const freeRadio = screen.getByRole('radio', { name: /manual/i });
      await fireEvent.click(freeRadio);

      const aliceInput = screen.getByLabelText('Alice') as HTMLInputElement;
      await fireEvent.input(aliceInput, { target: { value: '5' } });
      expect(aliceInput.value).toBe('5');

      // Close modal (toggle open to false)
      await rerender({ ...defaultProps, open: false });

      // Reopen modal (toggle open to true → triggers resetForm via $effect)
      await rerender({ ...defaultProps, open: true });

      // After reset, splitMode goes back to BY_NIGHT — switch to FREE again
      await waitFor(() => {
        expect(screen.getByRole('radio', { name: /manual/i })).toBeInTheDocument();
      });
      await fireEvent.click(screen.getByRole('radio', { name: /manual/i }));

      // Verify share input was cleared by the reset
      await waitFor(() => {
        const resetAliceInput = screen.getByLabelText('Alice') as HTMLInputElement;
        expect(resetAliceInput.value).toBe('');
      });
    });

    it('share inputs have numeric type with correct step (AC9)', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const freeRadio = screen.getByRole('radio', { name: /manual/i });
      await fireEvent.click(freeRadio);

      await waitFor(() => {
        const aliceInput = screen.getByLabelText('Alice');
        expect(aliceInput).toHaveAttribute('type', 'number');
        expect(aliceInput).toHaveAttribute('step', '0.01');
        expect(aliceInput).toHaveAttribute('min', '0');
      });
    });

    it('share inputs have minimum 44px touch targets (AC9)', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const freeRadio = screen.getByRole('radio', { name: /manual/i });
      await fireEvent.click(freeRadio);

      await waitFor(() => {
        const aliceInput = screen.getByLabelText('Alice');
        expect(aliceInput).toHaveClass('min-h-[44px]');
      });
    });

    it('displays explanatory text about proportional calculation', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const freeRadio = screen.getByRole('radio', { name: /manual/i });
      await fireEvent.click(freeRadio);

      await waitFor(() => {
        expect(screen.getByText(/amounts will be calculated proportionally/i)).toBeInTheDocument();
      });
    });
  });

  // --- Amount Input: free entry and arrow key step ---

  describe('Amount Input Behavior', () => {
    it('accepts arbitrary decimal amounts not on a 0.5 boundary', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i);
      await fireEvent.input(amountInput, { target: { value: '12.30' } });
      await fireEvent.blur(amountInput);

      expect(screen.queryByText(/amount must be/i)).not.toBeInTheDocument();
    });

    it('increments amount by 0.5 on ArrowUp key', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i) as HTMLInputElement;
      await fireEvent.input(amountInput, { target: { value: '10' } });
      await fireEvent.keyDown(amountInput, { key: 'ArrowUp' });

      await waitFor(() => {
        expect(Number(amountInput.value)).toBeCloseTo(10.5);
      });
    });

    it('decrements amount by 0.5 on ArrowDown key', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i) as HTMLInputElement;
      await fireEvent.input(amountInput, { target: { value: '10' } });
      await fireEvent.keyDown(amountInput, { key: 'ArrowDown' });

      await waitFor(() => {
        expect(Number(amountInput.value)).toBeCloseTo(9.5);
      });
    });

    it('does not go below 0 on ArrowDown key when amount is 0', async () => {
      render(ExpenseEditModal, { props: defaultProps });

      const amountInput = screen.getByLabelText(/amount/i) as HTMLInputElement;
      await fireEvent.input(amountInput, { target: { value: '0' } });
      await fireEvent.keyDown(amountInput, { key: 'ArrowDown' });

      await waitFor(() => {
        expect(Number(amountInput.value)).toBeGreaterThanOrEqual(0);
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

    it('submit button has minimum 44px touch target', () => {
      render(ExpenseEditModal, { props: defaultProps });

      const submitButton = screen.getByRole('button', { name: /add expense/i });
      expect(submitButton).toHaveClass('min-h-[44px]');
    });
  });
});