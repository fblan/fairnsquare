import { baseStyles } from '../../components/base-styles.js';
import { icon } from '../../components/icons.js';

const MODE_LABELS = { BY_NIGHT: 'By Night', BY_SHARE: 'By Share', EQUAL: 'Equal', FREE: 'Free', REIMBURSEMENT: 'Reimbursement' };
const euros = new Intl.NumberFormat('en-IE', { style: 'currency', currency: 'EUR' });

const styles = new CSSStyleSheet();
styles.replaceSync(`
  dialog.sheet { max-height: calc(100dvh - 2rem); }
  @media (min-width: 640px) { dialog.sheet { max-width: 520px; } }
  .modal-head h2 { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .modal-body { display: flex; flex-direction: column; gap: 0.75rem; }
  .none { margin: 0; padding: 2rem 0; font-size: 0.875rem; text-align: center; color: var(--muted-foreground); }
  .expense { display: flex; flex-direction: column; gap: 0.5rem; padding: 0.75rem; border: 1px solid var(--border); border-radius: var(--radius); }
  .row { display: flex; align-items: flex-start; justify-content: space-between; gap: 0.5rem; }
  .description { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 500; }
  .amount, .stat span { font-variant-numeric: tabular-nums; }
  .amount { flex: none; font-weight: 500; }
  .badge {
    align-self: flex-start; padding: 0.125rem 0.5rem; font-size: 0.75rem; color: #475569;
    background: #f1f5f9; border-radius: 9999px;
  }
  .stats { display: flex; align-items: center; justify-content: space-between; padding-top: 0.25rem; font-size: 0.875rem; border-top: 1px solid var(--border); }
  .stat { display: flex; align-items: center; gap: 0.25rem; color: var(--muted-foreground); }
  .stat svg { flex: none; width: 0.875rem; height: 0.875rem; }
  .stat span { font-weight: 500; color: var(--foreground); }
  .stat.up, .stat.up span { color: #16a34a; }
  .stat.down, .stat.down span { color: #dc2626; }
  .stat.even span { color: var(--muted-foreground); }
`);

const template = document.createElement('template');
template.innerHTML = `
  <dialog class="modal sheet" aria-labelledby="title">
    <div class="modal-head">
      <h2 id="title"></h2>
      <button class="button ghost icon close" type="button" aria-label="Close">${icon('x')}</button>
    </div>
    <div class="modal-body"></div>
    <div class="modal-foot"><button class="button primary done" type="button">Close</button></div>
  </dialog>
`;

/**
 * The expenses a participant paid or takes a share of, each with what they spent, what they owe and the
 * difference.
 *
 *   dialog.show({ participant, expenses });
 */
class ParticipantExpensesDialog extends HTMLElement {
  constructor() {
    super();
    const root = this.attachShadow({ mode: 'open' });
    root.adoptedStyleSheets = [baseStyles, styles];
    root.append(template.content.cloneNode(true));
    this.dialog = root.querySelector('dialog');
    this.heading = root.querySelector('h2');
    this.body = root.querySelector('.modal-body');
    root.querySelector('.close').addEventListener('click', () => this.dialog.close());
    root.querySelector('.done').addEventListener('click', () => this.dialog.close());
    this.dialog.addEventListener('click', (event) => {
      if (event.target === this.dialog) this.dialog.close();
    });
  }

  /** @param {{participant: {id: string, name: string}, expenses: object[]}} options */
  show({ participant, expenses }) {
    this.heading.textContent = `${participant.name}'s Expenses`;
    const theirs = expenses.filter((e) => e.payerId === participant.id
      || e.shares.some((s) => s.participantId === participant.id && s.amount > 0));
    if (!theirs.length) {
      const none = document.createElement('p');
      none.className = 'none';
      none.textContent = `No expenses found for ${participant.name}.`;
      this.body.replaceChildren(none);
    } else {
      this.body.replaceChildren(...theirs.map((expense) => renderExpense(expense, participant.id)));
    }
    this.dialog.showModal();
  }
}

function renderExpense(expense, participantId) {
  const spent = expense.payerId === participantId ? expense.amount : 0;
  const owed = expense.shares.find((s) => s.participantId === participantId)?.amount ?? 0;
  const balance = spent - owed;
  const trend = balance > 0.005 ? 'up' : balance < -0.005 ? 'down' : 'even';
  const trendIcon = { up: 'trending-up', down: 'trending-down', even: 'minus' }[trend];

  const item = document.createElement('div');
  item.className = 'expense';
  item.innerHTML = `
    <div class="row"><span class="description"></span><span class="amount"></span></div>
    <span class="badge"></span>
    <div class="stats">
      <span class="stat" title="Spent">${icon('wallet')}<span class="spent"></span></span>
      <span class="stat" title="Owned">${icon('receipt')}<span class="owed"></span></span>
      <span class="stat ${trend}" title="Balance">${icon(trendIcon)}<span class="balance"></span></span>
    </div>`;
  item.querySelector('.description').textContent = expense.description;
  item.querySelector('.amount').textContent = euros.format(expense.amount);
  item.querySelector('.badge').textContent = MODE_LABELS[expense.splitMode] ?? expense.splitMode;
  item.querySelector('.spent').textContent = euros.format(spent);
  item.querySelector('.owed').textContent = euros.format(owed);
  item.querySelector('.balance').textContent = euros.format(Math.abs(balance));
  return item;
}

customElements.define('fns-participant-expenses-dialog', ParticipantExpensesDialog);
