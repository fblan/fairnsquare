const LAST_SPLITS_KEY = 'fairnsquare_lastSplits';
const MAX_SPLITS = 10;

export interface LastSplit {
  id: string;
  name: string;
}

export function saveLastSplit(split: LastSplit): void {
  const current = loadLastSplits().filter((s) => s.id !== split.id);
  const updated = [split, ...current].slice(0, MAX_SPLITS);
  localStorage.setItem(LAST_SPLITS_KEY, JSON.stringify(updated));
}

export function loadLastSplits(): LastSplit[] {
  const stored = localStorage.getItem(LAST_SPLITS_KEY);
  if (!stored) return [];
  try {
    const parsed = JSON.parse(stored);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function removeLastSplit(id: string): void {
  const updated = loadLastSplits().filter((s) => s.id !== id);
  localStorage.setItem(LAST_SPLITS_KEY, JSON.stringify(updated));
}
