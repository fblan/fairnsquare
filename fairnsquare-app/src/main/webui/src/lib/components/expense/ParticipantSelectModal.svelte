<script lang="ts">
  /**
   * ParticipantSelectModal - Sub-modal for selecting which participants are included in a BY_NIGHT_CUSTOM expense.
   * Only shows checkboxes — nights per participant are read-only and not displayed here.
   */

  import { untrack } from 'svelte';
  import type { Participant } from '$lib/api/splits';
  import Button from '$lib/components/ui/button/button.svelte';
  import Label from '$lib/components/ui/label/label.svelte';
  import { Checkbox } from '$lib/components/ui/checkbox';
  import { X } from 'lucide-svelte';

  interface Props {
    open: boolean;
    participants: Participant[];
    initialSelectedIds: string[];
    onConfirm: (selectedIds: string[]) => void;
    onCancel: () => void;
  }

  let {
    open,
    participants,
    initialSelectedIds,
    onConfirm,
    onCancel,
  }: Props = $props();

  let localChecked = $state<Record<string, boolean>>({});

  $effect(() => {
    if (open) {
      untrack(() => {
        localChecked = {};
        for (const p of participants) {
          localChecked[p.id] = initialSelectedIds.includes(p.id);
        }
      });
    }
  });

  const selectedCount = $derived(Object.values(localChecked).filter(Boolean).length);
  const isValid = $derived(selectedCount > 0);

  function handleCheckChange(participantId: string, checked: boolean) {
    localChecked[participantId] = checked;
  }

  function handleConfirm() {
    const selectedIds = Object.entries(localChecked)
      .filter(([, checked]) => checked)
      .map(([id]) => id);
    onConfirm(selectedIds);
  }

  function handleBackdropClick(event: MouseEvent) {
    if (event.target === event.currentTarget) {
      onCancel();
    }
  }

  function handleKeydown(event: KeyboardEvent) {
    if (!open) return;
    if (event.key === 'Escape') {
      event.stopPropagation();
      onCancel();
    }
  }
</script>

<svelte:window onkeydown={handleKeydown} />

{#if open}
  <!-- svelte-ignore a11y_click_events_have_key_events -->
  <div
    class="fixed inset-0 z-[60] bg-black/50 flex items-center justify-center p-4"
    onclick={handleBackdropClick}
    role="dialog"
    aria-modal="true"
    aria-labelledby="participant-select-modal-title"
    tabindex="-1"
  >
    <!-- svelte-ignore a11y_click_events_have_key_events -->
    <div
      role="presentation"
      class="bg-background rounded-lg shadow-lg w-full max-w-[380px] animate-in fade-in zoom-in-95"
      onclick={(e) => e.stopPropagation()}
    >
      <!-- Header -->
      <div class="flex items-center justify-between p-4 border-b">
        <h2 id="participant-select-modal-title" class="text-lg font-semibold">Edit Participants</h2>
        <Button
          variant="ghost"
          size="sm"
          onclick={onCancel}
          class="min-h-[44px] min-w-[44px]"
          aria-label="Close"
        >
          <X class="h-4 w-4" />
        </Button>
      </div>

      <!-- Content -->
      <div class="p-4 space-y-3">
        <p class="text-xs text-muted-foreground">
          Select who is participating in this expense. Nights per participant are not modified here.
        </p>

        <!-- Participant checkboxes -->
        <div class="max-h-[300px] overflow-y-auto space-y-2" role="list" aria-label="Participants">
          {#each participants as participant (participant.id)}
            <div class="flex items-center gap-3 min-h-[44px]" role="listitem">
              <Checkbox
                id="participant-check-{participant.id}"
                checked={localChecked[participant.id]}
                onchange={(e: Event) => handleCheckChange(participant.id, (e.target as HTMLInputElement).checked)}
                aria-label="Include {participant.name}"
                class="min-w-[20px] min-h-[20px]"
              />
              <Label for="participant-check-{participant.id}" class="flex-1 text-sm cursor-pointer">
                {participant.name}
                <span class="text-xs text-muted-foreground ml-1">({participant.nights} night{participant.nights !== 1 ? 's' : ''})</span>
              </Label>
            </div>
          {/each}
        </div>

        {#if !isValid}
          <p class="text-xs text-destructive">At least one participant must be selected</p>
        {/if}
      </div>

      <!-- Actions -->
      <div class="flex gap-2 p-4 border-t">
        <Button
          onclick={handleConfirm}
          disabled={!isValid}
          class="flex-1 min-h-[44px]"
        >
          Confirm ({selectedCount} selected)
        </Button>
        <Button
          variant="outline"
          onclick={onCancel}
          class="flex-1 min-h-[44px]"
        >
          Cancel
        </Button>
      </div>
    </div>
  </div>
{/if}
