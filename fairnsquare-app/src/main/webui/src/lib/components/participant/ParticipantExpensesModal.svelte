<script lang="ts">
  import type { Participant, Expense } from '$lib/api/splits';
  import Button from '$lib/components/ui/button/button.svelte';
  import { X, Wallet, Receipt, TrendingUp, TrendingDown, Minus } from 'lucide-svelte';

  let {
    open,
    participant,
    expenses,
    participants,
    onClose,
  }: {
    open: boolean;
    participant: Participant;
    expenses: Expense[];
    participants: Participant[];
    onClose: () => void;
  } = $props();

  // Expenses where participant is payer or has a share
  const participantExpenses = $derived(
    expenses.filter(
      (e) =>
        e.payerId === participant.id ||
        e.shares.some((s) => s.participantId === participant.id)
    )
  );

  function formatSplitMode(mode: string): string {
    switch (mode) {
      case 'BY_NIGHT': return 'By Night';
      case 'BY_SHARE': return 'By Share';
      case 'EQUAL':    return 'Equal';
      case 'FREE':     return 'Free';
      default:         return mode;
    }
  }

  function formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IE', {
      style: 'currency',
      currency: 'EUR',
    }).format(amount);
  }

  function getExpenseRow(expense: Expense): { spent: number; owned: number; balance: number } {
    const spent = expense.payerId === participant.id ? expense.amount : 0;
    const owned = expense.shares.find((s) => s.participantId === participant.id)?.amount ?? 0;
    const balance = spent - owned;
    return { spent, owned, balance };
  }

  function balanceColorClass(balance: number): string {
    if (balance > 0.005) return 'text-green-600';
    if (balance < -0.005) return 'text-red-600';
    return 'text-muted-foreground';
  }

  function handleBackdropClick(event: MouseEvent) {
    if (event.target === event.currentTarget) {
      onClose();
    }
  }

  function handleKeydown(event: KeyboardEvent) {
    if (!open) return;
    if (event.key === 'Escape') {
      onClose();
    }
  }
</script>

<svelte:window onkeydown={handleKeydown} />

{#if open}
  <!-- svelte-ignore a11y_click_events_have_key_events -->
  <!-- svelte-ignore a11y_no_static_element_interactions -->
  <div
    class="fixed inset-0 z-50 bg-black/50 flex items-end justify-center sm:items-center sm:p-4"
    onclick={handleBackdropClick}
    role="dialog"
    aria-modal="true"
    aria-labelledby="participant-expenses-title"
    tabindex="-1"
  >
    <!-- svelte-ignore a11y_click_events_have_key_events a11y_no_static_element_interactions -->
    <div
      class="bg-background w-full max-w-[520px] rounded-t-xl sm:rounded-xl shadow-lg max-h-[calc(100vh-2rem)] flex flex-col animate-in fade-in slide-in-from-bottom-4 sm:zoom-in-95"
      onclick={(e) => e.stopPropagation()}
    >
      <!-- Header -->
      <div class="flex items-center justify-between p-4 border-b shrink-0">
        <h2 id="participant-expenses-title" class="text-lg font-semibold truncate min-w-0 flex-1 mr-2">
          {participant.name}'s Expenses
        </h2>
        <Button
          variant="ghost"
          size="sm"
          onclick={onClose}
          class="min-h-[44px] min-w-[44px] shrink-0"
          aria-label="Close"
        >
          <X class="h-4 w-4" />
        </Button>
      </div>

      <!-- Content -->
      <div class="overflow-y-auto flex-1 p-4 space-y-3">
        {#if participantExpenses.length === 0}
          <p class="text-sm text-muted-foreground text-center py-8">
            No expenses found for {participant.name}.
          </p>
        {:else}
          {#each participantExpenses as expense (expense.id)}
            {@const row = getExpenseRow(expense)}
            <div class="rounded-lg border p-3 space-y-2">
              <!-- Row 1: description + total amount -->
              <div class="flex items-start justify-between gap-2">
                <span class="font-medium truncate min-w-0 flex-1">{expense.description}</span>
                <span class="font-medium shrink-0 tabular-nums">{formatCurrency(expense.amount)}</span>
              </div>
              <!-- Row 2: type badge -->
              <div>
                <span class="text-xs px-2 py-0.5 rounded-full bg-slate-100 text-slate-600">
                  {formatSplitMode(expense.splitMode)}
                </span>
              </div>
              <!-- Row 3: spent / owned / balance -->
              <div class="flex items-center justify-between text-sm pt-1 border-t">
                <span class="flex items-center gap-1 text-muted-foreground" title="Spent">
                  <Wallet class="h-3.5 w-3.5 shrink-0" />
                  <span class="font-medium text-foreground tabular-nums">{formatCurrency(row.spent)}</span>
                </span>
                <span class="flex items-center gap-1 text-muted-foreground" title="Owned">
                  <Receipt class="h-3.5 w-3.5 shrink-0" />
                  <span class="font-medium text-foreground tabular-nums">{formatCurrency(row.owned)}</span>
                </span>
                <span class="flex items-center gap-1 {balanceColorClass(row.balance)}" title="Balance">
                  {#if row.balance > 0.005}
                    <TrendingUp class="h-3.5 w-3.5 shrink-0" />
                  {:else if row.balance < -0.005}
                    <TrendingDown class="h-3.5 w-3.5 shrink-0" />
                  {:else}
                    <Minus class="h-3.5 w-3.5 shrink-0" />
                  {/if}
                  <span class="font-medium tabular-nums">{formatCurrency(Math.abs(row.balance))}</span>
                </span>
              </div>
            </div>
          {/each}
        {/if}
      </div>

      <!-- Footer -->
      <div class="p-4 border-t shrink-0">
        <Button onclick={onClose} class="w-full min-h-[44px]">Close</Button>
      </div>
    </div>
  </div>
{/if}
