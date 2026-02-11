<script lang="ts">
  import type { Expense, Split } from '$lib/api/splits';
  import { ChevronDown, ChevronUp } from 'lucide-svelte';

  let {
    expense,
    split,
    expanded = false,
    onToggle,
  }: {
    expense: Expense;
    split: Split;
    expanded?: boolean;
    onToggle?: () => void;
  } = $props();

  function formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IE', {
      style: 'currency',
      currency: 'EUR',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(amount);
  }

  function formatSplitMode(mode: string): string {
    switch (mode) {
      case 'BY_NIGHT': return 'By Night';
      case 'EQUAL': return 'Equal';
      case 'FREE': return 'Free';
      default: return mode;
    }
  }

  function getPayerName(): string {
    return split.participants.find(p => p.id === expense.payerId)?.name || 'Unknown';
  }

  function getParticipantName(participantId: string): string {
    return split.participants.find(p => p.id === participantId)?.name || participantId;
  }

  function formatShareCalculation(participantId: string): string {
    const participant = split.participants.find(p => p.id === participantId);
    if (!participant) return '';

    if (expense.splitMode === 'BY_NIGHT') {
      const totalNights = split.participants.reduce((sum, p) => sum + p.nights, 0);
      return `${participant.nights}/${totalNights} nights`;
    } else if (expense.splitMode === 'EQUAL') {
      return 'Split equally';
    } else if (expense.splitMode === 'FREE') {
      return 'Manual split';
    }
    return '';
  }

  function toggleExpanded() {
    if (onToggle) {
      onToggle();
    }
  }
</script>

<button
  type="button"
  class="w-full text-left p-3 border rounded-lg hover:bg-secondary/50 transition-colors cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
  onclick={toggleExpanded}
  aria-expanded={expanded}
>
  <div class="flex items-center justify-between">
    <div class="flex-1 min-w-0 mr-3">
      <p class="font-medium text-foreground truncate">{expense.description}</p>
      <p class="text-sm text-muted-foreground">Paid by {getPayerName()}</p>
    </div>
    <div class="flex items-center gap-3">
      <div class="text-right shrink-0">
        <p class="font-medium text-foreground">{formatCurrency(expense.amount)}</p>
        <span class="text-xs px-2 py-1 rounded bg-secondary text-secondary-foreground">
          {formatSplitMode(expense.splitMode)}
        </span>
      </div>
      <div class="shrink-0">
        {#if expanded}
          <ChevronUp class="h-5 w-5 text-muted-foreground" />
        {:else}
          <ChevronDown class="h-5 w-5 text-muted-foreground" />
        {/if}
      </div>
    </div>
  </div>

  {#if expanded}
    <div class="mt-4 pt-4 border-t space-y-2" onclick={(e) => e.stopPropagation()}>
      <h4 class="text-sm font-semibold text-foreground mb-2">Share Breakdown</h4>
      {#each expense.shares as share}
        <div class="flex items-center justify-between text-sm py-1">
          <span class="text-muted-foreground">{getParticipantName(share.participantId)}</span>
          <div class="flex items-center gap-3">
            <span class="text-muted-foreground font-mono text-xs">{formatShareCalculation(share.participantId)}</span>
            <span class="font-medium text-foreground font-mono">{formatCurrency(share.amount)}</span>
          </div>
        </div>
      {/each}
      <div class="flex items-center justify-between text-sm pt-2 border-t font-semibold">
        <span class="text-foreground">Total</span>
        <span class="text-foreground font-mono">{formatCurrency(expense.amount)} ✓</span>
      </div>
    </div>
  {/if}
</button>
