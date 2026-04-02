<script lang="ts">
  /**
   * NightParticipantModal - Sub-modal for selecting which participants participate
   * in a BY_NIGHT_CUSTOM expense. Only checkboxes — no parts editing.
   * The nb nights per participant is shown for info but cannot be changed here.
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
    initialParticipantIds: string[];
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

  // Local working copy of selected IDs
  let localSelected = $state<Record<string, boolean>>({});

  // Initialize local state when modal opens
  $effect(() => {
    if (open) {
      untrack(() => {
        localSelected = {};
        for (const p of participants) {
          localSelected[p.id] = initialParticipantIds.includes(p.id);
        }
      });
    }
  });

  const selectedCount = $derived(Object.values(localSelected).filter(Boolean).length);
  const isValid = $derived(selectedCount > 0);

  function handleConfirm() {
    const ids = participants.filter((p) => localSelected[p.id]).map((p) => p.id);
    onConfirm(ids);
  }

  function handleSelectAll() {
    const newSelected: Record<string, boolean> = {};
    for (const p of participants) {
      newSelected[p.id] = true;
    }
    localSelected = newSelected;
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
    aria-labelledby="night-participant-modal-title"
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
        <h2 id="night-participant-modal-title" class="text-lg font-semibold">Edit Participants</h2>
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
            Select who participates in this expense
          </p>
          <span class="text-sm {isValid ? 'text-green-600' : 'text-destructive'}">
            {selectedCount} selected {isValid ? '✓' : '⚠'}
          </span>
        </div>

        <!-- Scrollable participant list -->
        <div class="max-h-[300px] overflow-y-auto space-y-2" role="list" aria-label="Participant selection">
          {#each participants as participant (participant.id)}
            <div class="flex items-center gap-3 min-h-[44px] py-1" role="listitem">
              <Checkbox
                id="night-check-{participant.id}"
                checked={localSelected[participant.id]}
                onchange={(e: Event) => {
                  localSelected[participant.id] = (e.target as HTMLInputElement).checked;
                }}
                aria-label="Include {participant.name}"
                class="min-w-[20px] min-h-[20px]"
              />
              <Label for="night-check-{participant.id}" class="flex-1 text-sm cursor-pointer">
                {participant.name}
              </Label>
              <span class="text-xs text-muted-foreground shrink-0">
                🌙 {participant.nights} night{participant.nights !== 1 ? 's' : ''}
              </span>
            </div>
          {/each}
        </div>

        <!-- Select all shortcut -->
        <button
          type="button"
          onclick={handleSelectAll}
          class="text-xs text-primary underline cursor-pointer"
        >
          Select all
        </button>
      </div>

      <!-- Actions -->
      <div class="flex gap-2 p-4 border-t">
        <Button
          onclick={handleConfirm}
          disabled={!isValid}
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
