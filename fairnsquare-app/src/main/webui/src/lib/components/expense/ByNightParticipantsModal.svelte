<script lang="ts">
  /**
   * ByNightParticipantsModal - Sub-modal for selecting which participants are
   * included in a BY_NIGHT_CUSTOM expense.
   * Opens from ExpenseEditModal or ExpenseList when the user wants to customise
   * the participant list for a "by night" expense.
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
    initialParticipantIds: string[]; // initially checked participant IDs (all if empty = new BY_NIGHT)
    onConfirm: (participantIds: string[]) => void;
    onCancel: () => void;
  }

  let {
    open,
    participants,
    initialParticipantIds,
    onConfirm,
    onCancel,
  }: Props = $props();

  // Local working copy: map of participantId -> checked
  let localChecked = $state<Record<string, boolean>>({});

  // Initialize local state when modal opens
  $effect(() => {
    if (open) {
      untrack(() => {
        const checked: Record<string, boolean> = {};
        for (const p of participants) {
          // If initialParticipantIds is empty, select everyone by default
          checked[p.id] = initialParticipantIds.length === 0
            ? true
            : initialParticipantIds.includes(p.id);
        }
        localChecked = checked;
      });
    }
  });

  const selectedCount = $derived(
    Object.values(localChecked).filter(Boolean).length
  );

  const isConfirmDisabled = $derived(selectedCount === 0);

  function handleCheckChange(participantId: string, checked: boolean) {
    localChecked = { ...localChecked, [participantId]: checked };
  }

  function handleConfirm() {
    const ids = participants
      .filter(p => localChecked[p.id])
      .map(p => p.id);
    onConfirm(ids);
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
    aria-labelledby="by-night-participants-modal-title"
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
        <h2 id="by-night-participants-modal-title" class="text-lg font-semibold">Edit Participants</h2>
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
        <div class="flex items-center justify-between">
          <p class="text-xs text-muted-foreground">
            Select participants to include in this expense
          </p>
          <span class="text-sm {selectedCount > 0 ? 'text-green-600' : 'text-destructive'}">
            {selectedCount} / {participants.length}
          </span>
        </div>

        <!-- Scrollable participant list -->
        <div class="max-h-[300px] overflow-y-auto space-y-2" role="list" aria-label="Participants">
          {#each participants as participant (participant.id)}
            <div class="flex items-center gap-2 min-h-[44px]" role="listitem">
              <Checkbox
                id="night-check-{participant.id}"
                checked={localChecked[participant.id]}
                onchange={(e: Event) => handleCheckChange(participant.id, (e.target as HTMLInputElement).checked)}
                aria-label="Include {participant.name}"
                class="min-w-[20px] min-h-[20px]"
              />
              <Label for="night-check-{participant.id}" class="flex-1 text-sm cursor-pointer">
                {participant.name}
              </Label>
              <span class="text-xs text-muted-foreground whitespace-nowrap">
                {participant.nights} night{participant.nights === 1 ? '' : 's'}
              </span>
              <span class="text-xs text-muted-foreground whitespace-nowrap">
                {participant.share} share{participant.share === 1 ? '' : 's'}
              </span>
            </div>
          {/each}
        </div>
      </div>

      <!-- Actions -->
      <div class="flex gap-2 p-4 border-t">
        <Button
          onclick={handleConfirm}
          disabled={isConfirmDisabled}
          class="flex-1 min-h-[44px]"
        >
          Confirm
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
