import { apiRequest } from '../../infra/api.js';
import { navigate } from '../../infra/navigation.js';
import { showToast } from '../../infra/toasts.js';
import { getCachedSplitName, setCachedSplitName } from '../../infra/stores/split-names.js';
import { loadNightsDefault, saveNightsDefault } from '../../infra/stores/nights-default.js';
import { baseStyles, SPINNER } from '../../components/base-styles.js';
import { icon, solidIcon } from '../../components/icons.js';
import { FIELD_INFO } from '../../components/info-dialog.js';
import '../../components/split-header.js';
import '../../components/participant-summary.js';
import '../../components/confirm-dialog.js';
import '../../components/expense-dialog.js';
import './participant-dialog.js';
import './participant-expenses-dialog.js';

const LOADING_DELAY = 500;
const HIGHLIGHT_TIME = 2000;
const MAX_NAME = 50;
const euros = new Intl.NumberFormat('en-IE', { style: 'currency', currency: 'EUR' });

const styles = new CSSStyleSheet();
styles.replaceSync(`
  .page { display: flex; flex-direction: column; align-items: center; gap: 1rem; width: 100%; max-width: 520px; margin: 0 auto; }
  .page > *, .content > * { width: 100%; }
  .loading { display: flex; flex-direction: column; align-items: center; gap: 1rem; padding: 3rem 0; }
  .loading .spinner { width: 2rem; height: 2rem; color: var(--primary); }
  .loading p { margin: 0; color: var(--muted-foreground); }
  .content { display: flex; flex-direction: column; gap: 1rem; }
  .settled {
    box-sizing: border-box; display: flex; align-items: center; gap: 0.5rem; padding: 0.5rem 0.75rem; font-size: 0.875rem;
    color: #92400e; background: #fffbeb; border: 1px solid #fde68a; border-radius: calc(var(--radius) - 2px);
  }
  .settled span { flex: 1; }
  .settled button {
    padding: 0; font: inherit; font-weight: 500; white-space: nowrap; color: inherit;
    background: none; border: 0; text-decoration: underline; cursor: pointer;
  }
  .add-buttons { display: flex; flex-direction: column; gap: 0.5rem; }
  .add-buttons .second { display: flex; gap: 0.5rem; }
  .add-buttons .button.outline { flex: 1; }
  .add-buttons .help { flex: none; min-width: 44px; padding: 0.5rem; color: var(--muted-foreground); }
  .add-buttons .help svg { width: 1.25rem; height: 1.25rem; }
  @media (min-width: 640px) {
    .add-buttons { flex-direction: row; }
    .add-buttons .second { display: contents; }
  }
  .add-form { padding: 2.5rem 1.5rem; }
  .add-form form { display: flex; flex-direction: column; gap: 0.75rem; }
  .add-form .kind { margin: 0; font-size: 0.875rem; font-weight: 500; color: #0f766e; }
  .pair { display: flex; gap: 0.75rem; }
  .pair .field { flex: 1; min-width: 0; }
  .label-row { display: flex; align-items: center; gap: 0.25rem; }
  .info {
    display: inline-flex; padding: 0.125rem; color: var(--muted-foreground); background: none; border: 0;
    border-radius: 9999px; cursor: pointer;
  }
  .info:hover { background: var(--muted); }
  .info svg { width: 0.875rem; height: 0.875rem; }
  .form-buttons { display: flex; gap: 0.5rem; }
  .form-buttons .button { flex: 1; }
  .empty {
    box-sizing: border-box; padding: 2rem 1.5rem; text-align: center;
    background: #f0fdfa; border: 1px dashed #5eead4; border-radius: var(--radius);
  }
  .empty p { margin: 0; }
  .empty .title { font-weight: 600; color: #115e59; }
  .empty .text { margin-top: 0.5rem; font-size: 0.875rem; color: #0f766e; }
  ul { display: flex; flex-direction: column; gap: 1rem; margin: 0; padding: 0; list-style: none; }
  .participant { padding: 0; overflow: hidden; }
  .participant .inner { display: flex; flex-direction: column; gap: 0.25rem; padding: 2.5rem 1.5rem; transition: background-color 500ms; }
  .participant.highlight .inner { background: #f0fdfa; }
  .top { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; }
  .name {
    flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    font-size: 1.125rem; line-height: 1.75rem; font-weight: 600;
  }
  .actions { display: flex; flex: none; align-items: center; gap: 0.25rem; }
  .actions .button { padding: 0.5rem; }
  .actions .add-expense { color: var(--primary); }
  .actions .add-expense:hover { color: var(--primary); background: color-mix(in oklch, var(--primary) 10%, transparent); }
  .actions .delete { color: var(--destructive); }
  .actions .delete:hover { color: var(--destructive); background: color-mix(in oklch, var(--destructive) 10%, transparent); }
  .badges { display: flex; flex-wrap: wrap; align-items: center; gap: 0.25rem; }
  .badge { display: inline-flex; align-items: center; gap: 0.25rem; padding: 0.125rem 0.5rem; font-size: 0.75rem; line-height: 1rem; border-radius: 9999px; }
  .badge svg { flex: none; width: 0.75rem; height: 0.75rem; }
  .badge.nights { color: #0f766e; background: #ccfbf1; }
  .badge.shares { color: #1d4ed8; background: #dbeafe; }
  .badge.count { color: #475569; background: #f1f5f9; }
  .badge.partners { color: #6d28d9; background: #ede9fe; }
  .stats { display: flex; align-items: center; justify-content: space-between; }
  .stat { display: flex; align-items: center; gap: 0.25rem; color: var(--muted-foreground); }
  .stat svg { flex: none; width: 1rem; height: 1rem; }
  .stat span { font-weight: 500; color: var(--foreground); }
  .stat.up, .stat.up span { color: #16a34a; }
  .stat.down, .stat.down span { color: #dc2626; }
  .stat.even span { color: var(--muted-foreground); }
  .help-body { display: flex; flex-direction: column; gap: 1rem; font-size: 0.875rem; color: var(--muted-foreground); }
  .help-body section { display: flex; flex-direction: column; gap: 0.25rem; }
  .help-body p { margin: 0; }
  .help-body .heading { display: flex; align-items: center; gap: 0.375rem; font-weight: 600; color: var(--foreground); }
  .help-body .heading svg { width: 1rem; height: 1rem; }
  .help-body .single svg { color: #0d9488; }
  .help-body .family svg { color: #2563eb; }
  .help-body .example { padding: 0.375rem 0.5rem; font-size: 0.75rem; background: var(--muted); border-radius: 0.25rem; }
  dialog.help { max-width: 480px; }
  dialog.help .modal-head h2 { font-size: 1rem; }
`);

const template = document.createElement('template');
template.innerHTML = `
  <div class="page">
    <fns-split-header></fns-split-header>
    <div class="loading" hidden>${SPINNER}<p>Loading participants...</p></div>
    <div class="content" hidden>
      <div class="settled" hidden>
        <span>This split is settled — participants are read-only.</span>
        <button type="button" class="to-settlement">View settlement</button>
      </div>
      <fns-participant-summary no-title></fns-participant-summary>
      <div class="card add-form" hidden>
        <form novalidate>
          <p class="kind"></p>
          <div class="field">
            <label for="participant-name">Name</label>
            <input id="participant-name" type="text" placeholder="Enter name" aria-describedby="name-error">
            <p id="name-error" class="error" hidden></p>
          </div>
          <div class="pair">
            <div class="field">
              <div class="label-row">
                <label for="participant-nights">Nights</label>
                <button class="info" type="button" data-info="nights" aria-label="Info about Nights">${icon('info')}</button>
              </div>
              <input id="participant-nights" type="number" step="1" min="1" aria-describedby="nights-error">
              <p id="nights-error" class="error" hidden></p>
            </div>
            <div class="field members">
              <div class="label-row">
                <label for="participant-members">Members</label>
                <button class="info" type="button" data-info="members" aria-label="Info about Members">${icon('info')}</button>
              </div>
              <input id="participant-members" type="number" step="any" min="0.5" max="50" aria-describedby="members-error">
              <p id="members-error" class="error" hidden></p>
            </div>
          </div>
          <div class="form-buttons">
            <button class="button primary submit" type="submit">Add</button>
            <button class="button outline cancel" type="button">Cancel</button>
          </div>
        </form>
      </div>
      <div class="add-buttons">
        <button class="button outline single" type="button">${icon('user')} Add Single Participant</button>
        <div class="second">
          <button class="button outline family" type="button">${icon('users')} Add Family Participant</button>
          <button class="button ghost help" type="button" aria-label="Help">${icon('help-circle')}</button>
        </div>
      </div>
      <div class="empty" hidden>
        <p class="title">Add your first participant to get started</p>
        <p class="text">Use the buttons above to add a single person or a family group. You need at least one participant before adding expenses.</p>
      </div>
      <ul></ul>
    </div>
  </div>

  <dialog class="modal sheet help" aria-labelledby="help-title">
    <div class="modal-head">
      <h2 id="help-title">How to add participants</h2>
      <button class="button ghost icon help-close" type="button" aria-label="Close">${icon('x')}</button>
    </div>
    <div class="modal-body help-body">
      <section class="single">
        <p class="heading">${icon('user')} Single Participant</p>
        <p>One person attending the trip on their own. Use this for individuals who travel solo or whose costs should not be shared with anyone else.</p>
        <p class="example">Example: Alice stays for 4 nights → add Alice as a single participant with 4 nights.</p>
      </section>
      <section class="family">
        <p class="heading">${icon('users')} Family Participant</p>
        <p>A group (couple, family) whose costs are pooled together. Set <strong>Members</strong> to the total head-count — adults count as 1, children can count as 0.5.</p>
        <p class="example">Example: The Smiths (2 adults + 2 kids) stay 7 nights → add them as a family with 7 nights and 3 members (2 × 1 + 2 × 0.5).</p>
      </section>
      <section>
        <p class="heading">Mixed scenario</p>
        <p>If one parent leaves early, add them separately as a single participant with fewer nights. The rest of the family keeps the full stay.</p>
        <p class="example">Example: Dad leaves after 3 nights → add Dad (single, 3 nights) + the rest of the family (family, 7 nights, 2.5 members).</p>
      </section>
    </div>
    <div class="modal-foot"><button class="button primary help-ok" type="button">Got it</button></div>
  </dialog>

  <fns-info-dialog></fns-info-dialog>
  <fns-confirm-dialog></fns-confirm-dialog>
  <fns-expense-dialog></fns-expense-dialog>
  <fns-participant-dialog></fns-participant-dialog>
  <fns-participant-expenses-dialog></fns-participant-expenses-dialog>
`;

/**
 * The participants of a split (`/splits/<id>/participants`): add single persons or families, and one card per
 * participant with their nights, shares, expenses and balance. Until the split has a participant, the header
 * keeps the person on this page. Attribute: `split-id`.
 */
class ParticipantsPage extends HTMLElement {
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
    this.list = $('ul');
    this.addForm = $('.add-form');
    this.addButtons = $('.add-buttons');
    this.fields = {
      name: { input: $('#participant-name'), error: $('#name-error') },
      nights: { input: $('#participant-nights'), error: $('#nights-error') },
      members: { input: $('#participant-members'), error: $('#members-error') },
    };
    this.submit = $('.add-form .submit');
    this.info = $('fns-info-dialog');
    this.confirm = $('fns-confirm-dialog');
    this.expenseDialog = $('fns-expense-dialog');
    this.participantDialog = $('fns-participant-dialog');
    this.expensesDialog = $('fns-participant-expenses-dialog');
    this.help = $('dialog.help');

    $('.single').addEventListener('click', () => this.showForm('single'));
    $('.family').addEventListener('click', () => this.showForm('family'));
    $('.add-form .cancel').addEventListener('click', () => this.hideForm());
    $('.add-form form').addEventListener('submit', (event) => {
      event.preventDefault();
      this.addParticipant();
    });
    for (const field of Object.values(this.fields)) {
      field.input.addEventListener('input', () => this.validate(false));
    }
    this.fields.members.input.addEventListener('keydown', (event) => this.stepMembers(event));
    for (const button of root.querySelectorAll('.add-form .info')) {
      button.addEventListener('click', () => {
        const info = FIELD_INFO[button.dataset.info];
        this.info.show(info.title, info.text);
      });
    }
    $('.help').addEventListener('click', () => this.help.showModal());
    $('.help-close').addEventListener('click', () => this.help.close());
    $('.help-ok').addEventListener('click', () => this.help.close());
    this.help.addEventListener('click', (event) => {
      if (event.target === this.help) this.help.close();
    });
    $('.to-settlement').addEventListener('click', () => navigate(`/splits/${this.splitId}/settlement`));
    this.expenseDialog.addEventListener('expensechange', () => this.load());
    this.participantDialog.addEventListener('participantchange', async (event) => {
      await this.load();
      if (!event.detail.deleted) this.highlight(event.detail.id);
    });
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
      this.hideForm();
      this.load();
    }
  }

  async load() {
    const id = this.splitId;
    if (!id) return;
    this.loadedId = id;
    this.header.setAttribute('split-id', id);
    this.header.setAttribute('split-name', getCachedSplitName(id));
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
      showToast({ type: 'error', message: error.detail || 'Failed to load participants' });
    } finally {
      clearTimeout(timer);
      this.loading.hidden = true;
    }
  }

  get settled() {
    return this.split?.settlement != null;
  }

  render() {
    const split = this.split;
    this.header.setAttribute('split-name', split.name);
    // No participant yet: the header keeps the person here until they add one.
    if (split.participants.length === 0) this.header.setAttribute('require-participant', '');
    else this.header.removeAttribute('require-participant');
    this.$('.settled').hidden = !this.settled;
    this.$('fns-participant-summary').participants = split.participants;
    if (this.settled) this.hideForm();
    this.updateAddArea();
    const sorted = [...split.participants].sort((a, b) => a.name.localeCompare(b.name));
    this.list.replaceChildren(...sorted.map((participant) => this.renderParticipant(participant)));
    this.content.hidden = false;
  }

  /** The add buttons, the form or nothing (settled), and the empty state. */
  updateAddArea() {
    const formOpen = !!this.mode;
    this.addForm.hidden = !formOpen;
    this.addButtons.hidden = formOpen || this.settled;
    this.$('.empty').hidden = !(this.split && this.split.participants.length === 0 && !formOpen);
  }

  /** @param {'single' | 'family'} mode */
  showForm(mode) {
    this.mode = mode;
    this.$('.add-form .kind').textContent = mode === 'family' ? 'New Family' : 'New Participant';
    this.$('.add-form .members').hidden = mode !== 'family';
    this.fields.name.input.value = '';
    this.fields.nights.input.value = loadNightsDefault();
    this.fields.members.input.value = 1;
    this.validate(false);
    this.updateAddArea();
    this.fields.name.input.focus();
  }

  hideForm() {
    this.mode = null;
    for (const field of Object.values(this.fields)) showError(field, null);
    this.updateAddArea();
  }

  /**
   * @param {boolean} submitting while typing, an empty name is not an error yet (frontend rule)
   * @returns {boolean} whether the form is valid
   */
  validate(submitting) {
    const name = this.fields.name.input.value.trim();
    const nights = Number(this.fields.nights.input.value);
    const members = Number(this.fields.members.input.value);
    const errors = {};
    if (!name) errors.name = submitting ? 'Name is required' : null;
    else if (name.length > MAX_NAME) errors.name = `Name cannot exceed ${MAX_NAME} characters`;
    else if (this.split?.participants.some((p) => p.name.toLowerCase() === name.toLowerCase())) {
      errors.name = 'A participant with this name already exists';
    }
    if (this.fields.nights.input.value === '' || nights < 1) errors.nights = 'Nights must be at least 1';
    else if (nights > 365) errors.nights = 'Nights cannot exceed 365';
    if (this.mode === 'family') {
      if (this.fields.members.input.value === '' || members < 0.5) errors.members = 'Must be at least 0.5';
      else if (members > 50) errors.members = 'Cannot exceed 50';
    }
    for (const [key, field] of Object.entries(this.fields)) showError(field, errors[key]);
    return !name ? false : !Object.values(errors).some(Boolean);
  }

  /** Arrow keys move the members by 0.5, between 0.5 and 50 (typing stays free, e.g. 2.25). */
  stepMembers(event) {
    if (event.key !== 'ArrowUp' && event.key !== 'ArrowDown') return;
    event.preventDefault();
    const input = this.fields.members.input;
    const current = Number(input.value) || 0;
    const next = event.key === 'ArrowUp' ? Math.min(50, current + 0.5) : Math.max(0.5, current - 0.5);
    input.value = Math.round(next * 100) / 100;
    this.validate(false);
  }

  async addParticipant() {
    if (this.submitting || !this.validate(true)) return;
    const name = this.fields.name.input.value.trim();
    const nights = Number(this.fields.nights.input.value);
    const share = this.mode === 'family' ? Number(this.fields.members.input.value) : 1;
    this.setSubmitting(true);
    try {
      await apiRequest(`/splits/${encodeURIComponent(this.splitId)}/participants`, {
        method: 'POST', body: JSON.stringify({ name, nights, share }),
      });
      saveNightsDefault(nights);
      this.hideForm();
      await this.load();
      showToast({
        type: 'success', message: `${name} successfully added`, description: `${nights} nights · share ${share}`, duration: 4000,
      });
    } catch (error) {
      showToast({ type: 'error', message: error.detail || 'Failed to add participant. Please try again.' });
    } finally {
      this.setSubmitting(false);
    }
  }

  setSubmitting(submitting) {
    this.submitting = submitting;
    for (const control of this.$('.add-form form').elements) control.disabled = submitting;
    this.submit.textContent = submitting ? 'Adding...' : 'Add';
  }

  renderParticipant(participant) {
    const split = this.split;
    const spent = split.expenses.filter((e) => e.payerId === participant.id).reduce((sum, e) => sum + e.amount, 0);
    const cost = split.expenses.reduce(
      (sum, e) => sum + (e.shares.find((s) => s.participantId === participant.id)?.amount || 0), 0);
    const expenseCount = split.expenses.filter((e) => e.payerId === participant.id).length;
    const balance = spent - cost;
    const trend = balance > 0.005 ? 'up' : balance < -0.005 ? 'down' : 'even';
    const partners = participant.sharedAccountId
      ? split.participants
        .filter((p) => p.id !== participant.id && p.sharedAccountId === participant.sharedAccountId)
        .map((p) => p.name).sort()
      : [];

    const item = document.createElement('li');
    item.className = 'card participant';
    item.dataset.id = participant.id;
    item.innerHTML = `
      <div class="inner">
        <div class="top">
          <span class="name"></span>
          <div class="actions">
            <button type="button" class="button ghost icon expenses">${icon('list')}</button>
            <button type="button" class="button ghost icon add-expense">${icon('plus')}</button>
            <button type="button" class="button ghost icon edit">${solidIcon('pencil-solid')}</button>
            <button type="button" class="button ghost icon delete">${solidIcon('trash-solid')}</button>
          </div>
        </div>
        <div class="badges">
          <span class="badge nights"></span>
          <span class="badge shares"></span>
          <span class="badge count"></span>
          <span class="badge partners">${icon('users')}<span></span></span>
        </div>
        <div class="stats">
          <span class="stat" title="Spent">${icon('wallet')}<span class="spent"></span></span>
          <span class="stat" title="Cost">${icon('receipt')}<span class="cost"></span></span>
          <span class="stat ${trend}" title="Balance">${icon({ up: 'trending-up', down: 'trending-down', even: 'minus' }[trend])}<span class="balance"></span></span>
        </div>
      </div>`;
    const $ = (selector) => item.querySelector(selector);
    $('.name').textContent = formatName(participant.name);
    $('.nights').textContent = `${participant.nights} ${participant.nights <= 1 ? 'night' : 'nights'}`;
    $('.shares').textContent = `${participant.share} shares`;
    $('.shares').hidden = !(participant.share > 1);
    $('.count').textContent = `${expenseCount} ${expenseCount === 1 ? 'expense' : 'expenses'}`;
    $('.partners span').textContent = partners.join(' & ');
    $('.partners').hidden = !partners.length;
    $('.spent').textContent = euros.format(spent);
    $('.cost').textContent = euros.format(cost);
    $('.balance').textContent = trend === 'even' ? 'Settled' : euros.format(Math.abs(balance));

    const buttons = {
      expenses: [`View expenses for ${participant.name}`, () => this.expensesDialog.show({ participant, expenses: split.expenses })],
      'add-expense': [`Add expense for ${participant.name}`, () => this.expenseDialog.openAdd({
        splitId: this.splitId, participants: split.participants, payerId: participant.id,
      })],
      edit: [`Edit ${participant.name}`, () => this.participantDialog.openEdit({
        splitId: this.splitId, participant, participants: split.participants, hasExpenses: expenseCount > 0,
      })],
      delete: [`Delete ${participant.name}`, () => this.deleteParticipant(participant)],
    };
    for (const [name, [label, action]] of Object.entries(buttons)) {
      const button = $(`.${name}`);
      if (this.settled && name !== 'expenses') {
        button.remove();
        continue;
      }
      button.setAttribute('aria-label', label);
      button.addEventListener('click', action);
    }
    return item;
  }

  /** Marks a just-edited participant's card for a moment, so the person sees what changed. */
  highlight(id) {
    const card = [...this.list.children].find((item) => item.dataset.id === id);
    if (!card) return;
    card.classList.add('highlight');
    setTimeout(() => card.classList.remove('highlight'), HIGHLIGHT_TIME);
  }

  async deleteParticipant(participant) {
    // A refusal (e.g. they have expenses) closes the window too: retrying would not help.
    await this.confirm.ask({
      title: `Remove ${participant.name}?`,
      description: 'This cannot be undone.',
      confirmLabel: 'Remove',
      onConfirm: async () => {
        try {
          await apiRequest(`/splits/${encodeURIComponent(this.splitId)}/participants/${participant.id}`, {
            method: 'DELETE',
          });
        } catch (error) {
          showToast({
            type: 'error',
            message: error.status === 409
              ? `Cannot remove ${participant.name} - they have associated expenses. Remove or reassign their expenses first.`
              : error.detail || 'Failed to remove participant. Please try again.',
          });
          return;
        }
        await this.load();
        showToast({ type: 'success', message: 'Participant removed successfully', description: participant.name });
      },
    });
  }
}

/** Shows or clears a field's error message, and marks the field invalid for screen readers. */
function showError(field, message) {
  field.error.textContent = message ?? '';
  field.error.hidden = !message;
  if (message) field.input.setAttribute('aria-invalid', 'true');
  else field.input.removeAttribute('aria-invalid');
}

/** "aLICE" → "Alice". */
function formatName(name) {
  return name ? name.charAt(0).toUpperCase() + name.slice(1).toLowerCase() : name;
}

customElements.define('fns-participants-page', ParticipantsPage);
