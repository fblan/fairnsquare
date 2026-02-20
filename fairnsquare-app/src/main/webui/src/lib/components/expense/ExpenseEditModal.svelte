<script lang="ts">
  /**
   * ExpenseEditModal - Unified modal for adding and editing expenses.
   * Mode is determined by the `expense` prop: null = add, non-null = edit.
   */

  import { untrack } from 'svelte';
  import { addExpense, addFreeExpense, updateExpense, deleteExpense, type Expense, type Participant, type SplitMode } from '$lib/api/splits';
  import type { ApiError } from '$lib/api/client';
  import Button from '$lib/components/ui/button/button.svelte';
  import Input from '$lib/components/ui/input/input.svelte';
  import Label from '$lib/components/ui/label/label.svelte';
  import * as Select from '$lib/components/ui/select';
  import * as RadioGroup from '$lib/components/ui/radio-group';
  import ConfirmDialog from '$lib/components/ui/confirm-dialog/confirm-dialog.svelte';
  import { addToast } from '$lib/stores/toastStore.svelte';
  import { Moon, Equal, Edit3, Users, X } from 'lucide-svelte';

  // Props
  let {
    open,
    splitId,
    expense = null,
    preselectedPayerId = null,
    participants,
    onClose,
    onSuccess,
  }: {
    open: boolean;
    splitId: string;
    expense?: Expense | null;
    preselectedPayerId?: string | null;
    participants: Participant[];
    onClose: () => void;
    onSuccess: () => Promise<void>;
  } = $props();

  // Derived mode
  const isEditMode = $derived(expense != null);

  // Form state
  let amount = $state<number | ''>('');
  let description = $state('');
  let payerId = $state('');
  let payerSelected = $state<{ value: string; label: string }>({ value: '', label: '' });
  let splitMode = $state<SplitMode>('BY_NIGHT');
  let shareParts = $state<Record<string, number | ''>>({});
  let isLoading = $state(false);

  // Validation state
  let validationErrors = $state<{ amount?: string; description?: string }>({});
  let amountTouched = $state(false);
  let descriptionTouched = $state(false);

  // Confirmation state
  let showDiscardConfirm = $state(false);
  let showDeleteConfirm = $state(false);
  let isDeleting = $state(false);

  // Dirty tracking (edit mode)
  const isDirtyEdit = $derived(
    expense != null && (
      (typeof amount === 'number' ? amount : parseFloat(amount as string) || 0) !== expense.amount ||
      description !== expense.description ||
      payerId !== expense.payerId ||
      splitMode !== expense.splitMode
    )
  );

  // Dirty tracking (add mode)
  const isDirtyAdd = $derived(
    amount !== '' ||
    description !== '' ||
    (payerId !== '' && payerId !== preselectedPayerId) ||
    splitMode !== 'BY_NIGHT' ||
    Object.values(shareParts).some(val => val !== '')
  );

  const isDirty = $derived(isEditMode ? isDirtyEdit : isDirtyAdd);

  // Derived validation
  const amountValue = $derived(typeof amount === 'number' ? amount : parseFloat(amount as string) || 0);
  const isAmountValid = $derived(amountValue >= 0.01 && amountValue <= 999999.99);
  const isDescriptionValid = $derived(description.trim().length > 0 && description.length <= 200);
  const isPayerValid = $derived(payerId !== '');

  // FREE mode parts validation
  const totalParts: number = $derived(
    Object.values(shareParts).reduce((sum: number, val) => {
      const numVal = typeof val === 'number' ? val : (parseFloat(String(val)) || 0);
      return sum + numVal;
    }, 0)
  );

  const isPartsValid = $derived(
    splitMode !== 'FREE' || totalParts > 0
  );

  const isValid = $derived(isAmountValid && isDescriptionValid && isPayerValid && isPartsValid);

  // In add mode: only disable during loading (validation errors shown on submit)
  // In edit mode: disable when not dirty, invalid, or loading/deleting
  const canSubmit = $derived(
    isEditMode
      ? (isDirtyEdit && isValid && !isLoading && !isDeleting)
      : !isLoading
  );

  // Initialize form when modal opens
  $effect(() => {
    if (open) {
      untrack(() => resetForm());
      setTimeout(() => {
        if (typeof document !== 'undefined') {
          document.getElementById('expense-amount-modal')?.focus();
        }
      }, 50);
    }
  });

  function resetForm() {
    validationErrors = {};
    amountTouched = false;
    descriptionTouched = false;
    showDiscardConfirm = false;
    showDeleteConfirm = false;
    isLoading = false;
    isDeleting = false;

    // Initialize share parts for all participants
    shareParts = {};
    for (const participant of participants) {
      shareParts[participant.id] = '';
    }

    if (expense) {
      // Edit mode: populate from expense
      amount = expense.amount;
      description = expense.description;
      payerId = expense.payerId;
      splitMode = expense.splitMode;

      const payer = participants.find(p => p.id === expense.payerId);
      if (payer) {
        payerSelected = { value: payer.id, label: payer.name };
      }
    } else {
      // Add mode: blank form with optional pre-selected payer
      amount = '';
      description = '';
      splitMode = 'BY_NIGHT';

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
    if (description.trim().length === 0) {
      validationErrors.description = 'Description is required';
    } else if (description.length > 200) {
      validationErrors.description = 'Description cannot exceed 200 characters';
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

  function handleAmountKeydown(event: KeyboardEvent) {
    if (event.key === 'ArrowUp' || event.key === 'ArrowDown') {
      event.preventDefault();
      const current = typeof amount === 'number' ? amount : parseFloat(amount as string) || 0;
      const next = event.key === 'ArrowUp' ? current + 0.5 : Math.max(0, current - 0.5);
      amount = Math.round(next * 100) / 100;
    }
  }

  async function handleSubmit() {
    amountTouched = true;
    descriptionTouched = true;

    validateAmount();
    validateDescription();

    if (!canSubmit) return;

    isLoading = true;

    try {
      if (isEditMode && expense) {
        await updateExpense(splitId, expense.id, {
          amount: amountValue,
          description: description.trim(),
          payerId,
          splitMode,
        });
        addToast({ type: 'success', message: 'Expense updated' });
      } else if (splitMode === 'FREE') {
        const shares = participants.map(p => ({
          participantId: p.id,
          parts: typeof shareParts[p.id] === 'number'
            ? shareParts[p.id] as number
            : parseFloat(shareParts[p.id] as string) || 0,
        }));
        await addFreeExpense(splitId, {
          amount: amountValue,
          description: description.trim(),
          payerId,
          shares,
        });
        addToast({ type: 'success', message: 'Expense added' });
      } else {
        await addExpense(splitId, {
          amount: amountValue,
          description: description.trim(),
          payerId,
          splitMode,
        });
        addToast({ type: 'success', message: 'Expense added' });
      }

      await onSuccess();
      onClose();
    } catch (err: unknown) {
      const error = err as ApiError;
      addToast({
        type: 'error',
        message: error.detail || (isEditMode ? 'Failed to update expense. Please try again.' : 'Failed to add expense. Please try again.'),
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
    showDeleteConfirm = true;
  }

  async function handleConfirmDelete() {
    if (!expense) return;
    isDeleting = true;

    try {
      await deleteExpense(splitId, expense.id);
      addToast({ type: 'success', message: 'Expense deleted' });
      showDeleteConfirm = false;
      await onSuccess();
      onClose();
    } catch (err: unknown) {
      const error = err as ApiError;
      addToast({
        type: 'error',
        message: error.detail || 'Failed to delete expense',
      });
    } finally {
      isDeleting = false;
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
    if (event.target === event.currentTarget && !isLoading && !isDeleting) {
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
    aria-labelledby="expense-modal-title"
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
        <h2 id="expense-modal-title" class="text-lg font-semibold">
          {isEditMode ? 'Edit Expense' : 'Add Expense'}
        </h2>
        <Button
          variant="ghost"
          size="sm"
          onclick={handleCloseAttempt}
          disabled={isLoading || isDeleting}
          class="min-h-[44px] min-w-[44px]"
          aria-label="Close"
        >
          <X class="h-4 w-4" />
        </Button>
      </div>

      <!-- Form -->
      <form onsubmit={(e) => { e.preventDefault(); handleSubmit(); }} class="p-4 space-y-4">
        <!-- Amount Field -->
        <div class="space-y-2">
          <Label for="expense-amount-modal">Amount (€)</Label>
          <Input
            id="expense-amount-modal"
            type="number"
            step="any"
            placeholder="€0.00"
            bind:value={amount}
            onblur={handleAmountBlur}
            onkeydown={handleAmountKeydown}
            class="min-h-[44px]"
            aria-invalid={amountTouched && !!validationErrors.amount}
            disabled={isLoading || isDeleting}
          />
          {#if amountTouched && validationErrors.amount}
            <p class="text-sm text-destructive">{validationErrors.amount}</p>
          {/if}
        </div>

        <!-- Description Field -->
        <div class="space-y-2">
          <Label for="expense-description-modal">Description</Label>
          <Input
            id="expense-description-modal"
            type="text"
            placeholder="e.g., Groceries"
            bind:value={description}
            onblur={handleDescriptionBlur}
            class="min-h-[44px]"
            aria-invalid={descriptionTouched && !!validationErrors.description}
            disabled={isLoading || isDeleting}
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
            <Select.Trigger id="expense-payer-modal" class="min-h-[44px]" disabled={isLoading || isDeleting}>
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
          <RadioGroup.Root bind:value={splitMode} class="flex flex-col space-y-2" disabled={isLoading || isDeleting}>
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
            <div class="flex items-center space-x-2 p-3 border rounded-lg hover:bg-accent min-h-[44px]">
              <RadioGroup.Item value="BY_PERSON" id="modal-mode-by-person" />
              <Label for="modal-mode-by-person" class="flex items-center gap-2 cursor-pointer flex-1">
                <Users class="h-4 w-4" aria-hidden="true" />
                <span>By Person</span>
              </Label>
            </div>
            <div class="flex items-center space-x-2 p-3 border rounded-lg hover:bg-accent min-h-[44px]">
              <RadioGroup.Item value="FREE" id="modal-mode-free" />
              <Label for="modal-mode-free" class="flex items-center gap-2 cursor-pointer flex-1">
                <Edit3 class="h-4 w-4" aria-hidden="true" />
                <span>Manual</span>
              </Label>
            </div>
          </RadioGroup.Root>
        </div>

        <!-- FREE Mode Parts Inputs -->
        {#if splitMode === 'FREE'}
          <div class="space-y-3 p-4 border rounded-lg bg-muted/30">
            <div class="flex items-center justify-between">
              <Label class="text-sm font-medium">Share Parts</Label>
              <span class="text-sm {isPartsValid ? 'text-green-600' : 'text-destructive'}">
                Total: {totalParts.toFixed(2)} parts {totalParts > 0 ? '✓' : '⚠'}
              </span>
            </div>
            <p class="text-xs text-muted-foreground">
              Amounts will be calculated proportionally. E.g., 2 parts & 3 parts splits €100 as €40 & €60
            </p>
            {#if !isPartsValid && (amountTouched || descriptionTouched)}
              <p class="text-xs text-destructive">At least one participant must have positive parts</p>
            {/if}
            {#each participants as participant (participant.id)}
              <div class="flex items-center gap-2">
                <Label for="share-{participant.id}" class="flex-1 text-sm">
                  {participant.name}
                </Label>
                <Input
                  id="share-{participant.id}"
                  type="number"
                  step="0.01"
                  min="0"
                  placeholder="0"
                  bind:value={shareParts[participant.id]}
                  disabled={isLoading || isDeleting}
                  class="w-32 min-h-[44px]"
                />
              </div>
            {/each}
          </div>
        {/if}

        <!-- Delete Expense Button (edit mode only) -->
        {#if isEditMode}
          <div class="pt-2 border-t">
            <Button
              type="button"
              variant="destructive"
              onclick={handleDeleteClick}
              disabled={isLoading || isDeleting}
              class="w-full min-h-[44px]"
              aria-label="Delete expense"
            >
              Delete Expense
            </Button>
          </div>
        {/if}

        <!-- Action Buttons -->
        <div class="flex gap-2 pt-2">
          <Button
            type="submit"
            disabled={!canSubmit}
            class="flex-1 min-h-[44px]"
          >
            {#if isLoading}
              <svg class="animate-spin h-4 w-4 mr-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              {isEditMode ? 'Saving...' : 'Adding...'}
            {:else}
              {isEditMode ? 'Save Changes' : 'Add Expense'}
            {/if}
          </Button>
          <Button
            type="button"
            variant="outline"
            onclick={handleCloseAttempt}
            disabled={isLoading || isDeleting}
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

<!-- Delete Confirmation Dialog (edit mode only) -->
<ConfirmDialog
  open={showDeleteConfirm}
  title="Delete this expense?"
  description="This will recalculate balances."
  confirmLabel="Delete"
  cancelLabel="Cancel"
  isLoading={isDeleting}
  onConfirm={handleConfirmDelete}
  onCancel={handleCancelDelete}
/>