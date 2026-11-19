/**
 * Styles shared by the components: cards, buttons, form fields. Each component adopts this sheet in its
 * Shadow DOM, then adds its own styles:
 *
 *   this.attachShadow({ mode: 'open' }).adoptedStyleSheets = [baseStyles];
 *
 * Colours come from the theme variables (infra/theme.css).
 */
export const baseStyles = new CSSStyleSheet();
baseStyles.replaceSync(`
  :host { display: block; }
  [hidden] { display: none !important; }

  .card {
    box-sizing: border-box; width: 100%; padding: 1.5rem;
    color: var(--card-foreground); background: var(--card);
    border: 1px solid var(--border); border-radius: 0.75rem;
    box-shadow: 0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1);
  }
  .card-title { margin: 0; font-size: 1rem; line-height: 1; font-weight: 600; }

  .button {
    box-sizing: border-box; display: inline-flex; align-items: center; justify-content: center; gap: 0.5rem;
    min-height: 44px; padding: 0.5rem 1rem; font: inherit; font-size: 0.875rem; font-weight: 500;
    white-space: nowrap; color: inherit; background: none; border: 0;
    border-radius: calc(var(--radius) - 2px); cursor: pointer;
  }
  .button:focus-visible { outline: none; box-shadow: 0 0 0 3px color-mix(in oklch, var(--ring) 50%, transparent); }
  .button:disabled { pointer-events: none; opacity: 0.5; }
  .button svg { flex: none; width: 1rem; height: 1rem; }
  .button.primary {
    color: var(--primary-foreground); background: var(--primary); box-shadow: 0 1px 2px 0 rgb(0 0 0 / 0.05);
  }
  .button.primary:hover { background: color-mix(in oklch, var(--primary) 90%, transparent); }
  .button.outline {
    background: var(--background); border: 1px solid var(--border); box-shadow: 0 1px 2px 0 rgb(0 0 0 / 0.05);
  }
  .button.outline:hover, .button.ghost:hover { color: var(--accent-foreground); background: var(--accent); }
  .button.small { min-height: 36px; padding: 0.25rem 0.75rem; }
  .button.wide { width: 100%; }

  .field { display: flex; flex-direction: column; gap: 0.5rem; }
  label { font-size: 0.875rem; line-height: 1; font-weight: 500; }
  input {
    box-sizing: border-box; width: 100%; min-height: 44px; padding: 0.25rem 0.75rem;
    font: inherit; font-size: 1rem; color: inherit; background: var(--background);
    border: 1px solid var(--input); border-radius: calc(var(--radius) - 2px);
    box-shadow: 0 1px 2px 0 rgb(0 0 0 / 0.05); outline: none;
  }
  /* 16px on phones: iOS zooms into a field whose text is smaller when it gets focus. */
  @media (min-width: 768px) { input { font-size: 0.875rem; } }
  input:focus-visible {
    border-color: var(--ring); box-shadow: 0 0 0 3px color-mix(in oklch, var(--ring) 50%, transparent);
  }
  input[aria-invalid="true"] { border-color: var(--destructive); }
  input:disabled { cursor: not-allowed; opacity: 0.5; }
  .error { margin: 0; font-size: 0.875rem; color: var(--destructive); }
  .muted { color: var(--muted-foreground); }

  .spinner { width: 1rem; height: 1rem; animation: spin 1s linear infinite; }
  @keyframes spin { to { transform: rotate(360deg); } }
`);

/** A spinning circle, for buttons and loading states. Size it with the `.spinner` class. */
export const SPINNER = `<svg class="spinner" viewBox="0 0 24 24" fill="none" aria-hidden="true">
  <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" opacity="0.25"></circle>
  <path fill="currentColor" opacity="0.75" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
</svg>`;
