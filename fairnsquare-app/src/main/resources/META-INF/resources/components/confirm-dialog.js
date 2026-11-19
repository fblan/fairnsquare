import { baseStyles, SPINNER } from './base-styles.js';

const styles = new CSSStyleSheet();
styles.replaceSync(`
  dialog.modal { max-width: 28rem; padding: 1.5rem; }
  h2 { margin: 0 0 0.5rem; font-size: 1.125rem; line-height: 1.75rem; font-weight: 600; }
  p { margin: 0 0 1.5rem; color: var(--muted-foreground); }
  .actions { display: flex; flex-direction: column-reverse; gap: 0.5rem; }
  .actions .button { width: 100%; }
  @media (min-width: 640px) {
    .actions { flex-direction: row; justify-content: flex-end; }
    .actions .button { width: auto; }
  }
`);

const template = document.createElement('template');
template.innerHTML = `
  <dialog class="modal" aria-labelledby="confirm-title" aria-describedby="confirm-description">
    <h2 id="confirm-title"></h2>
    <p id="confirm-description"></p>
    <div class="actions">
      <button class="button outline cancel" type="button"></button>
      <button class="button destructive confirm" type="button"></button>
    </div>
  </dialog>
`;

/**
 * Asks to confirm a destructive action ("Delete this expense?").
 *
 *   const done = await confirmDialog.ask({ title, description, confirmLabel, cancelLabel, onConfirm });
 *
 * While `onConfirm` runs, the buttons are disabled and a spinner shows; if it throws, the window stays open so the
 * person can try again or cancel. Resolves true once confirmed and done, false when cancelled.
 */
class ConfirmDialog extends HTMLElement {
  constructor() {
    super();
    const root = this.attachShadow({ mode: 'open' });
    root.adoptedStyleSheets = [baseStyles, styles];
    root.append(template.content.cloneNode(true));
    this.dialog = root.querySelector('dialog');
    this.title_ = root.querySelector('h2');
    this.description = root.querySelector('p');
    this.cancelButton = root.querySelector('.cancel');
    this.confirmButton = root.querySelector('.confirm');

    this.cancelButton.addEventListener('click', () => this.finish(false));
    this.confirmButton.addEventListener('click', () => this.confirm());
    this.dialog.addEventListener('cancel', (event) => {
      event.preventDefault();
      if (!this.busy) this.finish(false);
    });
    this.dialog.addEventListener('click', (event) => {
      if (event.target === this.dialog && !this.busy) this.finish(false);
    });
  }

  /**
   * @param {{title: string, description: string, confirmLabel?: string, cancelLabel?: string,
   *   onConfirm?: () => Promise<void> | void}} options
   * @returns {Promise<boolean>}
   */
  ask({ title, description, confirmLabel = 'Confirm', cancelLabel = 'Cancel', onConfirm }) {
    this.title_.textContent = title;
    this.description.textContent = description;
    this.cancelButton.textContent = cancelLabel;
    this.confirmLabel = confirmLabel;
    this.onConfirm = onConfirm;
    this.setBusy(false);
    this.dialog.showModal();
    return new Promise((resolve) => {
      this.resolve = resolve;
    });
  }

  async confirm() {
    if (this.onConfirm) {
      this.setBusy(true);
      try {
        await this.onConfirm();
      } catch {
        // The action reported its own error (a toast); stay open.
        this.setBusy(false);
        return;
      }
    }
    this.finish(true);
  }

  finish(confirmed) {
    this.dialog.close();
    this.resolve?.(confirmed);
    this.resolve = null;
  }

  setBusy(busy) {
    this.busy = busy;
    this.cancelButton.disabled = busy;
    this.confirmButton.disabled = busy;
    if (busy) this.confirmButton.innerHTML = SPINNER;
    else this.confirmButton.textContent = '';
    this.confirmButton.append(this.confirmLabel ?? '');
  }
}

customElements.define('fns-confirm-dialog', ConfirmDialog);
