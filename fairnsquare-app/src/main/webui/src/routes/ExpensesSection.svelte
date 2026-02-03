<script lang="ts">
  import { addExpense, type Split, type SplitMode } from '$lib/api/splits';
  import Button from '$lib/components/ui/button/button.svelte';
  import * as Card from '$lib/components/ui/card';
  import Input from '$lib/components/ui/input/input.svelte';
  import Label from '$lib/components/ui/label/label.svelte';
  import * as Select from '$lib/components/ui/select';
  import * as RadioGroup from '$lib/components/ui/radio-group';
  import { addToast } from '$lib/stores/toastStore.svelte';
  import { Plus, Moon, Equal } from 'lucide-svelte';
  import { ExpenseCard } from '$lib/components/ui/expense-card';

  let {
    split,
    onSplitUpdated,
  }: {
    split: Split;
    onSplitUpdated: () => Promise<void>;
  } = $props();

  // Add Expense form state (Story 4.1)
  let showAddExpenseForm = $state(false);
  let expenseAmount = $state<number | ''>('');
  let expenseDescription = $state('');
  let expensePayerId = $state('');
  let expensePayerSelected = $state({ value: '', label: '' });
  let expenseSplitMode = $state<SplitMode>('BY_NIGHT');
  let expenseValidationErrors = $state<{amount?: string; description?: string; payer?: string}>({});
  let isExpenseSubmitting = $state(false);
  
  // Track expanded expenses per expense ID (Story 4.2)
  let expandedExpenses = $state<Map<string, boolean>>(new Map());

  const canAddExpense = $derived(split.participants.length > 0);

  // Debug: Log when component mounts
  $effect(() => {
    console.log('ExpensesSection mounted, split:', split.name, 'expenses:', split.expenses.length);
  });

  // Helper functions
  function formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IE', {
      style: 'currency',
      currency: 'EUR',
    }).format(amount);
  }

  function formatSplitMode(mode: SplitMode): string {
    switch (mode) {
      case 'BY_NIGHT': return 'By Night';
      case 'EQUAL': return 'Equal';
      case 'FREE': return 'Free';
      default: return mode;
    }
  }

  function getPayerName(expense: any): string {
    return split.participants.find(p => p.id === expense.payerId)?.name || 'Unknown';
  }

  // Sort expenses by createdAt (newest first)
  const sortedExpenses = $derived(
    split.expenses ? [...split.expenses].sort((a, b) =>
      new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    ) : []
  );

  // Add Expense handlers (Story 4.1)
  function handleShowAddExpenseForm() {
    if (!split || split.participants.length === 0) return;
    showAddExpenseForm = true;
    expenseAmount = '';
    expenseDescription = '';
    const firstParticipant = split.participants[0];
    expensePayerId = firstParticipant?.id || '';
    expensePayerSelected = { value: firstParticipant?.id || '', label: firstParticipant?.name || '' };
    expenseSplitMode = 'BY_NIGHT';
    expenseValidationErrors = {};
  }

  function handleCancelAddExpenseForm() {
    showAddExpenseForm = false;
    expenseAmount = '';
    expenseDescription = '';
    expensePayerId = '';
    expensePayerSelected = { value: '', label: '' };
    expenseSplitMode = 'BY_NIGHT';
    expenseValidationErrors = {};
  }

  function validateExpenseForm(): boolean {
    const errors: {amount?: string; description?: string; payer?: string} = {};

    const amount = typeof expenseAmount === 'number' ? expenseAmount : parseFloat(expenseAmount as string);
    if (isNaN(amount) || amount < 0.01) {
      errors.amount = 'Amount must be at least €0.01';
    }

    if (!expenseDescription.trim()) {
      errors.description = 'Description is required';
    } else if (expenseDescription.trim().length > 200) {
      errors.description = 'Description cannot exceed 200 characters';
    }

    if (!expensePayerId) {
      errors.payer = 'Payer is required';
    }

    expenseValidationErrors = errors;
    return Object.keys(errors).length === 0;
  }

  async function handleAddExpense() {
    if (!validateExpenseForm()) return;
    if (!split.id) return;

    isExpenseSubmitting = true;

    try {
      const amount = typeof expenseAmount === 'number' ? expenseAmount : parseFloat(expenseAmount as string);
      await addExpense(split.id, {
        amount,
        description: expenseDescription.trim(),
        payerId: expensePayerId,
        splitMode: expenseSplitMode,
      });

      showAddExpenseForm = false;
      expenseAmount = '';
      expenseDescription = '';
      expensePayerId = '';
      expensePayerSelected = { value: '', label: '' };
      expenseSplitMode = 'BY_NIGHT';
      expenseValidationErrors = {};
      await onSplitUpdated();

      addToast({
        type: 'success',
        message: 'Expense added',
      });
    } catch (err: any) {
      addToast({
        type: 'error',
        message: err.detail || 'Failed to add expense. Please try again.',
      });
    } finally {
      isExpenseSubmitting = false;
    }
  }
</script>

<!-- Expenses Section (AC 1, 2, 4) -->
<section class="w-full">
  <Card.Root>
    <Card.Header class="pb-2 flex flex-row items-center justify-between">
      <Card.Title class="text-lg">Expenses</Card.Title>
      <Button
        onclick={handleShowAddExpenseForm}
        disabled={!canAddExpense}
        size="sm"
        class="min-h-[44px]"
      >
        <Plus class="h-4 w-4 mr-2" />
        Add Expense
      </Button>
    </Card.Header>
    <Card.Content>
      {#if !canAddExpense}
        <p class="text-muted-foreground text-center py-4">Add participants before adding expenses</p>
      {:else if showAddExpenseForm}
        <!-- Add Expense Form (AC 2, 6, 7, 8, 14) -->
        <form onsubmit={(e) => { e.preventDefault(); handleAddExpense(); }} class="space-y-4 mb-4 p-4 bg-secondary/30 rounded-lg">
          <div class="space-y-2">
            <Label for="expense-amount">Amount (€)</Label>
            <Input
              id="expense-amount"
              type="number"
              step="0.01"
              min="0.01"
              placeholder="0.00"
              bind:value={expenseAmount}
              class="min-h-[44px]"
              aria-invalid={!!expenseValidationErrors.amount}
            />
            {#if expenseValidationErrors.amount}
              <p class="text-sm text-destructive">{expenseValidationErrors.amount}</p>
            {/if}
          </div>

          <div class="space-y-2">
            <Label for="expense-description">Description</Label>
            <Input
              id="expense-description"
              type="text"
              placeholder="What was this expense for?"
              bind:value={expenseDescription}
              class="min-h-[44px]"
              aria-invalid={!!expenseValidationErrors.description}
            />
            {#if expenseValidationErrors.description}
              <p class="text-sm text-destructive">{expenseValidationErrors.description}</p>
            {/if}
          </div>

          <div class="space-y-2">
            <Label for="expense-payer">Payer</Label>
            <Select.Root
              bind:selected={expensePayerSelected}
              onSelectedChange={(selected) => { 
                if (selected) {
                  expensePayerId = selected.value;
                  expensePayerSelected = selected;
                }
              }}
            >
              <Select.Trigger id="expense-payer" class="min-h-[44px]" aria-invalid={!!expenseValidationErrors.payer}>
                <Select.Value placeholder="Select payer" />
              </Select.Trigger>
              <Select.Content>
                {#each split.participants as participant}
                  <Select.Item value={participant.id} label={participant.name}>{participant.name}</Select.Item>
                {/each}
              </Select.Content>
            </Select.Root>
            {#if expenseValidationErrors.payer}
              <p class="text-sm text-destructive">{expenseValidationErrors.payer}</p>
            {/if}
          </div>

          <div class="space-y-2">
            <Label>Split Mode</Label>
            <RadioGroup.Root bind:value={expenseSplitMode} class="flex flex-col space-y-2">
              <div class="flex items-center space-x-2 p-3 border rounded-lg hover:bg-accent min-h-[44px]">
                <RadioGroup.Item value="BY_NIGHT" id="mode-by-night" />
                <Label for="mode-by-night" class="flex items-center gap-2 cursor-pointer flex-1">
                  <Moon class="h-4 w-4" aria-label="Night icon" />
                  <span>By Night (proportional to nights stayed)</span>
                </Label>
              </div>
              <div class="flex items-center space-x-2 p-3 border rounded-lg hover:bg-accent min-h-[44px]">
                <RadioGroup.Item value="EQUAL" id="mode-equal" />
                <Label for="mode-equal" class="flex items-center gap-2 cursor-pointer flex-1">
                  <Equal class="h-4 w-4" aria-label="Equal icon" />
                  <span>Equal (split evenly among all participants)</span>
                </Label>
              </div>
              <!-- FREE mode temporarily removed until Story 4.3 implements manual share specification -->
              <!-- Uncomment when Story 4.3 is implemented:
              <div class="flex items-center space-x-2 p-3 border rounded-lg hover:bg-accent min-h-[44px]">
                <RadioGroup.Item value="FREE" id="mode-free" />
                <Label for="mode-free" class="flex items-center gap-2 cursor-pointer flex-1">
                  <Edit3 class="h-4 w-4" aria-hidden="true" />
                  <span>Free (manual share specification)</span>
                </Label>
              </div>
              -->
            </RadioGroup.Root>
          </div>

          <div class="flex gap-2 pt-2">
            <Button type="submit" disabled={isExpenseSubmitting} class="flex-1 min-h-[44px]">
              {isExpenseSubmitting ? 'Adding...' : 'Add'}
            </Button>
            <Button type="button" variant="outline" onclick={handleCancelAddExpenseForm} disabled={isExpenseSubmitting} class="flex-1 min-h-[44px]">
              Cancel
            </Button>
          </div>
        </form>
      {:else}
        <!-- Expenses List (AC 4, Story 4.2 AC 5) -->
        {#if sortedExpenses.length === 0}
          <div class="text-sm text-muted-foreground text-center py-4">No expenses yet</div>
        {:else}
          <div class="space-y-2">
            {#each sortedExpenses as expense (expense.id)}
              {@const isExpanded = expandedExpenses.get(expense.id) || false}
              <ExpenseCard 
                {expense} 
                {split} 
                expanded={isExpanded}
                onToggle={() => {
                  expandedExpenses.set(expense.id, !isExpanded);
                  expandedExpenses = new Map(expandedExpenses);
                }}
              />
            {/each}
          </div>
        {/if}
      {/if}
    </Card.Content>
  </Card.Root>
</section>
