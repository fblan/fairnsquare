import { navigate } from '../infra/navigation.js';
import { showToast } from '../infra/toasts.js';
import { baseStyles } from './base-styles.js';
import { icon } from './icons.js';

const TABS = [
  { label: 'Home', icon: 'home', path: '' },
  { label: 'Participants', icon: 'users', path: '/participants' },
  { label: 'Expenses', icon: 'receipt', path: '/expenses' },
  { label: 'Settlement', icon: 'arrow-left-right', path: '/settlement' },
];

const styles = new CSSStyleSheet();
styles.replaceSync(`
  :host { width: 100%; }
  .header { display: flex; flex-direction: column; gap: 0.75rem; width: 100%; }
  .title { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; }
  h1 {
    min-width: 0; margin: 0; font-size: 1.125rem; line-height: 1.75rem; font-weight: 700;
    color: var(--primary); overflow-wrap: break-word;
  }
  .share { flex: none; min-height: 44px; padding: 0.25rem 0.75rem; gap: 0.375rem; }
  nav { display: flex; border-bottom: 1px solid var(--border); }
  nav button {
    display: flex; align-items: center; gap: 0.375rem; min-height: 44px; padding: 0.5rem 0.75rem;
    font: inherit; font-size: 0.875rem; font-weight: 500; white-space: nowrap;
    color: var(--muted-foreground); background: none; border: 0; cursor: pointer;
  }
  nav button:hover { color: var(--foreground); }
  nav button:focus-visible { outline: none; box-shadow: inset 0 0 0 2px color-mix(in oklch, var(--ring) 50%, transparent); }
  nav button[aria-current="page"] { margin-bottom: -1px; color: var(--primary); border-bottom: 2px solid var(--primary); }
  nav svg { flex: none; width: 1rem; height: 1rem; }
  nav .label { display: none; }
  @media (min-width: 640px) { nav .label { display: inline; } }
  nav .close { margin-left: auto; }

  dialog {
    box-sizing: border-box; width: 100%; max-width: 100%; margin: auto auto 0; padding: 0;
    color: var(--foreground); background: var(--background); border: 0; border-radius: 0.75rem 0.75rem 0 0;
    box-shadow: 0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1);
  }
  dialog::backdrop { background: rgb(0 0 0 / 0.5); }
  @media (min-width: 640px) {
    dialog { width: calc(100% - 2rem); max-width: 420px; margin: auto; border-radius: 0.75rem; }
  }
  .dialog-body { padding: 1.5rem; text-align: center; }
  .dialog-body h2 { margin: 0; font-size: 1rem; font-weight: 600; }
  .dialog-body p { margin: 0.75rem 0 0; font-size: 0.875rem; color: var(--muted-foreground); }
  .dialog-foot { padding: 1rem; border-top: 1px solid var(--border); }
`);

const template = document.createElement('template');
template.innerHTML = `
  <div class="header">
    <header class="title">
      <h1></h1>
      <button class="button outline share" type="button" aria-label="Share link">${icon('share-2')} Share</button>
    </header>
    <nav aria-label="Split navigation">
      ${TABS.map((tab) => `<button type="button" data-path="${tab.path}" aria-label="${tab.label}">
        ${icon(tab.icon)}<span class="label">${tab.label}</span></button>`).join('')}
      <button type="button" class="close" aria-label="Close split">${icon('x')}</button>
    </nav>
  </div>
  <dialog aria-labelledby="blocked-title">
    <div class="dialog-body">
      <h2 id="blocked-title">A participant is required</h2>
      <p>Please add at least one participant before navigating away from this page.</p>
    </div>
    <div class="dialog-foot"><button class="button primary wide" type="button">Got it</button></div>
  </dialog>
`;

/**
 * The header of every split page: the split's name, Share, and the tabs between the split's pages.
 *
 * Attributes: `split-id`, `split-name`, and `require-participant` (present while the split has no participant:
 * leaving the page is then blocked with a dialog that says why).
 */
class SplitHeader extends HTMLElement {
  static observedAttributes = ['split-name'];

  constructor() {
    super();
    const root = this.attachShadow({ mode: 'open' });
    root.adoptedStyleSheets = [baseStyles, styles];
    root.append(template.content.cloneNode(true));
    this.heading = root.querySelector('h1');
    this.tabs = [...root.querySelectorAll('nav button[data-path]')];
    this.dialog = root.querySelector('dialog');

    root.querySelector('.share').addEventListener('click', () => this.share());
    for (const tab of this.tabs) {
      tab.addEventListener('click', () => this.go(`/splits/${this.getAttribute('split-id')}${tab.dataset.path}`));
    }
    root.querySelector('.close').addEventListener('click', () => this.go('/'));
    root.querySelector('.dialog-foot button').addEventListener('click', () => this.dialog.close());
    // A click on the dimmed background (outside the dialog box) closes it too.
    this.dialog.addEventListener('click', (event) => {
      if (event.target === this.dialog) this.dialog.close();
    });
    this.onLocationChange = () => this.markActiveTab();
  }

  connectedCallback() {
    this.markActiveTab();
    window.addEventListener('popstate', this.onLocationChange);
  }

  disconnectedCallback() {
    window.removeEventListener('popstate', this.onLocationChange);
  }

  attributeChangedCallback() {
    this.heading.textContent = this.getAttribute('split-name') ?? '';
  }

  markActiveTab() {
    const path = location.pathname;
    const active = this.tabs.find((tab) => tab.dataset.path && path.endsWith(tab.dataset.path)) ?? this.tabs[0];
    for (const tab of this.tabs) {
      if (tab === active) tab.setAttribute('aria-current', 'page');
      else tab.removeAttribute('aria-current');
    }
  }

  /** @param {string} path */
  go(path) {
    if (this.hasAttribute('require-participant')) {
      this.dialog.showModal();
      return;
    }
    navigate(path);
  }

  async share() {
    const url = location.href;
    try {
      await navigator.clipboard.writeText(url);
      showToast({ type: 'success', message: 'Link copied!' });
    } catch {
      // No clipboard (insecure context, refused permission): show the link instead.
      showToast({ type: 'info', message: `Share link: ${url}` });
    }
  }
}

customElements.define('fns-split-header', SplitHeader);
