/**
 * Toast notifications for the Svelte pages.
 *
 * Toasts are shown by the <fns-toaster> web component (META-INF/resources/components/toaster.js), which listens
 * to `fns-toast` events on window — the same events the web component pages send (infra/toasts.js).
 */

export interface Toast {
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  description?: string;
  duration?: number;
}

export function addToast(toast: Toast): void {
  window.dispatchEvent(new CustomEvent('fns-toast', { detail: toast }));
}
