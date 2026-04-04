 <script lang="ts">
  // Participants Edition Page

  import { getSplit, addParticipant, deleteParticipant, type Split, type Participant } from '$lib/api/splits';
  import type { ApiError } from '$lib/api/client';
  import Button from '$lib/components/ui/button/button.svelte';
  import * as Card from '$lib/components/ui/card';
  import Input from '$lib/components/ui/input/input.svelte';
  import Label from '$lib/components/ui/label/label.svelte';
  import ConfirmDialog from '$lib/components/ui/confirm-dialog/confirm-dialog.svelte';
  import ExpenseEditModal from '$lib/components/expense/ExpenseEditModal.svelte';
  import EditParticipantModal from '$lib/components/participant/EditParticipantModal.svelte';
  import ParticipantSummaryCard from '$lib/components/participant/ParticipantSummaryCard.svelte';
  import { addToast } from '$lib/stores/toastStore.svelte';
  import { route, navigate } from '$lib/router';
  import { loadNightsDefault, saveNightsDefault } from '$lib/stores/nightsDefaultStore';
  import { Plus, Wallet, Receipt, TrendingUp, TrendingDown, Minus, Info, X, Users } from 'lucide-svelte';

  // Field info modal state (add form)
  let activeFieldInfo = $state<'nights' | 'share' | null>(null);

  const fieldInfo = {
    nights: {
      title: 'Nights',
      description: `The number of nights this participant stays during the trip.\n\nUsed in the "By Night" split mode: costs are divided proportionally based on each participant's nights × share weight.\n\nExample: a participant staying 3 nights out of a 7-night trip pays for 3/7 of the night-based expenses (adjusted by their share).`,
    },
    share: {
      title: 'Share',
      description: `The number of persons this participant represents.\n\nA solo traveller has a share of 1. A couple sharing costs would have a share of 2, a family of 3 would use 3, etc.\n\nUsed in "By Night" mode (nights × share) and "By Share" mode (share only) to calculate each participant's proportional contribution.\n\nExample: a couple (share 2) staying 3 nights counts as 6 night-shares, compared to 3 night-shares for a solo person staying the same time.`,
    },
  };
  import SplitPageHeader from '$lib/components/ui/split-page-header/SplitPageHeader.svelte';
  import { getCachedSplitName, setCachedSplitName } from '$lib/stores/splitTitleCache';
  import { tick } from 'svelte';

  const splitId = $derived(route.params.splitId || '');

  // State
  let split = $state<Split | null>(null);
  let splitDisplayName = $state(getCachedSplitName(route.params.splitId || ''));
  const sortedParticipants = $derived(
    split?.participants.slice().sort((a, b) => a.name.localeCompare(b.name)) ?? []
  );
  const isSettled = $derived(split?.settlement != null);
  let isLoading = $state(true);
  let showLoading = $state(false);
  let loadingTimer: ReturnType<typeof setTimeout> | null = null;

  // Add Participant form state
  let showAddForm = $state(false);
  let nameInputEl = $state<HTMLInputElement | null>(null);
  let formName = $state('');
  let formNights = $state(1);
  let formShare = $state(1);
  let validationErrors = $state<{name?: string; nights?: string; share?: string}>({});
  let isSubmitting = $state(false);

  // Edit Participant state
  let showEditParticipantModal = $state(false);
  let editingParticipant = $state<Participant | null>(null);
  let highlightedParticipantId = $state<string | null>(null);

  // Delete Participant state
  let deletingParticipantId = $state<string | null>(null);
  let deletingParticipantName = $state('');
  let isDeleteSubmitting = $state(false);

  // Add Expense Modal state
  let showAddExpenseModal = $state(false);
  let selectedPayerId = $state<string | null>(null);


  // Load split data
  $effect(() => {
    if (splitId) {
      loadSplit(splitId);
    }
  });

  // Auto-open add form when navigated from the Home creation flow
  $effect(() => {
    if (route.search?.addParticipant) {
      handleShowAddForm();
      // Remove the param from the URL without triggering re-navigation
      const url = new URL(window.location.href);
      url.searchParams.delete('addParticipant');
      window.history.replaceState(null, '', url.toString());
    }
  });

  async function loadSplit(id: string) {
    isLoading = true;
    showLoading = false;

    if (loadingTimer) clearTimeout(loadingTimer);
    loadingTimer = setTimeout(() => { showLoading = true; }, 500);

    try {
      split = await getSplit(id);
      splitDisplayName = split.name;
      setCachedSplitName(id, split.name);
    } catch (err) {
      const apiError = err as ApiError;
      if (apiError.status === 404 || apiError.status === 400) {
        addToast({ type: 'info', message: 'Split not found — create a new one.' });
        navigate('/');
      } else {
        addToast({
          type: 'error',
          message: apiError.detail || 'Failed to load participants',
        });
      }
    } finally {
      isLoading = false;
      showLoading = false;
      if (loadingTimer) { clearTimeout(loadingTimer); loadingTimer = null; }
    }
  }

  // Per-participant stats calculation
  function getParticipantStats(participantId: string) {
    if (!split) return { spent: 0, cost: 0, balance: 0, expenseCount: 0 };

    const spent = split.expenses
      .filter(e => e.payerId === participantId)
      .reduce((sum, e) => sum + e.amount, 0);

    const cost = split.expenses
      .reduce((sum, e) => {
        const share = e.shares.find(s => s.participantId === participantId);
        return sum + (share?.amount || 0);
      }, 0);

    const expenseCount = split.expenses
      .filter(e => e.payerId === participantId)
      .length;

    const balance = spent - cost;
    return { spent, cost, balance, expenseCount };
  }

  function formatName(name: string): string {
    if (!name) return name;
    return name.charAt(0).toUpperCase() + name.slice(1).toLowerCase();
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


  // Returns the names of co-members in the same shared account group, or null if not grouped.
  function getSharedAccountPartners(participant: Participant): string[] | null {
    if (!participant.sharedAccountId || !split) return null;
    const partners = split.participants
      .filter(p => p.id !== participant.id && p.sharedAccountId === participant.sharedAccountId)
      .map(p => p.name)
      .sort();
    return partners.length > 0 ? partners : null;
  }

  // Add Participant handlers
  async function handleShowAddForm() {
    showAddForm = true;
    formName = '';
    formNights = loadNightsDefault();
    formShare = 1;
    validationErrors = {};
    await tick();
    nameInputEl?.focus();
  }

  function handleCancelAddForm() {
    showAddForm = false;
    formName = '';
    formNights = 1;
    formShare = 1;
    validationErrors = {};
  }

  function validateNameOnInput() {
    if (!formName.trim()) {
      validationErrors = { ...validationErrors, name: undefined };
      return;
    }
    let nameError: string | undefined;
    if (formName.trim().length > 50) {
      nameError = 'Name cannot exceed 50 characters';
    } else if (split?.participants.some(p => p.name.toLowerCase() === formName.trim().toLowerCase())) {
      nameError = 'A participant with this name already exists';
    }
    validationErrors = { ...validationErrors, name: nameError };
  }

  function validateNightsOnInput() {
    let nightsError: string | undefined;
    if (formNights < 1) {
      nightsError = 'Nights must be at least 1';
    } else if (formNights > 365) {
      nightsError = 'Nights cannot exceed 365';
    }
    validationErrors = { ...validationErrors, nights: nightsError };
  }

  function validateShareOnInput() {
    let error: string | undefined;
    if (formShare < 0.5) {
      error = 'Must be at least 0.5';
    } else if (formShare > 50) {
      error = 'Cannot exceed 50';
    }
    validationErrors = { ...validationErrors, share: error };
  }

  function validateAddForm(): boolean {
    const errors: {name?: string; nights?: string; share?: string} = {};

    if (!formName.trim()) {
      errors.name = 'Name is required';
    } else if (formName.trim().length > 50) {
      errors.name = 'Name cannot exceed 50 characters';
    } else if (split?.participants.some(p => p.name.toLowerCase() === formName.trim().toLowerCase())) {
      errors.name = 'A participant with this name already exists';
    }

    if (formNights < 1) {
      errors.nights = 'Nights must be at least 1';
    } else if (formNights > 365) {
      errors.nights = 'Nights cannot exceed 365';
    }

    if (formShare < 0.5) {
      errors.share = 'Must be at least 0.5';
    } else if (formShare > 50) {
      errors.share = 'Cannot exceed 50';
    }

    validationErrors = errors;
    return Object.keys(errors).length === 0;
  }

  async function handleAddParticipant() {
    if (!validateAddForm()) return;

    isSubmitting = true;

    try {
      const addedName = formName.trim();
      const addedNights = formNights;
      const addedShare = formShare;
      await addParticipant(splitId, {
        name: addedName,
        nights: formNights,
        share: formShare,
      });

      saveNightsDefault(formNights);
      showAddForm = false;
      formName = '';
      formNights = 1;
      formShare = 1;
      validationErrors = {};
      await loadSplit(splitId);

      addToast({
        type: 'success',
        message: `${addedName} successfully added`,
        description: `${addedNights} nights · share ${addedShare}`,
        duration: 4000,
      });
    } catch (err: any) {
      addToast({
        type: 'error',
        message: err.detail || 'Failed to add participant. Please try again.',
      });
    } finally {
      isSubmitting = false;
    }
  }

  // Edit Participant handlers
  function handleStartEdit(participant: Participant) {
    editingParticipant = participant;
    showEditParticipantModal = true;
  }

  function handleEditModalClose() {
    showEditParticipantModal = false;
    editingParticipant = null;
  }

  async function handleEditSuccess() {
    const updatedParticipantId = editingParticipant?.id;
    showEditParticipantModal = false;
    editingParticipant = null;
    await loadSplit(splitId);

    if (updatedParticipantId) {
      highlightedParticipantId = updatedParticipantId;
      setTimeout(() => {
        highlightedParticipantId = null;
      }, 2000);
    }
  }

  // Delete Participant handlers
  function handleDeleteClick(event: MouseEvent, participant: Participant) {
    event.stopPropagation();
    deletingParticipantId = participant.id;
    deletingParticipantName = participant.name;
  }

  function handleCancelDelete() {
    deletingParticipantId = null;
    deletingParticipantName = '';
  }

  async function handleConfirmDelete() {
    if (!deletingParticipantId) return;

    isDeleteSubmitting = true;

    try {
      await deleteParticipant(splitId, deletingParticipantId);
      await loadSplit(splitId);

      addToast({
        type: 'success',
        message: 'Participant removed successfully',
        description: deletingParticipantName,
      });

      deletingParticipantId = null;
      deletingParticipantName = '';
    } catch (err: any) {
      if (err.status === 409) {
        addToast({
          type: 'error',
          message: `Cannot remove ${deletingParticipantName} - they have associated expenses. Remove or reassign their expenses first.`,
        });
      } else {
        addToast({
          type: 'error',
          message: err.detail || 'Failed to remove participant. Please try again.',
        });
      }

      deletingParticipantId = null;
      deletingParticipantName = '';
    } finally {
      isDeleteSubmitting = false;
    }
  }

  // Add Expense Modal handlers
  function handleAddExpenseClick(event: MouseEvent, participant: Participant) {
    event.stopPropagation();
    selectedPayerId = participant.id;
    showAddExpenseModal = true;
  }

  function handleAddExpenseModalClose() {
    showAddExpenseModal = false;
    selectedPayerId = null;
  }

  async function handleAddExpenseSuccess() {
    await loadSplit(splitId);
  }
</script>

<div class="flex flex-col items-center space-y-4 w-full max-w-[520px] mx-auto">
  <!-- Header always visible once splitId is known -->
  {#if splitId}
    <SplitPageHeader splitName={splitDisplayName} {splitId} />
  {/if}

  {#if showLoading}
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
      <p class="text-muted-foreground">Loading participants...</p>
    </div>
  {:else if !isLoading && split}

    <!-- Settled Banner -->
    {#if isSettled}
      <div class="w-full flex items-center gap-2 px-3 py-2 rounded-md bg-amber-50 border border-amber-200 text-sm text-amber-800">
        <span class="flex-1">This split is settled — participants are read-only.</span>
        <button
          onclick={() => navigate(`/splits/${splitId}/settlement`)}
          class="underline font-medium whitespace-nowrap"
        >
          View settlement
        </button>
      </div>
    {/if}

    <!-- Participants Summary Card -->
    <ParticipantSummaryCard participants={split.participants} showTitle={false} />

    <!-- Add Participant Form / Button (hidden when settled) -->
    {#if !isSettled && showAddForm}
      <Card.Root class="w-full">
        <Card.Content class="py-4">
          <form onsubmit={(e) => { e.preventDefault(); handleAddParticipant(); }} class="space-y-3">
            <p class="text-sm font-medium text-teal-700">New Participant</p>

            <div class="space-y-2">
              <Label for="participant-name">Name</Label>
              <Input
                id="participant-name"
                type="text"
                placeholder="Enter name"
                bind:value={formName}
                bind:ref={nameInputEl}
                oninput={validateNameOnInput}
                class="min-h-[44px]"
                disabled={isSubmitting}
              />
              {#if validationErrors.name}
                <p class="text-sm text-destructive">{validationErrors.name}</p>
              {/if}
            </div>

            <div class="flex gap-3">
              <div class="space-y-2 flex-1">
                <div class="flex items-center gap-1">
                  <Label for="participant-nights">Nights</Label>
                  <button type="button" onclick={() => { activeFieldInfo = 'nights'; }} class="p-0.5 rounded-full hover:bg-muted text-muted-foreground" aria-label="Info about Nights">
                    <Info class="h-3.5 w-3.5" />
                  </button>
                </div>
                <Input
                  id="participant-nights"
                  type="number"
                  step="1"
                  min="1"
                  bind:value={formNights}
                  oninput={validateNightsOnInput}
                  class="min-h-[44px]"
                  disabled={isSubmitting}
                />
                {#if validationErrors.nights}
                  <p class="text-sm text-destructive">{validationErrors.nights}</p>
                {/if}
              </div>

              <div class="space-y-2 flex-1">
                <div class="flex items-center gap-1">
                  <Label for="participant-share">Share</Label>
                  <button type="button" onclick={() => { activeFieldInfo = 'share'; }} class="p-0.5 rounded-full hover:bg-muted text-muted-foreground" aria-label="Info about Share">
                    <Info class="h-3.5 w-3.5" />
                  </button>
                </div>
                <Input
                  id="participant-share"
                  type="number"
                  step="0.5"
                  min="0.5"
                  max="50"
                  bind:value={formShare}
                  oninput={validateShareOnInput}
                  class="min-h-[44px]"
                  disabled={isSubmitting}
                />
                {#if validationErrors.share}
                  <p class="text-sm text-destructive">{validationErrors.share}</p>
                {/if}
              </div>
            </div>

            <div class="flex gap-2">
              <Button type="submit" disabled={isSubmitting} class="flex-1 min-h-[44px]">
                {isSubmitting ? 'Adding...' : 'Add'}
              </Button>
              <Button type="button" variant="outline" onclick={handleCancelAddForm} disabled={isSubmitting} class="flex-1 min-h-[44px]">
                Cancel
              </Button>
            </div>
          </form>
        </Card.Content>
      </Card.Root>
    {:else if !isSettled}
      <Button
        onclick={handleShowAddForm}
        variant="outline"
        class="w-full min-h-[44px]"
      >
        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor">
          <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd" />
        </svg>
        Add Participant
      </Button>
    {/if}

    {#if sortedParticipants.length === 0 && !showAddForm}
      <p class="text-muted-foreground text-center py-4">No participants yet</p>
    {/if}

    <!-- Participant Cards -->
    {#each sortedParticipants as participant (participant.id)}
      {@const stats = getParticipantStats(participant.id)}
      {@const sharedAccountPartners = getSharedAccountPartners(participant)}
      <Card.Root class="w-full">
        <Card.Content class="py-4 transition-colors duration-500 {highlightedParticipantId === participant.id ? 'bg-teal-50' : ''}">
          <div class="flex flex-col gap-1">
            <!-- Row 1: name + action buttons -->
            <div class="flex items-center justify-between gap-2">
              <span class="font-semibold text-lg truncate min-w-0 flex-1">{formatName(participant.name)}</span>
              <!-- Action Buttons (hidden when settled) -->
              {#if !isSettled}
                <div class="flex-none flex items-center gap-1">
                  <Button
                    variant="ghost"
                    size="sm"
                    onclick={(e) => handleAddExpenseClick(e, participant)}
                    class="text-primary hover:text-primary hover:bg-primary/10 min-h-[44px] min-w-[44px]"
                    aria-label={`Add expense for ${participant.name}`}
                  >
                    <Plus class="h-4 w-4" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onclick={() => handleStartEdit(participant)}
                    class="min-h-[44px] min-w-[44px]"
                    aria-label={`Edit ${participant.name}`}
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                      <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
                    </svg>
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onclick={(e) => handleDeleteClick(e, participant)}
                    class="text-destructive hover:text-destructive hover:bg-destructive/10 min-h-[44px] min-w-[44px]"
                    aria-label={`Delete ${participant.name}`}
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                      <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
                    </svg>
                  </Button>
                </div>
              {/if}
            </div>
            <!-- Row 2: badges -->
            <div class="flex flex-wrap items-center gap-1">
              <span class="text-xs px-2 py-0.5 rounded-full bg-teal-100 text-teal-700">
                {participant.nights} {participant.nights <= 1 ? 'night' : 'nights'}
              </span>
              {#if participant.share > 1}
                <span class="text-xs px-2 py-0.5 rounded-full bg-blue-100 text-blue-700">
                  {participant.share} {participant.share <= 1 ? 'share' : 'shares'}
                </span>
              {/if}
              <span class="text-xs px-2 py-0.5 rounded-full bg-slate-100 text-slate-600">
                {stats.expenseCount} {stats.expenseCount === 1 ? 'expense' : 'expenses'}
              </span>
              {#if sharedAccountPartners}
                <span class="flex items-center gap-1 text-xs px-2 py-0.5 rounded-full bg-violet-100 text-violet-700">
                  <Users class="h-3 w-3 shrink-0" />
                  {sharedAccountPartners.join(' & ')}
                </span>
              {/if}
            </div>
            <!-- Row 3: stats (full width) -->
            <div class="flex items-center justify-between text-base">
              <span class="flex items-center gap-1 text-muted-foreground" title="Spent">
                <Wallet class="h-4 w-4 shrink-0" />
                <span class="font-medium text-foreground">{formatCurrency(stats.spent)}</span>
              </span>
              <span class="flex items-center gap-1 text-muted-foreground" title="Cost">
                <Receipt class="h-4 w-4 shrink-0" />
                <span class="font-medium text-foreground">{formatCurrency(stats.cost)}</span>
              </span>
              <span class="flex items-center gap-1 {balanceColorClass(stats.balance)}" title="Balance">
                {#if stats.balance > 0.005}
                  <TrendingUp class="h-4 w-4 shrink-0" />
                  <span class="font-medium">{formatCurrency(stats.balance)}</span>
                {:else if stats.balance < -0.005}
                  <TrendingDown class="h-4 w-4 shrink-0" />
                  <span class="font-medium">{formatCurrency(Math.abs(stats.balance))}</span>
                {:else}
                  <Minus class="h-4 w-4 shrink-0" />
                  <span class="font-medium">Settled</span>
                {/if}
              </span>
            </div>
          </div>
        </Card.Content>
      </Card.Root>
    {/each}
  {/if}
</div>

<!-- Delete Confirmation Dialog -->
<ConfirmDialog
  open={!!deletingParticipantId}
  title={`Remove ${deletingParticipantName}?`}
  description="This cannot be undone."
  confirmLabel="Remove"
  cancelLabel="Cancel"
  onConfirm={handleConfirmDelete}
  onCancel={handleCancelDelete}
  isLoading={isDeleteSubmitting}
/>

<!-- Add Expense Modal -->
{#if split}
  <ExpenseEditModal
    open={showAddExpenseModal}
    splitId={split.id}
    preselectedPayerId={selectedPayerId}
    participants={split.participants}
    onClose={handleAddExpenseModalClose}
    onSuccess={handleAddExpenseSuccess}
  />
{/if}

<!-- Field Info Modal (add participant form) -->
{#if activeFieldInfo}
  <!-- svelte-ignore a11y_click_events_have_key_events -->
  <div
    class="fixed inset-0 z-[70] bg-black/50 flex items-end justify-center sm:items-center sm:p-4"
    onclick={() => { activeFieldInfo = null; }}
    role="dialog"
    aria-modal="true"
    aria-labelledby="field-info-title"
    tabindex="-1"
  >
    <!-- svelte-ignore a11y_click_events_have_key_events -->
    <div
      role="presentation"
      class="bg-background w-full sm:max-w-[420px] rounded-t-xl sm:rounded-xl shadow-lg animate-in fade-in slide-in-from-bottom-4 sm:zoom-in-95"
      onclick={(e) => e.stopPropagation()}
    >
      <div class="flex items-center justify-between p-4 border-b">
        <h2 id="field-info-title" class="text-base font-semibold">{fieldInfo[activeFieldInfo].title}</h2>
        <Button variant="ghost" size="sm" onclick={() => { activeFieldInfo = null; }} class="min-h-[44px] min-w-[44px]" aria-label="Close">
          <X class="h-4 w-4" />
        </Button>
      </div>
      <div class="p-4 space-y-3">
        {#each fieldInfo[activeFieldInfo].description.split('\n\n') as paragraph}
          <p class="text-sm text-muted-foreground leading-relaxed">{paragraph}</p>
        {/each}
      </div>
      <div class="p-4 border-t">
        <Button onclick={() => { activeFieldInfo = null; }} class="w-full min-h-[44px]">Got it</Button>
      </div>
    </div>
  </div>
{/if}

<!-- Edit Participant Modal -->
{#if split}
  <EditParticipantModal
    open={showEditParticipantModal}
    splitId={split.id}
    participant={editingParticipant}
    participants={split.participants}
    {split}
    onClose={handleEditModalClose}
    onSuccess={handleEditSuccess}
  />
{/if}