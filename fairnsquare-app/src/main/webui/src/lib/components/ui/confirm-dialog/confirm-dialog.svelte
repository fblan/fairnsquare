<script lang="ts">
  import Button from '$lib/components/ui/button/button.svelte';

  interface Props {
    open: boolean;
    title: string;
    description: string;
    confirmLabel?: string;
    cancelLabel?: string;
    isLoading?: boolean;
    onConfirm: () => void;
    onCancel: () => void;
  }

  let {
    open,
    title,
    description,
    confirmLabel = 'Confirm',
    cancelLabel = 'Cancel',
    isLoading = false,
    onConfirm,
    onCancel
  }: Props = $props();

  function handleBackdropClick(event: MouseEvent) {
    if (event.target === event.currentTarget && !isLoading) {
      onCancel();
    }
  }

  function handleKeydown(event: KeyboardEvent) {
    if (event.key === 'Escape' && !isLoading) {
      onCancel();
    }
  }
</script>

<svelte:window onkeydown={handleKeydown} />

{#if open}
  <!-- Backdrop -->
  <div
    class="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4"
    onclick={handleBackdropClick}
    onkeydown={(e) => e.key === 'Escape' && !isLoading && onCancel()}
    role="dialog"
    aria-modal="true"
    aria-labelledby="confirm-dialog-title"
    aria-describedby="confirm-dialog-description"
    tabindex="-1"
  >
    <!-- Dialog -->
    <div class="bg-background rounded-lg shadow-lg w-full max-w-md p-6 animate-in fade-in zoom-in-95">
      <h2 id="confirm-dialog-title" class="text-lg font-semibold mb-2">
        {title}
      </h2>
      <p id="confirm-dialog-description" class="text-muted-foreground mb-6">
        {description}
      </p>
      <div class="flex flex-col-reverse sm:flex-row sm:justify-end gap-2">
        <Button
          variant="outline"
          onclick={onCancel}
          disabled={isLoading}
          class="min-h-[44px] w-full sm:w-auto"
        >
          {cancelLabel}
        </Button>
        <Button
          variant="destructive"
          onclick={onConfirm}
          disabled={isLoading}
          class="min-h-[44px] w-full sm:w-auto"
        >
          {#if isLoading}
            <svg class="animate-spin h-4 w-4 mr-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
          {/if}
          {confirmLabel}
        </Button>
      </div>
    </div>
  </div>
{/if}
