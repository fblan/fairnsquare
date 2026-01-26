<script lang="ts">
  // Split Overview Page
  // Story 2.3: Access Split via Link & View Overview
  // Story 3.1: Add Participant with Smart Defaults
  // Story 3.2: Edit Participant Inline

  import { getSplit, addParticipant, updateParticipant, type Split, type Participant, type Expense } from '$lib/api/splits';
  import type { ApiError } from '$lib/api/client';
  import { Button } from '$lib/components/ui/button';
  import { Input } from '$lib/components/ui/input';
  import { Label } from '$lib/components/ui/label';
  import * as Card from '$lib/components/ui/card';
  import { addToast } from '$lib/stores/toastStore.svelte';
  import { route, navigate } from '$lib/router';

  // Extract splitId from route params using sv-router
  const splitId = $derived(route.params.splitId || '');

  // State using Svelte 5 runes (per architecture pattern)
  let split = $state<Split | null>(null);
  let isLoading = $state(true);
  let error = $state<string | null>(null);
  let notFound = $state(false);
  let copyConfirmation = $state(false);

  // Add Participant form state (Story 3.1)
  let showAddForm = $state(false);
  let formName = $state('');
  let formNights = $state(1);
  let isSubmitting = $state(false);
  let validationErrors = $state<{name?: string; nights?: string}>({});

  // Validation constants (must match backend)
  const MAX_NIGHTS = 365;

  // Edit Participant state (Story 3.2)
  let editingParticipantId = $state<string | null>(null);
  let editName = $state('');
  let editNights = $state(1);
  let editValidationErrors = $state<{name?: string; nights?: string}>({});
  let isEditSubmitting = $state(false);

  // Smart default for nights - persist to localStorage (Story 3.1 AC 3, 4)
  const NIGHTS_STORAGE_KEY = 'fairnsquare_lastParticipantNights';

  function getSmartDefaultNights(): number {
    if (typeof window === 'undefined') return 1;
    const stored = localStorage.getItem(NIGHTS_STORAGE_KEY);
    return stored ? parseInt(stored, 10) : 1;
  }

  function saveSmartDefaultNights(nights: number): void {
    if (typeof window !== 'undefined') {
      localStorage.setItem(NIGHTS_STORAGE_KEY, nights.toString());
    }
  }

  // Shareable URL
  const shareableUrl = $derived(
    typeof window !== 'undefined' ? window.location.href : ''
  );

  // Fetch split data when splitId changes
  $effect(() => {
    if (splitId) {
      loadSplit(splitId);
    }
  });

  async function loadSplit(id: string) {
    isLoading = true;
    error = null;
    notFound = false;
    split = null;

    try {
      split = await getSplit(id);
    } catch (err) {
      const apiError = err as ApiError;
      if (apiError.status === 404) {
        notFound = true;
      } else {
        error = apiError.detail || 'Failed to load split. Please try again.';
      }
    } finally {
      isLoading = false;
    }
  }

  async function handleRetry() {
    if (splitId) {
      await loadSplit(splitId);
    }
  }

  async function handleShare() {
    try {
      await navigator.clipboard.writeText(shareableUrl);
      copyConfirmation = true;
      addToast({
        type: 'success',
        message: 'Link copied to clipboard!',
        duration: 3000,
      });
      setTimeout(() => {
        copyConfirmation = false;
      }, 2000);
    } catch {
      addToast({
        type: 'error',
        message: 'Failed to copy link. Please copy manually.',
      });
    }
  }

  function handleGoHome() {
    navigate('/');
  }

  // Add Participant handlers (Story 3.1)
  function handleShowAddForm() {
    showAddForm = true;
    formName = '';
    formNights = getSmartDefaultNights();
    validationErrors = {};
  }

  function handleCancelAddForm() {
    showAddForm = false;
    formName = '';
    formNights = 1;
    validationErrors = {};
  }

  function validateAddForm(): boolean {
    const errors: {name?: string; nights?: string} = {};

    if (!formName.trim()) {
      errors.name = 'Name is required';
    }

    if (formNights < 1) {
      errors.nights = 'Nights must be at least 1';
    } else if (formNights > MAX_NIGHTS) {
      errors.nights = 'Nights cannot exceed 365';
    }

    validationErrors = errors;
    return Object.keys(errors).length === 0;
  }

  async function handleAddParticipant() {
    if (!validateAddForm()) return;
    if (!splitId) return;

    isSubmitting = true;

    try {
      await addParticipant(splitId, {
        name: formName.trim(),
        nights: formNights,
      });

      // Save smart default
      saveSmartDefaultNights(formNights);

      // Refresh split data
      await loadSplit(splitId);

      // Close form and show success
      showAddForm = false;
      formName = '';
      addToast({
        type: 'success',
        message: 'Participant added',
        duration: 3000,
      });
    } catch (err) {
      const apiError = err as ApiError;
      addToast({
        type: 'error',
        message: apiError.detail || 'Failed to add participant. Please try again.',
      });
      // Keep form open for retry (AC 8)
    } finally {
      isSubmitting = false;
    }
  }

  // Edit Participant handlers (Story 3.2)
  function handleStartEdit(participant: Participant) {
    // Don't start edit if already submitting
    if (isEditSubmitting) return;
    // Close add form if open
    if (showAddForm) {
      showAddForm = false;
    }
    editingParticipantId = participant.id;
    editName = participant.name;
    editNights = participant.nights;
    editValidationErrors = {};
  }

  function handleCancelEdit() {
    editingParticipantId = null;
    editName = '';
    editNights = 1;
    editValidationErrors = {};
  }

  function validateEditForm(): boolean {
    const errors: {name?: string; nights?: string} = {};

    if (!editName.trim()) {
      errors.name = 'Name is required';
    }

    if (editNights < 1) {
      errors.nights = 'Nights must be at least 1';
    } else if (editNights > MAX_NIGHTS) {
      errors.nights = 'Nights cannot exceed 365';
    }

    editValidationErrors = errors;
    return Object.keys(errors).length === 0;
  }

  async function handleSaveEdit() {
    if (!validateEditForm()) return;
    if (!splitId || !editingParticipantId) return;

    isEditSubmitting = true;

    try {
      await updateParticipant(splitId, editingParticipantId, {
        name: editName.trim(),
        nights: editNights,
      });

      // Refresh split data
      await loadSplit(splitId);

      // Close edit mode and show success
      editingParticipantId = null;
      editName = '';
      editNights = 1;
      addToast({
        type: 'success',
        message: 'Participant updated',
        duration: 3000,
      });
    } catch (err) {
      const apiError = err as ApiError;
      addToast({
        type: 'error',
        message: apiError.detail || 'Failed to update participant. Please try again.',
      });
      // Keep edit mode open for retry (AC 8 pattern)
    } finally {
      isEditSubmitting = false;
    }
  }

  // Helper function to get payer name for an expense
  function getPayerName(expense: Expense): string {
    if (!split) return 'Unknown';
    const payer = split.participants.find(p => p.id === expense.payerId);
    return payer?.name || 'Unknown';
  }

  // Helper function to format split mode badge
  function formatSplitMode(mode: Expense['splitMode']): string {
    switch (mode) {
      case 'BY_NIGHT': return 'By Night';
      case 'EQUAL': return 'Equal';
      case 'FREE': return 'Free';
      default: return mode;
    }
  }

  // Helper function to format currency
  function formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IE', {
      style: 'currency',
      currency: 'EUR',
    }).format(amount);
  }

  // Sort expenses by createdAt (newest first)
  const sortedExpenses = $derived(
    split?.expenses ? [...split.expenses].sort((a, b) =>
      new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    ) : []
  );
</script>

<div class="flex flex-col items-center space-y-6 w-full max-w-[420px] mx-auto">
  {#if isLoading}
    <!-- Loading State -->
    <div class="flex flex-col items-center justify-center py-12 space-y-4">
      <svg class="animate-spin h-8 w-8 text-primary" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
      </svg>
      <p class="text-muted-foreground">Loading split...</p>
    </div>

  {:else if notFound}
    <!-- 404 Not Found State (AC 5) -->
    <Card.Root class="w-full">
      <Card.Header>
        <Card.Title class="text-center text-destructive">Split not found</Card.Title>
      </Card.Header>
      <Card.Content class="text-center space-y-4">
        <p class="text-muted-foreground">
          The split you're looking for doesn't exist or may have been removed.
        </p>
        <Button onclick={handleGoHome} class="min-h-[44px]">
          Create a new split
        </Button>
      </Card.Content>
    </Card.Root>

  {:else if error}
    <!-- Network Error State (AC 6) -->
    <Card.Root class="w-full">
      <Card.Header>
        <Card.Title class="text-center text-destructive">Error</Card.Title>
      </Card.Header>
      <Card.Content class="text-center space-y-4">
        <p class="text-muted-foreground">{error}</p>
        <Button onclick={handleRetry} class="min-h-[44px]">
          Retry
        </Button>
      </Card.Content>
    </Card.Root>

  {:else if split}
    <!-- Split Overview (AC 1, 2) -->

    <!-- Header with split name and share button (AC 1) -->
    <header class="w-full">
      <Card.Root>
        <Card.Content class="flex items-center justify-between py-4">
          <h1 class="text-xl font-bold text-foreground truncate">{split.name}</h1>
          <Button
            onclick={handleShare}
            variant={copyConfirmation ? 'secondary' : 'outline'}
            size="sm"
            class="shrink-0 min-h-[44px]"
          >
            {copyConfirmation ? 'Copied!' : 'Share'}
          </Button>
        </Card.Content>
      </Card.Root>
    </header>

    <!-- Participants Section (AC 2, 3) + Add Participant (Story 3.1) -->
    <section class="w-full">
      <Card.Root>
        <Card.Header class="pb-2 flex flex-row items-center justify-between">
          <Card.Title class="text-lg">Participants</Card.Title>
          {#if !showAddForm}
            <Button
              onclick={handleShowAddForm}
              variant="outline"
              size="sm"
              class="min-h-[44px]"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd" />
              </svg>
              Add Participant
            </Button>
          {/if}
        </Card.Header>
        <Card.Content>
          <!-- Add Participant Form (Story 3.1 AC 1, 2) -->
          {#if showAddForm}
            <form onsubmit={(e) => { e.preventDefault(); handleAddParticipant(); }} class="space-y-4 mb-4 p-4 bg-secondary/30 rounded-lg">
              <div class="space-y-2">
                <Label for="participant-name">Name</Label>
                <Input
                  id="participant-name"
                  type="text"
                  placeholder="Enter name"
                  bind:value={formName}
                  class="min-h-[44px]"
                  disabled={isSubmitting}
                />
                {#if validationErrors.name}
                  <p class="text-sm text-destructive">{validationErrors.name}</p>
                {/if}
              </div>

              <div class="space-y-2">
                <Label for="participant-nights">Nights</Label>
                <Input
                  id="participant-nights"
                  type="number"
                  bind:value={formNights}
                  class="min-h-[44px]"
                  disabled={isSubmitting}
                />
                {#if validationErrors.nights}
                  <p class="text-sm text-destructive">{validationErrors.nights}</p>
                {/if}
              </div>

              <div class="flex gap-2">
                <Button
                  type="submit"
                  class="flex-1 min-h-[44px]"
                  disabled={isSubmitting}
                >
                  {#if isSubmitting}
                    <svg class="animate-spin h-4 w-4 mr-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Adding...
                  {:else}
                    Add
                  {/if}
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  onclick={handleCancelAddForm}
                  class="min-h-[44px]"
                  disabled={isSubmitting}
                >
                  Cancel
                </Button>
              </div>
            </form>
          {/if}

          <!-- Participants List -->
          {#if split.participants.length === 0 && !showAddForm}
            <p class="text-muted-foreground text-center py-4">No participants yet</p>
          {:else if split.participants.length > 0}
            <div class="space-y-3">
              {#each split.participants as participant}
                {#if editingParticipantId === participant.id}
                  <!-- Edit Mode (Story 3.2 AC 1) -->
                  <form
                    onsubmit={(e) => { e.preventDefault(); handleSaveEdit(); }}
                    class="p-3 bg-secondary/30 rounded-lg space-y-3"
                  >
                    <div class="space-y-2">
                      <Label for="edit-participant-name">Name</Label>
                      <Input
                        id="edit-participant-name"
                        type="text"
                        bind:value={editName}
                        class="min-h-[44px]"
                        disabled={isEditSubmitting}
                      />
                      {#if editValidationErrors.name}
                        <p class="text-sm text-destructive">{editValidationErrors.name}</p>
                      {/if}
                    </div>

                    <div class="space-y-2">
                      <Label for="edit-participant-nights">Nights</Label>
                      <Input
                        id="edit-participant-nights"
                        type="number"
                        bind:value={editNights}
                        class="min-h-[44px]"
                        disabled={isEditSubmitting}
                      />
                      {#if editValidationErrors.nights}
                        <p class="text-sm text-destructive">{editValidationErrors.nights}</p>
                      {/if}
                    </div>

                    <div class="flex gap-2">
                      <Button
                        type="submit"
                        class="flex-1 min-h-[44px]"
                        disabled={isEditSubmitting}
                      >
                        {#if isEditSubmitting}
                          <svg class="animate-spin h-4 w-4 mr-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                          </svg>
                          Saving...
                        {:else}
                          Save
                        {/if}
                      </Button>
                      <Button
                        type="button"
                        variant="outline"
                        onclick={handleCancelEdit}
                        class="min-h-[44px]"
                        disabled={isEditSubmitting}
                      >
                        Cancel
                      </Button>
                    </div>
                  </form>
                {:else}
                  <!-- Display Mode (clickable for edit) -->
                  <button
                    type="button"
                    onclick={() => handleStartEdit(participant)}
                    class="flex items-center justify-between p-3 bg-secondary/50 rounded-lg w-full text-left hover:bg-secondary/70 transition-colors cursor-pointer"
                    disabled={isEditSubmitting}
                  >
                    <div>
                      <p class="font-medium text-foreground">{participant.name}</p>
                      <p class="text-sm text-muted-foreground">{participant.nights} night{participant.nights !== 1 ? 's' : ''}</p>
                    </div>
                    <div class="text-right">
                      <p class="font-medium text-foreground">{formatCurrency(0)}</p>
                      <p class="text-xs text-muted-foreground">balance</p>
                    </div>
                  </button>
                {/if}
              {/each}
            </div>
          {/if}
        </Card.Content>
      </Card.Root>
    </section>

    <!-- Expenses Section (AC 2, 4) -->
    <section class="w-full">
      <Card.Root>
        <Card.Header class="pb-2">
          <Card.Title class="text-lg">Expenses</Card.Title>
        </Card.Header>
        <Card.Content>
          {#if split.expenses.length === 0}
            <p class="text-muted-foreground text-center py-4">No expenses yet</p>
          {:else}
            <div class="space-y-3">
              {#each sortedExpenses as expense}
                <div class="flex items-center justify-between p-3 bg-secondary/50 rounded-lg">
                  <div class="min-w-0 flex-1">
                    <p class="font-medium text-foreground truncate">{expense.description}</p>
                    <p class="text-sm text-muted-foreground">Paid by {getPayerName(expense)}</p>
                  </div>
                  <div class="text-right shrink-0 ml-3">
                    <p class="font-medium text-foreground">{formatCurrency(expense.amount)}</p>
                    <span class="text-xs px-2 py-1 rounded bg-secondary text-secondary-foreground">
                      {formatSplitMode(expense.splitMode)}
                    </span>
                  </div>
                </div>
              {/each}
            </div>
          {/if}
        </Card.Content>
      </Card.Root>
    </section>

    <!-- Balance Summary Section (AC 2) -->
    <section class="w-full">
      <Card.Root>
        <Card.Header class="pb-2">
          <Card.Title class="text-lg">Balance Summary</Card.Title>
        </Card.Header>
        <Card.Content>
          {#if split.participants.length === 0 || split.expenses.length === 0}
            <p class="text-muted-foreground text-center py-4">
              Add participants and expenses to see balances
            </p>
          {:else}
            <p class="text-muted-foreground text-center py-4">
              Balance calculation coming soon...
            </p>
          {/if}
        </Card.Content>
      </Card.Root>
    </section>
  {/if}
</div>
