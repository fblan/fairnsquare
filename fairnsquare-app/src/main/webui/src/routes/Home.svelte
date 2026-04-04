<script lang="ts">
  // Home page - Create Split
  // Issue #94: Rework welcome page — list of recent splits, no first-participant form
  import { Button } from '$lib/components/ui/button';
  import { Input } from '$lib/components/ui/input';
  import * as Card from '$lib/components/ui/card';
  import { Label } from '$lib/components/ui/label';
  import { createSplit, getSplit } from '$lib/api/splits';
  import { addToast } from '$lib/stores/toastStore.svelte';
  import type { ApiError } from '$lib/api/client';
  import { navigate } from '$lib/router';
  import { saveLastSplit, loadLastSplits, removeLastSplit } from '$lib/stores/lastSplitStore';
  import { hasValidToken, loadToken, clearToken, CAPTCHA_TOKEN_HEADER } from '$lib/stores/captchaStore';
  import CaptchaModal from '$lib/components/ui/captcha/CaptchaModal.svelte';

  // CAPTCHA state — shown when user clicks Create Split without a valid token
  let showCaptcha = $state(false);

  function handleCaptchaSuccess() {
    showCaptcha = false;
    doCreateSplit();
  }

  // Recent splits state
  interface RecentSplit { id: string; name: string }
  let recentSplits = $state<RecentSplit[]>([]);

  $effect(() => {
    const stored = loadLastSplits();
    if (stored.length === 0) return;
    Promise.allSettled(stored.map((s) => getSplit(s.id))).then((results) => {
      const verified: RecentSplit[] = [];
      results.forEach((result, i) => {
        if (result.status === 'fulfilled') {
          verified.push({ id: result.value.id, name: result.value.name });
        } else {
          removeLastSplit(stored[i].id);
        }
      });
      recentSplits = verified;
    });
  });

  function handleResume(id: string) {
    navigate('/splits/:splitId', { params: { splitId: id } });
  }

  function handleDismiss(id: string) {
    removeLastSplit(id);
    recentSplits = recentSplits.filter((s) => s.id !== id);
  }

  // Form state
  let splitName = $state('');
  let isLoading = $state(false);
  let splitNameTouched = $state(false);

  let splitNameError = $derived.by(() => {
    if (!splitNameTouched) return null;
    if (!splitName.trim()) return 'Split name is required';
    if (splitName.length > 100) return 'Split name cannot exceed 100 characters';
    return null;
  });

  let isValid = $derived(
    splitName.trim().length > 0 && splitName.length <= 100
  );

  function handleCreateSplit() {
    splitNameTouched = true;
    if (!isValid) return;
    if (!hasValidToken()) {
      showCaptcha = true;
      return;
    }
    doCreateSplit();
  }

  async function doCreateSplit() {
    isLoading = true;
    try {
      const token = loadToken();
      if (!token) {
        showCaptcha = true;
        return;
      }
      const split = await createSplit({ name: splitName.trim() }, { [CAPTCHA_TOKEN_HEADER]: token });
      navigate('/splits/:splitId/participants', { params: { splitId: split.id } });
    } catch (err) {
      const apiError = err as ApiError;
      if (apiError.status === 401) {
        clearToken();
        showCaptcha = true;
      } else {
        addToast({
          type: 'error',
          message: apiError.detail || 'Failed to create split',
        });
      }
    } finally {
      isLoading = false;
    }
  }
</script>

<CaptchaModal open={showCaptcha} onSuccess={handleCaptchaSuccess} />

<div class="flex flex-col items-center space-y-6">
  <!-- Header -->
  <header class="text-center py-8">
    <h1 class="text-2xl font-bold text-primary">FairNSquare</h1>
    <p class="text-muted-foreground mt-2">Split expenses fairly with friends</p>
  </header>

  <!-- Create Split Form -->
  <div class="w-full max-w-[520px]">
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

  <!-- Recent Splits -->
  {#if recentSplits.length > 0}
    <div class="w-full max-w-[520px]">
      <Card.Root class="border-teal-300 bg-teal-50">
        <Card.Header class="pb-2">
          <Card.Title class="text-base">Recent splits</Card.Title>
        </Card.Header>
        <Card.Content class="space-y-2">
          {#each recentSplits as split (split.id)}
            <div class="flex items-center gap-3 min-h-[44px]">
              <span class="flex-1 min-w-0 truncate text-sm font-medium text-foreground">{split.name}</span>
              <Button size="sm" onclick={() => handleResume(split.id)} class="shrink-0 min-h-[36px]">
                Resume
              </Button>
              <button
                onclick={() => handleDismiss(split.id)}
                class="shrink-0 text-sm text-muted-foreground underline underline-offset-2 hover:text-foreground"
              >
                Dismiss
              </button>
            </div>
          {/each}
        </Card.Content>
      </Card.Root>
    </div>
  {/if}

  <!-- Info Section -->
  <section class="text-center text-muted-foreground text-sm max-w-[520px]">
    <p>No account needed. Create a split and share the link!</p>
  </section>

  <!-- Dev tools -->
  {#if import.meta.env.DEV}
    <section class="w-full max-w-[520px] border border-dashed border-orange-300 rounded-lg p-3 space-y-1">
      <p class="text-xs font-semibold text-orange-500 uppercase tracking-wide">Dev tools</p>
      <button
        onclick={() => clearToken()}
        class="text-xs text-orange-600 underline underline-offset-2 hover:text-orange-800"
      >
        Clear CAPTCHA token
      </button>
    </section>
  {/if}
</div>
