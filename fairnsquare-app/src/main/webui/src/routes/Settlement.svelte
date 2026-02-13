<script lang="ts">
  // Settlement Page - Balances & Reimbursement Proposals

  import { getSettlement, type Settlement } from '$lib/api/splits';
  import type { ApiError } from '$lib/api/client';
  import { Button } from '$lib/components/ui/button';
  import * as Card from '$lib/components/ui/card';
  import { addToast } from '$lib/stores/toastStore.svelte';
  import { route, navigate } from '$lib/router';
  import { ArrowLeft } from 'lucide-svelte';

  const splitId = $derived(route.params.splitId || '');

  // Check if we should show resolved view directly (coming from a persisted settlement)
  function checkInitialResolved(): boolean {
    if (typeof window === 'undefined') return false;
    const resolved = sessionStorage.getItem('settlement-resolved') === 'true';
    sessionStorage.removeItem('settlement-resolved');
    return resolved;
  }

  // State
  let settlement = $state<Settlement | null>(null);
  let isLoading = $state(true);
  let showReimbursements = $state(checkInitialResolved());

  // Load settlement data
  $effect(() => {
    if (splitId) {
      loadSettlement(splitId);
    }
  });

  async function loadSettlement(id: string) {
    isLoading = true;
    settlement = null;

    try {
      settlement = await getSettlement(id);
    } catch (err) {
      const apiError = err as ApiError;
      addToast({
        type: 'error',
        message: apiError.detail || 'Failed to load settlement',
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

  function balanceColorClass(balance: number): string {
    if (balance > 0.005) return 'text-green-600';
    if (balance < -0.005) return 'text-red-600';
    return 'text-muted-foreground';
  }

  function balanceLabel(balance: number): string {
    if (balance > 0.005) return `Owed ${formatCurrency(balance)}`;
    if (balance < -0.005) return `Owes ${formatCurrency(Math.abs(balance))}`;
    return 'Settled';
  }

  function handleBack() {
    navigate(`/splits/${splitId}`);
  }

  function handleResolve() {
    showReimbursements = true;
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
      <p class="text-muted-foreground">Loading settlement...</p>
    </div>
  {:else if settlement}
    <!-- Header -->
    <header class="w-full flex items-center">
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
        <h1 class="text-xl font-bold text-primary">Settlement</h1>
      </div>
    </header>

    <!-- Balance Cards -->
    {#if settlement.balances.length === 0}
      <p class="text-muted-foreground text-center py-4">No participants</p>
    {:else}
      <div class="w-full space-y-3">
        {#each settlement.balances as balance (balance.participantId)}
          <Card.Root class="w-full">
            <Card.Content class="py-4">
              <div class="flex items-start justify-between">
                <div class="flex-1">
                  <span class="font-semibold text-lg">{balance.participantName}</span>
                  <div class="flex flex-wrap gap-x-4 gap-y-1 text-sm mt-1">
                    <span class="text-muted-foreground">Paid: <span class="font-medium text-foreground">{formatCurrency(balance.totalPaid)}</span></span>
                    <span class="text-muted-foreground">Cost: <span class="font-medium text-foreground">{formatCurrency(balance.totalCost)}</span></span>
                  </div>
                </div>
                <div class="text-right">
                  <span class="text-sm font-medium {balanceColorClass(balance.balance)}">
                    {balanceLabel(balance.balance)}
                  </span>
                </div>
              </div>

              <!-- Reimbursement details for this participant -->
              {#if showReimbursements}
                {@const outgoing = settlement!.reimbursements.filter(r => r.fromId === balance.participantId)}
                {@const incoming = settlement!.reimbursements.filter(r => r.toId === balance.participantId)}
                {#if outgoing.length > 0 || incoming.length > 0}
                  <div class="mt-3 pt-3 border-t border-border space-y-1">
                    {#each outgoing as r}
                      <p class="text-sm text-red-600">
                        Pay {formatCurrency(r.amount)} to {r.toName}
                      </p>
                    {/each}
                    {#each incoming as r}
                      <p class="text-sm text-green-600">
                        Receive {formatCurrency(r.amount)} from {r.fromName}
                      </p>
                    {/each}
                  </div>
                {/if}
              {/if}
            </Card.Content>
          </Card.Root>
        {/each}
      </div>

      <!-- Resolve Button -->
      {#if !showReimbursements}
        <Button
          onclick={handleResolve}
          class="w-full min-h-[44px]"
        >
          Resolve
        </Button>
      {/if}
    {/if}
  {/if}
</div>
