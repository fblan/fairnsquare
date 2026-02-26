<script lang="ts">
  // Split Dashboard - Participant-Centric View
  // Story FNS-002-2: Main Dashboard with Participant Cards

  import { getSplit, type Split } from '$lib/api/splits';
  import type { ApiError } from '$lib/api/client';
  import { Button } from '$lib/components/ui/button';
  import * as Card from '$lib/components/ui/card';
  import { addToast } from '$lib/stores/toastStore.svelte';
  import { route, navigate } from '$lib/router';
  import ParticipantSummaryCard from '$lib/components/participant/ParticipantSummaryCard.svelte';

  // Extract splitId from route params
  const splitId = $derived(route.params.splitId || '');

  // State
  let split = $state<Split | null>(null);
  let isLoading = $state(true);
  let error = $state<string | null>(null);

  // Expense summary stats
  const expenseCount = $derived(split?.expenses.length ?? 0);
  const expenseTotal = $derived(
    split?.expenses.reduce((sum, e) => sum + e.amount, 0) ?? 0
  );

  // Settlement status
  const isSettled = $derived(split?.settlement != null);

  // Participant count (used for settlement card visibility)
  const participantCount = $derived(split?.participants.length ?? 0);

  // Fetch split data when splitId changes
  $effect(() => {
    if (splitId) {
      loadSplit(splitId);
    }
  });

  async function loadSplit(id: string) {
    isLoading = true;
    error = null;
    split = null;

    try {
      split = await getSplit(id);
    } catch (err) {
      const apiError = err as ApiError;
      if (apiError.status === 404 || apiError.status === 400) {
        addToast({ type: 'info', message: 'Split not found — create a new one.' });
        navigate('/');
      } else {
        error = apiError.detail || 'Failed to load split. Please try again.';
      }
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

  async function handleShare() {
    if (typeof window === 'undefined') return;

    const url = window.location.href;

    if (!navigator.clipboard) {
      // Fallback: clipboard API not available (older browsers)
      addToast({
        type: 'info',
        message: `Share link: ${url}`,
      });
      return;
    }

    try {
      await navigator.clipboard.writeText(url);
      addToast({
        type: 'success',
        message: 'Link copied!',
      });
    } catch {
      // Fallback: clipboard access denied
      addToast({
        type: 'info',
        message: `Share link: ${url}`,
      });
    }
  }

</script>

<div class="flex flex-col items-center space-y-4 w-full max-w-[420px] mx-auto">
  {#if isLoading}
    <!-- Loading State -->
    <div class="flex flex-col items-center justify-center py-12 space-y-4">
      <svg class="animate-spin h-8 w-8 text-primary" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
      </svg>
      <p class="text-muted-foreground">Loading split...</p>
    </div>

  {:else if error}
    <!-- Error State -->
    <Card.Root class="w-full">
      <Card.Header>
        <Card.Title class="text-center text-destructive">Error loading split</Card.Title>
      </Card.Header>
      <Card.Content class="text-center space-y-4">
        <p class="text-muted-foreground">{error}</p>
        <Button onclick={() => loadSplit(splitId)} class="min-h-[44px]">
          Retry
        </Button>
      </Card.Content>
    </Card.Root>

  {:else if split}
    <!-- Dashboard Header -->
    <header class="w-full flex items-center justify-between">
      <h1 class="text-xl font-bold text-primary">{split.name}</h1>
      <Button
        onclick={handleShare}
        variant="outline"
        size="sm"
        class="min-h-[44px]"
        aria-label="Share"
      >
        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor">
          <path d="M15 8a3 3 0 10-2.977-2.63l-4.94 2.47a3 3 0 100 4.319l4.94 2.47a3 3 0 10.895-1.789l-4.94-2.47a3.027 3.027 0 000-.74l4.94-2.47C13.456 7.68 14.19 8 15 8z" />
        </svg>
        Share
      </Button>
    </header>

    <!-- Expense Summary Card (clickable → navigates to expense list) -->
    <section class="w-full">
      <button
        class="w-full text-left"
        onclick={() => navigate(`/splits/${splitId}/expenses`)}
        aria-label="View all expenses"
      >
        <Card.Root class="border-teal-200 bg-teal-50/50 hover:bg-teal-50 transition-colors cursor-pointer">
          <Card.Content class="py-4">
            <div class="flex items-center justify-between">
              <div>
                <p class="text-sm text-muted-foreground">Expenses</p>
                <p class="text-lg font-semibold">{expenseCount} {expenseCount === 1 ? 'expense' : 'expenses'}</p>
              </div>
              <div class="text-right">
                <p class="text-sm text-muted-foreground">Total</p>
                <p class="text-lg font-semibold text-primary">{formatCurrency(expenseTotal)}</p>
              </div>
            </div>
          </Card.Content>
        </Card.Root>
      </button>
    </section>

    <!-- Participants Summary Card (clickable → navigates to participants page) -->
    <section class="w-full">
      <button
        class="w-full text-left hover:opacity-90 transition-opacity cursor-pointer"
        onclick={() => navigate(`/splits/${splitId}/participants`)}
        aria-label="View all participants"
      >
        <ParticipantSummaryCard participants={split.participants} />
      </button>
    </section>

    <!-- Solve Card (clickable → navigates to settlement page) -->
    {#if expenseCount > 0 && participantCount > 0}
      <section class="w-full">
        <button
          class="w-full text-left"
          onclick={() => {
            if (isSettled) sessionStorage.setItem('settlement-resolved', 'true');
            navigate(`/splits/${splitId}/settlement`);
          }}
          aria-label="View settlement"
        >
          <Card.Root class="border-teal-200 bg-teal-50/50 hover:bg-teal-50 transition-colors cursor-pointer">
            <Card.Content class="py-4">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm text-muted-foreground">Settlement</p>
                  <p class="text-lg font-semibold">{isSettled ? 'Settled' : 'Solve'}</p>
                </div>
                <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
                </svg>
              </div>
            </Card.Content>
          </Card.Root>
        </button>
      </section>
    {/if}

  {/if}
</div>
