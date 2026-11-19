/**
 * Shows a short notification at the bottom of the screen (see components/toaster.js).
 *
 * Toasts travel as a `fns-toast` event on `window`, so any page or window can show one.
 *
 * @param {{type?: 'success' | 'error' | 'warning' | 'info', message: string, description?: string,
 *   duration?: number}} toast `duration` in milliseconds, 5000 by default
 */
export function showToast(toast) {
  window.dispatchEvent(new CustomEvent('fns-toast', { detail: toast }));
}
