<script lang="ts">
  // Home page - Landing / Create Split
  // Story 2.2: Create Split Frontend
  import { Button } from '$lib/components/ui/button';
  import { Input } from '$lib/components/ui/input';
  import * as Card from '$lib/components/ui/card';
  import { Label } from '$lib/components/ui/label';
  import { createSplit, type Split } from '$lib/api/splits';
  import { addToast } from '$lib/stores/toastStore.svelte';
  import type { ApiError } from '$lib/api/client';
  import { p, navigate } from '$lib/router';

  // State using Svelte 5 runes
  let splitName = $state('');
  let isLoading = $state(false);
  let validationError = $state<string | null>(null);
  let createdSplit = $state<Split | null>(null);
  let copyConfirmation = $state(false);

  // Derived: shareable URL
  let shareableUrl = $derived(
    createdSplit ? `${window.location.origin}/splits/${createdSplit.id}` : ''
  );

  function clearValidationError() {
    if (validationError) {
      validationError = null;
    }
  }

  async function handleCreateSplit() {
    // Client-side validation
    if (!splitName.trim()) {
      validationError = 'Split name is required';
      return;
    }

    validationError = null;
    isLoading = true;

    try {
      const split = await createSplit({ name: splitName.trim() });
      createdSplit = split;
    } catch (err) {
      const apiError = err as ApiError;
      addToast({
        type: 'error',
        message: apiError.detail || 'Failed to create split. Please try again.',
      });
    } finally {
      isLoading = false;
    }
  }

  async function handleCopyLink() {
    if (!shareableUrl) return;

    try {
      await navigator.clipboard.writeText(shareableUrl);
      copyConfirmation = true;
      addToast({
        type: 'success',
        message: 'Link copied to clipboard!',
        duration: 3000,
      });
      // Reset confirmation after 2 seconds
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

  function handleGoToSplit() {
    if (createdSplit) {
      navigate('/splits/:splitId', { params: { splitId: createdSplit.id } });
    }
  }

  function handleCreateAnother() {
    splitName = '';
    createdSplit = null;
    validationError = null;
    copyConfirmation = false;
  }
</script>

<div class="flex flex-col items-center space-y-6">
  <!-- Header -->
  <header class="text-center py-8">
    <h1 class="text-2xl font-bold text-foreground">FairNSquare</h1>
    <p class="text-muted-foreground mt-2">Split expenses fairly with friends</p>
  </header>

  <!-- Create Split Form / Success State -->
  <div class="w-full max-w-[420px]">
    {#if createdSplit}
      <!-- Success State -->
      <Card.Root>
        <Card.Header>
          <Card.Title class="text-center text-success">Split Created!</Card.Title>
        </Card.Header>
        <Card.Content class="space-y-4">
          <div class="space-y-2">
            <Label>Your shareable link</Label>
            <div class="flex gap-2">
              <Input
                type="text"
                value={shareableUrl}
                readonly
                class="flex-1 text-sm"
              />
              <Button
                onclick={handleCopyLink}
                variant={copyConfirmation ? 'secondary' : 'outline'}
                class="min-h-[44px] shrink-0"
              >
                {copyConfirmation ? 'Copied!' : 'Copy Link'}
              </Button>
            </div>
          </div>

          <div class="flex flex-col gap-2">
            <Button
              onclick={handleGoToSplit}
              class="w-full min-h-[44px]"
            >
              Go to Split
            </Button>
            <Button
              onclick={handleCreateAnother}
              variant="outline"
              class="w-full min-h-[44px]"
            >
              Create Another Split
            </Button>
          </div>
        </Card.Content>
      </Card.Root>
    {:else}
      <!-- Create Split Form -->
      <Card.Root>
        <Card.Header>
          <Card.Title>Create a New Split</Card.Title>
        </Card.Header>
        <Card.Content>
          <form onsubmit={(e) => { e.preventDefault(); handleCreateSplit(); }} class="space-y-4">
            <div class="space-y-2">
              <Label for="splitName">Split Name</Label>
              <Input
                type="text"
                id="splitName"
                bind:value={splitName}
                oninput={clearValidationError}
                placeholder="e.g., Bordeaux Weekend 2026"
                disabled={isLoading}
                class="min-h-[44px]"
                aria-invalid={validationError ? 'true' : undefined}
                aria-describedby={validationError ? 'splitName-error' : undefined}
              />
              {#if validationError}
                <p id="splitName-error" class="text-sm text-destructive">
                  {validationError}
                </p>
              {/if}
            </div>

            <Button
              type="submit"
              disabled={isLoading}
              class="w-full min-h-[44px]"
            >
              {#if isLoading}
                <svg class="animate-spin -ml-1 mr-2 h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Creating...
              {:else}
                Create Split
              {/if}
            </Button>
          </form>
        </Card.Content>
      </Card.Root>
    {/if}
  </div>

  <!-- Info Section -->
  <section class="text-center text-muted-foreground text-sm max-w-[420px]">
    <p>No account needed. Create a split and share the link!</p>
  </section>
</div>
