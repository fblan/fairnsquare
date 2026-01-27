<script lang="ts">
  import { addParticipant, updateParticipant, deleteParticipant, type Split, type Participant } from '$lib/api/splits';
  import Button from '$lib/components/ui/button/button.svelte';
  import * as Card from '$lib/components/ui/card';
  import Input from '$lib/components/ui/input/input.svelte';
  import Label from '$lib/components/ui/label/label.svelte';
  import ConfirmDialog from '$lib/components/ui/confirm-dialog/confirm-dialog.svelte';
  import { addToast } from '$lib/stores/toastStore.svelte';

  let {
    split,
    onSplitUpdated,
  }: {
    split: Split;
    onSplitUpdated: () => Promise<void>;
  } = $props();

  // Add Participant form state (Story 3.1)
  let showAddForm = $state(false);
  let formName = $state('');
  let formNights = $state(1);
  let validationErrors = $state<{name?: string; nights?: string}>({});
  let isSubmitting = $state(false);

  // Edit Participant state (Story 3.2)
  let editingParticipantId = $state<string | null>(null);
  let editName = $state('');
  let editNights = $state(1);
  let editValidationErrors = $state<{name?: string; nights?: string}>({});
  let isEditSubmitting = $state(false);

  // Delete Participant state (Story 3.3)
  let deletingParticipantId = $state<string | null>(null);
  let deletingParticipantName = $state('');
  let isDeleteSubmitting = $state(false);

  // Smart default for nights - persist to localStorage (Story 3.1 AC 3, 4)
  const NIGHTS_STORAGE_KEY = 'fairnsquare_lastParticipantNights';

  function getSmartDefaultNights(): number {
    if (typeof window === 'undefined') return 1;
    const stored = localStorage.getItem(NIGHTS_STORAGE_KEY);
    return stored ? parseInt(stored, 10) : 1;
  }

  function saveSmartDefaultNights(nights: number): void {
    if (typeof window !== 'undefined') {
      localStorage.setItem(NIGHTS_STORAGE_KEY, nights.toString());
    }
  }

  // Add Participant handlers (Story 3.1)
  function handleShowAddForm() {
    showAddForm = true;
    formName = '';
    formNights = getSmartDefaultNights();
    validationErrors = {};
  }

  function handleCancelAddForm() {
    showAddForm = false;
    formName = '';
    formNights = 1;
    validationErrors = {};
  }

  function validateAddForm(): boolean {
    const errors: {name?: string; nights?: string} = {};

    if (!formName.trim()) {
      errors.name = 'Name is required';
    } else if (formName.trim().length > 50) {
      errors.name = 'Name cannot exceed 50 characters';
    }

    if (formNights < 1) {
      errors.nights = 'Nights must be at least 1';
    } else if (formNights > 365) {
      errors.nights = 'Nights cannot exceed 365';
    }

    validationErrors = errors;
    return Object.keys(errors).length === 0;
  }

  async function handleAddParticipant() {
    if (!validateAddForm()) return;

    isSubmitting = true;

    try {
      await addParticipant(split.id, {
        name: formName.trim(),
        nights: formNights,
      });

      saveSmartDefaultNights(formNights);
      showAddForm = false;
      formName = '';
      formNights = 1;
      validationErrors = {};
      await onSplitUpdated();

      addToast({
        type: 'success',
        message: 'Participant added successfully',
      });
    } catch (err: any) {
      addToast({
        type: 'error',
        message: err.detail || 'Failed to add participant. Please try again.',
      });
    } finally {
      isSubmitting = false;
    }
  }

  // Edit Participant handlers (Story 3.2)
  function handleStartEdit(participant: Participant) {
    editingParticipantId = participant.id;
    editName = participant.name;
    editNights = participant.nights;
    editValidationErrors = {};
  }

  function handleCancelEdit() {
    editingParticipantId = null;
    editName = '';
    editNights = 1;
    editValidationErrors = {};
  }

  function validateEditForm(): boolean {
    const errors: {name?: string; nights?: string} = {};

    if (!editName.trim()) {
      errors.name = 'Name is required';
    } else if (editName.trim().length > 50) {
      errors.name = 'Name cannot exceed 50 characters';
    }

    if (editNights < 1) {
      errors.nights = 'Nights must be at least 1';
    } else if (editNights > 365) {
      errors.nights = 'Nights cannot exceed 365';
    }

    editValidationErrors = errors;
    return Object.keys(errors).length === 0;
  }

  async function handleSaveEdit() {
    if (!validateEditForm()) return;
    if (!editingParticipantId) return;

    isEditSubmitting = true;

    try {
      await updateParticipant(split.id, editingParticipantId, {
        name: editName.trim(),
        nights: editNights,
      });

      editingParticipantId = null;
      editName = '';
      editNights = 1;
      editValidationErrors = {};
      await onSplitUpdated();

      addToast({
        type: 'success',
        message: 'Participant updated successfully',
      });
    } catch (err: any) {
      addToast({
        type: 'error',
        message: err.detail || 'Failed to update participant. Please try again.',
      });
    } finally {
      isEditSubmitting = false;
    }
  }

  // Delete Participant handlers (Story 3.3)
  function handleDeleteClick(event: MouseEvent, participant: Participant) {
    event.stopPropagation();
    deletingParticipantId = participant.id;
    deletingParticipantName = participant.name;
  }

  function handleCancelDelete() {
    deletingParticipantId = null;
    deletingParticipantName = '';
  }

  async function handleConfirmDelete() {
    if (!deletingParticipantId) return;

    isDeleteSubmitting = true;

    try {
      await deleteParticipant(split.id, deletingParticipantId);
      await onSplitUpdated();

      addToast({
        type: 'success',
        message: 'Participant removed successfully',
      });

      deletingParticipantId = null;
      deletingParticipantName = '';
    } catch (err: any) {
      if (err.status === 409) {
        addToast({
          type: 'error',
          message: `Cannot remove ${deletingParticipantName} - they have associated expenses. Remove or reassign their expenses first.`,
        });
      } else {
        addToast({
          type: 'error',
          message: err.detail || 'Failed to remove participant. Please try again.',
        });
      }

      deletingParticipantId = null;
      deletingParticipantName = '';
    } finally {
      isDeleteSubmitting = false;
    }
  }
</script>

<!-- Participants Section (AC 2, 3) + Add Participant (Story 3.1) -->
<section class="w-full">
  <Card.Root>
    <Card.Header class="pb-2 flex flex-row items-center justify-between">
      <Card.Title class="text-lg">Participants</Card.Title>
      {#if !showAddForm}
        <Button
          onclick={handleShowAddForm}
          variant="outline"
          size="sm"
          class="min-h-[44px]"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd" />
          </svg>
          Add Participant
        </Button>
      {/if}
    </Card.Header>
    <Card.Content>
      <!-- Add Participant Form (Story 3.1 AC 1, 2) -->
      {#if showAddForm}
        <form onsubmit={(e) => { e.preventDefault(); handleAddParticipant(); }} class="space-y-4 mb-4 p-4 bg-secondary/30 rounded-lg">
          <div class="space-y-2">
            <Label for="participant-name">Name</Label>
            <Input
              id="participant-name"
              type="text"
              placeholder="Enter name"
              bind:value={formName}
              class="min-h-[44px]"
              disabled={isSubmitting}
            />
            {#if validationErrors.name}
              <p class="text-sm text-destructive">{validationErrors.name}</p>
            {/if}
          </div>

          <div class="space-y-2">
            <Label for="participant-nights">Nights</Label>
            <Input
              id="participant-nights"
              type="number"
              bind:value={formNights}
              class="min-h-[44px]"
              disabled={isSubmitting}
            />
            {#if validationErrors.nights}
              <p class="text-sm text-destructive">{validationErrors.nights}</p>
            {/if}
          </div>

          <div class="flex gap-2">
            <Button type="submit" disabled={isSubmitting} class="flex-1 min-h-[44px]">
              {isSubmitting ? 'Adding...' : 'Add'}
            </Button>
            <Button type="button" variant="outline" onclick={handleCancelAddForm} disabled={isSubmitting} class="flex-1 min-h-[44px]">
              Cancel
            </Button>
          </div>
        </form>
      {/if}

      <!-- Participants List (AC 3) -->
      <div class="space-y-2">
        {#each split.participants as participant (participant.id)}
          <div class="flex items-center justify-between p-3 border rounded-lg hover:bg-secondary/50">
            {#if editingParticipantId === participant.id}
              <!-- Edit Mode (Story 3.2 AC 1, 2, 3) -->
              <form onsubmit={(e) => { e.preventDefault(); handleSaveEdit(); }} class="flex-1 flex items-center gap-2">
                <div class="flex-1 space-y-1">
                  <Input
                    type="text"
                    bind:value={editName}
                    class="min-h-[44px]"
                    disabled={isEditSubmitting}
                    aria-label="Edit name"
                  />
                  {#if editValidationErrors.name}
                    <p class="text-sm text-destructive">{editValidationErrors.name}</p>
                  {/if}
                </div>

                <div class="w-24 space-y-1">
                  <Input
                    type="number"
                    bind:value={editNights}
                    class="min-h-[44px]"
                    disabled={isEditSubmitting}
                    aria-label="Edit nights"
                  />
                  {#if editValidationErrors.nights}
                    <p class="text-sm text-destructive">{editValidationErrors.nights}</p>
                  {/if}
                </div>

                <div class="flex gap-1">
                  <Button type="submit" size="sm" disabled={isEditSubmitting} class="min-h-[44px] min-w-[44px]">
                    {isEditSubmitting ? '...' : '✓'}
                  </Button>
                  <Button type="button" size="sm" variant="outline" onclick={handleCancelEdit} disabled={isEditSubmitting} class="min-h-[44px] min-w-[44px]">
                    ✕
                  </Button>
                </div>
              </form>
            {:else}
              <!-- View Mode with inline edit (Story 3.2 AC 1) -->
              <button
                type="button"
                onclick={() => handleStartEdit(participant)}
                class="flex-1 flex items-center justify-between text-left hover:bg-secondary/30 p-2 rounded transition-colors"
              >
                <span class="font-medium">{participant.name}</span>
                <span class="text-sm text-muted-foreground">
                  {participant.nights} {participant.nights === 1 ? 'night' : 'nights'}
                </span>
              </button>

              <!-- Delete Button (Story 3.3 AC 1) -->
              <Button
                variant="ghost"
                size="sm"
                onclick={(e) => handleDeleteClick(e, participant)}
                class="ml-2 text-destructive hover:text-destructive hover:bg-destructive/10 min-h-[44px] min-w-[44px]"
                aria-label={`Remove ${participant.name}`}
              >
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
                </svg>
              </Button>
            {/if}
          </div>
        {/each}
      </div>
    </Card.Content>
  </Card.Root>
</section>

<!-- Delete Confirmation Dialog (Story 3.3 AC 2, 3) -->
<ConfirmDialog
  isOpen={!!deletingParticipantId}
  title="Remove Participant"
  message={`Are you sure you want to remove ${deletingParticipantName}? This action cannot be undone.`}
  confirmText="Remove"
  cancelText="Cancel"
  variant="destructive"
  onConfirm={handleConfirmDelete}
  onCancel={handleCancelDelete}
  isSubmitting={isDeleteSubmitting}
/>
