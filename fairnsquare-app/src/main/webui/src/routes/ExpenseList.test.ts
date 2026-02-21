import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';
import ExpenseList from './ExpenseList.svelte';
import type { Split as SplitType } from '$lib/api/splits';

// Mock the router
vi.mock('$lib/router', () => {
  return {
    p: vi.fn((path: string) => path),
    navigate: vi.fn(),
    isActive: vi.fn(),
    route: {
      params: { splitId: 'test-split-id-00001' },
      pathname: '/splits/test-split-id-00001/expenses',
    },
  };
});

// Mock the API
vi.mock('$lib/api/splits', () => ({
  getSplit: vi.fn(),
  deleteExpense: vi.fn(),
  addExpense: vi.fn(),
}));

// Mock the toast store
vi.mock('$lib/stores/toastStore.svelte', () => ({
  addToast: vi.fn(),
}));

import { getSplit, deleteExpense } from '$lib/api/splits';
import { navigate, route } from '$lib/router';
import { addToast } from '$lib/stores/toastStore.svelte';

const mockSplitEmpty: SplitType = {
  id: 'test-split-id-00001',
  name: 'Weekend Trip',
  createdAt: '2026-01-24T12:00:00Z',
  participants: [
    { id: 'p1', name: 'Alice', nights: 4 },
    { id: 'p2', name: 'Bob', nights: 2 },
  ],
  expenses: [],
};

const mockSplitWithExpenses: SplitType = {
  id: 'test-split-id-00001',
  name: 'Weekend Trip',
  createdAt: '2026-01-24T12:00:00Z',
  participants: [
    { id: 'p1', name: 'Alice', nights: 4 },
    { id: 'p2', name: 'Bob', nights: 2 },
  ],
  expenses: [
    {
      id: 'e1',
      description: 'Groceries',
      amount: 90.0,
      payerId: 'p1',
      splitMode: 'BY_NIGHT' as const,
      createdAt: '2026-01-25T12:00:00Z',
      shares: [
        { participantId: 'p1', amount: 60.0 },
        { participantId: 'p2', amount: 30.0 },
      ],
    },
    {
      id: 'e2',
      description: 'Dinner',
      amount: 60.0,
      payerId: 'p2',
      splitMode: 'EQUAL' as const,
      createdAt: '2026-01-26T14:00:00Z',
      shares: [
        { participantId: 'p1', amount: 30.0 },
        { participantId: 'p2', amount: 30.0 },
      ],
    },
  ],
};

// Mock data with 3 participants for filter tests
const mockSplitForFilters: SplitType = {
  id: 'test-split-id-00001',
  name: 'Weekend Trip',
  createdAt: '2026-01-24T12:00:00Z',
  participants: [
    { id: 'p1', name: 'Alice', nights: 4 },
    { id: 'p2', name: 'Bob', nights: 2 },
    { id: 'p3', name: 'Charlie', nights: 3 },
  ],
  expenses: [
    {
      id: 'e1',
      description: 'Groceries',
      amount: 90.0,
      payerId: 'p1',
      splitMode: 'BY_NIGHT' as const,
      createdAt: '2026-01-25T12:00:00Z',
      shares: [
        { participantId: 'p1', amount: 40.0 },
        { participantId: 'p2', amount: 50.0 },
      ],
    },
    {
      id: 'e2',
      description: 'Dinner',
      amount: 60.0,
      payerId: 'p2',
      splitMode: 'EQUAL' as const,
      createdAt: '2026-01-26T14:00:00Z',
      shares: [
        { participantId: 'p1', amount: 20.0 },
        { participantId: 'p2', amount: 20.0 },
        { participantId: 'p3', amount: 20.0 },
      ],
    },
    {
      id: 'e3',
      description: 'Taxi',
      amount: 30.0,
      payerId: 'p1',
      splitMode: 'EQUAL' as const,
      createdAt: '2026-01-27T10:00:00Z',
      shares: [
        { participantId: 'p2', amount: 15.0 },
        { participantId: 'p3', amount: 15.0 },
      ],
    },
  ],
};

describe('ExpenseList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (route as any).params = { splitId: 'test-split-id-00001' };
    (route as any).search = {};
  });

  // --- Task 1: Route and Navigation ---

  describe('Navigation', () => {
    it('renders the Expenses title (AC 2)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithExpenses);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByText('Expenses')).toBeInTheDocument();
      });
    });

    it('navigates back to dashboard when back button clicked (AC 8)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithExpenses);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Back to dashboard' })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Back to dashboard' }));
      expect(navigate).toHaveBeenCalledWith('/splits/test-split-id-00001');
    });

    it('shows add button in header (AC 2)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithExpenses);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Add expense' })).toBeInTheDocument();
      });
    });
  });

  // --- Task 2: Screen Layout and Header ---

  describe('Screen Layout', () => {
    it('shows summary bar with expense count and total (AC 2)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithExpenses);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByText('2 total expenses')).toBeInTheDocument();
      });
      expect(screen.getByText(/€150\.00/)).toBeInTheDocument();
    });

    it('shows loading state initially', () => {
      vi.mocked(getSplit).mockImplementation(() => new Promise(() => {}));
      render(ExpenseList);

      expect(screen.getByText('Loading expenses...')).toBeInTheDocument();
    });
  });

  // --- Task 3: Empty State ---

  describe('Empty State', () => {
    it('shows empty state when no expenses (AC 3)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitEmpty);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByText('No expenses yet')).toBeInTheDocument();
      });
      expect(screen.getByText('Tap + to add your first expense')).toBeInTheDocument();
    });
  });

  // --- Task 4: Expense Card List ---

  describe('Expense Cards', () => {
    it('renders expense cards with description and amount (AC 4)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithExpenses);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByText('Groceries')).toBeInTheDocument();
      });
      expect(screen.getByText('Dinner')).toBeInTheDocument();
    });

    it('shows payer name on expense cards (AC 4)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithExpenses);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByText('Paid by Alice')).toBeInTheDocument();
      });
      expect(screen.getByText('Paid by Bob')).toBeInTheDocument();
    });

    it('shows split mode icon and text (AC 4)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithExpenses);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByText(/By Night/)).toBeInTheDocument();
      });
      expect(screen.getByText(/Equal/)).toBeInTheDocument();
    });

    it('shows participants as Everyone when all participate (AC 4)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithExpenses);
      render(ExpenseList);

      await waitFor(() => {
        const everyoneLabels = screen.getAllByText('Everyone');
        expect(everyoneLabels.length).toBeGreaterThanOrEqual(1);
      });
    });

    it('shows edit and delete buttons on each card (AC 4)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithExpenses);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Edit expense: Groceries' })).toBeInTheDocument();
      });
      expect(screen.getByRole('button', { name: 'Delete expense: Groceries' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Edit expense: Dinner' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Delete expense: Dinner' })).toBeInTheDocument();
    });

    it('orders expenses reverse chronological - newest first (AC 4)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithExpenses);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByText('Dinner')).toBeInTheDocument();
      });

      // Dinner (Jan 26) should appear before Groceries (Jan 25)
      const descriptions = screen.getAllByTestId('expense-description');
      expect(descriptions[0].textContent).toBe('Dinner');
      expect(descriptions[1].textContent).toBe('Groceries');
    });
  });

  // --- Task 5: Delete Expense Flow ---

  describe('Delete Expense', () => {
    it('shows confirmation dialog when delete clicked (AC 5)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithExpenses);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Delete expense: Groceries' })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Delete expense: Groceries' }));

      await waitFor(() => {
        expect(screen.getByText('Delete this expense?')).toBeInTheDocument();
        expect(screen.getByText('This will recalculate balances.')).toBeInTheDocument();
      });
    });

    it('closes dialog without deleting on cancel (AC 5)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithExpenses);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Delete expense: Groceries' })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Delete expense: Groceries' }));

      await waitFor(() => {
        expect(screen.getByText('Delete this expense?')).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));

      await waitFor(() => {
        expect(screen.queryByText('Delete this expense?')).not.toBeInTheDocument();
      });
      expect(deleteExpense).not.toHaveBeenCalled();
    });

    it('calls API and updates list on confirm (AC 5)', async () => {
      const updatedSplit: SplitType = {
        ...mockSplitWithExpenses,
        expenses: [mockSplitWithExpenses.expenses[1]], // Only Dinner remains
      };

      vi.mocked(getSplit)
        .mockResolvedValueOnce(mockSplitWithExpenses)
        .mockResolvedValueOnce(updatedSplit);
      vi.mocked(deleteExpense).mockResolvedValue(undefined);

      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Delete expense: Groceries' })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Delete expense: Groceries' }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Delete' })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

      await waitFor(() => {
        expect(deleteExpense).toHaveBeenCalledWith('test-split-id-00001', 'e1');
      });

      await waitFor(() => {
        expect(addToast).toHaveBeenCalledWith({
          type: 'success',
          message: 'Expense deleted',
        });
      });
    });

    it('shows error toast on delete API failure (AC 6)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithExpenses);
      vi.mocked(deleteExpense).mockRejectedValue({
        status: 500,
        detail: 'Internal server error',
      });

      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Delete expense: Groceries' })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Delete expense: Groceries' }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Delete' })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

      await waitFor(() => {
        expect(addToast).toHaveBeenCalledWith({
          type: 'error',
          message: 'Internal server error',
        });
      });

      // Expense should still be in the list
      expect(screen.getByText('Groceries')).toBeInTheDocument();
    });

    it('updates summary bar after successful delete (AC 9)', async () => {
      const updatedSplit: SplitType = {
        ...mockSplitWithExpenses,
        expenses: [mockSplitWithExpenses.expenses[1]], // Only Dinner remains (€60)
      };

      vi.mocked(getSplit)
        .mockResolvedValueOnce(mockSplitWithExpenses)
        .mockResolvedValueOnce(updatedSplit);
      vi.mocked(deleteExpense).mockResolvedValue(undefined);

      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByText('2 total expenses')).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Delete expense: Groceries' }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Delete' })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

      await waitFor(() => {
        expect(screen.getByText('1 total expense')).toBeInTheDocument();
      });
    });
  });

  // --- Task 6: Add Expense Modal ---

  describe('Add Expense Modal', () => {
    it('opens Add Expense Modal when + button clicked (AC 7)', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitWithExpenses);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Add expense' })).toBeInTheDocument();
      });

      await fireEvent.click(screen.getByRole('button', { name: 'Add expense' }));

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: 'Add Expense' })).toBeInTheDocument();
      });
    });
  });

  // --- Task 7: Expense List Filtering ---

  describe('Filtering', () => {
    it('renders filter bar with payer and beneficiary selects', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitForFilters);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByLabelText('Filter by payer')).toBeInTheDocument();
      });
      expect(screen.getByLabelText('Filter by beneficiary')).toBeInTheDocument();
    });

    it('shows all expenses when no filters are set', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitForFilters);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByText('Groceries')).toBeInTheDocument();
      });
      expect(screen.getByText('Dinner')).toBeInTheDocument();
      expect(screen.getByText('Taxi')).toBeInTheDocument();
      expect(screen.getByText('3 total expenses')).toBeInTheDocument();
    });

    it('filters by payer via URL query param', async () => {
      (route as any).search = { payer: 'p1' };
      vi.mocked(getSplit).mockResolvedValue(mockSplitForFilters);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByText('Groceries')).toBeInTheDocument();
      });
      expect(screen.getByText('Taxi')).toBeInTheDocument();
      // Dinner is paid by p2, should be hidden
      expect(screen.queryByText('Dinner')).not.toBeInTheDocument();
    });

    it('filters by beneficiary via URL query param', async () => {
      (route as any).search = { beneficiary: 'p3' };
      vi.mocked(getSplit).mockResolvedValue(mockSplitForFilters);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByText('Dinner')).toBeInTheDocument();
      });
      expect(screen.getByText('Taxi')).toBeInTheDocument();
      // Groceries only has p1 and p2 shares, should be hidden
      expect(screen.queryByText('Groceries')).not.toBeInTheDocument();
    });

    it('filters by both payer and beneficiary combined', async () => {
      (route as any).search = { payer: 'p1', beneficiary: 'p3' };
      vi.mocked(getSplit).mockResolvedValue(mockSplitForFilters);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByText('Taxi')).toBeInTheDocument();
      });
      // Groceries: paid by p1 but p3 is not a beneficiary
      expect(screen.queryByText('Groceries')).not.toBeInTheDocument();
      // Dinner: p3 is beneficiary but paid by p2
      expect(screen.queryByText('Dinner')).not.toBeInTheDocument();
    });

    it('updates summary bar to show filtered count', async () => {
      (route as any).search = { payer: 'p2' };
      vi.mocked(getSplit).mockResolvedValue(mockSplitForFilters);
      render(ExpenseList);

      await waitFor(() => {
        // Should show "1 of 3 expenses" when filtered
        expect(screen.getByText(/1 of 3/)).toBeInTheDocument();
      });
      // Total should reflect only filtered expense (Dinner = €60)
      expect(screen.getByText(/€60\.00 total/)).toBeInTheDocument();
    });

    it('shows "No matching expenses" when filter has no results', async () => {
      // p3 never paid for anything
      (route as any).search = { payer: 'p3' };
      vi.mocked(getSplit).mockResolvedValue(mockSplitForFilters);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByText('No matching expenses')).toBeInTheDocument();
      });
      expect(screen.getByText('Try adjusting your filters')).toBeInTheDocument();
      // Two "Clear filters" buttons: one in the filter bar (X icon) and one in the empty state
      const clearButtons = screen.getAllByRole('button', { name: 'Clear filters' });
      expect(clearButtons.length).toBe(2);
    });

    it('shows clear filters button when filters are active', async () => {
      (route as any).search = { payer: 'p1' };
      vi.mocked(getSplit).mockResolvedValue(mockSplitForFilters);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByLabelText('Clear filters')).toBeInTheDocument();
      });
    });

    it('does not show clear filters button when no filters active', async () => {
      vi.mocked(getSplit).mockResolvedValue(mockSplitForFilters);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByLabelText('Filter by payer')).toBeInTheDocument();
      });
      expect(screen.queryByLabelText('Clear filters')).not.toBeInTheDocument();
    });

    it('filters expenses and updates URL when payer select is changed', async () => {
      const replaceStateSpy = vi.spyOn(window.history, 'replaceState');
      vi.mocked(getSplit).mockResolvedValue(mockSplitForFilters);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByText('Groceries')).toBeInTheDocument();
      });

      const payerSelect = screen.getByLabelText('Filter by payer');
      await fireEvent.change(payerSelect, { target: { value: 'p2' } });

      // List should update reactively
      await waitFor(() => {
        expect(screen.getByText('Dinner')).toBeInTheDocument();
      });
      expect(screen.queryByText('Groceries')).not.toBeInTheDocument();
      expect(screen.queryByText('Taxi')).not.toBeInTheDocument();

      // URL should be updated
      expect(replaceStateSpy).toHaveBeenCalled();
      const lastCall = replaceStateSpy.mock.calls[replaceStateSpy.mock.calls.length - 1];
      const updatedUrl = new URL(lastCall[2] as string);
      expect(updatedUrl.searchParams.get('payer')).toBe('p2');

      replaceStateSpy.mockRestore();
    });

    it('filters expenses and updates URL when beneficiary select is changed', async () => {
      const replaceStateSpy = vi.spyOn(window.history, 'replaceState');
      vi.mocked(getSplit).mockResolvedValue(mockSplitForFilters);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByText('Groceries')).toBeInTheDocument();
      });

      const beneficiarySelect = screen.getByLabelText('Filter by beneficiary');
      await fireEvent.change(beneficiarySelect, { target: { value: 'p1' } });

      // p1 is in shares of Groceries (p1,p2) and Dinner (p1,p2,p3) but NOT Taxi (p2,p3)
      await waitFor(() => {
        expect(screen.getByText('Groceries')).toBeInTheDocument();
      });
      expect(screen.getByText('Dinner')).toBeInTheDocument();
      expect(screen.queryByText('Taxi')).not.toBeInTheDocument();

      // URL should be updated
      expect(replaceStateSpy).toHaveBeenCalled();
      const lastCall = replaceStateSpy.mock.calls[replaceStateSpy.mock.calls.length - 1];
      const updatedUrl = new URL(lastCall[2] as string);
      expect(updatedUrl.searchParams.get('beneficiary')).toBe('p1');

      replaceStateSpy.mockRestore();
    });

    it('payer select reflects URL param value', async () => {
      (route as any).search = { payer: 'p1' };
      vi.mocked(getSplit).mockResolvedValue(mockSplitForFilters);
      render(ExpenseList);

      await waitFor(() => {
        const payerSelect = screen.getByLabelText('Filter by payer') as HTMLSelectElement;
        expect(payerSelect.value).toBe('p1');
      });
    });

    it('handles FREE mode beneficiary filtering correctly', async () => {
      const mockWithFree: SplitType = {
        ...mockSplitForFilters,
        expenses: [
          {
            id: 'e4',
            description: 'Custom Split',
            amount: 100.0,
            payerId: 'p1',
            splitMode: 'FREE' as const,
            createdAt: '2026-01-28T10:00:00Z',
            shares: [
              { participantId: 'p1', amount: 50.0, parts: 2 },
              { participantId: 'p2', amount: 50.0, parts: 0 }, // inactive share
              { participantId: 'p3', amount: 0.0, parts: 3 },
            ],
          },
        ],
      };

      // Filter by p2 as beneficiary - should NOT match because p2 has parts=0 in FREE mode
      (route as any).search = { beneficiary: 'p2' };
      vi.mocked(getSplit).mockResolvedValue(mockWithFree);
      render(ExpenseList);

      await waitFor(() => {
        expect(screen.getByText('No matching expenses')).toBeInTheDocument();
      });
    });
  });
});