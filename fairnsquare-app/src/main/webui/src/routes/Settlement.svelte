<script lang="ts">
  // Settlement Page - Balances & Reimbursement Proposals

  import { getSplit, resolveSettlement, unsettleSettlement, updateParticipant, type Settlement, type Split } from '$lib/api/splits';
  import type { ApiError } from '$lib/api/client';
  import { Button } from '$lib/components/ui/button';
  import * as Card from '$lib/components/ui/card';
  import { addToast } from '$lib/stores/toastStore.svelte';
  import { route, navigate } from '$lib/router';
  import SplitPageHeader from '$lib/components/ui/split-page-header/SplitPageHeader.svelte';

  const splitId = $derived(route.params.splitId || '');

  // State
  let settlement = $state<Settlement | null>(null);
  let split = $state<Split | null>(null);
  let splitName = $state('');
  let isLoading = $state(true);
  let showReimbursements = $state(false);
  let isUnsettling = $state(false);

  // On mount: calculate the settlement (POST) to show balances immediately.
  // If a settlement is already persisted, show reimbursements directly.
  $effect(() => {
    if (splitId) {
      loadSettlement(splitId);
    }
  });

  async function loadSettlement(id: string) {
    isLoading = true;
    settlement = null;
    showReimbursements = false;

    try {
      const [splitData, settlementData] = await Promise.all([getSplit(id), resolveSettlement(id)]);
      split = splitData;
      splitName = splitData.name;
      settlement = settlementData;

      if (splitData.settlement != null) {
        // Already resolved before — show reimbursements directly.
        showReimbursements = true;
      }
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

  function handleResolve() {
    showReimbursements = true;
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

  function formatSettlementText(url: string): string {
    if (!split || !settlement) return '';

    const totalExpenses = split.expenses.reduce((sum, e) => sum + e.amount, 0);
    const expenseCount = split.expenses.length;
    const participantCount = split.participants.length;

    const sorted = [...settlement.reimbursements].sort((a, b) =>
      a.fromName.localeCompare(b.fromName)
    );

    const lines: string[] = [
      `=== ${split.name} ===`,
      `${expenseCount} expense${expenseCount !== 1 ? 's' : ''} — Total: ${formatCurrency(totalExpenses)}`,
      `${participantCount} participant${participantCount !== 1 ? 's' : ''}`,
    ];

    if (sorted.length > 0) {
      lines.push('');
      lines.push('Settlements:');
      for (const r of sorted) {
        lines.push(`${r.fromName} → ${r.toName}: ${formatCurrency(r.amount)}`);
      }

      // Group by receiver to build summary
      const byReceiver = new Map<string, { name: string; amounts: number[] }>();
      for (const r of sorted) {
        if (!byReceiver.has(r.toId)) {
          byReceiver.set(r.toId, { name: r.toName, amounts: [] });
        }
        byReceiver.get(r.toId)!.amounts.push(r.amount);
      }

      lines.push('');
      lines.push('Summary:');
      for (const { name, amounts } of byReceiver.values()) {
        if (amounts.length === 1) {
          lines.push(`${name} receives ${formatCurrency(amounts[0])}`);
        } else {
          const total = amounts.reduce((s, a) => s + a, 0);
          lines.push(`${name} receives ${amounts.map(formatCurrency).join(' + ')} = ${formatCurrency(total)}`);
        }
      }
    } else {
      lines.push('');
      lines.push('All settled — no transfers needed!');
    }

    lines.push('');
    lines.push(url);

    return lines.join('\n');
  }

  async function handleExportSettlement() {
    const url = typeof window !== 'undefined' ? window.location.href : '';
    const text = formatSettlementText(url);

    if (!navigator.clipboard) {
      addToast({ type: 'info', message: text });
      return;
    }

    try {
      await navigator.clipboard.writeText(text);
      addToast({ type: 'success', message: 'Settlement copied to clipboard!' });
    } catch {
      addToast({ type: 'info', message: text });
    }
  }

  async function handleUnsettle() {
    isUnsettling = true;
    try {
      await unsettleSettlement(splitId);
      showReimbursements = false;
      const [splitData, settlementData] = await Promise.all([getSplit(splitId), resolveSettlement(splitId)]);
      split = splitData;
      settlement = settlementData;
    } catch (err: any) {
      addToast({ type: 'error', message: err.detail || 'Failed to unsettle' });
    } finally {
      isUnsettling = false;
    }
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
      // Preferred creditor change clears the persisted settlement — recalculate and reset to pre-resolve state.
      showReimbursements = false;
      const [splitData, settlementData] = await Promise.all([getSplit(splitId), resolveSettlement(splitId)]);
      split = splitData;
      settlement = settlementData;
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
  {:else if split}
    <!-- Header -->
    <SplitPageHeader splitName={splitName} {splitId} showBackButton />

    {#if settlement}
      {#if settlement.balances.length === 0}
        <p class="text-muted-foreground text-center py-4">No participants</p>
      {:else}
        <!-- Action Buttons (before cards, consistent with Add Participant / Add Expense placement) -->
        {#if showReimbursements}
          <Button
            onclick={handleExportSettlement}
            variant="outline"
            class="w-full min-h-[44px]"
          >
            Export Settlement
          </Button>
          <Button
            onclick={handleUnsettle}
            disabled={isUnsettling}
            variant="ghost"
            class="w-full min-h-[44px] text-muted-foreground"
          >
            {isUnsettling ? 'Unsettling...' : 'Unsettle'}
          </Button>
        {:else}
          <Button
            onclick={handleResolve}
            class="w-full min-h-[44px]"
          >
            Resolve
          </Button>
        {/if}

        <!-- Balance Cards -->
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
                      class="flex-1 h-8 rounded-md border border-input bg-background px-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
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
      {/if}
    {:else}
      <!-- Settlement not yet loaded (e.g. error on initial load) -->
      <Button
        onclick={handleResolve}
        class="w-full min-h-[44px]"
      >
        Resolve
      </Button>
    {/if}
  {/if}
</div>