/**
 * Session-level cache for split names.
 * Populated when any per-split page loads, read on component mount so the
 * header title is stable during page transitions instead of blinking blank.
 */
const cache = new Map<string, string>();

export function getCachedSplitName(id: string): string {
  return cache.get(id) ?? '';
}

export function setCachedSplitName(id: string, name: string): void {
  cache.set(id, name);
}
