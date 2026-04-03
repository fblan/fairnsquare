<script lang="ts">
  // Settlement Page - Balances & Reimbursement Proposals

  import { getSplit, getSettlement, resolveSettlement, unsettleSettlement, updateParticipant, type Settlement, type Split } from '$lib/api/splits';
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
  let isUpdatingGroup = $state(false);
  let groupModalParticipantId = $state<string | null>(null);
  let groupModalPartnerId = $state('');

  interface DisplayEntry {
    key: string;
    displayName: string;
    totalPaid: number;
    totalCost: number;
    balance: number;
    type: 'standard' | 'group';
    matchId: string;
    participantId?: string;   // standard only: for "Group with" modal
    memberIds?: string[];     // group only: for Ungroup action
  }

  // Collapsed balance list: shared-account members are merged into one group entry.
  const displayBalances = $derived((() => {
    if (!split || !settlement) return [] as DisplayEntry[];
    const seen = new Set<string>();
    const result: DisplayEntry[] = [];

    for (const p of split.participants) {
      if (seen.has(p.id)) continue;

      if (!p.sharedAccountId) {
        const b = settlement.balances.find(b => b.participantId === p.id);
        if (!b) { seen.add(p.id); continue; }
        result.push({
          key: p.id,
          displayName: p.name,
          totalPaid: b.totalPaid,
          totalCost: b.totalCost,
          balance: b.balance,
          type: 'standard',
          matchId: p.id,
          participantId: p.id,
        });
        seen.add(p.id);
      } else {
        const members = split.participants.filter(m => m.sharedAccountId === p.sharedAccountId);
        members.forEach(m => seen.add(m.id));
        const memberBalances = members
          .map(m => settlement!.balances.find(b => b.participantId === m.id))
          .filter((b): b is NonNullable<typeof b> => b != null);
        const displayName = [...members]
          .sort((a, b) => a.name.localeCompare(b.name))
          .map(m => m.name)
          .join(' & ');
        result.push({
          key: p.sharedAccountId,
          displayName,
          totalPaid: memberBalances.reduce((s, b) => s + b.totalPaid, 0),
          totalCost: memberBalances.reduce((s, b) => s + b.totalCost, 0),
          balance: memberBalances.reduce((s, b) => s + b.balance, 0),
          type: 'group',
          matchId: p.sharedAccountId,
          memberIds: members.map(m => m.id),
        });
      }
    }
    return result;
  })());

  const groupModalParticipant = $derived(
    groupModalParticipantId ? split?.participants.find(p => p.id === groupModalParticipantId) ?? null : null
  );

  // Modal options: standard participants shown individually, existing groups shown as one entry.
  const groupModalOptions = $derived((() => {
    if (!groupModalParticipantId || !split) return [] as { id: string; displayName: string }[];
    const seen = new Set<string>();
    const result: { id: string; displayName: string }[] = [];
    for (const p of split.participants) {
      if (p.id === groupModalParticipantId || seen.has(p.id)) continue;
      if (!p.sharedAccountId) {
        result.push({ id: p.id, displayName: p.name });
        seen.add(p.id);
      } else {
        const members = split.participants.filter(m => m.sharedAccountId === p.sharedAccountId);
        members.forEach(m => seen.add(m.id));
        const displayName = [...members]
          .sort((a, b) => a.name.localeCompare(b.name))
          .map(m => m.name)
          .join(' & ');
        result.push({ id: members[0].id, displayName });
      }
    }
    return result;
  })());

  // On mount: load the split and derive balances from participant computed fields.
  // Never POST on load — that would persist a settlement and undo an intentional unsettle.
  // If a settlement is already persisted, fetch its reimbursements and show them directly.
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
      const splitData = await getSplit(id);
      split = splitData;
      splitName = splitData.name;

      // Derive balances from participant computed fields (always available, no POST needed).
      const balances = splitData.participants.map(p => ({
        participantId: p.id,
        participantName: p.name,
        totalPaid: p.totalPaid ?? 0,
        totalCost: p.totalCost ?? 0,
        balance: p.balance ?? 0,
      }));

      if (splitData.settlement != null) {
        // Already resolved — fetch persisted reimbursements and show them directly.
        const persisted = await getSettlement(id);
        settlement = { balances, reimbursements: persisted.reimbursements };
        showReimbursements = true;
      } else {
        settlement = { balances, reimbursements: [] };
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

  async function handleResolve() {
    try {
      const settlementData = await resolveSettlement(splitId);
      settlement = settlementData;
      showReimbursements = true;
    } catch (err: any) {
      addToast({ type: 'error', message: err.detail || 'Failed to resolve' });
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

  function generateSharedAccountId(): string {
    const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-';
    const bytes = crypto.getRandomValues(new Uint8Array(21));
    return Array.from(bytes, b => alphabet[b & 63]).join('');
  }

  async function handleSharedAccountChange(participantId: string, newPartnerIdOrEmpty: string) {
    if (!split) return;
    const participant = split.participants.find(p => p.id === participantId);
    if (!participant) return;

    isUpdatingGroup = true;
    try {
      if (!newPartnerIdOrEmpty) {
        // Ungroup: clear shared account
        await updateParticipant(splitId, participantId, {
          name: participant.name,
          nights: participant.nights,
          share: participant.share,
          sharedAccountId: null,
        });
      } else {
        const partner = split.participants.find(p => p.id === newPartnerIdOrEmpty);
        if (!partner) return;

        if (partner.sharedAccountId) {
          // Join partner's existing group
          await updateParticipant(splitId, participantId, {
            name: participant.name,
            nights: participant.nights,
            share: participant.share,
            sharedAccountId: partner.sharedAccountId,
          });
        } else {
          // Create new group for both
          const newGroupId = generateSharedAccountId();
          await updateParticipant(splitId, participantId, {
            name: participant.name,
            nights: participant.nights,
            share: participant.share,
            sharedAccountId: newGroupId,
          });
          await updateParticipant(splitId, newPartnerIdOrEmpty, {
            name: partner.name,
            nights: partner.nights,
            share: partner.share,
            sharedAccountId: newGroupId,
          });
        }
      }

      // Reload split and recompute balances from fresh participant data
      const updatedSplit = await getSplit(splitId);
      split = updatedSplit;
      const balances = updatedSplit.participants.map(p => ({
        participantId: p.id,
        participantName: p.name,
        totalPaid: p.totalPaid ?? 0,
        totalCost: p.totalCost ?? 0,
        balance: p.balance ?? 0,
      }));
      settlement = { ...settlement!, balances };
    } catch (err: any) {
      addToast({ type: 'error', message: err.detail || 'Failed to update group' });
    } finally {
      isUpdatingGroup = false;
    }
  }

  async function handleUngroup(memberIds: string[]) {
    if (!split) return;
    isUpdatingGroup = true;
    try {
      for (const memberId of memberIds) {
        const p = split.participants.find(m => m.id === memberId);
        if (!p) continue;
        await updateParticipant(splitId, memberId, {
          name: p.name,
          nights: p.nights,
          share: p.share,
          sharedAccountId: null,
        });
      }
      const updatedSplit = await getSplit(splitId);
      split = updatedSplit;
      const balances = updatedSplit.participants.map(p => ({
        participantId: p.id,
        participantName: p.name,
        totalPaid: p.totalPaid ?? 0,
        totalCost: p.totalCost ?? 0,
        balance: p.balance ?? 0,
      }));
      settlement = { ...settlement!, balances };
    } catch (err: any) {
      addToast({ type: 'error', message: err.detail || 'Failed to ungroup' });
    } finally {
      isUpdatingGroup = false;
    }
  }

  async function handleUnsettle() {
    isUnsettling = true;
    try {
      await unsettleSettlement(splitId);
      showReimbursements = false;
    } catch (err: any) {
      addToast({ type: 'error', message: err.detail || 'Failed to unsettle' });
    } finally {
      isUnsettling = false;
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
    <SplitPageHeader splitName={splitName} {splitId} />

    {#if settlement}
      {#if displayBalances.length === 0}
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

        <!-- Balance Cards (shared-account members collapsed into one group card) -->
        <div class="w-full space-y-3">
          {#each displayBalances as entry (entry.key)}
            <Card.Root class="w-full">
              <Card.Content class="py-4">
                <div class="flex items-start justify-between">
                  <div class="flex-1">
                    <span class="font-semibold text-lg">{entry.displayName}</span>
                    <div class="flex flex-wrap gap-x-4 gap-y-1 text-sm mt-1">
                      <span class="text-muted-foreground">Paid: <span class="font-medium text-foreground">{formatCurrency(entry.totalPaid)}</span></span>
                      <span class="text-muted-foreground">Cost: <span class="font-medium text-foreground">{formatCurrency(entry.totalCost)}</span></span>
                    </div>
                  </div>
                  <div class="text-right">
                    <span class="text-sm font-medium {balanceColorClass(entry.balance)}">
                      {balanceLabel(entry.balance)}
                    </span>
                  </div>
                </div>

                <!-- Group actions (only when not yet settled) -->
                {#if !showReimbursements}
                  <div class="mt-2 flex gap-3">
                    {#if entry.type === 'standard'}
                      <button
                        onclick={() => { groupModalParticipantId = entry.participantId!; groupModalPartnerId = ''; }}
                        disabled={isUpdatingGroup}
                        class="text-xs text-muted-foreground hover:text-foreground underline disabled:opacity-50"
                      >
                        Group with...
                      </button>
                    {:else}
                      <button
                        onclick={() => handleUngroup(entry.memberIds!)}
                        disabled={isUpdatingGroup}
                        class="text-xs text-muted-foreground hover:text-foreground underline disabled:opacity-50"
                      >
                        Ungroup
                      </button>
                    {/if}
                  </div>
                {/if}

                <!-- Reimbursement details for this entry -->
                {#if showReimbursements}
                  {@const outgoing = settlement!.reimbursements.filter(r => r.fromId === entry.matchId)}
                  {@const incoming = settlement!.reimbursements.filter(r => r.toId === entry.matchId)}
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

<!-- Group with modal -->
{#if groupModalParticipantId}
  <!-- Backdrop -->
  <div
    class="fixed inset-0 z-50 bg-black/50"
    role="presentation"
    onclick={() => (groupModalParticipantId = null)}
  ></div>

  <!-- Dialog -->
  <div
    role="dialog"
    aria-modal="true"
    aria-label="Group with"
    class="fixed inset-x-4 top-1/2 -translate-y-1/2 z-50 bg-background rounded-lg shadow-lg p-6 max-w-sm mx-auto"
  >
    <h2 class="text-lg font-semibold mb-4">Group {groupModalParticipant?.name} with</h2>

    <label for="group-modal-select" class="text-sm text-muted-foreground block mb-1">Partner</label>
    <select
      id="group-modal-select"
      bind:value={groupModalPartnerId}
      class="w-full text-sm border border-input rounded-md px-3 py-2 bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring mb-6"
    >
      <option value="">(none)</option>
      {#each groupModalOptions as opt (opt.id)}
        <option value={opt.id}>{opt.displayName}</option>
      {/each}
    </select>

    <div class="flex gap-3 justify-end">
      <Button variant="ghost" onclick={() => (groupModalParticipantId = null)}>
        Cancel
      </Button>
      <Button
        onclick={async () => {
          const pid = groupModalParticipantId!;
          const partner = groupModalPartnerId;
          groupModalParticipantId = null;
          await handleSharedAccountChange(pid, partner);
        }}
      >
        OK
      </Button>
    </div>
  </div>
{/if}