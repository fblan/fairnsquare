<script lang="ts">
  // Settlement Page - Balances & Reimbursement Proposals

  import { getSplit, getSettlement, updateParticipant, type Settlement, type Split } from '$lib/api/splits';
  import type { ApiError } from '$lib/api/client';
  import { Button } from '$lib/components/ui/button';
  import * as Card from '$lib/components/ui/card';
  import { addToast } from '$lib/stores/toastStore.svelte';
  import { route, navigate } from '$lib/router';
  import SplitPageHeader from '$lib/components/ui/split-page-header/SplitPageHeader.svelte';

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
  let split = $state<Split | null>(null);
  let splitName = $state('');
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
      const [settlementData, splitData] = await Promise.all([getSettlement(id), getSplit(id)]);
      settlement = settlementData;
      split = splitData;
      splitName = splitData.name;
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


  function handleResolve() {
    showReimbursements = true;
  }

  async function handlePreferredCreditorChange(participantId: string, creditorId: string) {
    if (!split) return;
    const participant = split.participants.find(p => p.id === participantId);
    if (!participant) return;
    try {
      await updateParticipant(splitId, participantId, {
        name: participant.name,
        nights: participant.nights,
        share: participant.share,
        preferredCreditorId: creditorId || null,
      });
      const refreshed = await getSettlement(splitId);
      participant.preferredCreditorId = creditorId || null;
      settlement = refreshed;
    } catch (err: any) {
      addToast({ type: 'error', message: err.detail || 'Failed to save preference' });
    }
  }
</script>

<div class="flex flex-col items-center space-y-4 w-full max-w-[520px] mx-auto">
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
    <SplitPageHeader splitName={splitName} {splitId} showBackButton />

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

              <!-- Preferred creditor select for debtors -->
              {#if balance.balance < -0.005}
                {@const participant = split?.participants.find(p => p.id === balance.participantId)}
                <div class="mt-2 pt-2 border-t border-border flex items-center gap-2">
                  <label class="text-xs text-muted-foreground whitespace-nowrap" for="preferred-{balance.participantId}">
                    Reimburse first:
                  </label>
                  <select
                    id="preferred-{balance.participantId}"
                    aria-label="Preferred creditor for {balance.participantName}"
                    value={participant?.preferredCreditorId ?? ''}
                    onchange={(e) => handlePreferredCreditorChange(balance.participantId, e.currentTarget.value)}
                    disabled={showReimbursements}
                    class="flex-1 h-8 rounded-md border border-input bg-background px-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <option value="">No preference</option>
                    {#each settlement.balances.filter(b => b.balance > 0.005) as creditor}
                      <option value={creditor.participantId}>{creditor.participantName}</option>
                    {/each}
                  </select>
                </div>
              {/if}

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
