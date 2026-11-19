import { baseStyles } from './base-styles.js';
import { icon } from './icons.js';

const styles = new CSSStyleSheet();
styles.replaceSync(`
  .modal-head h2 { font-size: 1rem; }
  .modal-body p { margin: 0 0 0.75rem; font-size: 0.875rem; line-height: 1.625; color: var(--muted-foreground); }
  .modal-body p:last-child { margin-bottom: 0; }
`);

const template = document.createElement('template');
template.innerHTML = `
  <dialog class="modal sheet" aria-labelledby="info-title">
    <div class="modal-head">
      <h2 id="info-title"></h2>
      <button class="button ghost icon close" type="button" aria-label="Close">${icon('x')}</button>
    </div>
    <div class="modal-body"></div>
    <div class="modal-foot"><button class="button primary ok" type="button">Got it</button></div>
  </dialog>
`;

/**
 * Explains a field or a choice in a bottom sheet (a centred window on larger screens).
 *
 *   infoDialog.show('Nights', ['The number of nights…', 'Example: …']);
 */
class InfoDialog extends HTMLElement {
  constructor() {
    super();
    const root = this.attachShadow({ mode: 'open' });
    root.adoptedStyleSheets = [baseStyles, styles];
    root.append(template.content.cloneNode(true));
    this.dialog = root.querySelector('dialog');
    this.heading = root.querySelector('h2');
    this.body = root.querySelector('.modal-body');
    root.querySelector('.close').addEventListener('click', () => this.dialog.close());
    root.querySelector('.ok').addEventListener('click', () => this.dialog.close());
    this.dialog.addEventListener('click', (event) => {
      if (event.target === this.dialog) this.dialog.close();
    });
  }

  /** @param {string} title @param {string[]} paragraphs */
  show(title, paragraphs) {
    this.heading.textContent = title;
    this.body.replaceChildren(...paragraphs.map((text) => {
      const paragraph = document.createElement('p');
      paragraph.textContent = text;
      return paragraph;
    }));
    this.dialog.showModal();
  }
}

customElements.define('fns-info-dialog', InfoDialog);

/** Field explanations shared by the participant forms. */
export const FIELD_INFO = {
  nights: {
    title: 'Nights',
    text: [
      'The number of nights this participant stays during the trip.',
      'Used in the "By Night" split mode: costs are divided proportionally based on each participant\'s nights × share weight.',
      'Example: a participant staying 3 nights out of a 7-night trip pays for 3/7 of the night-based expenses (adjusted by their share).',
    ],
  },
  members: {
    title: 'Members',
    text: [
      'The number of people in this family group.',
      'Adults count as 1 each. Children can count for 0.5 to reflect their lower consumption.',
      'Example: a family of 2 adults and 2 children staying 7 nights → 3 members (2 × 1 + 2 × 0.5).',
      'If one parent leaves early (e.g. stays only 3 nights), add them as a separate single participant with 3 nights and a share of 1. The rest of the family can then be entered with fewer members.',
    ],
  },
  share: {
    title: 'Share',
    text: [
      'The number of persons this participant represents.',
      'A solo traveller has a share of 1. A couple sharing costs would have a share of 2, a family of 3 would use 3, etc.',
      'Used in "By Night" mode (nights × share) and "By Share" mode (share only) to calculate each participant\'s proportional contribution.',
      'Example: a couple (share 2) staying 3 nights counts as 6 night-shares, compared to 3 night-shares for a solo person staying the same time.',
    ],
  },
};
