const styles = new CSSStyleSheet();
styles.replaceSync(`
  :host { display: block; }
  [hidden] { display: none !important; }
  .card {
    box-sizing: border-box; width: 100%; padding: 2.5rem 1.5rem;
    color: var(--card-foreground); background: rgb(240 253 250 / 0.5);
    border: 1px solid #99f6e4; border-radius: 0.75rem;
    box-shadow: 0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1);
  }
  p { margin: 0; }
  .caption { font-size: 0.875rem; color: var(--muted-foreground); }
  .count { font-size: 1.125rem; line-height: 1.75rem; font-weight: 600; }
  .names { margin-top: 0.25rem; font-size: 0.875rem; color: var(--muted-foreground); }
  :host([no-title]) .names { margin-top: 0; }
`);

const template = document.createElement('template');
template.innerHTML = `
  <div class="card">
    <div class="title">
      <p class="caption">Participants</p>
      <p class="count"></p>
    </div>
    <p class="names"></p>
  </div>
`;

/**
 * The participants of a split in one card: how many, then "Alice (2n), Bob (3n, ×2)" by name.
 *
 * Set the `participants` property (`{name, nights, share}[]`). With the `no-title` attribute, only the names
 * show, and nothing at all while there is no participant.
 */
class ParticipantSummary extends HTMLElement {
  constructor() {
    super();
    const root = this.attachShadow({ mode: 'open' });
    root.adoptedStyleSheets = [styles];
    root.append(template.content.cloneNode(true));
    this.card = root.querySelector('.card');
    this.titleBlock = root.querySelector('.title');
    this.count = root.querySelector('.count');
    this.names = root.querySelector('.names');
    this.list = [];
  }

  connectedCallback() {
    // A value set before this element was defined is an own property hiding the setter: replay it.
    if (Object.hasOwn(this, 'participants')) {
      const value = this.participants;
      delete this.participants;
      this.participants = value;
    }
    this.render();
  }

  get participants() {
    return this.list;
  }

  /** @param {Array<{name: string, nights: number, share: number}>} participants */
  set participants(participants) {
    this.list = participants ?? [];
    this.render();
  }

  render() {
    const sorted = [...this.list].sort((a, b) => a.name.localeCompare(b.name));
    const showTitle = !this.hasAttribute('no-title');
    this.titleBlock.hidden = !showTitle;
    this.count.textContent = `${this.list.length} ${this.list.length === 1 ? 'participant' : 'participants'}`;
    this.names.textContent = sorted
      .map((p) => `${formatName(p.name)} (${p.nights}n${p.share > 1 ? `, ×${p.share}` : ''})`)
      .join(', ');
    this.names.hidden = !sorted.length;
    this.card.hidden = !showTitle && !sorted.length;
  }
}

/** "aLICE" → "Alice", as on the participants page. */
function formatName(name) {
  return name ? name.charAt(0).toUpperCase() + name.slice(1).toLowerCase() : name;
}

customElements.define('fns-participant-summary', ParticipantSummary);
