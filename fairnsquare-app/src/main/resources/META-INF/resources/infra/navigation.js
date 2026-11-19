/**
 * Moves to another page of the app without reloading it.
 *
 * Pushes the new address to the browser history, then fires `popstate`: the router (sv-router while the
 * Svelte shell remains, the plain router afterwards) listens to it and shows the page for the address.
 *
 * @param {string} path the address, e.g. `/splits/abc/participants`
 * @param {{replace?: boolean}} [options] replace the current history entry instead of adding one
 */
export function navigate(path, options = {}) {
  if (options.replace) {
    history.replaceState({}, '', path);
  } else {
    history.pushState({}, '', path);
  }
  window.dispatchEvent(new PopStateEvent('popstate', { state: {} }));
}
