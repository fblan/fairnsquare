import { apiRequest } from '../infra/api.js';
import { saveToken } from '../infra/stores/captcha-token.js';
import { icon } from './icons.js';

const template = document.createElement('template');
template.innerHTML = `
  <style>
    [hidden] { display: none !important; }
    dialog {
      box-sizing: border-box; width: calc(100% - 2rem); max-width: 460px; padding: 0;
      color: var(--foreground); background: var(--background); border: 0; border-radius: var(--radius);
      box-shadow: 0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1);
    }
    dialog::backdrop { background: rgb(0 0 0 / 0.5); }
    .head { padding: 1.5rem 1.5rem 0.75rem; }
    h2 { margin: 0; font-size: 1.125rem; line-height: 1.75rem; font-weight: 600; }
    .hint { margin: 0.25rem 0 0; font-size: 0.875rem; color: var(--muted-foreground); }
    .challenge { display: flex; flex-direction: column; align-items: center; gap: 0.75rem; padding: 0 1.5rem 0.5rem; }
    .loading {
      box-sizing: border-box; display: flex; align-items: center; justify-content: center; width: 100%; height: 160px;
      color: var(--muted-foreground); background: var(--muted); border: 1px solid var(--border); border-radius: 0.25rem;
    }
    .spinner { width: 1.5rem; height: 1.5rem; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
    img {
      box-sizing: border-box; width: 100%; max-width: 400px; cursor: pointer; user-select: none;
      border: 1px solid var(--border); border-radius: 0.25rem;
    }
    .error { margin: 0; font-size: 0.875rem; color: var(--destructive); }
    .foot { display: flex; flex-direction: column; align-items: center; gap: 0.25rem; padding: 0.5rem 1.5rem 1.5rem; }
    .refresh {
      display: inline-flex; align-items: center; gap: 0.5rem; min-height: 44px; padding: 0 0.75rem;
      font: inherit; font-size: 0.875rem; font-weight: 500; color: inherit; background: none; border: 0;
      border-radius: calc(var(--radius) - 2px); cursor: pointer;
    }
    .refresh:hover { color: var(--accent-foreground); background: var(--accent); }
    .refresh:focus-visible { outline: none; box-shadow: 0 0 0 3px color-mix(in oklch, var(--ring) 50%, transparent); }
    .refresh:disabled { pointer-events: none; opacity: 0.5; }
    .refresh svg { width: 1rem; height: 1rem; }
    .key { margin: 0; font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; font-size: 0.75rem; color: var(--muted-foreground); }
  </style>
  <dialog aria-labelledby="captcha-title">
    <div class="head">
      <h2 id="captcha-title">Human Verification</h2>
      <p class="hint">Click the box showing the correct answer to continue.</p>
    </div>
    <div class="challenge">
      <div class="loading" aria-label="Loading challenge">
        <svg class="spinner" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" opacity="0.25"></circle>
          <path fill="currentColor" opacity="0.75" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
        </svg>
      </div>
      <img alt="Math addition question — click the correct answer" draggable="false" hidden>
      <p class="error" role="alert" hidden></p>
    </div>
    <div class="foot">
      <button class="refresh" type="button">${icon('refresh-cw')} New challenge</button>
      <p class="key" hidden></p>
    </div>
  </dialog>
`;

/**
 * The human verification: a math addition drawn by the server; clicking the box with the right answer gives a
 * signed token (stored by infra/stores/captcha-token.js) that creating a split needs.
 *
 * `open()` shows it and resolves once solved. It cannot be dismissed without solving it.
 */
class CaptchaDialog extends HTMLElement {
  constructor() {
    super();
    this.attachShadow({ mode: 'open' }).append(template.content.cloneNode(true));
    const $ = (selector) => this.shadowRoot.querySelector(selector);
    this.dialog = $('dialog');
    this.loading = $('.loading');
    this.image = $('img');
    this.error = $('.error');
    this.refresh = $('.refresh');
    this.key = $('.key');

    // Escape would close a modal <dialog>: the challenge must be solved.
    this.dialog.addEventListener('cancel', (event) => event.preventDefault());
    this.image.addEventListener('click', (event) => this.answer(event));
    this.refresh.addEventListener('click', () => this.loadChallenge());
  }

  /** @returns {Promise<void>} resolved when the challenge is solved */
  open() {
    if (!this.dialog.open) {
      this.dialog.showModal();
      this.showError(null);
      this.loadChallenge();
    }
    return new Promise((resolve) => {
      this.resolve = resolve;
    });
  }

  async loadChallenge() {
    this.challengeToken = null;
    this.setLoading(true);
    try {
      const challenge = await apiRequest('/captcha/challenges', { method: 'POST' });
      this.challengeToken = challenge.challengeToken;
      this.image.src = `data:image/png;base64,${challenge.imageBase64}`;
      this.key.textContent = `key: ${challenge.secretFingerprint}`;
      this.key.hidden = !challenge.secretFingerprint;
      this.showError(null);
    } catch {
      this.showError('Failed to load the verification challenge. Please try again.');
    } finally {
      this.setLoading(false);
    }
  }

  /** @param {MouseEvent} event a click on the image */
  async answer(event) {
    if (!this.challengeToken || this.busy) return;
    // From CSS pixels to the image's own pixels.
    const rect = this.image.getBoundingClientRect();
    const x = Math.round((event.clientX - rect.left) * (this.image.naturalWidth / rect.width));
    const y = Math.round((event.clientY - rect.top) * (this.image.naturalHeight / rect.height));
    this.busy = true;
    try {
      const { token } = await apiRequest('/captcha/challenges/verify', {
        method: 'POST',
        body: JSON.stringify({ challengeToken: this.challengeToken, x, y }),
      });
      saveToken(token);
      this.challengeToken = null;
      this.dialog.close();
      this.resolve?.();
    } catch {
      // Shown while the next challenge loads, and kept with it so the person knows why it changed
      // (unless loading it failed, which shows its own message).
      this.showError('Wrong answer — try again!');
      await this.loadChallenge();
      if (this.challengeToken) this.showError('Wrong answer — try again!');
    } finally {
      this.busy = false;
    }
  }

  setLoading(loading) {
    this.loading.hidden = !loading;
    this.image.hidden = loading || !this.challengeToken;
    this.refresh.disabled = loading;
  }

  /** @param {string | null} message */
  showError(message) {
    this.error.textContent = message ?? '';
    this.error.hidden = !message;
  }
}

customElements.define('fns-captcha-dialog', CaptchaDialog);
