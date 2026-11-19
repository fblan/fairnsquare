import { apiRequest } from '../../infra/api.js';
import { navigate } from '../../infra/navigation.js';
import { showToast } from '../../infra/toasts.js';
import { loadLastSplits, removeLastSplit, saveLastSplit } from '../../infra/stores/last-splits.js';
import { CAPTCHA_TOKEN_HEADER, clearToken, loadToken } from '../../infra/stores/captcha-token.js';
import { baseStyles, SPINNER } from '../../components/base-styles.js';
import '../../components/captcha-dialog.js';

const MAX_NAME = 100;

const styles = new CSSStyleSheet();
styles.replaceSync(`
  .page { display: flex; flex-direction: column; align-items: center; gap: 1.5rem; }
  header { padding: 2rem 0; text-align: center; }
  h1 { margin: 0; font-size: 1.5rem; line-height: 2rem; font-weight: 700; color: var(--primary); }
  header p { margin: 0.5rem 0 0; color: var(--muted-foreground); }
  .column { width: 100%; max-width: 520px; }
  .card-title { margin-bottom: 1.875rem; }
  form { display: flex; flex-direction: column; gap: 1rem; }
  .recent { border-color: #5eead4; background: #f0fdfa; }
  .recent .card-title { margin-bottom: 2.625rem; }
  ul { display: flex; flex-direction: column; gap: 0.5rem; margin: 0; padding: 0; list-style: none; }
  li { display: flex; align-items: center; gap: 0.75rem; min-height: 44px; }
  .name {
    flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    font-size: 0.875rem; font-weight: 500; color: var(--foreground);
  }
  .dismiss {
    flex: none; padding: 0; font: inherit; font-size: 0.875rem; color: var(--muted-foreground);
    background: none; border: 0; text-decoration: underline; text-underline-offset: 2px; cursor: pointer;
  }
  .dismiss:hover { color: var(--foreground); }
  .info { max-width: 520px; margin: 0; font-size: 0.875rem; text-align: center; color: var(--muted-foreground); }
  .dev {
    box-sizing: border-box; width: 100%; max-width: 520px; padding: 0.75rem;
    border: 1px dashed #fdba74; border-radius: var(--radius);
  }
  .dev p { margin: 0 0 0.25rem; font-size: 0.75rem; font-weight: 600; letter-spacing: 0.025em; text-transform: uppercase; color: #f97316; }
  .dev button { padding: 0; font: inherit; font-size: 0.75rem; color: #ea580c; background: none; border: 0; text-decoration: underline; cursor: pointer; }
`);

const template = document.createElement('template');
template.innerHTML = `
  <fns-captcha-dialog></fns-captcha-dialog>
  <div class="page">
    <header>
      <h1>FairNSquare</h1>
      <p>Split expenses fairly with friends</p>
    </header>

    <div class="column">
      <div class="card">
        <h2 class="card-title">Create a New Split</h2>
        <form novalidate>
          <div class="field">
            <label for="split-name">Split Name</label>
            <input id="split-name" type="text" placeholder="e.g., Weekend Trip" aria-describedby="split-name-error">
            <p id="split-name-error" class="error" hidden></p>
          </div>
          <button class="button primary wide" type="submit">Create Split</button>
        </form>
      </div>
    </div>

    <div class="column" hidden>
      <div class="card recent">
        <h2 class="card-title">Recent splits</h2>
        <ul></ul>
      </div>
    </div>

    <p class="info">No account needed. Create a split and share the link!</p>

    <section class="dev" hidden>
      <p>Dev tools</p>
      <button type="button">Clear CAPTCHA token</button>
    </section>
  </div>
`;

/**
 * The home page (`/`): create a split, or resume one of the splits this browser opened last.
 * Creating a split needs a CAPTCHA token; without a valid one, the CAPTCHA shows first.
 */
class HomePage extends HTMLElement {
  constructor() {
    super();
    const root = this.attachShadow({ mode: 'open' });
    root.adoptedStyleSheets = [baseStyles, styles];
    root.append(template.content.cloneNode(true));
    const $ = (selector) => root.querySelector(selector);
    this.captcha = $('fns-captcha-dialog');
    this.name = $('#split-name');
    this.nameError = $('#split-name-error');
    this.submit = $('button[type="submit"]');
    this.recent = $('.recent').parentElement;
    this.recentList = $('.recent ul');

    this.name.addEventListener('input', () => {
      this.touched = true;
      this.validate();
    });
    this.name.addEventListener('blur', () => {
      this.touched = true;
      this.validate();
    });
    $('form').addEventListener('submit', (event) => {
      event.preventDefault();
      this.create();
    });

    // Dev mode is the only time the app is served from localhost; production never is.
    const dev = $('.dev');
    dev.hidden = !['localhost', '127.0.0.1'].includes(location.hostname);
    dev.querySelector('button').addEventListener('click', () => clearToken());
  }

  connectedCallback() {
    this.showRecentSplits();
  }

  /** @returns {string | null} the error for the current name, once the field was touched */
  validate() {
    const value = this.name.value;
    let error = null;
    if (!value.trim()) error = 'Split name is required';
    else if (value.length > MAX_NAME) error = `Split name cannot exceed ${MAX_NAME} characters`;
    const shown = this.touched ? error : null;
    this.nameError.textContent = shown ?? '';
    this.nameError.hidden = !shown;
    if (shown) this.name.setAttribute('aria-invalid', 'true');
    else this.name.removeAttribute('aria-invalid');
    return error;
  }

  async create() {
    this.touched = true;
    if (this.validate() || this.loading) return;
    try {
      // Until created: a token refused by the server (401) means verifying again, then retrying.
      for (;;) {
        if (!loadToken()) await this.captcha.open();
        this.setLoading(true);
        try {
          const split = await apiRequest('/splits', {
            method: 'POST',
            headers: { [CAPTCHA_TOKEN_HEADER]: loadToken() ?? '' },
            body: JSON.stringify({ name: this.name.value.trim() }),
          });
          saveLastSplit({ id: split.id, name: split.name });
          navigate(`/splits/${split.id}/participants`);
          return;
        } catch (error) {
          if (error.status !== 401) {
            showToast({ type: 'error', message: error.detail || 'Failed to create split' });
            return;
          }
          clearToken();
          this.setLoading(false);
        }
      }
    } finally {
      this.setLoading(false);
    }
  }

  setLoading(loading) {
    this.loading = loading;
    this.name.disabled = loading;
    this.submit.disabled = loading;
    if (loading) this.submit.innerHTML = `${SPINNER} Creating...`;
    else this.submit.textContent = 'Create Split';
  }

  /** The stored recent splits that still exist on the server; the others are forgotten. */
  async showRecentSplits() {
    const stored = loadLastSplits();
    if (!stored.length) return;
    const results = await Promise.allSettled(stored.map((s) => apiRequest(`/splits/${encodeURIComponent(s.id)}`)));
    const splits = [];
    results.forEach((result, i) => {
      if (result.status === 'fulfilled') splits.push({ id: result.value.id, name: result.value.name });
      else removeLastSplit(stored[i].id);
    });
    this.renderRecentSplits(splits);
  }

  /** @param {Array<{id: string, name: string}>} splits */
  renderRecentSplits(splits) {
    this.recentList.replaceChildren(...splits.map((split) => {
      const item = document.createElement('li');
      const name = document.createElement('span');
      name.className = 'name';
      name.textContent = split.name;
      const resume = document.createElement('button');
      resume.type = 'button';
      resume.className = 'button primary small';
      resume.textContent = 'Resume';
      resume.addEventListener('click', () => navigate(`/splits/${split.id}`));
      const dismiss = document.createElement('button');
      dismiss.type = 'button';
      dismiss.className = 'dismiss';
      dismiss.textContent = 'Dismiss';
      dismiss.addEventListener('click', () => {
        removeLastSplit(split.id);
        item.remove();
        this.recent.hidden = !this.recentList.children.length;
      });
      item.append(name, resume, dismiss);
      return item;
    }));
    this.recent.hidden = !splits.length;
  }
}

customElements.define('fns-home-page', HomePage);
