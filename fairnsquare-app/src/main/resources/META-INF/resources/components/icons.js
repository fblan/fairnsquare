/**
 * Inline SVG icons (from Lucide, ISC licence), drawn with the current text colour.
 * Usage in a template: `${icon('refresh-cw')}`; size them with CSS (`svg { width: 1rem; height: 1rem; }`).
 */

const PATHS = {
  'refresh-cw': [
    'M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8',
    'M21 3v5h-5',
    'M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16',
    'M8 16H3v5',
  ],
};

/**
 * @param {keyof PATHS} name
 * @returns {string} the SVG markup, hidden from screen readers (the button or text around it says what it is)
 */
export function icon(name) {
  const paths = PATHS[name].map((d) => `<path d="${d}"></path>`).join('');
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor"
    stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths}</svg>`;
}
