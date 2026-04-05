<script lang="ts">
  /**
   * EditParticipantModal - Story FNS-002-4
   * Modal for editing participant name and nights with delete option
   */

  import { updateParticipant, deleteParticipant, type Participant, type Split } from '$lib/api/splits';

  // Constants
  const AUTO_FOCUS_DELAY_MS = 50; // Delay for auto-focusing name field after modal opens
  import Button from '$lib/components/ui/button/button.svelte';
  import Input from '$lib/components/ui/input/input.svelte';
  import Label from '$lib/components/ui/label/label.svelte';
  import ConfirmDialog from '$lib/components/ui/confirm-dialog/confirm-dialog.svelte';
  import { addToast } from '$lib/stores/toastStore.svelte';
  import { X, Info } from 'lucide-svelte';

  // Field info modal state
  let activeFieldInfo = $state<'nights' | 'share' | null>(null);

  const fieldInfo = {
    nights: {
      title: 'Nights',
      description: `The number of nights this participant stays during the trip.\n\nUsed in the "By Night" split mode: costs are divided proportionally based on each participant's nights × share weight.\n\nExample: a participant staying 3 nights out of a 7-night trip pays for 3/7 of the night-based expenses (adjusted by their share).`,
    },
    share: {
      title: 'Share',
      description: `The number of persons this participant represents.\n\nA solo traveller has a share of 1. A couple sharing costs would have a share of 2, a family of 3 would use 3, etc.\n\nUsed in "By Night" mode (nights × share) and "By Share" mode (share only) to calculate each participant's proportional contribution.\n\nExample: a couple (share 2) staying 3 nights counts as 6 night-shares, compared to 3 night-shares for a solo person staying the same time.`,
    },
  };

  // Props interface (Svelte 5 pattern)
  let {
    open,
    splitId,
    participant,
    participants,
    split,
    onClose,
    onSuccess,
  }: {
    open: boolean;
    splitId: string;
    participant: Participant | null;
    participants: Participant[];
    split: Split;
    onClose: () => void;
    onSuccess: () => Promise<void>;
  } = $props();

  // Form state
  let editName = $state('');
  let editNights = $state(1);
  let editShare = $state(1);
  let isLoading = $state(false);

  // Validation state
  let validationErrors = $state<{ name?: string; nights?: string; share?: string }>({});
  let nameTouched = $state(false);
  let nightsTouched = $state(false);
  let shareTouched = $state(false);

  // Confirmation dialogs
  let showDiscardConfirm = $state(false);
  let showDeleteConfirm = $state(false);
  let isDeleting = $state(false);

  // Dirty tracking
  const isDirty = $derived(
    participant != null && (
      editName !== participant.name ||
      editNights !== participant.nights ||
      editShare !== participant.share
    )
  );

  // Duplicate name check (case-insensitive, exclude current participant)
  const isDuplicate = $derived(
    participants.some(p =>
      p.id !== participant?.id &&
      p.name.toLowerCase() === editName.trim().toLowerCase()
    )
  );

  // Validation
  const isNameValid = $derived(
    editName.trim().length > 0 &&
    editName.trim().length <= 50 &&
    !isDuplicate
  );

  const isNightsValid = $derived(
    editNights >= 1 && editNights <= 365
  );

  const isShareValid = $derived(
    editShare >= 0.5 && editShare <= 50
  );

  const isValid = $derived(isNameValid && isNightsValid && isShareValid);

  // Has expenses check for delete button
  const hasExpenses = $derived(
    split.expenses.some(e => e.payerId === participant?.id)
  );

  // Reset form when modal opens or participant changes
  $effect(() => {
    if (open && participant) {
      editName = participant.name;
      editNights = Math.max(1, Math.round(participant.nights));
      editShare = participant.share;
      validationErrors = {};
      nameTouched = false;
      nightsTouched = false;
      shareTouched = false;
      showDiscardConfirm = false;
      showDeleteConfirm = false;
      isLoading = false;
      isDeleting = false;

      // Auto-focus name field
      setTimeout(() => {
        if (typeof document !== 'undefined') {
          document.getElementById('edit-participant-name-modal')?.focus();
        }
      }, AUTO_FOCUS_DELAY_MS);
    }
  });

  function validateName() {
    if (editName.trim().length === 0) {
      validationErrors.name = 'Name is required';
    } else if (editName.trim().length > 50) {
      validationErrors.name = 'Name cannot exceed 50 characters';
    } else if (isDuplicate) {
      validationErrors.name = 'Name must be unique (participant already exists)';
    } else {
      delete validationErrors.name;
      validationErrors = { ...validationErrors };
    }
  }

  function validateNights() {
    if (editNights < 1) {
      validationErrors.nights = 'Nights must be at least 1';
    } else if (editNights > 365) {
      validationErrors.nights = 'Nights cannot exceed 365';
    } else {
      delete validationErrors.nights;
      validationErrors = { ...validationErrors };
    }
  }

  function handleNameBlur() {
    nameTouched = true;
    validateName();
  }

  function handleNightsBlur() {
    nightsTouched = true;
    validateNights();
  }

  function validateShare() {
    if (editShare < 0.5) {
      validationErrors.share = 'Must be at least 0.5';
    } else if (editShare > 50) {
      validationErrors.share = 'Cannot exceed 50';
    } else {
      delete validationErrors.share;
      validationErrors = { ...validationErrors };
    }
  }

  function handleShareBlur() {
    shareTouched = true;
    validateShare();
  }

  async function handleSubmit() {
    if (!participant) return;

    // Touch all fields to show validation errors
    nameTouched = true;
    nightsTouched = true;
    shareTouched = true;

    // Final validation
    validateName();
    validateNights();
    validateShare();

    if (!isValid || !isDirty) return;
    if (isLoading) return;

    isLoading = true;

    try {
      await updateParticipant(splitId, participant.id, {
        name: editName.trim(),
        nights: editNights,
        share: editShare,
        // Preserve the current shared account grouping — it is managed from the Settlement page
        sharedAccountId: participant.sharedAccountId ?? null,
      });

      addToast({
        type: 'success',
        message: 'Participant updated',
        description: `${editName.trim()} · ${editNights} nights · share ${editShare}`,
      });

      await onSuccess();
      onClose();
    } catch (err: any) {
      addToast({
        type: 'error',
        message: err.detail || 'Failed to update participant. Please try again.',
      });
    } finally {
      isLoading = false;
    }
  }


  function handleCloseAttempt() {
    if (isDirty) {
      showDiscardConfirm = true;
    } else {
      onClose();
    }
  }

  function handleConfirmDiscard() {
    showDiscardConfirm = false;
    onClose();
  }

  function handleCancelDiscard() {
    showDiscardConfirm = false;
  }

  function handleDeleteClick() {
    if (hasExpenses) return;
    showDeleteConfirm = true;
  }

  async function handleConfirmDelete() {
    if (!participant) return;

    isDeleting = true;

    try {
      await deleteParticipant(splitId, participant.id);

      addToast({
        type: 'success',
        message: 'Participant removed',
        description: participant.name,
      });

      await onSuccess();
      onClose();
    } catch (err: any) {
      addToast({
        type: 'error',
        message: err.detail || 'Failed to remove participant. Please try again.',
      });
    } finally {
      isDeleting = false;
      showDeleteConfirm = false;
    }
  }

  function handleCancelDelete() {
    showDeleteConfirm = false;
  }

  function handleKeydown(event: KeyboardEvent) {
    if (!open) return;

    if (event.key === 'Escape' && !isLoading && !showDiscardConfirm && !showDeleteConfirm) {
      handleCloseAttempt();
    }

    // Focus trap - keep Tab within modal (WCAG 2.1)
    if (event.key === 'Tab') {
      const modal = document.querySelector('[role="dialog"]');
      if (!modal) return;

      const focusable = modal.querySelectorAll<HTMLElement>(
        'button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
      );
      if (focusable.length === 0) return;

      const first = focusable[0];
      const last = focusable[focusable.length - 1];

      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }
  }

  function handleBackdropClick(event: MouseEvent) {
    if (event.target === event.currentTarget && !isLoading) {
      handleCloseAttempt();
    }
  }
</script>

<svelte:window onkeydown={handleKeydown} />

{#if open}
  <!-- Modal Backdrop -->
  <!-- svelte-ignore a11y_click_events_have_key_events -->
  <!-- svelte-ignore a11y_no_static_element_interactions -->
  <div
    class="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4"
    onclick={handleBackdropClick}
    role="dialog"
    aria-modal="true"
    aria-labelledby="edit-participant-title"
    tabindex="-1"
  >
    <!-- Modal Content -->
    <!-- svelte-ignore a11y_click_events_have_key_events a11y_no_static_element_interactions -->
    <div
      class="bg-background rounded-lg shadow-lg w-full max-w-[420px] animate-in fade-in zoom-in-95"
      onclick={(e) => e.stopPropagation()}
    >
      <!-- Header -->
      <div class="flex items-center justify-between p-4 border-b">
        <h2 id="edit-participant-title" class="text-lg font-semibold">
          Edit Participant
        </h2>
        <Button
          variant="ghost"
          size="sm"
          onclick={handleCloseAttempt}
          disabled={isLoading}
          class="min-h-[44px] min-w-[44px]"
          aria-label="Close"
        >
          <X class="h-4 w-4" />
        </Button>
      </div>

      <!-- Form -->
      <form onsubmit={(e) => { e.preventDefault(); handleSubmit(); }} class="p-4 space-y-4">
        <!-- Name Field -->
        <div class="space-y-2">
          <Label for="edit-participant-name-modal">Name</Label>
          <Input
            id="edit-participant-name-modal"
            type="text"
            bind:value={editName}
            onblur={handleNameBlur}
            class="min-h-[44px]"
            aria-invalid={nameTouched && !!validationErrors.name}
            disabled={isLoading}
          />
          {#if nameTouched && validationErrors.name}
            <p class="text-sm text-destructive">{validationErrors.name}</p>
          {/if}
        </div>

        <!-- Nights & Persons Fields -->
        <div class="flex gap-3">
          <div class="space-y-2 flex-1">
            <div class="flex items-center gap-1">
              <Label for="edit-participant-nights-modal">Nights</Label>
              <button type="button" onclick={() => { activeFieldInfo = 'nights'; }} class="p-0.5 rounded-full hover:bg-muted text-muted-foreground" aria-label="Info about Nights">
                <Info class="h-3.5 w-3.5" />
              </button>
            </div>
            <Input
              id="edit-participant-nights-modal"
              type="number"
              step="1"
              min="1"
              max="365"
              bind:value={editNights}
              onblur={handleNightsBlur}
              class="min-h-[44px]"
              aria-invalid={nightsTouched && !!validationErrors.nights}
              disabled={isLoading}
            />
            {#if nightsTouched && validationErrors.nights}
              <p class="text-sm text-destructive">{validationErrors.nights}</p>
            {/if}
          </div>

          <div class="space-y-2 flex-1">
            <div class="flex items-center gap-1">
              <Label for="edit-participant-share-modal">Share</Label>
              <button type="button" onclick={() => { activeFieldInfo = 'share'; }} class="p-0.5 rounded-full hover:bg-muted text-muted-foreground" aria-label="Info about Share">
                <Info class="h-3.5 w-3.5" />
              </button>
            </div>
            <Input
              id="edit-participant-share-modal"
              type="number"
              step="any"
              min="0.5"
              max="50"
              bind:value={editShare}
              onblur={handleShareBlur}
              onkeydown={(e: KeyboardEvent) => {
                if (e.key === 'ArrowUp') { e.preventDefault(); editShare = Math.min(50, Math.round((editShare + 0.5) * 100) / 100); }
                else if (e.key === 'ArrowDown') { e.preventDefault(); editShare = Math.max(0.5, Math.round((editShare - 0.5) * 100) / 100); }
              }}
              class="min-h-[44px]"
              aria-invalid={shareTouched && !!validationErrors.share}
              disabled={isLoading}
            />
            {#if shareTouched && validationErrors.share}
              <p class="text-sm text-destructive">{validationErrors.share}</p>
            {/if}
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="flex gap-2 pt-2">
          <Button
            type="submit"
            disabled={!isDirty || !isValid || isLoading}
            class="flex-1 min-h-[44px]"
          >
            {#if isLoading}
              <svg class="animate-spin h-4 w-4 mr-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              Saving...
            {:else}
              Save Changes
            {/if}
          </Button>
          <Button
            type="button"
            variant="outline"
            onclick={handleCloseAttempt}
            disabled={isLoading}
            class="flex-1 min-h-[44px]"
            aria-label="Cancel edit"
          >
            Cancel
          </Button>
        </div>

        <!-- Danger Zone -->
        <div class="pt-4 border-t">
          <Button
            type="button"
            variant="destructive"
            class="w-full min-h-[44px]"
            disabled={hasExpenses || isLoading}
            onclick={handleDeleteClick}
            title={hasExpenses ? 'Remove expenses first' : ''}
          >
            Delete Participant
          </Button>
        </div>
      </form>
    </div>
  </div>

  <!-- Field Info Modal -->
  {#if activeFieldInfo}
    <!-- svelte-ignore a11y_click_events_have_key_events -->
    <div
      class="fixed inset-0 z-[70] bg-black/50 flex items-end justify-center sm:items-center sm:p-4"
      onclick={() => { activeFieldInfo = null; }}
      role="dialog"
      aria-modal="true"
      aria-labelledby="field-info-modal-title"
      tabindex="-1"
    >
      <!-- svelte-ignore a11y_click_events_have_key_events -->
      <div
        role="presentation"
        class="bg-background w-full sm:max-w-[420px] rounded-t-xl sm:rounded-xl shadow-lg animate-in fade-in slide-in-from-bottom-4 sm:zoom-in-95"
        onclick={(e) => e.stopPropagation()}
      >
        <div class="flex items-center justify-between p-4 border-b">
          <h2 id="field-info-modal-title" class="text-base font-semibold">
            {fieldInfo[activeFieldInfo].title}
          </h2>
          <Button variant="ghost" size="sm" onclick={() => { activeFieldInfo = null; }} class="min-h-[44px] min-w-[44px]" aria-label="Close">
            <X class="h-4 w-4" />
          </Button>
        </div>
        <div class="p-4 space-y-3">
          {#each fieldInfo[activeFieldInfo].description.split('\n\n') as paragraph}
            <p class="text-sm text-muted-foreground leading-relaxed">{paragraph}</p>
          {/each}
        </div>
        <div class="p-4 border-t">
          <Button onclick={() => { activeFieldInfo = null; }} class="w-full min-h-[44px]">Got it</Button>
        </div>
      </div>
    </div>
  {/if}
{/if}

<!-- Discard Confirmation Dialog -->
<ConfirmDialog
  open={showDiscardConfirm}
  title="Discard changes?"
  description="You have unsaved changes. Are you sure you want to close?"
  confirmLabel="Discard"
  cancelLabel="Keep editing"
  onConfirm={handleConfirmDiscard}
  onCancel={handleCancelDiscard}
/>

<!-- Delete Confirmation Dialog -->
<ConfirmDialog
  open={showDeleteConfirm}
  title={participant ? `Delete ${participant.name}?` : 'Delete participant?'}
  description="This cannot be undone."
  confirmLabel="Remove"
  cancelLabel="Cancel"
  isLoading={isDeleting}
  onConfirm={handleConfirmDelete}
  onCancel={handleCancelDelete}
/>
