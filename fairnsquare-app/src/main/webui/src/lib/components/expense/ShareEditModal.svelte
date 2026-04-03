<script lang="ts">
  /**
   * ShareEditModal - Sub-modal for editing participant shares in FREE split mode.
   * Opens from the ExpenseEditModal when user clicks "Edit shares".
   * Works on local copies and only commits changes on confirm.
   */

  import { untrack } from 'svelte';
  import type { Participant } from '$lib/api/splits';
  import Button from '$lib/components/ui/button/button.svelte';
  import Input from '$lib/components/ui/input/input.svelte';
  import Label from '$lib/components/ui/label/label.svelte';
  import { Checkbox } from '$lib/components/ui/checkbox';
  import { X, Moon, AlertTriangle } from 'lucide-svelte';

  interface ShareData {
    shareParts: Record<string, number | ''>;
    shareChecked: Record<string, boolean>;
    sharePreviousValues: Record<string, number>;
  }

  interface Props {
    open: boolean;
    participants: Participant[];
    initialData: ShareData;
    onConfirm: (data: ShareData) => void;
    onCancel: () => void;
  }

  let {
    open,
    participants,
    initialData,
    onConfirm,
    onCancel,
  }: Props = $props();

  // Local working copies
  let localParts = $state<Record<string, number | ''>>({});
  let localChecked = $state<Record<string, boolean>>({});
  let localPreviousValues = $state<Record<string, number>>({});

  // Initialize local state when modal opens
  $effect(() => {
    if (open) {
      untrack(() => {
        localParts = { ...initialData.shareParts };
        localChecked = { ...initialData.shareChecked };
        localPreviousValues = { ...initialData.sharePreviousValues };
      });
    }
  });

  const localTotalParts: number = $derived(
    Object.values(localParts).reduce((sum: number, val) => {
      const numVal = typeof val === 'number' ? val : (parseFloat(String(val)) || 0);
      return sum + numVal;
    }, 0)
  );

  const isLocalPartsValid = $derived(localTotalParts > 0);

  function handleCheckChange(participantId: string, checked: boolean) {
    localChecked[participantId] = checked;
    if (checked) {
      localParts[participantId] = localPreviousValues[participantId] || 1;
    } else {
      const currentVal = typeof localParts[participantId] === 'number'
        ? localParts[participantId] as number
        : parseFloat(String(localParts[participantId])) || 0;
      if (currentVal > 0) {
        localPreviousValues[participantId] = currentVal;
      }
      localParts[participantId] = 0;
    }
  }

  function handleConfirm() {
    onConfirm({
      shareParts: { ...localParts },
      shareChecked: { ...localChecked },
      sharePreviousValues: { ...localPreviousValues },
    });
  }

  let showNightsWarning = $state(false);

  function copyNightsShares() {
    showNightsWarning = true;
  }

  function applyNightsShares() {
    for (const participant of participants) {
      const value = participant.nights * participant.share;
      localParts[participant.id] = value;
      localChecked[participant.id] = true;
      localPreviousValues[participant.id] = value;
    }
    showNightsWarning = false;
  }

  function cancelNightsWarning() {
    showNightsWarning = false;
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
    aria-labelledby="share-edit-modal-title"
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
        <h2 id="share-edit-modal-title" class="text-lg font-semibold">Edit Shares</h2>
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
            Select participants and set their share parts
          </p>
          <span class="text-sm {isLocalPartsValid ? 'text-green-600' : 'text-destructive'}">
            {localTotalParts.toFixed(2)} parts {isLocalPartsValid ? '✓' : '⚠'}
          </span>
        </div>

        <Button
          variant="outline"
          size="sm"
          onclick={copyNightsShares}
          class="w-full min-h-[44px] gap-2"
        >
          <Moon class="h-4 w-4" />
          Copy nights shares
        </Button>

        {#if showNightsWarning}
          <div class="rounded-md border border-amber-300 bg-amber-50 p-3 space-y-2" role="alert">
            <div class="flex items-start gap-2">
              <AlertTriangle class="h-4 w-4 text-amber-600 mt-0.5 shrink-0" />
              <p class="text-xs text-amber-800">
                This will overwrite all current values with each participant's
                <strong>nights × share</strong>. These values are a static snapshot — future
                changes to nights or share will not be reflected here automatically.
              </p>
            </div>
            <div class="flex gap-2">
              <Button size="sm" onclick={applyNightsShares} class="flex-1 min-h-[36px]">
                Apply
              </Button>
              <Button variant="outline" size="sm" onclick={cancelNightsWarning} class="flex-1 min-h-[36px]">
                Cancel
              </Button>
            </div>
          </div>
        {/if}

        <!-- Scrollable participant list -->
        <div class="max-h-[300px] overflow-y-auto space-y-2" role="list" aria-label="Participant shares">
          {#each participants as participant (participant.id)}
            <div class="flex items-center gap-2 min-h-[44px]" role="listitem">
              <Checkbox
                id="share-check-{participant.id}"
                checked={localChecked[participant.id]}
                onchange={(e: Event) => handleCheckChange(participant.id, (e.target as HTMLInputElement).checked)}
                aria-label="Include {participant.name}"
                class="min-w-[20px] min-h-[20px]"
              />
              <Label for="share-check-{participant.id}" class="flex-1 text-sm cursor-pointer">
                {participant.name}
              </Label>
              <Input
                id="share-parts-{participant.id}"
                type="number"
                step="0.01"
                min="0"
                placeholder="0"
                bind:value={localParts[participant.id]}
                disabled={!localChecked[participant.id]}
                aria-label="{participant.name} parts"
                class="w-24 min-h-[44px]"
              />
            </div>
          {/each}
        </div>
      </div>

      <!-- Actions -->
      <div class="flex gap-2 p-4 border-t">
        <Button
          onclick={handleConfirm}
          disabled={!isLocalPartsValid}
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