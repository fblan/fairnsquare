<script lang="ts">
  import { addParticipant, updateParticipant, deleteParticipant, type Split, type Participant } from '$lib/api/splits';
  import Button from '$lib/components/ui/button/button.svelte';
  import * as Card from '$lib/components/ui/card';
  import Input from '$lib/components/ui/input/input.svelte';
  import Label from '$lib/components/ui/label/label.svelte';
  import ConfirmDialog from '$lib/components/ui/confirm-dialog/confirm-dialog.svelte';
  import ExpenseEditModal from '$lib/components/expense/ExpenseEditModal.svelte';
  import EditParticipantModal from '$lib/components/participant/EditParticipantModal.svelte';
  import { addToast } from '$lib/stores/toastStore.svelte';
  import { Plus } from 'lucide-svelte';

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

  // Edit Participant state (Story FNS-002-4)
  let showEditParticipantModal = $state(false);
  let editingParticipant = $state<Participant | null>(null);
  let highlightedParticipantId = $state<string | null>(null);

  // Delete Participant state (Story 3.3)
  let deletingParticipantId = $state<string | null>(null);
  let deletingParticipantName = $state('');
  let isDeleteSubmitting = $state(false);

  // Add Expense Modal state (Story FNS-002-3)
  let showAddExpenseModal = $state(false);
  let selectedPayerId = $state<string | null>(null);

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

  // Per-participant stats calculation (FNS-002-2)
  function getParticipantStats(participantId: string) {
    const spent = split.expenses
      .filter(e => e.payerId === participantId)
      .reduce((sum, e) => sum + e.amount, 0);

    const cost = split.expenses
      .reduce((sum, e) => {
        const share = e.shares.find(s => s.participantId === participantId);
        return sum + (share?.amount || 0);
      }, 0);

    const balance = spent - cost;
    return { spent, cost, balance };
  }

  function formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IE', {
      style: 'currency',
      currency: 'EUR',
    }).format(amount);
  }

  function balanceColorClass(balance: number): string {
    if (balance > 0.005) return 'text-green-600';
    if (balance < -0.005) return 'text-red-600';
    return 'text-muted-foreground';
  }

  function balanceLabel(balance: number): string {
    if (balance > 0.005) return `Owed ${formatCurrency(balance)}`;
    if (balance < -0.005) return `Owes ${formatCurrency(Math.abs(balance))}`;
    return 'Settled';
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

    if (formNights < 0.5) {
      errors.nights = 'Nights must be at least 0.5';
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

  // Edit Participant handlers (Story FNS-002-4)
  function handleStartEdit(participant: Participant) {
    editingParticipant = participant;
    showEditParticipantModal = true;
  }

  function handleEditModalClose() {
    showEditParticipantModal = false;
    editingParticipant = null;
  }

  async function handleEditSuccess() {
    const updatedParticipantId = editingParticipant?.id;
    showEditParticipantModal = false;
    editingParticipant = null;
    await onSplitUpdated();

    // Highlight the updated participant card (AC 7 - FNS-002-4)
    if (updatedParticipantId) {
      highlightedParticipantId = updatedParticipantId;
      setTimeout(() => {
        highlightedParticipantId = null;
      }, 2000);
    }
  }

  // Delete Participant handlers (Story 3.3)
  function handleDeleteClick(event: MouseEvent, participant: Participant) {
    event.stopPropagation();
    deletingParticipantId = participant.id;
    deletingParticipantName = participant.name;
  }

  // Add Expense Modal handlers (Story FNS-002-3)
  function handleAddExpenseClick(event: MouseEvent, participant: Participant) {
    event.stopPropagation();
    selectedPayerId = participant.id;
    showAddExpenseModal = true;
  }

  function handleAddExpenseModalClose() {
    showAddExpenseModal = false;
    selectedPayerId = null;
  }

  async function handleAddExpenseSuccess() {
    await onSplitUpdated();
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

<!-- Participants Section - Participant Cards (FNS-002-2) -->
<section class="w-full space-y-4">
  <div class="flex items-center justify-between">
    <h2 class="text-lg font-semibold">Participants</h2>
  </div>

  {#if split.participants.length === 0 && !showAddForm}
    <p class="text-muted-foreground text-center py-4">No participants yet</p>
  {/if}

  <!-- Participant Cards -->
  {#each split.participants as participant (participant.id)}
    {@const stats = getParticipantStats(participant.id)}
    <Card.Root>
      <Card.Content class="py-4 transition-colors duration-500 {highlightedParticipantId === participant.id ? 'bg-teal-50' : ''}">
        <!-- Card Display Mode -->
        <div class="flex items-start justify-between">
          <div class="flex-1">
            <div class="flex items-center gap-2 mb-2">
              <span class="font-semibold text-lg">{participant.name}</span>
              <span class="text-xs px-2 py-0.5 rounded-full bg-teal-100 text-teal-700">
                {participant.nights} {participant.nights <= 1 ? 'night' : 'nights'}
              </span>
            </div>
            <!-- Stats Row -->
            <div class="flex flex-wrap gap-x-4 gap-y-1 text-sm">
              <span class="text-muted-foreground">Spent: <span class="font-medium text-foreground">{formatCurrency(stats.spent)}</span></span>
              <span class="text-muted-foreground">Cost: <span class="font-medium text-foreground">{formatCurrency(stats.cost)}</span></span>
              <span class={balanceColorClass(stats.balance)}>
                <span class="font-medium">{balanceLabel(stats.balance)}</span>
              </span>
            </div>
          </div>

          <!-- Action Buttons -->
          <div class="flex items-center gap-1 ml-2">
            <Button
              variant="ghost"
              size="sm"
              onclick={(e) => handleAddExpenseClick(e, participant)}
              class="text-primary hover:text-primary hover:bg-primary/10 min-h-[44px] min-w-[44px]"
              aria-label={`Add expense for ${participant.name}`}
            >
              <Plus class="h-4 w-4" />
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onclick={() => handleStartEdit(participant)}
              class="min-h-[44px] min-w-[44px]"
              aria-label={`Edit ${participant.name}`}
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
              </svg>
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onclick={(e) => handleDeleteClick(e, participant)}
              class="text-destructive hover:text-destructive hover:bg-destructive/10 min-h-[44px] min-w-[44px]"
              aria-label={`Delete ${participant.name}`}
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
              </svg>
            </Button>
          </div>
        </div>
      </Card.Content>
    </Card.Root>
  {/each}

  <!-- Add Participant Form -->
  {#if showAddForm}
    <Card.Root>
      <Card.Content class="py-4">
        <form onsubmit={(e) => { e.preventDefault(); handleAddParticipant(); }} class="space-y-3">
          <p class="text-sm font-medium text-teal-700">New Participant</p>

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
              step="0.5"
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
      </Card.Content>
    </Card.Root>
  {:else}
    <Button
      onclick={handleShowAddForm}
      variant="outline"
      class="w-full min-h-[44px]"
    >
      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 mr-1" viewBox="0 0 20 20" fill="currentColor">
        <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd" />
      </svg>
      Add Participant
    </Button>
  {/if}
</section>

<!-- Delete Confirmation Dialog (Story 3.3) -->
<ConfirmDialog
  open={!!deletingParticipantId}
  title={`Remove ${deletingParticipantName}?`}
  description="This cannot be undone."
  confirmLabel="Remove"
  cancelLabel="Cancel"
  onConfirm={handleConfirmDelete}
  onCancel={handleCancelDelete}
  isLoading={isDeleteSubmitting}
/>

<!-- Add Expense Modal (Story FNS-002-3) -->
<ExpenseEditModal
  open={showAddExpenseModal}
  splitId={split.id}
  preselectedPayerId={selectedPayerId}
  participants={split.participants}
  onClose={handleAddExpenseModalClose}
  onSuccess={handleAddExpenseSuccess}
/>

<!-- Edit Participant Modal (Story FNS-002-4) -->
<EditParticipantModal
  open={showEditParticipantModal}
  splitId={split.id}
  participant={editingParticipant}
  participants={split.participants}
  split={split}
  onClose={handleEditModalClose}
  onSuccess={handleEditSuccess}
/>