import { apiRequest } from '../../infra/api.js';
import { navigate } from '../../infra/navigation.js';
import { showToast } from '../../infra/toasts.js';
import { saveLastSplit } from '../../infra/stores/last-splits.js';
import { getCachedSplitName, setCachedSplitName } from '../../infra/stores/split-names.js';
import { baseStyles, SPINNER } from '../../components/base-styles.js';
import { icon } from '../../components/icons.js';
import '../../components/split-header.js';
import '../../components/participant-summary.js';

/** Show the loading state only after this delay, so fast loads do not flash it. */
const LOADING_DELAY = 500;

const styles = new CSSStyleSheet();
styles.replaceSync(`
  .page { display: flex; flex-direction: column; align-items: center; gap: 1rem; width: 100%; max-width: 520px; margin: 0 auto; }
  .page > * { width: 100%; }
  .loading { display: flex; flex-direction: column; align-items: center; gap: 1rem; padding: 3rem 0; }
  .loading .spinner { width: 2rem; height: 2rem; color: var(--primary); }
  .loading p { margin: 0; color: var(--muted-foreground); }
  .failed { text-align: center; }
  .failed h2 { margin: 0; font-size: 1rem; font-weight: 600; color: var(--destructive); }
  .failed p { margin: 1.5rem 0 1rem; color: var(--muted-foreground); }
  .link {
    display: block; width: 100%; padding: 0; font: inherit; text-align: left; color: inherit;
    background: none; border: 0; border-radius: 0.75rem; cursor: pointer;
  }
  .link:focus-visible { outline: none; box-shadow: 0 0 0 3px color-mix(in oklch, var(--ring) 50%, transparent); }
  .link:hover fns-participant-summary { opacity: 0.9; }
  .tile {
    display: flex; align-items: center; justify-content: space-between; padding: 2.5rem 1.5rem;
    background: rgb(240 253 250 / 0.5); border-color: #99f6e4; transition: background-color 150ms;
  }
  .link:hover .tile { background: #f0fdfa; }
  .tile p { margin: 0; }
  .caption { font-size: 0.875rem; color: var(--muted-foreground); }
  .value { font-size: 1.125rem; line-height: 1.75rem; font-weight: 600; }
  .right { text-align: right; }
  .total { color: var(--primary); }
  .tile svg { width: 1.5rem; height: 1.5rem; color: var(--primary); }
`);

const template = document.createElement('template');
template.innerHTML = `
  <div class="page">
    <fns-split-header></fns-split-header>
    <div class="loading" hidden>${SPINNER}<p>Loading split...</p></div>
    <div class="card failed" hidden>
      <h2>Error loading split</h2>
      <p class="message"></p>
      <button class="button primary retry" type="button">Retry</button>
    </div>
    <section hidden>
      <button class="link" type="button" data-to="/expenses" aria-label="View all expenses">
        <div class="card tile">
          <div><p class="caption">Expenses</p><p class="value expense-count"></p></div>
          <div class="right"><p class="caption">Total</p><p class="value total"></p></div>
        </div>
      </button>
    </section>
    <section hidden>
      <button class="link" type="button" data-to="/participants" aria-label="View all participants">
        <fns-participant-summary></fns-participant-summary>
      </button>
    </section>
    <section hidden>
      <button class="link" type="button" data-to="/settlement" aria-label="View settlement">
        <div class="card tile">
          <div><p class="caption">Settlement</p><p class="value settlement"></p></div>
          ${icon('chevron-right')}
        </div>
      </button>
    </section>
  </div>
`;

const euros = new Intl.NumberFormat('en-IE', { style: 'currency', currency: 'EUR' });

/**
 * The split dashboard (`/splits/<id>`): the expenses, the participants and the settlement at a glance, each
 * opening its page. Attribute: `split-id`.
 */
class SplitPage extends HTMLElement {
  static observedAttributes = ['split-id'];

  constructor() {
    super();
    const root = this.attachShadow({ mode: 'open' });
    root.adoptedStyleSheets = [baseStyles, styles];
    root.append(template.content.cloneNode(true));
    const $ = (selector) => root.querySelector(selector);
    this.header = $('fns-split-header');
    this.loading = $('.loading');
    this.failed = $('.failed');
    this.sections = [...root.querySelectorAll('section')];
    this.summary = $('fns-participant-summary');
    this.$ = $;

    $('.retry').addEventListener('click', () => this.load());
    for (const link of root.querySelectorAll('.link')) {
      link.addEventListener('click', () => navigate(`/splits/${this.splitId}${link.dataset.to}`));
    }
  }

  get splitId() {
    return this.getAttribute('split-id') ?? '';
  }

  // The split id can arrive before or after the element is in the page: load once per id.
  attributeChangedCallback() {
    if (this.isConnected) this.loadIfNew();
  }

  connectedCallback() {
    this.loadIfNew();
  }

  loadIfNew() {
    if (this.splitId && this.splitId !== this.loadedId) this.load();
  }

  async load() {
    const id = this.splitId;
    if (!id) return;
    this.loadedId = id;
    this.header.setAttribute('split-id', id);
    this.header.setAttribute('split-name', getCachedSplitName(id));
    this.failed.hidden = true;
    for (const section of this.sections) section.hidden = true;
    const timer = setTimeout(() => { this.loading.hidden = false; }, LOADING_DELAY);
    try {
      const split = await apiRequest(`/splits/${encodeURIComponent(id)}`);
      if (id !== this.splitId) return; // another split was asked for meanwhile
      setCachedSplitName(id, split.name);
      saveLastSplit({ id: split.id, name: split.name });
      this.render(split);
    } catch (error) {
      if (error.status === 404 || error.status === 400) {
        showToast({ type: 'info', message: 'Split not found — create a new one.' });
        navigate('/');
        return;
      }
      this.$('.failed .message').textContent = error.detail || 'Failed to load split. Please try again.';
      this.failed.hidden = false;
    } finally {
      clearTimeout(timer);
      this.loading.hidden = true;
    }
  }

  render(split) {
    const expenses = split.expenses.length;
    const total = split.expenses.reduce((sum, expense) => sum + expense.amount, 0);
    this.header.setAttribute('split-name', split.name);
    this.$('.expense-count').textContent = `${expenses} ${expenses === 1 ? 'expense' : 'expenses'}`;
    this.$('.total').textContent = euros.format(total);
    this.summary.participants = split.participants;
    this.$('.settlement').textContent = split.settlement != null ? 'Settled' : 'Solve';

    const [expensesSection, participantsSection, settlementSection] = this.sections;
    expensesSection.hidden = false;
    participantsSection.hidden = false;
    settlementSection.hidden = !(expenses > 0 && split.participants.length > 0);
  }
}

customElements.define('fns-split-page', SplitPage);
