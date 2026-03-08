<script lang="ts">
  import type { Snippet } from 'svelte';
  import { ArrowLeft, Share2 } from 'lucide-svelte';
  import { Button } from '$lib/components/ui/button';
  import { addToast } from '$lib/stores/toastStore.svelte';
  import { navigate } from '$lib/router';

  interface Props {
    splitName: string;
    splitId: string;
    showBackButton?: boolean;
    children?: Snippet;
  }

  const { splitName, splitId, showBackButton = false, children }: Props = $props();

  async function handleShare() {
    if (typeof window === 'undefined') return;

    const url = window.location.href;

    if (!navigator.clipboard) {
      addToast({ type: 'info', message: `Share link: ${url}` });
      return;
    }

    try {
      await navigator.clipboard.writeText(url);
      addToast({ type: 'success', message: 'Link copied!' });
    } catch {
      addToast({ type: 'info', message: `Share link: ${url}` });
    }
  }
</script>

<header class="w-full flex items-center justify-between">
  <div class="flex items-center gap-2 min-w-0">
    {#if showBackButton}
      <Button
        variant="ghost"
        size="sm"
        onclick={() => navigate(`/splits/${splitId}`)}
        class="min-h-[44px] min-w-[44px] shrink-0"
        aria-label="Back to dashboard"
      >
        <ArrowLeft class="h-5 w-5" />
      </Button>
    {/if}
    <h1 class="text-lg font-bold text-primary break-words">{splitName}</h1>
  </div>
  <div class="flex items-center gap-1 shrink-0">
    {#if children}
      {@render children()}
    {/if}
    <Button
      onclick={handleShare}
      variant="outline"
      size="sm"
      class="min-h-[44px]"
      aria-label="Share"
    >
      <Share2 class="h-4 w-4 mr-1" />
      Share
    </Button>
  </div>
</header>