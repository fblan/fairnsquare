import { baseStyles } from './base-styles.js';
import { icon } from './icons.js';

/** What each copy button puts in the parts: nights × share, or the share (number of persons) alone. */
const COPIES = {
  nights: { copied: 'nights × share', changes: 'nights or share', parts: (p) => p.nights * p.share },
  persons: { copied: 'share (number of persons)', changes: 'share', parts: (p) => p.share },
};

const styles = new CSSStyleSheet();
styles.replaceSync(`
  dialog.modal { max-width: 380px; }
  .modal-body { display: flex; flex-direction: column; gap: 0.75rem; }
  .top { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; }
  .hint { margin: 0; font-size: 0.75rem; color: var(--muted-foreground); }
  .total { flex: none; font-size: 0.875rem; color: #16a34a; }
  .total.invalid { color: var(--destructive); }
  .copies { display: flex; flex-wrap: wrap; gap: 0.5rem; }
  .copies .button { flex: 1 1 10rem; }
  .warning {
    display: flex; flex-direction: column; gap: 0.5rem; padding: 0.75rem;
    border: 1px solid #fcd34d; border-radius: calc(var(--radius) - 2px); background: #fffbeb;
  }
  .warning .text { display: flex; align-items: flex-start; gap: 0.5rem; }
  .warning svg { flex: none; width: 1rem; height: 1rem; margin-top: 0.125rem; color: #d97706; }
  .warning p { margin: 0; font-size: 0.75rem; color: #92400e; }
  .warning .buttons { display: flex; gap: 0.5rem; }
  .warning .button { flex: 1; }
  ul { display: flex; flex-direction: column; gap: 0.5rem; max-height: 300px; margin: 0; padding: 0; overflow-y: auto; list-style: none; }
  li { display: flex; align-items: center; gap: 0.5rem; min-height: 44px; }
  li input[type="checkbox"] { width: 20px; height: 20px; min-height: 0; margin: 0; accent-color: var(--primary); box-shadow: none; }
  li label { flex: 1; font-size: 0.875rem; line-height: 1.25rem; font-weight: 400; cursor: pointer; }
  li input[type="number"] { width: 6rem; }
`);

const template = document.createElement('template');
template.innerHTML = `
  <dialog class="modal" aria-labelledby="share-title">
    <div class="modal-head">
      <h2 id="share-title">Edit Shares</h2>
      <button class="button ghost icon close" type="button" aria-label="Close">${icon('x')}</button>
    </div>
    <div class="modal-body">
      <div class="top">
        <p class="hint">Select participants and set their share parts</p>
        <span class="total"></span>
      </div>
      <div class="copies">
        <button class="button outline copy" type="button" data-copy="nights">${icon('moon')} Copy nights shares</button>
        <button class="button outline copy" type="button" data-copy="persons">${icon('users')} Copy persons shares</button>
      </div>
      <div class="warning" role="alert" hidden>
        <div class="text">
          ${icon('alert-triangle')}
          <p>This will overwrite all current values with each participant's <strong class="copied"></strong>.
            These values are a static snapshot — future changes to <span class="changes"></span> will not be
            reflected here automatically.</p>
        </div>
        <div class="buttons">
          <button class="button primary small apply" type="button">Apply</button>
          <button class="button outline small keep" type="button">Cancel</button>
        </div>
      </div>
      <ul aria-label="Participant shares"></ul>
    </div>
    <div class="modal-foot">
      <button class="button primary confirm" type="button">Confirm</button>
      <button class="button outline cancel" type="button">Cancel</button>
    </div>
  </dialog>
`;

/**
 * Who shares a "Manual" expense, and how many parts each one takes.
 *
 *   const shares = await shareDialog.edit({ participants, parts, checked, previous });
 *
 * `parts`, `checked` and `previous` are maps by participant id (`previous` remembers the parts of an unchecked
 * participant, restored when checked again). Resolves with the edited maps, or null when cancelled.
 */
class ShareDialog extends HTMLElement {
  constructor() {
    super();
    const root = this.attachShadow({ mode: 'open' });
    root.adoptedStyleSheets = [baseStyles, styles];
    root.append(template.content.cloneNode(true));
    const $ = (selector) => root.querySelector(selector);
    this.dialog = $('dialog');
    this.total = $('.total');
    this.warning = $('.warning');
    this.list = $('ul');
    this.confirmButton = $('.confirm');

    $('.close').addEventListener('click', () => this.finish(null));
    $('.cancel').addEventListener('click', () => this.finish(null));
    for (const button of root.querySelectorAll('.copy')) {
      button.addEventListener('click', () => this.askCopy(button.dataset.copy));
    }
    $('.keep').addEventListener('click', () => { this.warning.hidden = true; });
    $('.apply').addEventListener('click', () => this.applyCopy());
    this.confirmButton.addEventListener('click', () => this.finish({
      parts: { ...this.parts }, checked: { ...this.checked }, previous: { ...this.previous },
    }));
    // Escape closes only this window, not the expense window under it.
    this.dialog.addEventListener('cancel', (event) => {
      event.preventDefault();
      this.finish(null);
    });
    this.dialog.addEventListener('click', (event) => {
      if (event.target === this.dialog) this.finish(null);
    });
  }

  /**
   * @param {{participants: Array<{id: string, name: string, nights: number, share: number}>,
   *   parts: Record<string, number | ''>, checked: Record<string, boolean>, previous: Record<string, number>}} data
   * @returns {Promise<{parts: Record<string, number | ''>, checked: Record<string, boolean>,
   *   previous: Record<string, number>} | null>}
   */
  edit({ participants, parts, checked, previous }) {
    this.participants = participants;
    this.parts = { ...parts };
    this.checked = { ...checked };
    this.previous = { ...previous };
    this.warning.hidden = true;
    this.renderList();
    this.dialog.showModal();
    return new Promise((resolve) => {
      this.resolve = resolve;
    });
  }

  renderList() {
    this.list.replaceChildren(...this.participants.map((participant) => {
      const item = document.createElement('li');
      const box = document.createElement('input');
      box.type = 'checkbox';
      box.id = `share-check-${participant.id}`;
      box.checked = !!this.checked[participant.id];
      box.setAttribute('aria-label', `Include ${participant.name}`);
      const label = document.createElement('label');
      label.htmlFor = box.id;
      label.textContent = participant.name;
      const parts = document.createElement('input');
      parts.type = 'number';
      parts.step = '0.01';
      parts.min = '0';
      parts.placeholder = '0';
      parts.value = this.parts[participant.id] ?? '';
      parts.disabled = !box.checked;
      parts.setAttribute('aria-label', `${participant.name} parts`);

      box.addEventListener('change', () => {
        this.checked[participant.id] = box.checked;
        if (box.checked) {
          this.parts[participant.id] = this.previous[participant.id] || 1;
        } else {
          const current = toNumber(this.parts[participant.id]);
          if (current > 0) this.previous[participant.id] = current;
          this.parts[participant.id] = 0;
        }
        parts.value = this.parts[participant.id];
        parts.disabled = !box.checked;
        this.updateTotal();
      });
      parts.addEventListener('input', () => {
        this.parts[participant.id] = parts.value === '' ? '' : Number(parts.value);
        this.updateTotal();
      });
      item.append(box, label, parts);
      return item;
    }));
    this.updateTotal();
  }

  /** Shows the warning before overwriting the parts with what `kind` (a key of COPIES) copies. */
  askCopy(kind) {
    this.copyKind = kind;
    this.warning.querySelector('.copied').textContent = COPIES[kind].copied;
    this.warning.querySelector('.changes').textContent = COPIES[kind].changes;
    this.warning.hidden = false;
  }

  applyCopy() {
    const { parts } = COPIES[this.copyKind];
    for (const participant of this.participants) {
      const value = parts(participant);
      this.parts[participant.id] = value;
      this.checked[participant.id] = true;
      this.previous[participant.id] = value;
    }
    this.warning.hidden = true;
    this.renderList();
  }

  updateTotal() {
    const total = Object.values(this.parts).reduce((sum, value) => sum + toNumber(value), 0);
    this.total.textContent = `${total.toFixed(2)} parts ${total > 0 ? '✓' : '⚠'}`;
    this.total.classList.toggle('invalid', !(total > 0));
    this.confirmButton.disabled = !(total > 0);
  }

  finish(result) {
    this.dialog.close();
    this.resolve?.(result);
    this.resolve = null;
  }
}

/** @param {number | string | ''} value */
function toNumber(value) {
  return typeof value === 'number' ? value : parseFloat(value) || 0;
}

customElements.define('fns-share-dialog', ShareDialog);
