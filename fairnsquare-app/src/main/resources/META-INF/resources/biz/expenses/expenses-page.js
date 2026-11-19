import { apiRequest } from '../../infra/api.js';
import { navigate } from '../../infra/navigation.js';
import { showToast } from '../../infra/toasts.js';
import { getCachedSplitName, setCachedSplitName } from '../../infra/stores/split-names.js';
import { baseStyles, SPINNER } from '../../components/base-styles.js';
import { icon } from '../../components/icons.js';
import '../../components/split-header.js';
import '../../components/expense-dialog.js';
import '../../components/confirm-dialog.js';

const LOADING_DELAY = 500;
const MODE_LABELS = { BY_NIGHT: '🌙 By Night', EQUAL: '⊜ Equal', BY_SHARE: '👥 By Members', FREE: '✏️ Manual' };
const euros = new Intl.NumberFormat('en-IE', { style: 'currency', currency: 'EUR' });

const styles = new CSSStyleSheet();
styles.replaceSync(`
  .page { display: flex; flex-direction: column; align-items: center; gap: 1rem; width: 100%; max-width: 520px; margin: 0 auto; }
  .page > * { width: 100%; }
  .loading { display: flex; flex-direction: column; align-items: center; gap: 1rem; padding: 3rem 0; }
  .loading .spinner { width: 2rem; height: 2rem; color: var(--primary); }
  .loading p { margin: 0; color: var(--muted-foreground); }
  .content { display: flex; flex-direction: column; gap: 1rem; }
  .settled {
    display: flex; align-items: center; gap: 0.5rem; padding: 0.5rem 0.75rem; font-size: 0.875rem;
    color: #92400e; background: #fffbeb; border: 1px solid #fde68a; border-radius: calc(var(--radius) - 2px);
  }
  .settled span { flex: 1; }
  .settled button {
    padding: 0; font: inherit; font-weight: 500; white-space: nowrap; color: inherit;
    background: none; border: 0; text-decoration: underline; cursor: pointer;
  }
  .filters { display: flex; align-items: center; gap: 0.5rem; }
  .filters > svg { flex: none; width: 1rem; height: 1rem; color: var(--muted-foreground); }
  .filters select {
    flex: 1; min-width: 0; min-height: 36px; height: 2.25rem; padding: 0 0.75rem; font-size: 0.875rem;
  }
  .filters .button { min-height: 36px; min-width: 36px; padding: 0.25rem; }
  .summary {
    display: flex; align-items: center; justify-content: space-between; padding: 0.75rem 1rem;
    background: rgb(240 253 250 / 0.5); border: 1px solid #99f6e4; border-radius: var(--radius);
  }
  .summary .count { font-size: 0.875rem; font-weight: 500; }
  .summary .total { font-size: 0.875rem; font-weight: 600; color: var(--primary); }
  .empty { display: flex; flex-direction: column; align-items: center; gap: 0.75rem; padding: 3rem 0; text-align: center; }
  .empty svg { width: 3rem; height: 3rem; color: color-mix(in oklch, var(--muted-foreground) 50%, transparent); }
  .empty h2 { margin: 0; font-size: 1.125rem; font-weight: 600; color: var(--muted-foreground); }
  .empty p { margin: 0; font-size: 0.875rem; color: var(--muted-foreground); }
  ul { display: flex; flex-direction: column; gap: 0.75rem; margin: 0; padding: 0; list-style: none; }
  .expense { padding: 2.25rem 1rem; }
  .expense p { margin: 0; }
  .row { display: flex; justify-content: space-between; gap: 0.5rem; }
  .row.first { align-items: flex-start; margin-bottom: 0.25rem; }
  .row.second { align-items: center; margin-bottom: 0.5rem; }
  .description { min-width: 0; font-size: 0.875rem; font-weight: 700; overflow-wrap: anywhere; }
  .amount { flex: none; font-size: 0.875rem; font-weight: 600; color: var(--primary); }
  .small { font-size: 0.75rem; color: var(--muted-foreground); }
  .mode { margin-bottom: 0.25rem; }
  .shares { margin-bottom: 0.5rem; line-height: 1.625; }
  .actions { display: flex; justify-content: flex-end; gap: 0.25rem; }
  .actions .button svg { width: 1rem; height: 1rem; }
  .actions .delete { color: var(--destructive); }
`);

const template = document.createElement('template');
template.innerHTML = `
  <div class="page">
    <fns-split-header></fns-split-header>
    <div class="loading" hidden>${SPINNER}<p>Loading expenses...</p></div>
    <div class="content" hidden>
      <div class="settled" hidden>
        <span>This split is settled — expenses are read-only.</span>
        <button type="button" class="to-settlement">View settlement</button>
      </div>
      <section class="filters" aria-label="Expense filters" hidden>
        ${icon('list-filter')}
        <select class="payer" aria-label="Filter by payer"></select>
        <select class="beneficiary" aria-label="Filter by beneficiary"></select>
        <button type="button" class="button ghost clear" aria-label="Clear filters" hidden>${icon('x')}</button>
      </section>
      <section class="summary"><span class="count"></span><span class="total"></span></section>
      <button type="button" class="button outline wide add">${icon('plus')} Add Expense</button>
      <div class="empty none" hidden>
        ${icon('receipt')}
        <h2>No expenses yet</h2>
        <p>Tap + to add your first expense</p>
      </div>
      <div class="empty unmatched" hidden>
        ${icon('list-filter')}
        <h2>No matching expenses</h2>
        <p>Try adjusting your filters</p>
        <button type="button" class="button outline small clear">Clear filters</button>
      </div>
      <ul></ul>
    </div>
  </div>
  <fns-expense-dialog></fns-expense-dialog>
  <fns-confirm-dialog></fns-confirm-dialog>
`;

/**
 * The expenses of a split (`/splits/<id>/expenses`): filters by payer and beneficiary (kept in the address,
 * `?payer=…&beneficiary=…`), total, and the expenses, newest first, each editable or deletable until the split is
 * settled. Attribute: `split-id`.
 */
class ExpensesPage extends HTMLElement {
  static observedAttributes = ['split-id'];

  constructor() {
    super();
    const root = this.attachShadow({ mode: 'open' });
    root.adoptedStyleSheets = [baseStyles, styles];
    root.append(template.content.cloneNode(true));
    const $ = (selector) => root.querySelector(selector);
    this.$ = $;
    this.header = $('fns-split-header');
    this.loading = $('.loading');
    this.content = $('.content');
    this.payerFilter = $('.payer');
    this.beneficiaryFilter = $('.beneficiary');
    this.list = $('ul');
    this.dialog = $('fns-expense-dialog');
    this.confirm = $('fns-confirm-dialog');

    this.payerFilter.addEventListener('change', () => this.filtersChanged());
    this.beneficiaryFilter.addEventListener('change', () => this.filtersChanged());
    for (const button of root.querySelectorAll('.clear')) {
      button.addEventListener('click', () => {
        this.payerFilter.value = '';
        this.beneficiaryFilter.value = '';
        this.filtersChanged();
      });
    }
    $('.to-settlement').addEventListener('click', () => navigate(`/splits/${this.splitId}/settlement`));
    $('.add').addEventListener('click', () => this.dialog.openAdd({
      splitId: this.splitId, participants: this.split.participants,
    }));
    this.dialog.addEventListener('expensechange', () => this.load());
  }

  get splitId() {
    return this.getAttribute('split-id') ?? '';
  }

  attributeChangedCallback() {
    if (this.isConnected) this.loadIfNew();
  }

  connectedCallback() {
    this.loadIfNew();
  }

  // The split id can arrive before or after the element is in the page: load once per id.
  loadIfNew() {
    if (this.splitId && this.splitId !== this.loadedId) {
      this.split = null;
      this.payerFilter.replaceChildren(); // read the filters from the address again
      this.beneficiaryFilter.replaceChildren();
      this.load();
    }
  }

  async load() {
    const id = this.splitId;
    if (!id) return;
    this.loadedId = id;
    this.header.setAttribute('split-id', id);
    this.header.setAttribute('split-name', getCachedSplitName(id));
    // On a first load only: a reload after a change keeps the list on screen.
    const timer = this.split ? null : setTimeout(() => { this.loading.hidden = false; }, LOADING_DELAY);
    try {
      const split = await apiRequest(`/splits/${encodeURIComponent(id)}`);
      if (id !== this.splitId) return;
      setCachedSplitName(id, split.name);
      this.split = split;
      this.render();
    } catch (error) {
      if (error.status === 404 || error.status === 400) {
        showToast({ type: 'info', message: 'Split not found — create a new one.' });
        navigate('/');
        return;
      }
      showToast({ type: 'error', message: error.detail || 'Failed to load expenses' });
    } finally {
      clearTimeout(timer);
      this.loading.hidden = true;
    }
  }

  render() {
    const split = this.split;
    const settled = split.settlement != null;
    this.header.setAttribute('split-name', split.name);
    this.$('.settled').hidden = !settled;
    this.$('.add').hidden = settled;

    const filters = this.$('.filters');
    filters.hidden = split.participants.length === 0;
    const params = new URLSearchParams(location.search);
    const wantedPayer = this.payerFilter.options.length ? this.payerFilter.value : params.get('payer') ?? '';
    const wantedBeneficiary = this.beneficiaryFilter.options.length
      ? this.beneficiaryFilter.value : params.get('beneficiary') ?? '';
    fillFilter(this.payerFilter, 'All payers', split.participants, wantedPayer);
    fillFilter(this.beneficiaryFilter, 'All beneficiaries', split.participants, wantedBeneficiary);

    this.content.hidden = false;
    this.renderExpenses();
  }

  filtersChanged() {
    const url = new URL(location.href);
    for (const [name, select] of [['payer', this.payerFilter], ['beneficiary', this.beneficiaryFilter]]) {
      if (select.value) url.searchParams.set(name, select.value);
      else url.searchParams.delete(name);
    }
    history.replaceState(history.state, '', url);
    this.renderExpenses();
  }

  renderExpenses() {
    const split = this.split;
    const payer = this.payerFilter.value;
    const beneficiary = this.beneficiaryFilter.value;
    const filtering = !!payer || !!beneficiary;
    const all = [...split.expenses].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    const shown = all.filter((expense) => {
      if (payer && expense.payerId !== payer) return false;
      if (beneficiary) {
        const shares = expense.splitMode === 'FREE'
          ? expense.shares.filter((share) => share.parts != null && share.parts > 0)
          : expense.shares;
        if (!shares.some((share) => share.participantId === beneficiary)) return false;
      }
      return true;
    });

    const plural = (n) => (n === 1 ? 'expense' : 'expenses');
    this.$('.summary .count').textContent = filtering
      ? `${shown.length} of ${all.length} ${plural(all.length)}`
      : `${shown.length} total ${plural(shown.length)}`;
    this.$('.summary .total').textContent = `${euros.format(shown.reduce((sum, e) => sum + e.amount, 0))} total`;
    this.$('.filters .clear').hidden = !filtering;
    this.$('.empty.none').hidden = all.length > 0;
    this.$('.empty.unmatched').hidden = !(all.length > 0 && shown.length === 0);
    this.list.replaceChildren(...shown.map((expense) => this.renderExpense(expense)));
  }

  renderExpense(expense) {
    const split = this.split;
    const nameOf = (id) => split.participants.find((p) => p.id === id)?.name || 'Unknown';
    const item = document.createElement('li');
    item.className = 'card expense';
    item.innerHTML = `
      <div class="row first"><span class="description"></span><span class="amount"></span></div>
      <div class="row second"><span class="small payer"></span><span class="small date"></span></div>
      <p class="small mode"></p>
      <p class="small shares"></p>
      <div class="actions">
        <button type="button" class="button ghost icon edit">${icon('pencil')}</button>
        <button type="button" class="button ghost icon delete">${icon('trash-2')}</button>
      </div>`;
    const $ = (selector) => item.querySelector(selector);
    $('.description').textContent = expense.description;
    $('.amount').textContent = euros.format(expense.amount);
    $('.payer').textContent = `Paid by ${nameOf(expense.payerId)}`;
    $('.date').textContent = formatDate(expense.createdAt);
    $('.mode').textContent = MODE_LABELS[expense.splitMode] ?? expense.splitMode;
    const shares = expense.shares.filter((share) => share.amount > 0)
      .map((share) => `${nameOf(share.participantId)}: ${euros.format(share.amount)}`).join(' · ');
    $('.shares').textContent = shares;
    $('.shares').hidden = !shares;

    if (split.settlement != null) {
      $('.actions').remove();
    } else {
      const edit = $('.edit');
      edit.setAttribute('aria-label', `Edit expense: ${expense.description}`);
      edit.addEventListener('click', () => this.dialog.openEdit({
        splitId: this.splitId, participants: split.participants, expense,
      }));
      const remove = $('.delete');
      remove.setAttribute('aria-label', `Delete expense: ${expense.description}`);
      remove.addEventListener('click', () => this.deleteExpense(expense, nameOf(expense.payerId)));
    }
    return item;
  }

  async deleteExpense(expense, payerName) {
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
          type: 'success',
          message: 'Expense deleted',
          description: `${expense.description} · €${expense.amount.toFixed(2)} · Paid by ${payerName}`,
        });
      },
    });
    if (deleted) await this.load();
  }
}

/** Fills a filter select with "All …" and the participants, keeping the wanted value if it still exists. */
function fillFilter(select, allLabel, participants, wanted) {
  select.replaceChildren(new Option(allLabel, ''), ...participants.map((p) => new Option(p.name, p.id)));
  select.value = participants.some((p) => p.id === wanted) ? wanted : '';
}

/** "Just now", "3h ago", "Yesterday", then "12 Mar". */
function formatDate(iso) {
  const date = new Date(iso);
  const diffMs = Date.now() - date.getTime();
  const hours = diffMs / 3600000;
  if (diffMs >= 0) {
    if (hours < 1) return 'Just now';
    if (hours < 24) return `${Math.floor(hours)}h ago`;
    if (hours < 48) return 'Yesterday';
  }
  return date.toLocaleDateString('en-IE', { month: 'short', day: 'numeric' });
}

customElements.define('fns-expenses-page', ExpensesPage);
