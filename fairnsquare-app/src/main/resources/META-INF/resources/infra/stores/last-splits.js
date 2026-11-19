/**
 * The splits this browser opened last, newest first, so the home page can offer to resume them.
 * Same localStorage key as the former lib/stores/lastSplitStore.ts.
 *
 * @typedef {{id: string, name: string}} LastSplit
 */

const LAST_SPLITS_KEY = 'fairnsquare_lastSplits';
const MAX_SPLITS = 10;

/** @param {LastSplit} split */
export function saveLastSplit(split) {
  const others = loadLastSplits().filter((s) => s.id !== split.id);
  localStorage.setItem(LAST_SPLITS_KEY, JSON.stringify([split, ...others].slice(0, MAX_SPLITS)));
}

/** @returns {LastSplit[]} */
export function loadLastSplits() {
  try {
    const parsed = JSON.parse(localStorage.getItem(LAST_SPLITS_KEY) ?? '[]');
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

/** @param {string} id */
export function removeLastSplit(id) {
  localStorage.setItem(LAST_SPLITS_KEY, JSON.stringify(loadLastSplits().filter((s) => s.id !== id)));
}
