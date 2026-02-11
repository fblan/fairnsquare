<script lang="ts">
  // Expense List Screen - Story FNS-002-5

  import { getSplit, deleteExpense, type Split, type Expense, type SplitMode } from '$lib/api/splits';
  import type { ApiError } from '$lib/api/client';
  import { Button } from '$lib/components/ui/button';
  import * as Card from '$lib/components/ui/card';
  import ConfirmDialog from '$lib/components/ui/confirm-dialog/confirm-dialog.svelte';
  import ExpenseEditModal from '$lib/components/expense/ExpenseEditModal.svelte';
  import { addToast } from '$lib/stores/toastStore.svelte';
  import { route, navigate } from '$lib/router';
  import { ArrowLeft, Plus, Pencil, Trash2, Receipt } from 'lucide-svelte';

  const splitId = $derived(route.params.splitId || '');

  // State
  let split = $state<Split | null>(null);
  let isLoading = $state(true);

  // Delete confirmation state
  let showDeleteConfirm = $state(false);
  let expenseToDelete = $state<Expense | null>(null);
  let isDeleting = $state(false);

  // Add expense modal state
  let showAddExpense = $state(false);

  // Edit expense modal state
  let showEditExpense = $state(false);
  let expenseToEdit = $state<Expense | null>(null);

  // Derived values
  const sortedExpenses = $derived(
    split?.expenses
      ? [...split.expenses].sort(
          (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        )
      : []
  );

  const expenseCount = $derived(sortedExpenses.length);
  const expenseTotal = $derived(sortedExpenses.reduce((sum, e) => sum + e.amount, 0));

  // Load split data
  $effect(() => {
    if (splitId) {
      loadSplit(splitId);
    }
  });

  async function loadSplit(id: string) {
    isLoading = true;
    split = null;

    try {
      split = await getSplit(id);
    } catch (err) {
      const apiError = err as ApiError;
      addToast({
        type: 'error',
        message: apiError.detail || 'Failed to load expenses',
      });
    } finally {
      isLoading = false;
    }
  }

  function formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IE', {
      style: 'currency',
      currency: 'EUR',
    }).format(amount);
  }

  function formatDate(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffHours = diffMs / (1000 * 60 * 60);

    // Handle future dates defensively
    if (diffMs < 0) {
      return date.toLocaleDateString('en-IE', { month: 'short', day: 'numeric' });
    }

    if (diffHours < 1) return 'Just now';
    if (diffHours < 24) return `${Math.floor(diffHours)}h ago`;
    if (diffHours < 48) return 'Yesterday';

    return date.toLocaleDateString('en-IE', { month: 'short', day: 'numeric' });
  }

  function splitModeIcon(mode: SplitMode): string {
    switch (mode) {
      case 'BY_NIGHT':
        return '\u{1F319}';
      case 'EQUAL':
        return '\u229C';
      case 'FREE':
        return '\u{1F4CA}';
    }
  }

  function splitModeText(mode: SplitMode): string {
    switch (mode) {
      case 'BY_NIGHT':
        return 'By Night';
      case 'EQUAL':
        return 'Equal';
      case 'FREE':
        return 'By Share';
    }
  }

  function getPayerName(payerId: string): string {
    return split?.participants.find((p) => p.id === payerId)?.name || 'Unknown';
  }

  function getParticipantNames(expense: Expense): string {
    if (!split) return '';
    const participantIds = expense.shares.map((s) => s.participantId);
    if (participantIds.length === split.participants.length) return 'Everyone';
    return participantIds
      .map((id) => split!.participants.find((p) => p.id === id)?.name || 'Unknown')
      .join(', ');
  }

  function handleBack() {
    navigate(`/splits/${splitId}`);
  }

  function handleDeleteClick(expense: Expense) {
    if (!expense || !expense.id) {
      addToast({ type: 'error', message: 'Invalid expense' });
      return;
    }
    expenseToDelete = expense;
    showDeleteConfirm = true;
  }

  async function handleConfirmDelete() {
    if (!expenseToDelete) return;
    isDeleting = true;

    try {
      await deleteExpense(splitId, expenseToDelete.id);
      addToast({ type: 'success', message: 'Expense deleted' });
      showDeleteConfirm = false;
      expenseToDelete = null;
      await loadSplit(splitId);
    } catch (err: unknown) {
      const error = err as ApiError;
      addToast({
        type: 'error',
        message: error.detail || 'Failed to delete expense',
      });
    } finally {
      isDeleting = false;
    }
  }

  function handleCancelDelete() {
    showDeleteConfirm = false;
    expenseToDelete = null;
  }

  function handleAddExpense() {
    showAddExpense = true;
  }

  function handleCloseAddExpense() {
    showAddExpense = false;
  }

  async function handleAddExpenseSuccess() {
    await loadSplit(splitId);
  }

  function handleEditClick(expense: Expense) {
    if (!expense || !expense.id) {
      addToast({ type: 'error', message: 'Invalid expense' });
      return;
    }
    expenseToEdit = expense;
    showEditExpense = true;
  }

  function handleCloseEditExpense() {
    showEditExpense = false;
    expenseToEdit = null;
  }

  async function handleEditExpenseSuccess() {
    await loadSplit(splitId);
  }
</script>

<div class="flex flex-col items-center space-y-4 w-full max-w-[420px] mx-auto">
  {#if isLoading}
    <div class="flex flex-col items-center justify-center py-12 space-y-4">
      <svg
        class="animate-spin h-8 w-8 text-primary"
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
      >
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"
        ></circle>
        <path
          class="opacity-75"
          fill="currentColor"
          d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
        ></path>
      </svg>
      <p class="text-muted-foreground">Loading expenses...</p>
    </div>
  {:else if split}
    <!-- Header -->
    <header class="w-full flex items-center justify-between">
      <div class="flex items-center gap-2">
        <Button
          variant="ghost"
          size="sm"
          onclick={handleBack}
          class="min-h-[44px] min-w-[44px]"
          aria-label="Back to dashboard"
        >
          <ArrowLeft class="h-5 w-5" />
        </Button>
        <h1 class="text-xl font-bold text-primary">Expenses</h1>
      </div>
      <Button
        variant="ghost"
        size="sm"
        onclick={handleAddExpense}
        class="min-h-[44px] min-w-[44px]"
        aria-label="Add expense"
      >
        <Plus class="h-5 w-5" />
      </Button>
    </header>

    <!-- Summary Bar -->
    <section class="w-full">
      <div class="bg-teal-50/50 border border-teal-200 rounded-lg px-4 py-3">
        <div class="flex items-center justify-between">
          <span class="text-sm font-medium"
            >{expenseCount} total {expenseCount === 1 ? 'expense' : 'expenses'}</span
          >
          <span class="text-sm font-semibold text-primary">{formatCurrency(expenseTotal)} total</span
          >
        </div>
      </div>
    </section>

    {#if sortedExpenses.length === 0}
      <!-- Empty State -->
      <div class="flex flex-col items-center justify-center py-12 space-y-3">
        <Receipt class="h-12 w-12 text-muted-foreground/50" />
        <h2 class="text-lg font-semibold text-muted-foreground">No expenses yet</h2>
        <p class="text-sm text-muted-foreground">Tap + to add your first expense</p>
      </div>
    {:else}
      <!-- Expense Cards -->
      <div class="w-full space-y-3">
        {#each sortedExpenses as expense (expense.id)}
          <Card.Root class="w-full">
            <Card.Content class="py-3 px-4">
              <!-- Row 1: Description + Amount -->
              <div class="flex items-start justify-between mb-1">
                <span data-testid="expense-description" class="font-bold text-sm"
                  >{expense.description}</span
                >
                <span class="text-sm font-semibold text-primary"
                  >{formatCurrency(expense.amount)}</span
                >
              </div>

              <!-- Row 2: Payer + Date -->
              <div class="flex items-center justify-between mb-2">
                <span class="text-xs text-muted-foreground"
                  >Paid by {getPayerName(expense.payerId)}</span
                >
                <span class="text-xs text-muted-foreground">{formatDate(expense.createdAt)}</span>
              </div>

              <!-- Row 3: Split Mode + Participants -->
              <div class="flex items-center justify-between mb-2">
                <span class="text-xs text-muted-foreground">
                  {splitModeIcon(expense.splitMode)}
                  {splitModeText(expense.splitMode)}
                </span>
                <span class="text-xs text-muted-foreground">{getParticipantNames(expense)}</span>
              </div>

              <!-- Row 4: Actions -->
              <div class="flex items-center justify-end gap-1">
                <Button
                  variant="ghost"
                  size="sm"
                  onclick={() => handleEditClick(expense)}
                  class="min-h-[44px] min-w-[44px]"
                  aria-label="Edit expense: {expense.description}"
                >
                  <Pencil class="h-4 w-4" />
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onclick={() => handleDeleteClick(expense)}
                  class="min-h-[44px] min-w-[44px] text-destructive hover:text-destructive"
                  aria-label="Delete expense: {expense.description}"
                >
                  <Trash2 class="h-4 w-4" />
                </Button>
              </div>
            </Card.Content>
          </Card.Root>
        {/each}
      </div>
    {/if}
  {/if}
</div>

<!-- Delete Confirmation Dialog -->
<ConfirmDialog
  open={showDeleteConfirm}
  title="Delete this expense?"
  description="This will recalculate balances."
  confirmLabel="Delete"
  cancelLabel="Cancel"
  isLoading={isDeleting}
  onConfirm={handleConfirmDelete}
  onCancel={handleCancelDelete}
/>

<!-- Add Expense Modal -->
{#if split}
  <ExpenseEditModal
    open={showAddExpense}
    {splitId}
    preselectedPayerId={null}
    participants={split.participants}
    onClose={handleCloseAddExpense}
    onSuccess={handleAddExpenseSuccess}
  />
{/if}

<!-- Edit Expense Modal -->
{#if split}
  <ExpenseEditModal
    open={showEditExpense}
    {splitId}
    expense={expenseToEdit}
    participants={split.participants}
    onClose={handleCloseEditExpense}
    onSuccess={handleEditExpenseSuccess}
  />
{/if}