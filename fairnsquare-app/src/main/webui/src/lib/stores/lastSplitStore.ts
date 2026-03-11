const LAST_SPLIT_KEY = 'fairnsquare_lastSplit';

export interface LastSplit {
  id: string;
  name: string;
}

export function saveLastSplit(split: LastSplit): void {
  localStorage.setItem(LAST_SPLIT_KEY, JSON.stringify(split));
}

export function loadLastSplit(): LastSplit | null {
  const stored = localStorage.getItem(LAST_SPLIT_KEY);
  if (!stored) return null;
  try {
    return JSON.parse(stored) as LastSplit;
  } catch {
    return null;
  }
}

export function clearLastSplit(): void {
  localStorage.removeItem(LAST_SPLIT_KEY);
}
