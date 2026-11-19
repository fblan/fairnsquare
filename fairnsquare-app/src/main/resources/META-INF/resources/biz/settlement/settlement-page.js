import { apiRequest } from '../../infra/api.js';
import { navigate } from '../../infra/navigation.js';
import { showToast } from '../../infra/toasts.js';
import { getCachedSplitName, setCachedSplitName } from '../../infra/stores/split-names.js';
import { baseStyles, SPINNER } from '../../components/base-styles.js';
import '../../components/split-header.js';

const LOADING_DELAY = 500;
const euros = new Intl.NumberFormat('en-IE', { style: 'currency', currency: 'EUR' });

const styles = new CSSStyleSheet();
styles.replaceSync(`
  .page { display: flex; flex-direction: column; align-items: center; gap: 1rem; width: 100%; max-width: 520px; margin: 0 auto; }
  .page > *, .content > * { width: 100%; }
  .loading { display: flex; flex-direction: column; align-items: center; gap: 1rem; padding: 3rem 0; }
  .loading .spinner { width: 2rem; height: 2rem; color: var(--primary); }
  .loading p { margin: 0; color: var(--muted-foreground); }
  .content { display: flex; flex-direction: column; gap: 1rem; }
  .actions { display: flex; flex-direction: column; gap: 1rem; }
  .actions .button { width: 100%; }
  .actions .unsettle { color: var(--muted-foreground); }
  .none { margin: 0; padding: 1rem 0; text-align: center; color: var(--muted-foreground); }
  ul { display: flex; flex-direction: column; gap: 0.75rem; margin: 0; padding: 0; list-style: none; }
  .entry { padding: 2.5rem 1.5rem; }
  .top { display: flex; align-items: flex-start; justify-content: space-between; gap: 0.5rem; }
  .who { flex: 1; min-width: 0; }
  .name { font-size: 1.125rem; line-height: 1.75rem; font-weight: 600; overflow-wrap: anywhere; }
  .totals { display: flex; flex-wrap: wrap; gap: 0.25rem 1rem; margin-top: 0.25rem; font-size: 0.875rem; color: var(--muted-foreground); }
  .totals span span { font-weight: 500; color: var(--foreground); }
  .balance { flex: none; font-size: 0.875rem; font-weight: 500; text-align: right; }
  .up { color: #16a34a; }
  .down { color: #dc2626; }
  .even { color: var(--muted-foreground); }
  .group-actions { display: flex; gap: 0.75rem; margin-top: 0.5rem; }
  .link {
    padding: 0; font: inherit; font-size: 0.75rem; color: var(--muted-foreground);
    background: none; border: 0; text-decoration: underline; cursor: pointer;
  }
  .link:hover { color: var(--foreground); }
  .link:disabled { cursor: not-allowed; opacity: 0.5; }
  .transfers { display: flex; flex-direction: column; gap: 0.25rem; margin-top: 0.75rem; padding-top: 0.75rem; border-top: 1px solid var(--border); }
  .transfers p { margin: 0; font-size: 0.875rem; }
  dialog.group { max-width: 24rem; padding: 1.5rem; }
  dialog.group h2 { margin: 0 0 1rem; font-size: 1.125rem; line-height: 1.75rem; font-weight: 600; overflow-wrap: anywhere; }
  dialog.group label { display: block; margin-bottom: 0.25rem; font-size: 0.875rem; color: var(--muted-foreground); }
  dialog.group select { margin-bottom: 1.5rem; }
  dialog.group .buttons { display: flex; justify-content: flex-end; gap: 0.75rem; }
`);

const template = document.createElement('template');
template.innerHTML = `
  <div class="page">
    <fns-split-header></fns-split-header>
    <div class="loading" hidden>${SPINNER}<p>Loading settlement...</p></div>
    <div class="content" hidden>
      <div class="actions">
        <button class="button primary resolve" type="button">Resolve</button>
        <button class="button outline export" type="button">Export Settlement</button>
        <button class="button ghost unsettle" type="button">Unsettle</button>
      </div>
      <p class="none" hidden>No participants</p>
      <ul></ul>
    </div>
  </div>

  <dialog class="modal group" aria-labelledby="group-title">
    <form method="dialog">
      <h2 id="group-title"></h2>
      <label for="partner">Partner</label>
      <select id="partner" autofocus></select>
      <div class="buttons">
        <button class="button ghost cancel" type="button">Cancel</button>
        <button class="button primary ok" type="submit">OK</button>
      </div>
    </form>
  </dialog>
`;

/**
 * The settlement of a split (`/splits/<id>/settlement`): one balance per participant (members of a shared
 * account merged into one), and once resolved who pays whom. Before resolving, participants can be grouped into
 * a shared account. Opening the page never resolves: that saves a settlement and makes the split read-only.
 * Attribute: `split-id`.
 */
class SettlementPage extends HTMLElement {
  static observedAttributes = ['split-id'];

  constructor() {
    super();
    const root = this.attachShadow({ mode: 'open' });
    root.adoptedStyleSheets = [baseStyles, styles];
    root.append(template.content.cloneNode(true));
    const $ = (selector) => root.querySelector(selector);
    this.header = $('fns-split-header');
    this.loading = $('.loading');
    this.content = $('.content');
    this.none = $('.none');
    this.list = $('ul');
    this.resolveButton = $('.resolve');
    this.exportButton = $('.export');
    this.unsettleButton = $('.unsettle');
    this.groupDialog = $('dialog.group');
    this.groupTitle = $('#group-title');
    this.partner = $('#partner');
    this.groupOk = $('.ok');

    this.resolveButton.addEventListener('click', () => this.resolve());
    this.exportButton.addEventListener('click', () => this.exportSettlement());
    this.unsettleButton.addEventListener('click', () => this.unsettle());
    this.partner.addEventListener('change', () => { this.groupOk.disabled = !this.partner.value; });
    $('.cancel').addEventListener('click', () => this.groupDialog.close());
    this.groupDialog.addEventListener('click', (event) => {
      if (event.target === this.groupDialog) this.groupDialog.close();
    });
    this.groupDialog.querySelector('form').addEventListener('submit', () => {
      // method="dialog" closes the window; the change runs behind it, with the cards' buttons disabled.
      if (this.partner.value) this.group(this.groupParticipantId, this.partner.value);
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
      this.content.hidden = true;
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
      // Already resolved: show the saved reimbursements. Never POST here, it would undo an intentional unsettle.
      const reimbursements = split.settlement != null
        ? (await apiRequest(`/splits/${encodeURIComponent(id)}/settlement`)).reimbursements
        : [];
      if (id !== this.splitId) return;
      setCachedSplitName(id, split.name);
      this.split = split;
      this.reimbursements = reimbursements;
      this.render();
    } catch (error) {
      if (error.status === 404 || error.status === 400) {
        showToast({ type: 'info', message: 'Split not found — create a new one.' });
        navigate('/');
        return;
      }
      showToast({ type: 'error', message: error.detail || 'Failed to load settlement' });
    } finally {
      clearTimeout(timer);
      this.loading.hidden = true;
    }
  }

  get settled() {
    return this.split?.settlement != null;
  }

  render() {
    this.header.setAttribute('split-name', this.split.name);
    const entries = this.entries();
    this.none.hidden = entries.length > 0;
    this.resolveButton.hidden = this.settled || !entries.length;
    this.exportButton.hidden = !this.settled || !entries.length;
    this.unsettleButton.hidden = !this.settled || !entries.length;
    this.list.replaceChildren(...entries.map((entry) => this.renderEntry(entry)));
    this.content.hidden = false;
  }

  /**
   * One entry per participant in the split's order; the members of a shared account are merged into one entry
   * ("Alice & Bob"), whose id is the shared account id the reimbursements use.
   */
  entries() {
    const participants = this.split.participants;
    const seen = new Set();
    const entries = [];
    for (const p of participants) {
      if (seen.has(p.id)) continue;
      const members = p.sharedAccountId ? participants.filter((m) => m.sharedAccountId === p.sharedAccountId) : [p];
      members.forEach((m) => seen.add(m.id));
      const sum = (key) => members.reduce((total, m) => total + (m[key] ?? 0), 0);
      entries.push({
        id: p.sharedAccountId || p.id,
        name: groupName(members),
        group: Boolean(p.sharedAccountId),
        members,
        totalPaid: sum('totalPaid'),
        totalCost: sum('totalCost'),
        balance: sum('balance'),
      });
    }
    return entries;
  }

  renderEntry(entry) {
    const item = document.createElement('li');
    item.className = 'card entry';
    item.innerHTML = `
      <div class="top">
        <div class="who">
          <div class="name"></div>
          <div class="totals">
            <span>Paid: <span class="paid"></span></span>
            <span>Cost: <span class="cost"></span></span>
          </div>
        </div>
        <span class="balance"></span>
      </div>`;
    item.querySelector('.name').textContent = entry.name;
    item.querySelector('.paid').textContent = euros.format(entry.totalPaid);
    item.querySelector('.cost').textContent = euros.format(entry.totalCost);
    const balance = item.querySelector('.balance');
    if (entry.balance > 0.005) {
      balance.textContent = `Owed ${euros.format(entry.balance)}`;
      balance.classList.add('up');
    } else if (entry.balance < -0.005) {
      balance.textContent = `Owes ${euros.format(-entry.balance)}`;
      balance.classList.add('down');
    } else {
      balance.textContent = 'Settled';
      balance.classList.add('even');
    }

    if (!this.settled) {
      const actions = document.createElement('div');
      actions.className = 'group-actions';
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'link';
      button.disabled = Boolean(this.busy);
      if (entry.group) {
        button.textContent = 'Ungroup';
        button.addEventListener('click', () => this.ungroup(entry.members));
      } else {
        button.textContent = 'Group with...';
        button.addEventListener('click', () => this.openGroup(entry.members[0]));
      }
      actions.append(button);
      item.append(actions);
    } else {
      const outgoing = this.reimbursements.filter((r) => r.fromId === entry.id);
      const incoming = this.reimbursements.filter((r) => r.toId === entry.id);
      if (outgoing.length || incoming.length) {
        const transfers = document.createElement('div');
        transfers.className = 'transfers';
        const line = (text, className) => {
          const p = document.createElement('p');
          p.className = className;
          p.textContent = text;
          return p;
        };
        transfers.append(
          ...outgoing.map((r) => line(`Pay ${euros.format(r.amount)} to ${r.toName}`, 'down')),
          ...incoming.map((r) => line(`Receive ${euros.format(r.amount)} from ${r.fromName}`, 'up')),
        );
        item.append(transfers);
      }
    }
    return item;
  }

  async resolve() {
    this.resolveButton.disabled = true;
    this.resolveButton.innerHTML = `${SPINNER} Resolving...`;
    try {
      const settlement = await apiRequest(`/splits/${encodeURIComponent(this.splitId)}/settlement`, { method: 'POST' });
      this.split.settlement = settlement;
      this.reimbursements = settlement.reimbursements;
      this.render();
    } catch (error) {
      showToast({ type: 'error', message: error.detail || 'Failed to resolve' });
    } finally {
      this.resolveButton.disabled = false;
      this.resolveButton.textContent = 'Resolve';
    }
  }

  async unsettle() {
    this.unsettleButton.disabled = true;
    this.unsettleButton.textContent = 'Unsettling...';
    try {
      await apiRequest(`/splits/${encodeURIComponent(this.splitId)}/settlement`, { method: 'DELETE' });
      this.split.settlement = null;
      this.reimbursements = [];
      this.render();
    } catch (error) {
      showToast({ type: 'error', message: error.detail || 'Failed to unsettle' });
    } finally {
      this.unsettleButton.disabled = false;
      this.unsettleButton.textContent = 'Unsettle';
    }
  }

  /** Copies a text summary of the settlement; without a clipboard, shows it in a toast. */
  async exportSettlement() {
    const text = settlementText(this.split, this.reimbursements, window.location.href);
    try {
      await navigator.clipboard.writeText(text);
      showToast({ type: 'success', message: 'Settlement copied to clipboard!' });
    } catch {
      showToast({ type: 'info', message: text });
    }
  }

  /** The "Group with" window: the other participants, an existing group shown once (picking it joins it). */
  openGroup(participant) {
    this.groupParticipantId = participant.id;
    this.groupTitle.textContent = `Group ${participant.name} with`;
    const none = new Option('(none)', '');
    const others = this.entries()
      .filter((entry) => !entry.members.some((m) => m.id === participant.id))
      .map((entry) => new Option(entry.name, entry.members[0].id));
    this.partner.replaceChildren(none, ...others);
    this.groupOk.disabled = true;
    this.groupDialog.showModal();
  }

  /** Puts the participant in the partner's shared account, or in a new one with the partner. */
  async group(participantId, partnerId) {
    const participants = this.split.participants;
    const participant = participants.find((p) => p.id === participantId);
    const partner = participants.find((p) => p.id === partnerId);
    if (!participant || !partner) return;
    const accountId = partner.sharedAccountId || newSharedAccountId();
    const members = partner.sharedAccountId ? [participant] : [participant, partner];
    await this.changeAccounts(members, accountId, 'Failed to update group');
  }

  async ungroup(members) {
    await this.changeAccounts(members, null, 'Failed to ungroup');
  }

  async changeAccounts(members, sharedAccountId, failure) {
    this.setBusy(true);
    try {
      for (const member of members) {
        await apiRequest(`/splits/${encodeURIComponent(this.splitId)}/participants/${member.id}`, {
          method: 'PUT',
          body: JSON.stringify({ name: member.name, nights: member.nights, share: member.share, sharedAccountId }),
        });
      }
    } catch (error) {
      showToast({ type: 'error', message: error.detail || failure });
    }
    // Reload either way: part of the change may have been saved. The balances follow the new groups.
    await this.load();
    this.setBusy(false);
  }

  setBusy(busy) {
    this.busy = busy;
    for (const button of this.list.querySelectorAll('.link')) button.disabled = busy;
  }
}

function groupName(members) {
  return members.map((m) => m.name).sort((a, b) => a.localeCompare(b)).join(' & ');
}

/** A 21-character random id, like the server's ids. */
function newSharedAccountId() {
  const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-';
  return Array.from(crypto.getRandomValues(new Uint8Array(21)), (b) => alphabet[b & 63]).join('');
}

/** The summary "Export Settlement" copies: totals, each transfer, then what each person receives. */
export function settlementText(split, reimbursements, url) {
  const total = split.expenses.reduce((sum, e) => sum + e.amount, 0);
  const plural = (count, word) => `${count} ${word}${count !== 1 ? 's' : ''}`;
  const lines = [
    `=== ${split.name} ===`,
    `${plural(split.expenses.length, 'expense')} — Total: ${euros.format(total)}`,
    plural(split.participants.length, 'participant'),
    '',
  ];
  const sorted = [...reimbursements].sort((a, b) => a.fromName.localeCompare(b.fromName));
  if (sorted.length) {
    lines.push('Settlements:', ...sorted.map((r) => `${r.fromName} → ${r.toName}: ${euros.format(r.amount)}`));
    const byReceiver = new Map();
    for (const r of sorted) {
      if (!byReceiver.has(r.toId)) byReceiver.set(r.toId, { name: r.toName, amounts: [] });
      byReceiver.get(r.toId).amounts.push(r.amount);
    }
    lines.push('', 'Summary:');
    for (const { name, amounts } of byReceiver.values()) {
      const sum = amounts.reduce((s, a) => s + a, 0);
      lines.push(amounts.length === 1
        ? `${name} receives ${euros.format(sum)}`
        : `${name} receives ${amounts.map((a) => euros.format(a)).join(' + ')} = ${euros.format(sum)}`);
    }
  } else {
    lines.push('All settled — no transfers needed!');
  }
  lines.push('', url);
  return lines.join('\n');
}

customElements.define('fns-settlement-page', SettlementPage);
