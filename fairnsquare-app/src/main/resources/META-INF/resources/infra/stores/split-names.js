/**
 * The names of the splits opened in this tab, so a split page can show its title while it loads instead of
 * a blank header. Kept in sessionStorage, which the Svelte pages (lib/stores/splitTitleCache.ts) share.
 */

const SPLIT_NAMES_KEY = 'fairnsquare_splitNames';

/** @returns {Record<string, string>} */
function load() {
  try {
    return JSON.parse(sessionStorage.getItem(SPLIT_NAMES_KEY) ?? '{}') ?? {};
  } catch {
    return {};
  }
}

/** @param {string} id @returns {string} the cached name, or '' */
export function getCachedSplitName(id) {
  return load()[id] ?? '';
}

/** @param {string} id @param {string} name */
export function setCachedSplitName(id, name) {
  sessionStorage.setItem(SPLIT_NAMES_KEY, JSON.stringify({ ...load(), [id]: name }));
}
