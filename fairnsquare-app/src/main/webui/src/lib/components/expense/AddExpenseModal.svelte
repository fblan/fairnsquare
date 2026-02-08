<script lang="ts">
  /**
   * AddExpenseModal - Story FNS-002-3
   * Modal for adding expenses with pre-selected payer from participant card
   */

  import { addExpense, type Participant, type SplitMode } from '$lib/api/splits';
  import Button from '$lib/components/ui/button/button.svelte';
  import Input from '$lib/components/ui/input/input.svelte';
  import Label from '$lib/components/ui/label/label.svelte';
  import * as Select from '$lib/components/ui/select';
  import * as RadioGroup from '$lib/components/ui/radio-group';
  import ConfirmDialog from '$lib/components/ui/confirm-dialog/confirm-dialog.svelte';
  import { addToast } from '$lib/stores/toastStore.svelte';
  import { Moon, Equal, X } from 'lucide-svelte';

  // Props interface (Svelte 5 pattern)
  let {
    open,
    splitId,
    preselectedPayerId = null,
    participants,
    onClose,
    onSuccess,
  }: {
    open: boolean;
    splitId: string;
    preselectedPayerId?: string | null;
    participants: Participant[];
    onClose: () => void;
    onSuccess: () => Promise<void>;
  } = $props();

  // Form state
  let amount = $state<number | ''>('');
  let description = $state('');
  let payerId = $state('');
  let payerSelected = $state<{ value: string; label: string }>({ value: '', label: '' });
  let splitMode = $state<SplitMode>('BY_NIGHT');
  let isLoading = $state(false);

  // Validation state
  let validationErrors = $state<{ amount?: string; description?: string }>({});
  let amountTouched = $state(false);
  let descriptionTouched = $state(false);

  // Dirty form confirmation state
  let showDiscardConfirm = $state(false);

  // Track if form is dirty
  const isDirty = $derived(
    amount !== '' ||
    description !== '' ||
    (payerId !== '' && payerId !== preselectedPayerId) ||
    splitMode !== 'BY_NIGHT'
  );

  // Derived validation
  const amountValue = $derived(typeof amount === 'number' ? amount : parseFloat(amount as string) || 0);
  const isAmountValid = $derived(amountValue >= 0.01 && amountValue <= 999999.99);
  const isDescriptionValid = $derived(description.length <= 100);
  const isPayerValid = $derived(payerId !== '');

  const isValid = $derived(isAmountValid && isDescriptionValid && isPayerValid);

  // Reset form when modal opens
  $effect(() => {
    if (open) {
      resetForm();
      // Auto-focus amount field
      setTimeout(() => {
        document.getElementById('expense-amount-modal')?.focus();
      }, 50);
    }
  });

  function resetForm() {
    amount = '';
    description = '';
    validationErrors = {};
    amountTouched = false;
    descriptionTouched = false;
    splitMode = 'BY_NIGHT';
    showDiscardConfirm = false;
    isLoading = false;

    // Set pre-selected payer
    if (preselectedPayerId) {
      const participant = participants.find(p => p.id === preselectedPayerId);
      if (participant) {
        payerId = participant.id;
        payerSelected = { value: participant.id, label: participant.name };
      }
    } else if (participants.length > 0) {
      payerId = participants[0].id;
      payerSelected = { value: participants[0].id, label: participants[0].name };
    }
  }

  function validateAmount() {
    if (amount === '' || amountValue < 0.01) {
      validationErrors.amount = 'Amount must be at least €0.01';
    } else if (amountValue > 999999.99) {
      validationErrors.amount = 'Amount cannot exceed €999,999.99';
    } else {
      delete validationErrors.amount;
      validationErrors = { ...validationErrors };
    }
  }

  function validateDescription() {
    if (description.length > 100) {
      validationErrors.description = 'Description cannot exceed 100 characters';
    } else {
      delete validationErrors.description;
      validationErrors = { ...validationErrors };
    }
  }

  function handleAmountBlur() {
    amountTouched = true;
    validateAmount();
  }

  function handleDescriptionBlur() {
    descriptionTouched = true;
    validateDescription();
  }

  async function handleSubmit() {
    // Touch all fields to show validation errors
    amountTouched = true;
    descriptionTouched = true;

    // Final validation
    validateAmount();
    validateDescription();

    if (!isValid) return;
    if (isLoading) return;

    isLoading = true;

    try {
      await addExpense(splitId, {
        amount: amountValue,
        description: description.trim(),
        payerId,
        splitMode,
      });

      addToast({
        type: 'success',
        message: 'Expense added',
      });

      await onSuccess();
      onClose();
    } catch (err: any) {
      addToast({
        type: 'error',
        message: err.detail || 'Failed to add expense. Please try again.',
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

  function handleKeydown(event: KeyboardEvent) {
    if (!open) return;

    if (event.key === 'Escape' && !isLoading && !showDiscardConfirm) {
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
  <div
    class="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4"
    onclick={handleBackdropClick}
    role="dialog"
    aria-modal="true"
    aria-labelledby="add-expense-title"
    tabindex="-1"
  >
    <!-- Modal Content - onclick stops propagation to backdrop -->
    <!-- svelte-ignore a11y_click_events_have_key_events a11y_no_static_element_interactions -->
    <div
      class="bg-background rounded-lg shadow-lg w-full max-w-[420px] animate-in fade-in zoom-in-95"
      onclick={(e) => e.stopPropagation()}
    >
      <!-- Header -->
      <div class="flex items-center justify-between p-4 border-b">
        <h2 id="add-expense-title" class="text-lg font-semibold">
          Add Expense
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
        <!-- Amount Field (auto-focus) -->
        <div class="space-y-2">
          <Label for="expense-amount-modal">Amount (€)</Label>
          <Input
            id="expense-amount-modal"
            type="number"
            step="0.01"
            min="0.01"
            placeholder="€0.00"
            bind:value={amount}
            onblur={handleAmountBlur}
            class="min-h-[44px]"
            aria-invalid={amountTouched && !!validationErrors.amount}
            disabled={isLoading}
          />
          {#if amountTouched && validationErrors.amount}
            <p class="text-sm text-destructive">{validationErrors.amount}</p>
          {/if}
        </div>

        <!-- Description Field (optional) -->
        <div class="space-y-2">
          <Label for="expense-description-modal">Description (optional)</Label>
          <Input
            id="expense-description-modal"
            type="text"
            placeholder="e.g., Groceries"
            bind:value={description}
            onblur={handleDescriptionBlur}
            class="min-h-[44px]"
            aria-invalid={descriptionTouched && !!validationErrors.description}
            disabled={isLoading}
          />
          {#if descriptionTouched && validationErrors.description}
            <p class="text-sm text-destructive">{validationErrors.description}</p>
          {/if}
        </div>

        <!-- Payer Select -->
        <div class="space-y-2">
          <Label for="expense-payer-modal">Payer</Label>
          <Select.Root
            bind:selected={payerSelected}
            onSelectedChange={(selected) => {
              if (selected) {
                payerId = selected.value;
                payerSelected = selected;
              }
            }}
          >
            <Select.Trigger id="expense-payer-modal" class="min-h-[44px]" disabled={isLoading}>
              <Select.Value placeholder="Select payer" />
            </Select.Trigger>
            <Select.Content>
              {#each participants as participant}
                <Select.Item value={participant.id} label={participant.name}>
                  {participant.name}
                </Select.Item>
              {/each}
            </Select.Content>
          </Select.Root>
        </div>

        <!-- Split Mode Radio Group -->
        <div class="space-y-2">
          <Label>Split Mode</Label>
          <RadioGroup.Root bind:value={splitMode} class="flex flex-col space-y-2" disabled={isLoading}>
            <div class="flex items-center space-x-2 p-3 border rounded-lg hover:bg-accent min-h-[44px]">
              <RadioGroup.Item value="BY_NIGHT" id="modal-mode-by-night" />
              <Label for="modal-mode-by-night" class="flex items-center gap-2 cursor-pointer flex-1">
                <Moon class="h-4 w-4" aria-hidden="true" />
                <span>By Night</span>
              </Label>
            </div>
            <div class="flex items-center space-x-2 p-3 border rounded-lg hover:bg-accent min-h-[44px]">
              <RadioGroup.Item value="EQUAL" id="modal-mode-equal" />
              <Label for="modal-mode-equal" class="flex items-center gap-2 cursor-pointer flex-1">
                <Equal class="h-4 w-4" aria-hidden="true" />
                <span>Equal</span>
              </Label>
            </div>
          </RadioGroup.Root>
        </div>

        <!-- Action Buttons -->
        <div class="flex gap-2 pt-2">
          <Button
            type="submit"
            disabled={isLoading}
            class="flex-1 min-h-[44px]"
          >
            {#if isLoading}
              <svg class="animate-spin h-4 w-4 mr-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              Adding...
            {:else}
              Add Expense
            {/if}
          </Button>
          <Button
            type="button"
            variant="outline"
            onclick={handleCloseAttempt}
            disabled={isLoading}
            class="flex-1 min-h-[44px]"
          >
            Cancel
          </Button>
        </div>
      </form>
    </div>
  </div>
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