<script lang="ts">
  import type { Snippet } from 'svelte';
  import { Share2, Home, Users, Receipt, ArrowLeftRight, X } from 'lucide-svelte';
  import { Button } from '$lib/components/ui/button';
  import { addToast } from '$lib/stores/toastStore.svelte';
  import { navigate, route } from '$lib/router';
  interface Props {
    splitName: string;
    splitId: string;
    children?: Snippet;
    /** Optional guard called before any navigation. Return a warning string to block navigation with an info modal, or null to allow navigation. */
    navigateGuard?: () => string | null;
  }

  const { splitName, splitId, children, navigateGuard }: Props = $props();

  // Navigation guard state
  let showNavBlocked = $state(false);

  function handleNavigate(target: string) {
    if (navigateGuard) {
      const warning = navigateGuard();
      if (warning) {
        showNavBlocked = true;
        return;
      }
    }
    navigate(target);
  }

  const activeTab = $derived.by(() => {
    const path = route.pathname;
    if (path.endsWith('/participants')) return 'participants';
    if (path.endsWith('/expenses')) return 'expenses';
    if (path.endsWith('/settlement')) return 'settlement';
    return 'dashboard';
  });

  const tabs = [
    { id: 'dashboard', label: 'Home', icon: Home, path: () => `/splits/${splitId}` },
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
        onclick={() => handleNavigate(tab.path())}
        aria-current={isActive ? 'page' : undefined}
        aria-label={tab.label}
        class="flex items-center gap-1.5 px-3 py-2 text-sm font-medium whitespace-nowrap transition-colors min-h-[44px]
          {isActive
            ? 'border-b-2 border-primary text-primary -mb-px'
            : 'text-muted-foreground hover:text-foreground'}"
      >
        <Icon class="h-4 w-4 shrink-0" />
        <span class="hidden sm:inline">{tab.label}</span>
      </button>
    {/each}

    <!-- Close button — navigates to home, icon only -->
    <button
      onclick={() => handleNavigate('/')}
      aria-label="Close split"
      class="ml-auto flex items-center px-3 py-2 text-muted-foreground hover:text-foreground transition-colors min-h-[44px]"
    >
      <X class="h-4 w-4 shrink-0" />
    </button>
  </nav>
</div>

{#if showNavBlocked}
  <!-- svelte-ignore a11y_click_events_have_key_events -->
  <div
    class="fixed inset-0 z-50 bg-black/50 flex items-end justify-center sm:items-center sm:p-4"
    onclick={() => { showNavBlocked = false; }}
    role="dialog"
    aria-modal="true"
    aria-labelledby="nav-blocked-title"
    tabindex="-1"
  >
    <!-- svelte-ignore a11y_click_events_have_key_events -->
    <div
      role="presentation"
      class="bg-background w-full sm:max-w-[420px] rounded-t-xl sm:rounded-xl shadow-lg animate-in fade-in slide-in-from-bottom-4 sm:zoom-in-95"
      onclick={(e) => e.stopPropagation()}
    >
      <div class="p-6 space-y-3 text-center">
        <h2 id="nav-blocked-title" class="text-base font-semibold">A participant is required</h2>
        <p class="text-sm text-muted-foreground">Please add at least one participant before navigating away from this page.</p>
      </div>
      <div class="p-4 border-t">
        <Button onclick={() => { showNavBlocked = false; }} class="w-full min-h-[44px]">Got it</Button>
      </div>
    </div>
  </div>
{/if}
