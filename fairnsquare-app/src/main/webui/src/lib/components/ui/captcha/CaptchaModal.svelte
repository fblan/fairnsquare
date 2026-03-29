<script lang="ts">
  /**
   * CaptchaModal — human verification dialog.
   * Loads a math-addition CAPTCHA from the server (encrypted token + inline base64 image).
   * User must click the correct answer area; on success a signed token is stored
   * in localStorage and onSuccess() is called.
   * The dialog cannot be dismissed without solving the challenge.
   */

  import { createChallenge, verifyChallenge } from '$lib/api/captcha';
  import { saveToken } from '$lib/stores/captchaStore';
  import Button from '$lib/components/ui/button/button.svelte';
  import { RefreshCw } from 'lucide-svelte';

  interface Props {
    open: boolean;
    onSuccess: () => void;
  }

  let { open, onSuccess }: Props = $props();

  let challengeToken = $state<string | null>(null);
  let imageBase64 = $state<string | null>(null);
  let loading = $state(false);
  let errorMessage = $state<string | null>(null);
  let imgElement = $state<HTMLImageElement | null>(null);

  async function loadChallenge() {
    loading = true;
    challengeToken = null;
    imageBase64 = null;
    try {
      const response = await createChallenge();
      challengeToken = response.challengeToken;
      imageBase64 = response.imageBase64;
      errorMessage = null;
    } catch {
      errorMessage = 'Failed to load the verification challenge. Please try again.';
    } finally {
      loading = false;
    }
  }

  $effect(() => {
    if (open && !challengeToken && !loading) {
      loadChallenge();
    }
  });

  async function handleImageClick(event: MouseEvent) {
    if (!challengeToken || !imgElement || loading) return;

    // Scale CSS pixel coordinates to actual image pixel coordinates
    const rect = imgElement.getBoundingClientRect();
    const scaleX = imgElement.naturalWidth / rect.width;
    const scaleY = imgElement.naturalHeight / rect.height;
    const x = Math.round((event.clientX - rect.left) * scaleX);
    const y = Math.round((event.clientY - rect.top) * scaleY);

    try {
      const response = await verifyChallenge(challengeToken, x, y);
      saveToken(response.token);
      challengeToken = null;
      imageBase64 = null;
      onSuccess();
    } catch {
      errorMessage = 'Wrong answer — try again!';
      await loadChallenge();
    }
  }
</script>

{#if open}
  <div
    class="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4"
    role="dialog"
    aria-modal="true"
    aria-labelledby="captcha-modal-title"
    tabindex="-1"
  >
    <div class="bg-background rounded-lg shadow-lg w-full max-w-[460px] animate-in fade-in zoom-in-95">
      <!-- Header -->
      <div class="p-6 pb-3">
        <h2 id="captcha-modal-title" class="text-lg font-semibold">Human Verification</h2>
        <p class="text-sm text-muted-foreground mt-1">
          Click the box showing the correct answer to continue.
        </p>
      </div>

      <!-- CAPTCHA image area -->
      <div class="px-6 pb-2 flex flex-col items-center gap-3">
        {#if loading}
          <div
            class="flex items-center justify-center rounded border bg-muted w-full"
            style="height: 160px;"
            aria-label="Loading challenge"
          >
            <svg
              class="animate-spin h-6 w-6 text-muted-foreground"
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
          </div>
        {:else if imageBase64}
          <!-- svelte-ignore a11y_click_events_have_key_events -->
          <!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
          <img
            bind:this={imgElement}
            src="data:image/png;base64,{imageBase64}"
            alt="Math addition question — click the correct answer"
            class="rounded border cursor-pointer w-full max-w-[400px] select-none"
            onclick={handleImageClick}
            draggable="false"
          />
        {/if}

        {#if errorMessage}
          <p class="text-sm text-destructive" role="alert">{errorMessage}</p>
        {/if}
      </div>

      <!-- Footer -->
      <div class="px-6 pb-6 pt-2 flex justify-center">
        <Button
          variant="ghost"
          size="sm"
          onclick={loadChallenge}
          disabled={loading}
          class="min-h-[44px] gap-2"
        >
          <RefreshCw class="h-4 w-4" />
          New challenge
        </Button>
      </div>
    </div>
  </div>
{/if}
