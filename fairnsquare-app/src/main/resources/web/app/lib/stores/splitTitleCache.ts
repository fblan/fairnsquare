/**
 * The names of the splits opened in this tab, so a split page can show its title while it loads instead of
 * a blank header. Kept in sessionStorage, shared with the web components (infra/stores/split-names.js).
 */
const SPLIT_NAMES_KEY = 'fairnsquare_splitNames';

function load(): Record<string, string> {
  try {
    return JSON.parse(sessionStorage.getItem(SPLIT_NAMES_KEY) ?? '{}') ?? {};
  } catch {
    return {};
  }
}

export function getCachedSplitName(id: string): string {
  return load()[id] ?? '';
}

export function setCachedSplitName(id: string, name: string): void {
  sessionStorage.setItem(SPLIT_NAMES_KEY, JSON.stringify({ ...load(), [id]: name }));
}
