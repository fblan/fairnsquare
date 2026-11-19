import { apiRequest } from '../infra/api.js';
import { showToast } from '../infra/toasts.js';
import { baseStyles, SPINNER } from './base-styles.js';
import { icon } from './icons.js';
import './confirm-dialog.js';
import './share-dialog.js';

const MODES = [
  { value: 'BY_NIGHT', label: 'By Night', icon: 'moon' },
  { value: 'BY_SHARE', label: 'By Participant members', icon: 'users' },
  { value: 'FREE', label: 'Manual', icon: 'edit-3' },
];

const MODE_INFO = {
  BY_NIGHT: {
    title: 'By Night',
    text: [
      'The cost is shared proportionally based on each participant\'s total "night-shares" — calculated as nights × share weight.',
      'The share weight represents the number of persons. For example, a couple has a share of 2, while a solo traveller has a share of 1.',
      'Example: Alice (1 person) stays 3 nights = 3 night-shares. Bob & Carol (couple, 2 persons) stay 2 nights = 4 night-shares. Total = 7 night-shares.',
      'Alice pays 3/7 of the expense, Bob & Carol pay 4/7.',
    ],
  },
  BY_SHARE: {
    title: 'By Participant members',
    text: [
      'The cost is split proportionally to each participant\'s share weight, which represents their number of persons.',
      'This is ideal for shared expenses where everyone was present, like a restaurant meal.',
      'Example: Alice (1 person) and a couple Bob & Carol (2 persons) share a €90 restaurant bill. Total = 3 shares. Alice pays €30 (1/3), Bob & Carol pay €60 (2/3).',
    ],
  },
  FREE: {
    title: 'Manual',
    text: [
      'You define the exact parts for each participant. You choose who is included and assign them a relative weight — the total cost is then distributed proportionally.',
      'Example: you assign Alice 1 part, Bob 2 parts, and Carol 3 parts. Total = 6 parts. Alice pays 1/6, Bob pays 2/6, Carol pays 3/6 of the expense.',
    ],
  },
};

const MIN_AMOUNT = 0.01;
const MAX_AMOUNT = 999999.99;
const MAX_DESCRIPTION = 200;

const styles = new CSSStyleSheet();
styles.replaceSync(`
  form { display: flex; flex-direction: column; flex: 1; min-height: 0; }
  .modal-body { display: flex; flex-direction: column; gap: 1rem; }
  fieldset { display: flex; flex-direction: column; gap: 0.5rem; margin: 0; padding: 0; border: 0; min-width: 0; }
  legend { margin-bottom: 0.5rem; padding: 0; font-size: 0.875rem; line-height: 1; font-weight: 500; }
  .mode {
    display: flex; align-items: center; gap: 0.5rem; min-height: 44px; padding: 0 0.75rem;
    border: 1px solid var(--border); border-radius: var(--radius);
  }
  .mode:hover { background: var(--accent); }
  .mode label {
    display: flex; flex: 1; align-items: center; gap: 0.5rem; min-height: 44px;
    font-size: 0.875rem; line-height: 1; font-weight: 500; cursor: pointer;
  }
  .mode input { width: 1rem; height: 1rem; min-height: 0; margin: 0; accent-color: var(--primary); box-shadow: none; }
  .mode svg { flex: none; width: 1rem; height: 1rem; }
  .info {
    display: inline-flex; padding: 0.25rem; color: var(--muted-foreground); background: none; border: 0;
    border-radius: 9999px; cursor: pointer;
  }
  .info:hover { background: var(--muted); }
  .info svg { width: 1rem; height: 1rem; }
  .shares {
    display: flex; flex-direction: column; gap: 0.5rem; padding: 1rem;
    border: 1px solid var(--border); border-radius: var(--radius); background: color-mix(in oklch, var(--muted) 30%, transparent);
  }
  .shares .top { display: flex; align-items: center; justify-content: space-between; }
  .shares .title { font-size: 0.875rem; font-weight: 500; }
  .shares .total { font-size: 0.875rem; color: #16a34a; }
  .shares .total.invalid { color: var(--destructive); }
  .shares .who { margin: 0; font-size: 0.875rem; color: var(--muted-foreground); }
  .shares .error { font-size: 0.75rem; }
  .delete { padding-top: 0.5rem; border-top: 1px solid var(--border); }
  .delete .button { width: 100%; }
  .modal-foot { border-top: 0; padding-top: 0.5rem; }
  .info-text p { margin: 0 0 0.75rem; font-size: 0.875rem; line-height: 1.625; color: var(--muted-foreground); }
  .info-text p:last-child { margin-bottom: 0; }
  .info-dialog .modal-head h2 { font-size: 1rem; }
`);

const template = document.createElement('template');
template.innerHTML = `
  <dialog class="modal expense" aria-labelledby="expense-title">
    <div class="modal-head">
      <h2 id="expense-title"></h2>
      <button class="button ghost icon close" type="button" aria-label="Close">${icon('x')}</button>
    </div>
    <form novalidate>
      <div class="modal-body">
        <div class="field">
          <label for="description">Description</label>
          <input id="description" name="description" type="text" placeholder="e.g., Groceries" autofocus
            aria-describedby="description-error">
          <p id="description-error" class="error" hidden></p>
        </div>
        <div class="field">
          <label for="amount">Amount (€)</label>
          <input id="amount" name="amount" type="number" step="any" placeholder="€0.00" aria-describedby="amount-error">
          <p id="amount-error" class="error" hidden></p>
        </div>
        <div class="field">
          <label for="payer">Payer</label>
          <select id="payer" name="payer"></select>
        </div>
        <fieldset>
          <legend>Split Mode</legend>
          ${MODES.map((mode) => `
            <div class="mode">
              <label><input type="radio" name="mode" value="${mode.value}">${icon(mode.icon)}<span>${mode.label}</span></label>
              <button class="info" type="button" data-mode="${mode.value}" aria-label="Info about ${mode.label}">${icon('info')}</button>
            </div>`).join('')}
        </fieldset>
        <div class="shares" hidden>
          <div class="top"><span class="title">Share Parts</span><span class="total"></span></div>
          <p class="who"></p>
          <p class="error parts-error" hidden>At least one participant must have positive parts</p>
          <button class="button outline small wide edit-shares" type="button">${icon('edit-3')} Edit shares</button>
        </div>
        <div class="delete" hidden>
          <button class="button destructive" type="button" aria-label="Delete expense">Delete Expense</button>
        </div>
      </div>
      <div class="modal-foot">
        <button class="button primary submit" type="submit"></button>
        <button class="button outline cancel" type="button">Cancel</button>
      </div>
    </form>
  </dialog>

  <dialog class="modal sheet info-dialog" aria-labelledby="info-title">
    <div class="modal-head">
      <h2 id="info-title"></h2>
      <button class="button ghost icon info-close" type="button" aria-label="Close">${icon('x')}</button>
    </div>
    <div class="modal-body info-text"></div>
    <div class="modal-foot"><button class="button primary info-ok" type="button">Got it</button></div>
  </dialog>

  <fns-confirm-dialog></fns-confirm-dialog>
  <fns-share-dialog></fns-share-dialog>
`;

/**
 * The window that adds or edits an expense.
 *
 *   dialog.openAdd({ splitId, participants, payerId });      // payerId: preselected payer, optional
 *   dialog.openEdit({ splitId, participants, expense });
 *
 * After a save or a delete it fires `expensechange` (bubbles, crosses Shadow DOM) so the page reloads.
 */
class ExpenseDialog extends HTMLElement {
  constructor() {
    super();
    const root = this.attachShadow({ mode: 'open' });
    root.adoptedStyleSheets = [baseStyles, styles];
    root.append(template.content.cloneNode(true));
    const $ = (selector) => root.querySelector(selector);
    this.dialog = $('dialog.expense');
    this.heading = $('#expense-title');
    this.form = $('form');
    this.description = $('#description');
    this.descriptionError = $('#description-error');
    this.amount = $('#amount');
    this.amountError = $('#amount-error');
    this.payer = $('#payer');
    this.modes = [...root.querySelectorAll('input[name="mode"]')];
    this.shares = $('.shares');
    this.deleteBlock = $('.delete');
    this.submit = $('.submit');
    this.confirm = $('fns-confirm-dialog');
    this.shareDialog = $('fns-share-dialog');
    this.infoDialog = $('.info-dialog');

    this.description.addEventListener('blur', () => { this.descriptionTouched = true; this.validate(); });
    this.amount.addEventListener('blur', () => { this.amountTouched = true; this.validate(); });
    this.amount.addEventListener('keydown', (event) => this.stepAmount(event));
    this.form.addEventListener('input', () => {
      this.validate();
      this.refresh();
    });
    this.form.addEventListener('change', () => this.refresh());
    this.form.addEventListener('submit', (event) => {
      event.preventDefault();
      this.save();
    });
    $('.close').addEventListener('click', () => this.closeAttempt());
    $('.cancel').addEventListener('click', () => this.closeAttempt());
    $('.edit-shares').addEventListener('click', () => this.editShares());
    $('.delete .button').addEventListener('click', () => this.deleteExpense());
    this.dialog.addEventListener('cancel', (event) => {
      event.preventDefault();
      this.closeAttempt();
    });
    this.dialog.addEventListener('click', (event) => {
      if (event.target === this.dialog) this.closeAttempt();
    });

    for (const button of root.querySelectorAll('.info')) {
      button.addEventListener('click', () => this.showInfo(button.dataset.mode));
    }
    $('.info-close').addEventListener('click', () => this.infoDialog.close());
    $('.info-ok').addEventListener('click', () => this.infoDialog.close());
    this.infoDialog.addEventListener('click', (event) => {
      if (event.target === this.infoDialog) this.infoDialog.close();
    });
  }

  /** @param {{splitId: string, participants: object[], payerId?: string | null}} options */
  openAdd({ splitId, participants, payerId = null }) {
    this.open({ splitId, participants, expense: null, payerId });
  }

  /** @param {{splitId: string, participants: object[], expense: object}} options */
  openEdit({ splitId, participants, expense }) {
    this.open({ splitId, participants, expense, payerId: null });
  }

  open({ splitId, participants, expense, payerId }) {
    this.splitId = splitId;
    this.participants = participants;
    this.expense = expense;
    this.loading = false;
    this.descriptionTouched = false;
    this.amountTouched = false;
    this.submitted = false;

    this.heading.textContent = expense ? 'Edit Expense' : 'Add Expense';
    this.payer.replaceChildren(...participants.map((p) => new Option(p.name, p.id)));
    this.deleteBlock.hidden = !expense;

    // Shares (Manual mode): parts, included or not, and the parts to restore when included again.
    this.parts = {};
    this.checked = {};
    this.previous = {};
    for (const p of participants) {
      this.parts[p.id] = '';
      this.checked[p.id] = false;
      this.previous[p.id] = 1;
    }

    if (expense) {
      this.description.value = expense.description;
      this.amount.value = expense.amount;
      this.payer.value = expense.payerId;
      this.setMode(expense.splitMode);
      if (expense.splitMode === 'FREE') {
        for (const share of expense.shares ?? []) {
          const parts = share.parts ?? 0;
          if (parts > 0) {
            this.checked[share.participantId] = true;
            this.parts[share.participantId] = parts;
            this.previous[share.participantId] = parts;
          }
        }
      }
    } else {
      this.description.value = '';
      this.amount.value = '';
      const preselected = participants.find((p) => p.id === payerId) ?? participants[0];
      this.payer.value = preselected?.id ?? '';
      this.setMode('BY_NIGHT');
      if (this.payer.value) {
        this.checked[this.payer.value] = true;
        this.parts[this.payer.value] = 1;
      }
    }
    this.initial = this.snapshot();
    this.refresh();
    this.dialog.showModal();
  }

  /** @param {string} value */
  setMode(value) {
    for (const radio of this.modes) radio.checked = radio.value === value;
  }

  get mode() {
    return this.modes.find((radio) => radio.checked)?.value ?? 'BY_NIGHT';
  }

  get amountValue() {
    return parseFloat(this.amount.value) || 0;
  }

  get totalParts() {
    return Object.values(this.parts).reduce((sum, value) => sum + toNumber(value), 0);
  }

  /** What the person can change, to tell whether they changed anything. */
  snapshot() {
    return JSON.stringify({
      description: this.description.value,
      amount: this.amountValue,
      payer: this.payer.value,
      mode: this.mode,
      parts: this.participants.map((p) => toNumber(this.parts[p.id])),
    });
  }

  get dirty() {
    return this.snapshot() !== this.initial;
  }

  /** @returns {{description?: string, amount?: string}} */
  errors() {
    const errors = {};
    const description = this.description.value;
    if (!description.trim()) errors.description = 'Description is required';
    else if (description.length > MAX_DESCRIPTION) errors.description = `Description cannot exceed ${MAX_DESCRIPTION} characters`;
    if (this.amount.value === '' || this.amountValue < MIN_AMOUNT) errors.amount = 'Amount must be at least €0.01';
    else if (this.amountValue > MAX_AMOUNT) errors.amount = 'Amount cannot exceed €999,999.99';
    return errors;
  }

  get valid() {
    const errors = this.errors();
    return !errors.description && !errors.amount && !!this.payer.value && (this.mode !== 'FREE' || this.totalParts > 0);
  }

  /**
   * Shows the errors of the fields already left. A field that started empty (a new expense) is only "required"
   * once the person submits (frontend rule) — showing it on leaving the field also moved the buttons under the
   * finger. A field the person emptied says so right away: editing, Save stays disabled, so submit never comes.
   */
  validate() {
    const errors = this.errors();
    const shown = (touched, empty, startedEmpty, error) =>
      (touched && (this.submitted || !empty || !startedEmpty) ? error : null);
    showError(this.description, this.descriptionError, shown(this.descriptionTouched,
      !this.description.value.trim(), !this.expense, errors.description));
    showError(this.amount, this.amountError, shown(this.amountTouched,
      this.amount.value === '', !this.expense, errors.amount));
  }

  /** Brings the window in line with the form: Manual shares block, submit button. */
  refresh() {
    const free = this.mode === 'FREE';
    this.shares.hidden = !free;
    if (free) {
      const total = this.totalParts;
      const totalEl = this.shares.querySelector('.total');
      totalEl.textContent = `${total.toFixed(2)} parts ${total > 0 ? '✓' : '⚠'}`;
      totalEl.classList.toggle('invalid', !(total > 0));
      const who = this.participants.filter((p) => this.checked[p.id]);
      this.shares.querySelector('.who').textContent = who.length
        ? who.map((p) => `${p.name}: ${toNumber(this.parts[p.id])}`).join(', ')
        : 'No participants selected';
      this.shares.querySelector('.parts-error').hidden =
        total > 0 || !this.submitted;
    }
    this.submit.textContent = this.loading
      ? (this.expense ? 'Saving...' : 'Adding...')
      : (this.expense ? 'Save Changes' : 'Add Expense');
    if (this.loading) this.submit.insertAdjacentHTML('afterbegin', SPINNER);
    // Editing: only once something changed and the form is valid. Adding: errors show on submit.
    this.submit.disabled = this.loading || (this.expense ? !(this.dirty && this.valid) : false);
    for (const control of this.form.elements) {
      if (control !== this.submit) control.disabled = this.loading;
    }
  }

  /** Arrow keys move the amount by €0.50 (typing stays free, e.g. 12.30). */
  stepAmount(event) {
    if (event.key !== 'ArrowUp' && event.key !== 'ArrowDown') return;
    event.preventDefault();
    const next = event.key === 'ArrowUp' ? this.amountValue + 0.5 : Math.max(0, this.amountValue - 0.5);
    this.amount.value = Math.round(next * 100) / 100;
    this.refresh();
  }

  async editShares() {
    const result = await this.shareDialog.edit({
      participants: this.participants, parts: this.parts, checked: this.checked, previous: this.previous,
    });
    if (result) {
      ({ parts: this.parts, checked: this.checked, previous: this.previous } = result);
      this.refresh();
    }
  }

  showInfo(mode) {
    const info = MODE_INFO[mode];
    this.infoDialog.querySelector('h2').textContent = info.title;
    this.infoDialog.querySelector('.info-text').replaceChildren(...info.text.map((text) => {
      const paragraph = document.createElement('p');
      paragraph.textContent = text;
      return paragraph;
    }));
    this.infoDialog.showModal();
  }

  async save() {
    this.descriptionTouched = true;
    this.amountTouched = true;
    this.submitted = true;
    this.validate();
    this.refresh();
    if (this.loading || !this.valid) return;

    const body = { amount: this.amountValue, description: this.description.value.trim(), payerId: this.payer.value };
    const payerName = this.participants.find((p) => p.id === body.payerId)?.name ?? 'Unknown';
    const summary = `${body.description} · €${body.amount.toFixed(2)} · Paid by ${payerName}`;
    const base = `/splits/${encodeURIComponent(this.splitId)}/expenses`;
    this.loading = true;
    this.refresh();
    try {
      if (this.mode === 'FREE') {
        if (this.expense) {
          // The server cannot change the shares of an expense: replace it.
          await apiRequest(`${base}/${this.expense.id}`, { method: 'DELETE' });
        }
        const shares = this.participants.map((p) => ({ participantId: p.id, parts: toNumber(this.parts[p.id]) }));
        await apiRequest(`${base}/free`, { method: 'POST', body: JSON.stringify({ ...body, shares }) });
      } else if (this.expense) {
        await apiRequest(`${base}/${this.expense.id}`, {
          method: 'PUT', body: JSON.stringify({ ...body, splitMode: this.mode }),
        });
      } else {
        await apiRequest(`${base}/${this.mode === 'BY_NIGHT' ? 'by-night' : 'by-share'}`, {
          method: 'POST', body: JSON.stringify(body),
        });
      }
      showToast({ type: 'success', message: this.expense ? 'Expense updated' : 'Expense added', description: summary });
      this.changed();
      this.dialog.close();
    } catch (error) {
      showToast({
        type: 'error',
        message: error.detail || (this.expense
          ? 'Failed to update expense. Please try again.'
          : 'Failed to add expense. Please try again.'),
      });
    } finally {
      this.loading = false;
      this.refresh();
    }
  }

  async deleteExpense() {
    const expense = this.expense;
    const deleted = await this.confirm.ask({
      title: 'Delete this expense?',
      description: 'This will recalculate balances.',
      confirmLabel: 'Delete',
      onConfirm: async () => {
        try {
          await apiRequest(`/splits/${encodeURIComponent(this.splitId)}/expenses/${expense.id}`, { method: 'DELETE' });
        } catch (error) {
          showToast({ type: 'error', message: error.detail || 'Failed to delete expense' });
          throw error;
        }
        showToast({
          type: 'success', message: 'Expense deleted', description: `${expense.description} · €${expense.amount.toFixed(2)}`,
        });
      },
    });
    if (deleted) {
      this.changed();
      this.dialog.close();
    }
  }

  async closeAttempt() {
    if (this.loading) return;
    if (!this.dirty) {
      this.dialog.close();
      return;
    }
    const discard = await this.confirm.ask({
      title: 'Discard changes?',
      description: 'You have unsaved changes. Are you sure you want to close?',
      confirmLabel: 'Discard',
      cancelLabel: 'Keep editing',
    });
    if (discard) this.dialog.close();
  }

  changed() {
    this.dispatchEvent(new CustomEvent('expensechange', { bubbles: true, composed: true }));
  }
}

/** Shows or clears a field's error message, and marks the field invalid for screen readers. */
function showError(input, element, message) {
  element.textContent = message ?? '';
  element.hidden = !message;
  if (message) input.setAttribute('aria-invalid', 'true');
  else input.removeAttribute('aria-invalid');
}

/** @param {number | string | ''} value */
function toNumber(value) {
  return typeof value === 'number' ? value : parseFloat(value) || 0;
}

customElements.define('fns-expense-dialog', ExpenseDialog);
