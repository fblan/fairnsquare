<script lang="ts">
  // Split Overview Page - Refactored
  // Story 2.3: Access Split via Link & View Overview
  // Components extracted: ParticipantsSection, ExpensesSection

  import { getSplit, type Split } from '$lib/api/splits';
  import type { ApiError } from '$lib/api/client';
  import { Button } from '$lib/components/ui/button';
  import * as Card from '$lib/components/ui/card';
  import { addToast } from '$lib/stores/toastStore.svelte';
  import { route, navigate } from '$lib/router';
  import ParticipantsSection from './ParticipantsSection.svelte';
  import ExpensesSection from './ExpensesSection.svelte';

  // Extract splitId from route params
  const splitId = $derived(route.params.splitId || '');

  // State
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

  // Copy shareable URL to clipboard (Story 2.3 AC 3)
  async function handleCopyUrl() {
    if (typeof window === 'undefined') return;

    try {
      await navigator.clipboard.writeText(shareableUrl);
      copyConfirmation = true;
      setTimeout(() => {
        copyConfirmation = false;
      }, 2000);

      addToast({
        type: 'success',
        message: 'Link copied to clipboard',
      });
    } catch (err) {
      addToast({
        type: 'error',
        message: 'Failed to copy link',
      });
    }
  }

  function handleGoHome() {
    navigate('/');
  }
</script>

<div class="flex flex-col items-center space-y-6 w-full max-w-[420px] mx-auto">
  {#if isLoading}
    <!-- Loading State -->
    <div class="flex flex-col items-center justify-center py-12 space-y-4">
      <svg class="animate-spin h-8 w-4 text-primary" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
      </svg>
      <p class="text-muted-foreground">Loading split...</p>
    </div>

  {:else if notFound}
    <!-- 404 Not Found State -->
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
    <!-- Error State -->
    <Card.Root class="w-full">
      <Card.Header>
        <Card.Title class="text-center text-destructive">Error loading split</Card.Title>
      </Card.Header>
      <Card.Content class="text-center space-y-4">
        <p class="text-muted-foreground">{error}</p>
        <Button onclick={() => loadSplit(splitId)} class="min-h-[44px]">
          Try again
        </Button>
      </Card.Content>
    </Card.Root>

  {:else if split}
    <!-- Split Header (AC 2: Name, Shareable URL) -->
    <section class="w-full">
      <Card.Root>
        <Card.Header class="pb-2">
          <Card.Title class="text-xl">{split.name}</Card.Title>
        </Card.Header>
        <Card.Content class="space-y-3">
          <!-- Shareable URL (AC 3) -->
          <div class="flex items-center gap-2">
            <input
              type="text"
              readonly
              value={shareableUrl}
              class="flex-1 px-3 py-2 text-sm bg-secondary border border-input rounded-md truncate"
              onclick={(e) => e.currentTarget.select()}
            />
            <Button
              onclick={handleCopyUrl}
              variant="outline"
              size="sm"
              class="min-h-[44px] shrink-0"
            >
              {copyConfirmation ? 'Copied!' : 'Copy'}
            </Button>
          </div>
        </Card.Content>
      </Card.Root>
    </section>

    <!-- Participants Section -->
    <ParticipantsSection {split} onSplitUpdated={() => loadSplit(splitId)} />

    <!-- Expenses Section -->
    <ExpensesSection {split} onSplitUpdated={() => loadSplit(splitId)} />

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
