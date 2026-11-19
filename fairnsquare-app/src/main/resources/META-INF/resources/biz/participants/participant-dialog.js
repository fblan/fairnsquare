import { apiRequest } from '../../infra/api.js';
import { showToast } from '../../infra/toasts.js';
import { baseStyles, SPINNER } from '../../components/base-styles.js';
import { icon } from '../../components/icons.js';
import { FIELD_INFO } from '../../components/info-dialog.js';
import '../../components/confirm-dialog.js';

const MAX_NAME = 50;

const styles = new CSSStyleSheet();
styles.replaceSync(`
  form { display: flex; flex-direction: column; gap: 1rem; padding: 1rem; }
  .pair { display: flex; gap: 0.75rem; }
  .pair .field { flex: 1; min-width: 0; }
  .label-row { display: flex; align-items: center; gap: 0.25rem; }
  .info {
    display: inline-flex; padding: 0.125rem; color: var(--muted-foreground); background: none; border: 0;
    border-radius: 9999px; cursor: pointer;
  }
  .info:hover { background: var(--muted); }
  .info svg { width: 0.875rem; height: 0.875rem; }
  .buttons { display: flex; gap: 0.5rem; padding-top: 0.5rem; }
  .buttons .button { flex: 1; }
  .danger { padding-top: 1rem; border-top: 1px solid var(--border); }
`);

const template = document.createElement('template');
template.innerHTML = `
  <dialog class="modal" aria-labelledby="edit-title">
    <div class="modal-head">
      <h2 id="edit-title">Edit Participant</h2>
      <button class="button ghost icon close" type="button" aria-label="Close">${icon('x')}</button>
    </div>
    <form novalidate>
      <div class="field">
        <label for="name">Name</label>
        <input id="name" type="text" autofocus aria-describedby="name-error">
        <p id="name-error" class="error" hidden></p>
      </div>
      <div class="pair">
        <div class="field">
          <div class="label-row">
            <label for="nights">Nights</label>
            <button class="info" type="button" data-info="nights" aria-label="Info about Nights">${icon('info')}</button>
          </div>
          <input id="nights" type="number" step="1" min="1" max="365" aria-describedby="nights-error">
          <p id="nights-error" class="error" hidden></p>
        </div>
        <div class="field">
          <div class="label-row">
            <label for="share">Share</label>
            <button class="info" type="button" data-info="share" aria-label="Info about Share">${icon('info')}</button>
          </div>
          <input id="share" type="number" step="any" min="0.5" max="50" aria-describedby="share-error">
          <p id="share-error" class="error" hidden></p>
        </div>
      </div>
      <div class="buttons">
        <button class="button primary submit" type="submit">Save Changes</button>
        <button class="button outline cancel" type="button" aria-label="Cancel edit">Cancel</button>
      </div>
      <div class="danger">
        <button class="button destructive wide delete" type="button">Delete Participant</button>
      </div>
    </form>
  </dialog>
  <fns-info-dialog></fns-info-dialog>
  <fns-confirm-dialog></fns-confirm-dialog>
`;

/**
 * The window that edits a participant: name, nights, share, or deletes them (only without expenses paid).
 *
 *   dialog.openEdit({ splitId, participant, participants, hasExpenses });
 *
 * Fires `participantchange` (detail: `{id, deleted}`) after a save or a delete.
 */
class ParticipantDialog extends HTMLElement {
  constructor() {
    super();
    const root = this.attachShadow({ mode: 'open' });
    root.adoptedStyleSheets = [baseStyles, styles];
    root.append(template.content.cloneNode(true));
    const $ = (selector) => root.querySelector(selector);
    this.dialog = $('dialog');
    this.form = $('form');
    this.fields = {
      name: { input: $('#name'), error: $('#name-error') },
      nights: { input: $('#nights'), error: $('#nights-error') },
      share: { input: $('#share'), error: $('#share-error') },
    };
    this.submit = $('.submit');
    this.deleteButton = $('.delete');
    this.info = $('fns-info-dialog');
    this.confirm = $('fns-confirm-dialog');

    for (const [key, field] of Object.entries(this.fields)) {
      field.input.addEventListener('blur', () => {
        field.touched = true;
        this.validate();
      });
      field.input.addEventListener('input', () => {
        if (key === 'name') this.validate();
        this.refresh();
      });
    }
    this.fields.share.input.addEventListener('keydown', (event) => this.stepShare(event));
    this.form.addEventListener('submit', (event) => {
      event.preventDefault();
      this.save();
    });
    $('.close').addEventListener('click', () => this.closeAttempt());
    $('.cancel').addEventListener('click', () => this.closeAttempt());
    this.deleteButton.addEventListener('click', () => this.deleteParticipant());
    for (const button of root.querySelectorAll('.info')) {
      button.addEventListener('click', () => {
        const info = FIELD_INFO[button.dataset.info];
        this.info.show(info.title, info.text);
      });
    }
    this.dialog.addEventListener('cancel', (event) => {
      event.preventDefault();
      this.closeAttempt();
    });
    this.dialog.addEventListener('click', (event) => {
      if (event.target === this.dialog) this.closeAttempt();
    });
  }

  /**
   * @param {{splitId: string, participant: {id: string, name: string, nights: number, share: number,
   *   sharedAccountId?: string | null}, participants: Array<{id: string, name: string}>, hasExpenses: boolean}} options
   */
  openEdit({ splitId, participant, participants, hasExpenses }) {
    this.splitId = splitId;
    this.participant = participant;
    this.others = participants.filter((p) => p.id !== participant.id);
    this.loading = false;
    this.fields.name.input.value = participant.name;
    this.fields.nights.input.value = Math.max(1, Math.round(participant.nights));
    this.fields.share.input.value = participant.share;
    for (const field of Object.values(this.fields)) field.touched = false;
    this.initial = this.snapshot();
    this.deleteButton.disabled = hasExpenses;
    this.deleteButton.title = hasExpenses ? 'Remove expenses first' : '';
    this.validate();
    this.refresh();
    this.dialog.showModal();
  }

  snapshot() {
    return JSON.stringify({
      name: this.fields.name.input.value,
      nights: Number(this.fields.nights.input.value),
      share: Number(this.fields.share.input.value),
    });
  }

  get dirty() {
    return this.snapshot() !== this.initial;
  }

  errors() {
    const name = this.fields.name.input.value.trim();
    const nights = Number(this.fields.nights.input.value);
    const share = Number(this.fields.share.input.value);
    const errors = {};
    if (!name) errors.name = 'Name is required';
    else if (name.length > MAX_NAME) errors.name = `Name cannot exceed ${MAX_NAME} characters`;
    else if (this.others.some((p) => p.name.toLowerCase() === name.toLowerCase())) {
      errors.name = 'Name must be unique (participant already exists)';
    }
    if (this.fields.nights.input.value === '' || nights < 1) errors.nights = 'Nights must be at least 1';
    else if (nights > 365) errors.nights = 'Nights cannot exceed 365';
    if (this.fields.share.input.value === '' || share < 0.5) errors.share = 'Must be at least 0.5';
    else if (share > 50) errors.share = 'Cannot exceed 50';
    return errors;
  }

  /**
   * Shows the errors of the fields already left (the name also while typing, e.g. a duplicate). The fields start
   * filled: one the person empties says it is required, since Save stays disabled while the form is invalid.
   */
  validate() {
    const errors = this.errors();
    for (const [key, field] of Object.entries(this.fields)) {
      const message = field.touched || (key === 'name' && field.input.value.trim()) ? errors[key] : null;
      field.error.textContent = message ?? '';
      field.error.hidden = !message;
      if (message) field.input.setAttribute('aria-invalid', 'true');
      else field.input.removeAttribute('aria-invalid');
    }
  }

  refresh() {
    const valid = Object.keys(this.errors()).length === 0;
    this.submit.disabled = this.loading || !this.dirty || !valid;
    this.submit.textContent = this.loading ? 'Saving...' : 'Save Changes';
    if (this.loading) this.submit.insertAdjacentHTML('afterbegin', SPINNER);
  }

  /** Arrow keys move the share by 0.5, between 0.5 and 50 (typing stays free, e.g. 1.25). */
  stepShare(event) {
    if (event.key !== 'ArrowUp' && event.key !== 'ArrowDown') return;
    event.preventDefault();
    const input = this.fields.share.input;
    const current = Number(input.value) || 0;
    const next = event.key === 'ArrowUp' ? Math.min(50, current + 0.5) : Math.max(0.5, current - 0.5);
    input.value = Math.round(next * 100) / 100;
    this.refresh();
  }

  async save() {
    for (const field of Object.values(this.fields)) field.touched = true;
    this.validate();
    if (this.loading || !this.dirty || Object.keys(this.errors()).length) return;
    const name = this.fields.name.input.value.trim();
    const nights = Number(this.fields.nights.input.value);
    const share = Number(this.fields.share.input.value);
    this.loading = true;
    this.refresh();
    try {
      await apiRequest(`/splits/${encodeURIComponent(this.splitId)}/participants/${this.participant.id}`, {
        method: 'PUT',
        // The shared account grouping is managed from the Settlement page: keep it.
        body: JSON.stringify({ name, nights, share, sharedAccountId: this.participant.sharedAccountId ?? null }),
      });
      showToast({ type: 'success', message: 'Participant updated', description: `${name} · ${nights} nights · share ${share}` });
      this.changed(false);
      this.dialog.close();
    } catch (error) {
      showToast({ type: 'error', message: error.detail || 'Failed to update participant. Please try again.' });
    } finally {
      this.loading = false;
      this.refresh();
    }
  }

  async deleteParticipant() {
    const participant = this.participant;
    const deleted = await this.confirm.ask({
      title: `Delete ${participant.name}?`,
      description: 'This cannot be undone.',
      confirmLabel: 'Remove',
      onConfirm: async () => {
        try {
          await apiRequest(`/splits/${encodeURIComponent(this.splitId)}/participants/${participant.id}`, {
            method: 'DELETE',
          });
        } catch (error) {
          showToast({ type: 'error', message: error.detail || 'Failed to remove participant. Please try again.' });
          throw error;
        }
        showToast({ type: 'success', message: 'Participant removed', description: participant.name });
      },
    });
    if (deleted) {
      this.changed(true);
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

  changed(deleted) {
    this.dispatchEvent(new CustomEvent('participantchange', {
      bubbles: true, composed: true, detail: { id: this.participant.id, deleted },
    }));
  }
}

customElements.define('fns-participant-dialog', ParticipantDialog);
