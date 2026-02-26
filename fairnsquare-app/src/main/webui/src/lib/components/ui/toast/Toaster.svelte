<script lang="ts">
  import { getToasts, removeToast } from '$lib/stores/toastStore.svelte';

  const typeStyles: Record<string, string> = {
    success: 'bg-green-600 text-white',
    error: 'bg-destructive text-white',
    warning: 'bg-yellow-500 text-white',
    info: 'bg-primary text-primary-foreground',
  };

  const typeIcons: Record<string, string> = {
    success: '✓',
    error: '✕',
    warning: '⚠',
    info: 'ℹ',
  };
</script>

<!-- Fixed bottom-right toast container -->
<div
  class="fixed bottom-4 right-4 z-50 flex flex-col gap-2 w-80 max-w-[calc(100vw-2rem)]"
  aria-live="polite"
  aria-atomic="false"
>
  {#each getToasts() as toast (toast.id)}
    <div
      class="flex items-start gap-3 rounded-lg px-4 py-3 shadow-lg {typeStyles[toast.type] ?? typeStyles.info}"
      role="alert"
    >
      <span class="shrink-0 font-bold text-sm mt-0.5">{typeIcons[toast.type] ?? typeIcons.info}</span>
      <p class="flex-1 text-sm">{toast.message}</p>
      <button
        onclick={() => removeToast(toast.id)}
        class="shrink-0 opacity-70 hover:opacity-100 transition-opacity text-sm font-bold leading-none"
        aria-label="Dismiss"
      >✕</button>
    </div>
  {/each}
</div>