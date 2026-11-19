const DEFAULT_DURATION = 5000;
const ICONS = { success: '✓', error: '✕', warning: '⚠', info: 'ℹ' };

const template = document.createElement('template');
template.innerHTML = `
  <style>
    .stack {
      position: fixed; inset: auto 1rem 1rem auto; margin: 0; padding: 0; border: 0; overflow: visible;
      background: transparent; display: flex; flex-direction: column; gap: 0.5rem;
      width: 20rem; max-width: calc(100vw - 2rem);
    }
    .stack:not(:popover-open) { display: none; }
    .toast {
      display: flex; align-items: flex-start; gap: 0.75rem; padding: 0.75rem 1rem;
      border-radius: var(--radius);
      box-shadow: 0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1);
      color: #fff; background: var(--primary);
    }
    .success { background: #16a34a; }
    .error { background: var(--destructive); }
    .warning { background: #eab308; }
    .info { color: var(--primary-foreground); background: var(--primary); }
    .icon { flex: none; margin-top: 0.125rem; font-size: 0.875rem; font-weight: 700; }
    .text { flex: 1; min-width: 0; }
    .message { margin: 0; font-size: 0.875rem; font-weight: 500; white-space: pre-line; overflow-wrap: anywhere; }
    .description {
      margin: 0.125rem 0 0; font-size: 0.75rem; opacity: 0.8;
      overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    }
    button {
      flex: none; padding: 0; font: inherit; font-size: 0.875rem; font-weight: 700; line-height: 1;
      color: inherit; background: none; border: 0; opacity: 0.7; cursor: pointer;
    }
    button:hover { opacity: 1; }
  </style>
  <div class="stack" popover="manual" aria-live="polite" aria-atomic="false"></div>
`;

/**
 * The toast notifications, bottom right. Listens to the `fns-toast` events that showToast() (infra/toasts.js)
 * and the Svelte addToast() send. Put one in the page.
 *
 * The stack is a popover, so it stays above an open dialog (both live in the browser's top layer).
 */
class Toaster extends HTMLElement {
  constructor() {
    super();
    this.attachShadow({ mode: 'open' }).append(template.content.cloneNode(true));
    this.stack = this.shadowRoot.querySelector('.stack');
    this.onToast = (event) => this.add(event.detail);
  }

  connectedCallback() {
    window.addEventListener('fns-toast', this.onToast);
  }

  disconnectedCallback() {
    window.removeEventListener('fns-toast', this.onToast);
  }

  add({ type = 'info', message, description, duration = DEFAULT_DURATION }) {
    const toast = document.createElement('div');
    toast.className = `toast ${ICONS[type] ? type : 'info'}`;
    toast.setAttribute('role', 'alert');

    const iconEl = document.createElement('span');
    iconEl.className = 'icon';
    iconEl.textContent = ICONS[type] ?? ICONS.info;

    const text = document.createElement('div');
    text.className = 'text';
    const messageEl = document.createElement('p');
    messageEl.className = 'message';
    messageEl.textContent = message;
    text.append(messageEl);
    if (description) {
      const descriptionEl = document.createElement('p');
      descriptionEl.className = 'description';
      descriptionEl.textContent = description;
      text.append(descriptionEl);
    }

    const dismiss = document.createElement('button');
    dismiss.type = 'button';
    dismiss.textContent = '✕';
    dismiss.setAttribute('aria-label', 'Dismiss');
    dismiss.addEventListener('click', () => this.removeToast(toast));

    toast.append(iconEl, text, dismiss);
    this.stack.append(toast);
    this.bringToFront();
    setTimeout(() => this.removeToast(toast), duration);
  }

  removeToast(toast) {
    toast.remove();
    if (!this.stack.children.length && this.stack.matches(':popover-open')) {
      this.stack.hidePopover();
    }
  }

  /** Re-showing the popover puts it above a dialog opened since. */
  bringToFront() {
    if (this.stack.matches(':popover-open')) this.stack.hidePopover();
    this.stack.showPopover();
  }
}

customElements.define('fns-toaster', Toaster);
