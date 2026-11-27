import { showToast } from '../../infra/toasts.js';
import { apiRequest } from '../../infra/api.js';
import { baseStyles } from '../../components/base-styles.js';

/** The admin token stays for the browser tab only. Not used when admins sign in with the OIDC provider. */
const SESSION_KEY = 'admin_token';
const TABLE_ROWS = 5;

const styles = new CSSStyleSheet();
styles.replaceSync(`
    .page {
      display: flex; flex-direction: column; align-items: center; gap: 1.5rem;
      width: 100%; max-width: 900px; margin: 0 auto; padding: 2rem 1rem; box-sizing: border-box;
    }
    header { text-align: center; }
    h1 { margin: 0; font-size: 1.5rem; line-height: 2rem; font-weight: 700; color: var(--primary); }
    .subtitle { margin: 0.25rem 0 0; font-size: 0.875rem; color: var(--muted-foreground); }
    .signin { max-width: 400px; }
    .signin h2 { margin: 0 0 1.5rem; font-size: 1rem; line-height: 1; font-weight: 600; }
    form { display: flex; flex-direction: column; gap: 1rem; }
    .signin .error:not([hidden]) { margin: 0 0 1rem; }
    a.button { text-decoration: none; }
    .stats { display: flex; flex-direction: column; gap: 1.5rem; width: 100%; }
    .toolbar { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 0.5rem; }
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
    .backups .card { display: flex; flex-direction: column; gap: 1rem; }
    .backups-head { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 0.5rem; }
    .backups-status { margin: 0; font-size: 0.875rem; color: var(--muted-foreground); }
    .backups p.error { margin: 0; }
    .backups td:last-child { text-align: right; }
    .comparison { display: flex; flex-direction: column; gap: 0.75rem; }
    .comparison h3 { margin: 0; font-size: 0.875rem; font-weight: 600; }
    .comparison dl { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0.5rem; margin: 0; }
    @media (min-width: 640px) { .comparison dl { grid-template-columns: repeat(4, minmax(0, 1fr)); } }
    .comparison dl div {
      padding: 0.75rem; text-align: center; border: 1px solid var(--border); border-radius: calc(var(--radius) - 2px);
    }
    .comparison dt { font-size: 0.75rem; color: var(--muted-foreground); }
    .comparison dd { margin: 0.25rem 0 0; font-size: 1.25rem; font-weight: 700; color: var(--primary); }
    .comparison .caption { margin: 0; }
    td.hash { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; font-size: 0.75rem; color: inherit; }
    @media (max-width: 639px) {
      section .card { padding: 0.75rem 0.5rem; }
      th, td { padding: 0.5rem 0.25rem; }
    }
`);

const template = document.createElement('template');
template.innerHTML = `
  <div class="page">
    <header>
      <h1>Admin</h1>
      <p class="subtitle">FairNSquare administration</p>
    </header>

    <div class="card signin" hidden>
      <h2>Sign in</h2>
      <p class="error" role="alert" hidden></p>
      <form hidden>
        <div class="field">
          <label for="admin-password">Password</label>
          <input id="admin-password" type="password" placeholder="Admin password" autocomplete="current-password">
        </div>
        <button class="button primary wide" type="submit" disabled>Sign in</button>
      </form>
      <a class="button primary wide provider-sign-in" href="/api/admin/session/login" hidden></a>
    </div>

    <div class="stats" hidden>
      <div class="toolbar">
        <button class="button outline module-report" type="button">Module report ↗</button>
        <button class="button outline sign-out" type="button">Sign out</button>
      </div>
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
      <section class="backups" aria-labelledby="backups-title">
        <h2 id="backups-title">Backups</h2>
        <div class="card">
          <div class="backups-head">
            <p class="backups-status">Loading…</p>
            <button class="button outline back-up" type="button" hidden>Back up now</button>
          </div>
          <p class="error backups-problem" role="alert" hidden></p>
          <table class="archives" hidden>
            <thead><tr><th>Archive</th><th>Size</th><th><span hidden>Actions</span></th></tr></thead><tbody></tbody>
          </table>
          <div class="comparison" role="status" hidden>
            <h3></h3>
            <dl>
              <div><dt>New</dt><dd class="added"></dd></div>
              <div><dt>Deleted</dt><dd class="deleted"></dd></div>
              <div><dt>Changed</dt><dd class="changed"></dd></div>
              <div><dt>Unchanged</dt><dd class="unchanged"></dd></div>
            </dl>
            <p class="caption"></p>
          </div>
        </div>
      </section>
    </div>
  </div>
`;

/**
 * The admin page (`/admin`, not linked from the app): sign in with the OIDC provider when the server has one, else
 * with the admin password; then the storage statistics, the splits that were created or updated the longest and the
 * shortest time ago, and the backups: the archives in the S3 bucket, a backup on demand, and the comparison of an
 * archive with the current splits.
 */
class AdminPage extends HTMLElement {
  constructor() {
    super();
    const root = this.attachShadow({ mode: 'open' });
    root.adoptedStyleSheets = [baseStyles, styles];
    root.append(template.content.cloneNode(true));
    const $ = (selector) => this.shadowRoot.querySelector(selector);
    this.signIn = $('.signin');
    this.form = $('form');
    this.password = $('input');
    this.error = $('.error');
    this.submit = $('button[type="submit"]');
    this.stats = $('.stats');
    this.providerSignIn = $('.provider-sign-in');
    /** 'OIDC': the provider signs admins in, with a session cookie; 'PASSWORD': the admin password, as a header. */
    this.method = 'PASSWORD';

    this.password.addEventListener('input', () => this.updateSubmit());
    this.form.addEventListener('submit', (event) => {
      event.preventDefault();
      const token = this.password.value.trim();
      if (token) this.load(token);
    });
    $('.sign-out').addEventListener('click', () => this.signOut());
    $('.module-report').addEventListener('click', () => this.openModuleReport());
    this.backUpButton = $('.back-up');
    this.backUpButton.addEventListener('click', () => this.backUp());
  }

  async connectedCallback() {
    try {
      const { method, provider } = await apiRequest('/admin/session');
      this.method = method;
      if (method === 'OIDC') this.providerSignIn.textContent = `Sign in with ${provider}`;
    } catch {
      this.method = 'PASSWORD';
    }
    this.form.hidden = this.method !== 'PASSWORD';
    this.providerSignIn.hidden = this.method !== 'OIDC';
    if (this.method === 'OIDC') {
      this.load(null);
      return;
    }
    const token = sessionStorage.getItem(SESSION_KEY);
    if (token) this.load(token);
    else this.signIn.hidden = false;
  }

  /** The headers that prove the request comes from an admin. */
  adminHeaders(token = sessionStorage.getItem(SESSION_KEY)) {
    // With the provider, the session cookie does; the header makes an expired session answer 499, not a redirect.
    return this.method === 'OIDC' ? { 'X-Requested-With': 'JavaScript' } : { 'X-Admin-Token': token ?? '' };
  }

  updateSubmit() {
    this.submit.disabled = this.loading || !this.password.value.trim();
  }

  /** @param {string | null} token the admin password; null with the provider */
  async load(token) {
    this.loading = true;
    this.password.disabled = true;
    this.submit.textContent = 'Signing in…';
    this.updateSubmit();
    this.showError(null);
    try {
      const stats = await apiRequest('/admin/stats', { headers: this.adminHeaders(token) });
      if (this.method === 'PASSWORD') sessionStorage.setItem(SESSION_KEY, token);
      this.render(stats);
    } catch (error) {
      this.signIn.hidden = false;
      if (this.method === 'OIDC') {
        this.providerFailed(error);
      } else if (error.status === 401) {
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

  /**
   * With the provider, not signed in (401) or the session expired (499) just shows the sign-in button. A user who isn't
   * an admin (403) is signed out of the admin pages, so that another account can sign in.
   */
  async providerFailed(error) {
    if (error.status === 401 || error.status === 499) return;
    if (error.status === 403) {
      this.showError('This account is not an admin.');
      await this.endSession();
    } else {
      this.showError(error.detail || 'Failed to load admin stats.');
    }
  }

  /**
   * Opens modular's module report in a new tab. With the password, the endpoint needs the admin token header, which a
   * link can't send: the page fetches the report, then shows it in the tab, in a sandboxed frame, so its scripts can't
   * reach this origin and the token stored in it. The tab opens on the click, before the fetch, or browsers would block it.
   */
  async openModuleReport() {
    const tab = window.open('', '_blank');
    try {
      const response = await fetch('/api/admin/module-report', {
        headers: this.adminHeaders(),
      });
      if (!response.ok) throw new Error(`status ${response.status}`);
      const report = await response.text();
      const frame = `<!doctype html><html lang="en"><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Module report · FairNSquare</title>
        <style>html, body { margin: 0; height: 100%; } iframe { display: block; width: 100%; height: 100%; border: 0; }</style>
        </head><body><iframe sandbox="allow-scripts" title="Module report" srcdoc="${escapeAttribute(report)}"></iframe></body></html>`;
      const url = URL.createObjectURL(new Blob([frame], { type: 'text/html' }));
      if (tab) tab.location.href = url;
      setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch {
      tab?.close();
      showToast({ type: 'error', message: 'Failed to load the module report.' });
    }
  }

  /** @param {string | null} message */
  showError(message) {
    this.error.textContent = message ?? '';
    this.error.hidden = !message;
  }

  /** Ends the provider's session on the admin pages; the user stays signed in at the provider. */
  async endSession() {
    try {
      await apiRequest('/admin/session/logout', { method: 'POST' });
    } catch {
      // The session cookie expires with the session anyway.
    }
  }

  async signOut() {
    if (this.method === 'OIDC') await this.endSession();
    sessionStorage.removeItem(SESSION_KEY);
    this.password.value = '';
    this.updateSubmit();
    this.stats.hidden = true;
    this.shadowRoot.querySelector('.comparison').hidden = true;
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
    this.loadBackups();
  }

  /** Shows whether backups are on, and the archives. */
  async loadBackups() {
    const $ = (selector) => this.shadowRoot.querySelector(selector);
    const status = $('.backups-status');
    const problem = $('.backups-problem');
    const table = $('.archives');
    try {
      const backups = await apiRequest('/admin/backups', { headers: this.adminHeaders() });
      this.backUpButton.hidden = !backups.enabled;
      problem.textContent = backups.problem ?? '';
      problem.hidden = !backups.problem;
      if (!backups.enabled) {
        status.textContent = 'Backups are off: no S3 bucket is configured.';
      } else if (backups.problem) {
        status.textContent = 'Backups are on, but:';
      } else {
        const count = backups.archives.length;
        status.textContent = count === 0 ? 'No archive yet.' : `${count} archive${count === 1 ? '' : 's'} in the bucket.`;
      }
      table.hidden = backups.archives.length === 0;
      fillArchives($('.archives tbody'), backups.archives, (archive) => this.compare(archive));
    } catch (error) {
      status.textContent = '';
      problem.textContent = error.detail || 'Failed to load the backups.';
      problem.hidden = false;
    }
  }

  /** Makes an archive now. */
  async backUp() {
    this.backUpButton.disabled = true;
    this.backUpButton.textContent = 'Backing up…';
    try {
      const archive = await apiRequest('/admin/backups', { method: 'POST', headers: this.adminHeaders() });
      showToast({ type: 'success', message: 'Backup saved', description: formatDate(archive.createdAt) });
      await this.loadBackups();
    } catch (error) {
      showToast({ type: 'error', message: error.detail || 'The backup failed.' });
    } finally {
      this.backUpButton.disabled = false;
      this.backUpButton.textContent = 'Back up now';
    }
  }

  /** Shows how the current splits differ from an archive's. */
  async compare(archive) {
    const $ = (selector) => this.shadowRoot.querySelector(selector);
    try {
      const comparison = await apiRequest(`/admin/backups/${encodeURIComponent(archive.name)}/comparison`, {
        headers: this.adminHeaders(),
      });
      $('.comparison h3').textContent = `Since the archive of ${formatDate(archive.createdAt)}`;
      for (const total of ['added', 'deleted', 'changed', 'unchanged']) {
        $(`.comparison .${total}`).textContent = comparison[total];
      }
      $('.comparison .caption').textContent =
        `${splits(comparison.archived)} in the archive, ${comparison.current} now.`;
      $('.comparison').hidden = false;
    } catch (error) {
      showToast({ type: 'error', message: error.detail || 'Failed to compare with the archive.' });
    }
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

/**
 * @param {HTMLTableSectionElement} tbody
 * @param {Array<{name: string, createdAt: string, sizeBytes: number}>} archives newest first
 * @param {(archive: object) => void} onCompare
 */
function fillArchives(tbody, archives, onCompare) {
  tbody.replaceChildren(...archives.map((archive) => {
    const row = document.createElement('tr');
    const date = document.createElement('td');
    date.textContent = formatDate(archive.createdAt);
    const size = document.createElement('td');
    size.textContent = formatBytes(archive.sizeBytes);
    const action = document.createElement('td');
    const compare = document.createElement('button');
    compare.type = 'button';
    compare.className = 'button outline small';
    compare.textContent = 'Compare';
    compare.setAttribute('aria-label', `Compare with the archive of ${date.textContent}`);
    compare.addEventListener('click', () => onCompare(archive));
    action.append(compare);
    row.append(date, size, action);
    return row;
  }));
}

/** @param {number} count */
function splits(count) {
  return `${count} split${count === 1 ? '' : 's'}`;
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

/** @param {string} text */
function escapeAttribute(text) {
  return text.replaceAll('&', '&amp;').replaceAll('"', '&quot;');
}
