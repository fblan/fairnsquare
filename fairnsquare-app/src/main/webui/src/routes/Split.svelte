<script lang="ts">
  // Split Overview Page
  // Story 2.3: Access Split via Link & View Overview

  import { getSplit, type Split, type Participant, type Expense } from '$lib/api/splits';
  import type { ApiError } from '$lib/api/client';
  import { Button } from '$lib/components/ui/button';
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

    <!-- Participants Section (AC 2, 3) -->
    <section class="w-full">
      <Card.Root>
        <Card.Header class="pb-2">
          <Card.Title class="text-lg">Participants</Card.Title>
        </Card.Header>
        <Card.Content>
          {#if split.participants.length === 0}
            <p class="text-muted-foreground text-center py-4">No participants yet</p>
          {:else}
            <div class="space-y-3">
              {#each split.participants as participant}
                <div class="flex items-center justify-between p-3 bg-secondary/50 rounded-lg">
                  <div>
                    <p class="font-medium text-foreground">{participant.name}</p>
                    <p class="text-sm text-muted-foreground">{participant.nights} night{participant.nights !== 1 ? 's' : ''}</p>
                  </div>
                  <div class="text-right">
                    <p class="font-medium text-foreground">{formatCurrency(0)}</p>
                    <p class="text-xs text-muted-foreground">balance</p>
                  </div>
                </div>
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
