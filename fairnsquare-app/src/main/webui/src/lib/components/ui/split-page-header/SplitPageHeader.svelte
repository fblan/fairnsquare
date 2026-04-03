<script lang="ts">
  import type { Snippet } from 'svelte';
  import { Share2, LayoutDashboard, Users, Receipt, ArrowLeftRight } from 'lucide-svelte';
  import { Button } from '$lib/components/ui/button';
  import { addToast } from '$lib/stores/toastStore.svelte';
  import { navigate, route } from '$lib/router';

  interface Props {
    splitName: string;
    splitId: string;
    children?: Snippet;
  }

  const { splitName, splitId, children }: Props = $props();

  const activeTab = $derived.by(() => {
    const path = route.pathname;
    if (path.endsWith('/participants')) return 'participants';
    if (path.endsWith('/expenses')) return 'expenses';
    if (path.endsWith('/settlement')) return 'settlement';
    return 'dashboard';
  });

  const tabs = [
    { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard, path: () => `/splits/${splitId}` },
    { id: 'participants', label: 'Participants', icon: Users, path: () => `/splits/${splitId}/participants` },
    { id: 'expenses', label: 'Expenses', icon: Receipt, path: () => `/splits/${splitId}/expenses` },
    { id: 'settlement', label: 'Settlement', icon: ArrowLeftRight, path: () => `/splits/${splitId}/settlement` },
  ] as const;

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

<div class="w-full space-y-3">
  <!-- Title row -->
  <header class="w-full flex items-center justify-between">
    <h1 class="text-lg font-bold text-primary break-words min-w-0">{splitName}</h1>
    <div class="flex items-center gap-1 shrink-0">
      {#if children}
        {@render children()}
      {/if}
      <Button
        onclick={handleShare}
        variant="outline"
        size="sm"
        class="min-h-[44px]"
        aria-label="Share link"
      >
        <Share2 class="h-4 w-4 mr-1" />
        Share
      </Button>
    </div>
  </header>

  <!-- Navigation tabs -->
  <nav aria-label="Split navigation" class="flex border-b border-border">
    {#each tabs as tab}
      {@const isActive = activeTab === tab.id}
      {@const Icon = tab.icon}
      <button
        onclick={() => navigate(tab.path())}
        aria-current={isActive ? 'page' : undefined}
        class="flex items-center gap-1.5 px-3 py-2 text-sm font-medium whitespace-nowrap transition-colors min-h-[44px]
          {isActive
            ? 'border-b-2 border-primary text-primary -mb-px'
            : 'text-muted-foreground hover:text-foreground'}"
      >
        <Icon class="h-4 w-4 shrink-0" />
        {tab.label}
      </button>
    {/each}
  </nav>
</div>
