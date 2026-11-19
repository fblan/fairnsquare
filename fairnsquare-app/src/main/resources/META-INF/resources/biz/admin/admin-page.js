import { apiRequest } from '../../infra/api.js';

/** The admin token stays for the browser tab only. */
const SESSION_KEY = 'admin_token';
const TABLE_ROWS = 5;

const template = document.createElement('template');
template.innerHTML = `
  <style>
    :host { display: block; }
    [hidden] { display: none !important; }
    .page {
      display: flex; flex-direction: column; align-items: center; gap: 1.5rem;
      width: 100%; max-width: 900px; margin: 0 auto; padding: 2rem 1rem; box-sizing: border-box;
    }
    header { text-align: center; }
    h1 { margin: 0; font-size: 1.5rem; line-height: 2rem; font-weight: 700; color: var(--primary); }
    .subtitle { margin: 0.25rem 0 0; font-size: 0.875rem; color: var(--muted-foreground); }
    .card {
      box-sizing: border-box; width: 100%; padding: 1.5rem;
      color: var(--card-foreground); background: var(--card);
      border: 1px solid var(--border); border-radius: 0.75rem;
      box-shadow: 0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1);
    }
    .signin { max-width: 400px; }
    .signin h2 { margin: 0 0 1.5rem; font-size: 1rem; line-height: 1; font-weight: 600; }
    form { display: flex; flex-direction: column; gap: 1rem; }
    .field { display: flex; flex-direction: column; gap: 0.5rem; }
    label { font-size: 0.875rem; line-height: 1; font-weight: 500; }
    input {
      box-sizing: border-box; width: 100%; min-height: 44px; padding: 0.25rem 0.75rem;
      font: inherit; font-size: 0.875rem; color: inherit; background: var(--background);
      border: 1px solid var(--input); border-radius: calc(var(--radius) - 2px);
      box-shadow: 0 1px 2px 0 rgb(0 0 0 / 0.05); outline: none;
    }
    input:focus-visible {
      border-color: var(--ring); box-shadow: 0 0 0 3px color-mix(in oklch, var(--ring) 50%, transparent);
    }
    input:disabled { cursor: not-allowed; opacity: 0.5; }
    .error { margin: 0; font-size: 0.875rem; color: var(--destructive); }
    button {
      min-height: 44px; padding: 0.5rem 1rem; font: inherit; font-size: 0.875rem; font-weight: 500;
      border-radius: calc(var(--radius) - 2px); cursor: pointer;
    }
    button:focus-visible { outline: none; box-shadow: 0 0 0 3px color-mix(in oklch, var(--ring) 50%, transparent); }
    button:disabled { pointer-events: none; opacity: 0.5; }
    .primary {
      width: 100%; color: var(--primary-foreground); background: var(--primary); border: none;
      box-shadow: 0 1px 2px 0 rgb(0 0 0 / 0.05);
    }
    .primary:hover { background: color-mix(in oklch, var(--primary) 90%, transparent); }
    .outline {
      color: inherit; background: var(--background); border: 1px solid var(--border);
      box-shadow: 0 1px 2px 0 rgb(0 0 0 / 0.05);
    }
    .outline:hover { color: var(--accent-foreground); background: var(--accent); }
    .stats { display: flex; flex-direction: column; gap: 1.5rem; width: 100%; }
    .toolbar { display: flex; justify-content: flex-end; }
    .summary { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0.75rem; }
    .summary .card { padding: 2.5rem 1.5rem; text-align: center; }
    .summary .last { grid-column: span 2; text-align: left; }
    @media (min-width: 640px) {
      .summary { grid-template-columns: repeat(3, minmax(0, 1fr)); }
      .summary .last { grid-column: span 1; }
    }
    .big { margin: 0; font-size: 1.875rem; line-height: 2.25rem; font-weight: 700; color: var(--primary); }
    .medium { margin: 0; font-size: 1.25rem; line-height: 1.75rem; font-weight: 700; color: var(--primary); }
    .of { font-weight: 400; color: var(--muted-foreground); }
    .big .of { font-size: 1.125rem; }
    .medium .of { font-size: 0.875rem; }
    .caption { margin: 0.25rem 0 0; font-size: 0.875rem; color: var(--muted-foreground); }
    .overline {
      margin: 0; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.025em;
      color: var(--muted-foreground);
    }
    .when { margin: 0.25rem 0 0; font-size: 0.875rem; font-weight: 500; }
    section h2 {
      margin: 0 0 0.5rem; font-size: 0.875rem; font-weight: 600; text-transform: uppercase;
      letter-spacing: 0.025em; color: var(--muted-foreground);
    }
    section .card { padding: 1.5rem; overflow-x: auto; }
    table { width: 100%; font-size: 0.875rem; border-collapse: collapse; }
    th { padding: 0.5rem 0.75rem; text-align: left; font-weight: 500; color: var(--muted-foreground); }
    td { padding: 0.5rem 0.75rem; color: var(--muted-foreground); }
    tr { border-bottom: 1px solid var(--border); }
    tbody tr:last-child { border-bottom: none; }
    td.hash { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; font-size: 0.75rem; color: inherit; }
    @media (max-width: 639px) {
      section .card { padding: 0.75rem 0.5rem; }
      th, td { padding: 0.5rem 0.25rem; }
    }
  </style>
  <div class="page">
    <header>
      <h1>Admin</h1>
      <p class="subtitle">FairNSquare administration</p>
    </header>

    <div class="card signin">
      <h2>Sign in</h2>
      <form>
        <div class="field">
          <label for="admin-password">Password</label>
          <input id="admin-password" type="password" placeholder="Admin password" autocomplete="current-password">
        </div>
        <p class="error" role="alert" hidden></p>
        <button class="primary" type="submit" disabled>Sign in</button>
      </form>
    </div>

    <div class="stats" hidden>
      <div class="toolbar"><button class="outline sign-out" type="button">Sign out</button></div>
      <div class="summary">
        <div class="card">
          <p class="big"><span class="total"></span> <span class="of">/ <span class="limit"></span></span></p>
          <p class="caption">Total splits</p>
        </div>
        <div class="card">
          <p class="medium"><span class="used"></span> <span class="of">/ <span class="max"></span></span></p>
          <p class="caption">Storage used</p>
        </div>
        <div class="card last">
          <p class="overline">Last updated</p>
          <p class="when last-updated"></p>
        </div>
      </div>
      <section aria-labelledby="oldest-created-title">
        <h2 id="oldest-created-title">Oldest created splits</h2>
        <div class="card"><table class="oldest-created">
          <thead><tr><th>ID hash</th><th>Created</th></tr></thead><tbody></tbody>
        </table></div>
      </section>
      <section aria-labelledby="oldest-updated-title">
        <h2 id="oldest-updated-title">Oldest updated splits</h2>
        <div class="card"><table class="oldest-updated">
          <thead><tr><th>ID hash</th><th>Last updated</th><th>Created</th></tr></thead><tbody></tbody>
        </table></div>
      </section>
      <section aria-labelledby="recently-updated-title">
        <h2 id="recently-updated-title">Most recently updated splits</h2>
        <div class="card"><table class="recently-updated">
          <thead><tr><th>ID hash</th><th>Last updated</th><th>Created</th></tr></thead><tbody></tbody>
        </table></div>
      </section>
    </div>
  </div>
`;

/**
 * The admin page (`/admin`, not linked from the app): sign in with the admin password, then the storage
 * statistics and the splits that were created or updated the longest and the shortest time ago.
 */
class AdminPage extends HTMLElement {
  constructor() {
    super();
    this.attachShadow({ mode: 'open' }).append(template.content.cloneNode(true));
    const $ = (selector) => this.shadowRoot.querySelector(selector);
    this.signIn = $('.signin');
    this.form = $('form');
    this.password = $('input');
    this.error = $('.error');
    this.submit = $('button[type="submit"]');
    this.stats = $('.stats');

    this.password.addEventListener('input', () => this.updateSubmit());
    this.form.addEventListener('submit', (event) => {
      event.preventDefault();
      const token = this.password.value.trim();
      if (token) this.load(token);
    });
    $('.sign-out').addEventListener('click', () => this.signOut());
  }

  connectedCallback() {
    const token = sessionStorage.getItem(SESSION_KEY);
    if (token) this.load(token);
  }

  updateSubmit() {
    this.submit.disabled = this.loading || !this.password.value.trim();
  }

  /** @param {string} token the admin password */
  async load(token) {
    this.loading = true;
    this.password.disabled = true;
    this.submit.textContent = 'Signing in…';
    this.updateSubmit();
    this.showError(null);
    try {
      const stats = await apiRequest('/admin/stats', { headers: { 'X-Admin-Token': token } });
      sessionStorage.setItem(SESSION_KEY, token);
      this.render(stats);
    } catch (error) {
      if (error.status === 401) {
        sessionStorage.removeItem(SESSION_KEY);
        this.showError('Invalid password.');
      } else if (error.status === 503) {
        this.showError('Admin access is not configured on this server.');
      } else {
        this.showError(error.detail || 'Failed to load admin stats.');
      }
    } finally {
      this.loading = false;
      this.password.disabled = false;
      this.submit.textContent = 'Sign in';
      this.updateSubmit();
    }
  }

  /** @param {string | null} message */
  showError(message) {
    this.error.textContent = message ?? '';
    this.error.hidden = !message;
  }

  signOut() {
    sessionStorage.removeItem(SESSION_KEY);
    this.password.value = '';
    this.updateSubmit();
    this.stats.hidden = true;
    this.signIn.hidden = false;
  }

  render(stats) {
    const $ = (selector) => this.shadowRoot.querySelector(selector);
    $('.total').textContent = stats.totalSplits;
    $('.limit').textContent = stats.splitCountLimit;
    $('.used').textContent = formatBytes(stats.usedStorageBytes);
    $('.max').textContent = formatBytes(stats.maxStorageBytes);
    $('.last-updated').textContent = formatDate(stats.lastUpdated);

    const byCreated = [...stats.splits].sort((a, b) => a.createdAt.localeCompare(b.createdAt));
    const byUpdated = [...stats.splits].sort((a, b) => a.updatedAt.localeCompare(b.updatedAt));
    fillTable($('.oldest-created tbody'), byCreated.slice(0, TABLE_ROWS), ['createdAt']);
    fillTable($('.oldest-updated tbody'), byUpdated.slice(0, TABLE_ROWS), ['updatedAt', 'createdAt']);
    fillTable($('.recently-updated tbody'), byUpdated.reverse().slice(0, TABLE_ROWS), ['updatedAt', 'createdAt']);

    this.signIn.hidden = true;
    this.stats.hidden = false;
  }
}

/**
 * @param {HTMLTableSectionElement} tbody
 * @param {Array<{idHash: string}>} splits
 * @param {string[]} dates the date fields to show after the id hash
 */
function fillTable(tbody, splits, dates) {
  tbody.replaceChildren(...splits.map((split) => {
    const row = document.createElement('tr');
    const hash = document.createElement('td');
    hash.className = 'hash';
    hash.textContent = split.idHash;
    row.append(hash, ...dates.map((field) => {
      const cell = document.createElement('td');
      cell.textContent = formatDate(split[field]);
      return cell;
    }));
    return row;
  }));
}

/** @param {string | null} iso */
function formatDate(iso) {
  return iso ? new Date(iso).toLocaleString() : '—';
}

/** @param {number} bytes */
function formatBytes(bytes) {
  if (bytes >= 1024 ** 3) return (bytes / 1024 ** 3).toFixed(2) + ' GB';
  if (bytes >= 1024 ** 2) return (bytes / 1024 ** 2).toFixed(2) + ' MB';
  if (bytes >= 1024) return (bytes / 1024).toFixed(2) + ' KB';
  return bytes + ' B';
}

customElements.define('fns-admin-page', AdminPage);
