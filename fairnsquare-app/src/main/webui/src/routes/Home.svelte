<script lang="ts">
  // Home page - Create Split with First Participant
  // Story FNS-002-1: Create Split Flow & Direct Dashboard Redirect
  import { Button } from '$lib/components/ui/button';
  import { Input } from '$lib/components/ui/input';
  import * as Card from '$lib/components/ui/card';
  import { Label } from '$lib/components/ui/label';
  import { createSplit, addParticipant } from '$lib/api/splits';
  import { addToast } from '$lib/stores/toastStore.svelte';
  import type { ApiError } from '$lib/api/client';
  import { navigate } from '$lib/router';

  // Form state
  let splitName = $state('');
  let participantName = $state('');
  let nights = $state(1);
  let numberOfPersons = $state(1);
  let isLoading = $state(false);

  // Track whether fields have been touched for validation display
  let splitNameTouched = $state(false);
  let participantNameTouched = $state(false);
  let nightsTouched = $state(false);
  let numberOfPersonsTouched = $state(false);

  // Derived validation errors (only shown after field is touched)
  let splitNameError = $derived.by(() => {
    if (!splitNameTouched) return null;
    if (!splitName.trim()) return 'Split name is required';
    if (splitName.length > 100) return 'Split name cannot exceed 100 characters';
    return null;
  });

  let participantNameError = $derived.by(() => {
    if (!participantNameTouched) return null;
    if (!participantName.trim()) return 'Participant name is required';
    if (participantName.length > 50) return 'Participant name cannot exceed 50 characters';
    return null;
  });

  let nightsError = $derived.by(() => {
    if (!nightsTouched) return null;
    if (nights < 0.5) return 'Nights must be at least 0.5';
    if (nights > 365) return 'Nights cannot exceed 365';
    return null;
  });

  let numberOfPersonsError = $derived.by(() => {
    if (!numberOfPersonsTouched) return null;
    if (numberOfPersons < 0.5) return 'Persons must be at least 0.5';
    if (numberOfPersons > 50) return 'Persons cannot exceed 50';
    return null;
  });

  // Derived: form validity (independent of touched state)
  let isValid = $derived(
    splitName.trim().length > 0 &&
    splitName.length <= 100 &&
    participantName.trim().length > 0 &&
    participantName.length <= 50 &&
    nights >= 0.5 &&
    nights <= 365 &&
    numberOfPersons >= 0.5 &&
    numberOfPersons <= 50
  );

  async function handleCreateSplit() {
    // Touch all fields to show errors
    splitNameTouched = true;
    participantNameTouched = true;
    nightsTouched = true;
    numberOfPersonsTouched = true;

    if (!isValid) return;

    isLoading = true;

    try {
      // Step 1: Create the split
      const split = await createSplit({ name: splitName.trim() });

      // Step 2: Add the first participant
      await addParticipant(split.id, {
        name: participantName.trim(),
        nights,
        numberOfPersons,
      });

      // Step 3: Go to participants page with form pre-opened to add next participant
      navigate('/splits/:splitId/participants', { params: { splitId: split.id }, search: { addParticipant: 'true' } });
    } catch (err) {
      const apiError = err as ApiError;
      addToast({
        type: 'error',
        message: apiError.detail || 'Failed to create split',
      });
    } finally {
      isLoading = false;
    }
  }
</script>

<div class="flex flex-col items-center space-y-6">
  <!-- Header -->
  <header class="text-center py-8">
    <h1 class="text-2xl font-bold text-primary">FairNSquare</h1>
    <p class="text-muted-foreground mt-2">Split expenses fairly with friends</p>
  </header>

  <!-- Create Split Form -->
  <div class="w-full max-w-[420px]">
    <Card.Root>
      <Card.Header>
        <Card.Title>Create a New Split</Card.Title>
      </Card.Header>
      <Card.Content>
        <form onsubmit={(e) => { e.preventDefault(); handleCreateSplit(); }} class="space-y-4">
          <!-- Split Name -->
          <div class="space-y-2">
            <Label for="splitName">Split Name</Label>
            <Input
              type="text"
              id="splitName"
              bind:value={splitName}
              onblur={() => { splitNameTouched = true; }}
              oninput={() => { splitNameTouched = true; }}
              placeholder="e.g., Weekend Trip"
              disabled={isLoading}
              class="min-h-[44px]"
              aria-invalid={splitNameError ? 'true' : undefined}
              aria-describedby={splitNameError ? 'splitName-error' : undefined}
            />
            {#if splitNameError}
              <p id="splitName-error" class="text-sm text-destructive">
                {splitNameError}
              </p>
            {/if}
          </div>

          <!-- First Participant Section -->
          <div class="rounded-lg border border-teal-300 bg-teal-50 p-4 space-y-3">
            <p class="text-sm font-medium text-teal-700">First Participant</p>

            <div class="space-y-2">
              <Label for="participantName">Name</Label>
              <Input
                type="text"
                id="participantName"
                bind:value={participantName}
                onblur={() => { participantNameTouched = true; }}
                oninput={() => { participantNameTouched = true; }}
                placeholder="Your name"
                disabled={isLoading}
                class="min-h-[44px]"
                aria-invalid={participantNameError ? 'true' : undefined}
                aria-describedby={participantNameError ? 'participantName-error' : undefined}
              />
              {#if participantNameError}
                <p id="participantName-error" class="text-sm text-destructive">
                  {participantNameError}
                </p>
              {/if}
            </div>

            <div class="flex gap-3">
              <div class="space-y-2 flex-1">
                <Label for="nights">Nights</Label>
                <Input
                  type="number"
                  id="nights"
                  bind:value={nights}
                  onblur={() => { nightsTouched = true; }}
                  oninput={() => { nightsTouched = true; }}
                  step={0.5}
                  min={0.5}
                  max={365}
                  disabled={isLoading}
                  class="min-h-[44px]"
                  aria-invalid={nightsError ? 'true' : undefined}
                  aria-describedby={nightsError ? 'nights-error' : undefined}
                />
                {#if nightsError}
                  <p id="nights-error" class="text-sm text-destructive">
                    {nightsError}
                  </p>
                {/if}
              </div>

              <div class="space-y-2 flex-1">
                <Label for="numberOfPersons">Persons</Label>
                <Input
                  type="number"
                  id="numberOfPersons"
                  bind:value={numberOfPersons}
                  onblur={() => { numberOfPersonsTouched = true; }}
                  oninput={() => { numberOfPersonsTouched = true; }}
                  step={0.5}
                  min={0.5}
                  max={50}
                  disabled={isLoading}
                  class="min-h-[44px]"
                  aria-invalid={numberOfPersonsError ? 'true' : undefined}
                  aria-describedby={numberOfPersonsError ? 'numberOfPersons-error' : undefined}
                />
                {#if numberOfPersonsError}
                  <p id="numberOfPersons-error" class="text-sm text-destructive">
                    {numberOfPersonsError}
                  </p>
                {/if}
              </div>
            </div>
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
  </div>

  <!-- Info Section -->
  <section class="text-center text-muted-foreground text-sm max-w-[420px]">
    <p>No account needed. Create a split and share the link!</p>
  </section>
</div>